package cl.oylfrutossecos.pedidos.service;

import cl.oylfrutossecos.pedidos.client.InventarioClient;
import cl.oylfrutossecos.pedidos.client.ReservaRemota;
import cl.oylfrutossecos.pedidos.domain.ReglasEstadoPedido;
import cl.oylfrutossecos.pedidos.dto.PedidoResponse;
import cl.oylfrutossecos.pedidos.model.*;
import cl.oylfrutossecos.pedidos.repository.PedidoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GestionEstadoPedidoService {

    private final PedidoRepository repository;
    private final PedidoService pedidoService;
    private final InventarioClient inventarioClient;
    private final TransactionTemplate transacciones;

    public GestionEstadoPedidoService(
            PedidoRepository repository,
            PedidoService pedidoService,
            InventarioClient inventarioClient,
            PlatformTransactionManager transactionManager
    ) {

        this.repository = repository;
        this.pedidoService = pedidoService;
        this.inventarioClient = inventarioClient;

        this.transacciones =
                new TransactionTemplate(transactionManager);
    }

    // 1. COORDINAR EL CAMBIO DE ESTADO

    public PedidoResponse cambiarEstado(
            String numero,
            EstadoPedido nuevoEstado
    ) {

        if (nuevoEstado == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nuevo estado es obligatorio"
            );
        }

        // Guardar primero la intencion del cambio.

        Boolean requiereInventario = transacciones.execute(
                transaccion -> prepararCambio(
                        numero,
                        nuevoEstado
                )
        );

        // Procesar Inventario en una transaccion posterior.

        if (Boolean.TRUE.equals(requiereInventario)) {

            transacciones.executeWithoutResult(
                    transaccion -> procesarInventario(
                            numero,
                            nuevoEstado
                    )
            );
        }

        return pedidoService.buscarPorNumero(numero);
    }

    // 2. VALIDAR Y PREPARAR CAMBIO DE ESTADO

    private boolean prepararCambio(
            String numero,
            EstadoPedido nuevoEstado
    ) {

        Pedido pedido = buscarPedido(numero);

        boolean tieneReservas =
                pedido.getFaseCompra() == FaseCompra.REGISTRADA;

        // Comprobar si existe una operacion pendiente.

        if (pedido.getEstadoObjetivo() != null) {

            if (pedido.getEstadoObjetivo() != nuevoEstado) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Existe otro cambio de estado pendiente"
                );
            }

            // Reanudar la operacion anterior.

            return true;
        }

        // Permitir reintentos de operaciones finalizadas.

        if (
                tieneReservas
                        && esEstadoFinal(nuevoEstado)
                        && pedido.getEstado() == nuevoEstado
        ) {

            return false;
        }

        // Aplicar nuestras reglas de negocio.

        boolean permitido =
                ReglasEstadoPedido.puedeCambiarEstado(
                        pedido.getEstado(),
                        nuevoEstado,
                        pedido.getModalidad()
                );

        if (!permitido) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se permite cambiar de "
                            + pedido.getEstado()
                            + " a "
                            + nuevoEstado
            );
        }

        // Los estados finales requieren modificar Inventario.

        if (
                tieneReservas
                        && esEstadoFinal(nuevoEstado)
        ) {

            pedido.setEstadoObjetivo(nuevoEstado);

            repository.saveAndFlush(pedido);

            return true;
        }

        // Los estados intermedios no modifican el stock.

        pedido.setEstado(nuevoEstado);

        repository.saveAndFlush(pedido);

        return false;
    }

    // 3. CONFIRMAR O CANCELAR RESERVAS

    private void procesarInventario(
            String numero,
            EstadoPedido nuevoEstado
    ) {

        Pedido pedido = buscarPedido(numero);

        // Otro reintento pudo terminar la operacion.

        if (
                pedido.getEstadoObjetivo() == null
                        && pedido.getEstado() == nuevoEstado
        ) {

            return;
        }

        if (
                pedido.getEstadoObjetivo() != nuevoEstado
                        || !esEstadoFinal(nuevoEstado)
                        || pedido.getFaseCompra() != FaseCompra.REGISTRADA
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No existe una operacion compatible"
            );
        }

        for (DetallePedido detalle : pedido.getProductos()) {

            // Recuperar el mismo ID utilizado al comprar.

            String operacionId =
                    RegistroCompraService.generarOperacionId(
                            numero,
                            detalle.getIdProducto()
                    );

            ReservaRemota reserva;

            if (nuevoEstado == EstadoPedido.CANCELADO) {

                reserva = inventarioClient.cancelarReserva(
                        operacionId
                );

                verificarReserva(
                        reserva,
                        operacionId,
                        detalle,
                        "CANCELADA"
                );

            } else {

                reserva = inventarioClient.confirmarReserva(
                        operacionId
                );

                verificarReserva(
                        reserva,
                        operacionId,
                        detalle,
                        "CONFIRMADA"
                );
            }
        }

        // Todas las reservas fueron procesadas.

        pedido.setEstado(nuevoEstado);
        pedido.setEstadoObjetivo(null);

        repository.saveAndFlush(pedido);
    }

    // 4. VALIDAR RESPUESTA DE INVENTARIO

    private void verificarReserva(
            ReservaRemota reserva,
            String operacionId,
            DetallePedido detalle,
            String estadoEsperado
    ) {

        if (
                reserva == null

                        || !operacionId.equals(
                        reserva.operacionId()
                )

                        || !detalle.getIdProducto().equals(
                        reserva.productoId()
                )

                        || !detalle.getCantidad().equals(
                        reserva.cantidad()
                )

                        || !estadoEsperado.equals(
                        reserva.estado()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Respuesta de inventario inconsistente"
            );
        }
    }

    // 5. BUSCAR PEDIDO CON BLOQUEO

    private Pedido buscarPedido(String numero) {

        return repository.buscarConBloqueo(numero)
                .filter(Pedido::esVisible)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Pedido no encontrado"
                        )
                );
    }

    // 6. COMPROBAR ESTADOS FINALES

    private boolean esEstadoFinal(EstadoPedido estado) {

        return estado == EstadoPedido.ENTREGADO
                || estado == EstadoPedido.CANCELADO;
    }
}