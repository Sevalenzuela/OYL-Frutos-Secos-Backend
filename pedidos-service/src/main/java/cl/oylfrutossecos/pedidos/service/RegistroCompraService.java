package cl.oylfrutossecos.pedidos.service;

import cl.oylfrutossecos.pedidos.client.*;
import cl.oylfrutossecos.pedidos.dto.*;
import cl.oylfrutossecos.pedidos.model.*;
import cl.oylfrutossecos.pedidos.repository.PedidoRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class RegistroCompraService {

    private final ClientesClient clientesClient;
    private final InventarioClient inventarioClient;
    private final PreparadorCompraService preparador;
    private final PedidoService pedidoService;
    private final PedidoRepository repository;
    private final TransactionTemplate transacciones;

    public RegistroCompraService(
            ClientesClient clientesClient,
            InventarioClient inventarioClient,
            PreparadorCompraService preparador,
            PedidoService pedidoService,
            PedidoRepository repository,
            PlatformTransactionManager transactionManager
    ) {

        this.clientesClient = clientesClient;
        this.inventarioClient = inventarioClient;
        this.preparador = preparador;
        this.pedidoService = pedidoService;
        this.repository = repository;

        this.transacciones =
                new TransactionTemplate(transactionManager);
    }

    // 1. COORDINAR EL REGISTRO DE LA COMPRA

    public PedidoResponse comprar(
            Jwt jwt,
            CrearPedidoRequest datos
    ) {

        preparador.validarSolicitud(datos);

        ClienteRemoto cliente =
                clientesClient.consultarPerfil(jwt);

        if (!"CLIENTE".equals(cliente.rol())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo los clientes pueden realizar compras"
            );
        }

        // Identificador unico por cliente y solicitud.

        String numero =
                "OYL-"
                        + cliente.id()
                        + "-"
                        + datos.solicitudId();

        // Guardar la compra antes de reservar inventario.

        if (repository.findByNumero(numero).isEmpty()) {

            try {

                var compra = preparador.preparar(
                        jwt,
                        datos
                );

                if (!cliente.id().equals(compra.clienteId())) {

                    throw new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Identidad del comprador invalida"
                    );
                }

                transacciones.executeWithoutResult(
                        estado -> guardarBorrador(numero, compra)
                );

            } catch (DataIntegrityViolationException error) {

                // Otra solicitud pudo registrar el mismo numero.

                if (repository.findByNumero(numero).isEmpty()) {
                    throw error;
                }

            } catch (ResponseStatusException error) {

                // Comprobar si otro intento ya guardo la compra.

                if (repository.findByNumero(numero).isEmpty()) {
                    throw error;
                }
            }
        }

        // Procesar la compra utilizando un bloqueo.

        FaseCompra fase = transacciones.execute(
                estado -> procesarReservas(numero, datos)
        );

        if (fase == FaseCompra.REGISTRADA) {

            return pedidoService.buscarPorNumero(numero);
        }

        // Liberar reservas si la compra fue rechazada.

        if (fase == FaseCompra.CANCELANDO) {

            transacciones.executeWithoutResult(
                    estado -> liberarReservas(numero)
            );
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No fue posible completar la compra"
        );
    }

    // 2. GUARDAR EL INTENTO DE COMPRA

    private void guardarBorrador(
            String numero,
            PreparadorCompraService.CompraPreparada compra
    ) {

        if (repository.findByNumero(numero).isPresent()) {
            return;
        }

        Pedido pedido = new Pedido();

        pedido.setNumero(numero);
        pedido.setFecha(System.currentTimeMillis());
        pedido.setClienteId(compra.clienteId());
        pedido.setNombreCliente(compra.nombreCliente());
        pedido.setTelefono(compra.telefono());
        pedido.setModalidad(compra.modalidad());
        pedido.setDireccion(compra.direccion());
        pedido.setTotal(compra.total());

        pedido.setEstado(EstadoPedido.PENDIENTE);

        pedido.setFaseCompra(FaseCompra.RESERVANDO);

        for (DetallePedido producto : compra.productos()) {

            pedido.agregarProducto(
                    new DetallePedido(
                            producto.getIdProducto(),
                            producto.getNombre(),
                            producto.getCantidad(),
                            producto.getPrecioUnitario()
                    )
            );
        }

        repository.saveAndFlush(pedido);
    }

    // 3. RESERVAR PRODUCTOS

    private FaseCompra procesarReservas(
            String numero,
            CrearPedidoRequest datos
    ) {

        Pedido pedido = buscarConBloqueo(numero);

        if (datos != null) {
            verificarMismaSolicitud(pedido, datos);
        }

        // Si ya se proceso, no repetir descuentos.

        if (
                pedido.getFaseCompra() == FaseCompra.REGISTRADA
                        || pedido.getFaseCompra() == FaseCompra.RECHAZADA
                        || pedido.getFaseCompra() == FaseCompra.CANCELANDO
        ) {

            return pedido.getFaseCompra();
        }

        if (pedido.getFaseCompra() != FaseCompra.RESERVANDO) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La compra no tiene una reserva pendiente"
            );
        }

        for (DetallePedido detalle : pedido.getProductos()) {

            String operacionId = generarOperacionId(
                    numero,
                    detalle.getIdProducto()
            );

            try {

                ReservaRemota reserva =
                        inventarioClient.reservar(
                                operacionId,
                                detalle.getIdProducto(),
                                detalle.getCantidad()
                        );

                if (!"RESERVADA".equals(reserva.estado())) {

                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "La reserva no esta disponible"
                    );
                }

            } catch (ResponseStatusException error) {

                int codigo = error.getStatusCode().value();

                // Rechazo conocido: iniciar compensacion.

                if (codigo == 409 || codigo == 404) {

                    pedido.setFaseCompra(
                            FaseCompra.CANCELANDO
                    );

                    repository.saveAndFlush(pedido);

                    return FaseCompra.CANCELANDO;
                }

                // Error de comunicacion:
                // conservar el intento para reanudarlo.

                throw error;
            }
        }

        // Todos los productos fueron reservados.

        pedido.setFaseCompra(FaseCompra.REGISTRADA);

        repository.saveAndFlush(pedido);

        return FaseCompra.REGISTRADA;
    }

    // 4. LIBERAR RESERVAS DE UNA COMPRA FALLIDA

    private void liberarReservas(String numero) {

        Pedido pedido = buscarConBloqueo(numero);

        if (
                pedido.getFaseCompra() != FaseCompra.CANCELANDO
        ) {
            return;
        }

        for (DetallePedido detalle : pedido.getProductos()) {

            String operacionId = generarOperacionId(
                    numero,
                    detalle.getIdProducto()
            );

            ReservaRemota reserva;

            try {

                reserva = inventarioClient.consultarReserva(
                        operacionId
                );

            } catch (ResponseStatusException error) {

                // La reserva puede no haberse creado.

                if (error.getStatusCode().value() == 404) {
                    continue;
                }

                throw error;
            }

            if (
                    !detalle.getIdProducto().equals(
                            reserva.productoId()
                    )
                            || !detalle.getCantidad().equals(
                            reserva.cantidad()
                    )
            ) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Datos de reserva inconsistentes"
                );
            }

            if ("RESERVADA".equals(reserva.estado())) {

                inventarioClient.cancelarReserva(
                        operacionId
                );

            } else if (
                    !"CANCELADA".equals(reserva.estado())
            ) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La reserva requiere revision"
                );
            }
        }

        pedido.setFaseCompra(FaseCompra.RECHAZADA);

        repository.saveAndFlush(pedido);
    }

    // 5. BUSCAR PEDIDO CON BLOQUEO

    private Pedido buscarConBloqueo(String numero) {

        return repository.buscarConBloqueo(numero)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Solicitud de compra no encontrada"
                        )
                );
    }

    // 6. GENERAR ID REPRODUCIBLE PARA CADA RESERVA

    public static String generarOperacionId(
            String numero,
            Long productoId
    ) {

        String identificador =
                numero + ":" + productoId;

        return UUID.nameUUIDFromBytes(
                identificador.getBytes(
                        StandardCharsets.UTF_8
                )
        ).toString();
    }

    // 7. COMPROBAR QUE UN REINTENTO SEA LA MISMA COMPRA

    private void verificarMismaSolicitud(
            Pedido pedido,
            CrearPedidoRequest datos
    ) {

        String direccion =
                datos.modalidad() == ModalidadPedido.RETIRO
                        ? ""
                        : datos.direccion().trim();

        boolean mismosDatos =

                pedido.getNombreCliente().equals(
                        datos.nombreCliente().trim()
                )

                        && pedido.getTelefono().equals(
                        datos.telefono().trim()
                )

                        && pedido.getModalidad() == datos.modalidad()

                        && pedido.getDireccion().equals(direccion)

                        && pedido.getProductos().size()
                        == datos.productos().size();

        if (!mismosDatos) {
            throw solicitudDuplicada();
        }

        for (ProductoPedidoRequest solicitado
                : datos.productos()) {

            boolean coincide = pedido.getProductos()
                    .stream()
                    .anyMatch(detalle ->

                            detalle.getIdProducto().equals(
                                    solicitado.idProducto()
                            )

                                    && detalle.getCantidad().equals(
                                    solicitado.cantidad()
                            )
                    );

            if (!coincide) {
                throw solicitudDuplicada();
            }
        }
    }

    // 8. ERROR POR REUTILIZAR UNA SOLICITUD DIFERENTE

    private ResponseStatusException solicitudDuplicada() {

        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El identificador pertenece a otra compra"
        );
    }

    // 9. RECUPERAR UNA COMPRA INTERRUMPIDA

    public void recuperarCompra(String numero) {

        FaseCompra fase = transacciones.execute(

                estado -> procesarReservas(
                        numero,
                        null
                )
        );

        if (fase == FaseCompra.CANCELANDO) {

            transacciones.executeWithoutResult(

                    estado -> liberarReservas(numero)
            );
        }
    }
}