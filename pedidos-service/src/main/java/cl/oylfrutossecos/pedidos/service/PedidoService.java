package cl.oylfrutossecos.pedidos.service;

import cl.oylfrutossecos.pedidos.domain.ReglasCreacionPedido;
import cl.oylfrutossecos.pedidos.dto.PedidoResponse;

import cl.oylfrutossecos.pedidos.model.Pedido;
import cl.oylfrutossecos.pedidos.model.DetallePedido;
import cl.oylfrutossecos.pedidos.model.EstadoPedido;
import cl.oylfrutossecos.pedidos.model.ModalidadPedido;

import cl.oylfrutossecos.pedidos.repository.PedidoRepository;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class PedidoService {

    private final PedidoRepository repository;

    public PedidoService(PedidoRepository repository) {
        this.repository = repository;
    }

    // 1. LISTAR TODOS LOS PEDIDOS

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPedidos() {

        return repository.findAll(
                        Sort.by(Sort.Direction.DESC, "fecha")
                )
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    // 2. BUSCAR PEDIDO POR NUMERO

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorNumero(String numero) {

        Pedido pedido = repository.findByNumero(numero)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Pedido no encontrado"
                        )
                );

        return convertirRespuesta(pedido);
    }

    // 3. CONSULTAR HISTORIAL DE UN CLIENTE

    @Transactional(readOnly = true)
    public List<PedidoResponse> buscarPorCliente(
            Long clienteId
    ) {

        return repository
                .findByClienteIdOrderByFechaDesc(clienteId)
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    // 4. REGISTRAR PEDIDO INTERNAMENTE
    // Los productos y precios deben estar previamente
    // verificados por los microservicios correspondientes.

    @Transactional
    public PedidoResponse registrarPedido(
            Long clienteId,
            String nombreCliente,
            String telefono,
            ModalidadPedido modalidad,
            String direccion,
            List<DetallePedido> productos
    ) {

        validarDatosCliente(
                clienteId,
                nombreCliente,
                telefono,
                modalidad,
                direccion
        );

        int total;

        try {

            total = ReglasCreacionPedido.calcularTotal(
                    productos
            );

        } catch (IllegalArgumentException error) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    error.getMessage()
            );
        }

        Pedido pedido = new Pedido();

        pedido.setNumero(
                "OYL-" + UUID.randomUUID().toString()
        );

        pedido.setFecha(System.currentTimeMillis());
        pedido.setClienteId(clienteId);
        pedido.setNombreCliente(nombreCliente.trim());
        pedido.setTelefono(telefono.trim());
        pedido.setModalidad(modalidad);

        pedido.setDireccion(
                modalidad == ModalidadPedido.RETIRO
                        ? ""
                        : direccion.trim()
        );

        pedido.setTotal(total);
        pedido.setEstado(EstadoPedido.PENDIENTE);

        // Crear los detalles asociados al nuevo pedido.

        for (DetallePedido producto : productos) {

            DetallePedido detalle = new DetallePedido(
                    producto.getIdProducto(),
                    producto.getNombre(),
                    producto.getCantidad(),
                    producto.getPrecioUnitario()
            );

            pedido.agregarProducto(detalle);
        }

        Pedido guardado = repository.saveAndFlush(pedido);

        return convertirRespuesta(guardado);
    }

    // 5. VALIDAR DATOS DEL COMPRADOR

    private void validarDatosCliente(
            Long clienteId,
            String nombre,
            String telefono,
            ModalidadPedido modalidad,
            String direccion
    ) {

        if (clienteId == null || clienteId <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El cliente no es valido"
            );
        }

        if (
                nombre == null
                        || nombre.trim().length() < 3
                        || nombre.trim().length() > 120
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre del cliente no es valido"
            );
        }

        if (
                telefono == null
                        || telefono.length() > 30
                        || !telefono.matches("[+0-9 ()-]+")
                        || telefono.chars()
                        .filter(c -> c >= '0' && c <= '9')
                        .count() < 9
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El telefono no es valido"
            );
        }

        if (modalidad == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La modalidad es obligatoria"
            );
        }

        if (modalidad == ModalidadPedido.DESPACHO) {

            if (
                    direccion == null
                            || direccion.trim().length() < 10
                            || direccion.trim().length() > 250
            ) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La direccion de despacho no es valida"
                );
            }
        }
    }

    // 6. CONVERTIR ENTIDAD A DTO

    private PedidoResponse convertirRespuesta(
            Pedido pedido
    ) {

        List<PedidoResponse.DetalleResponse> productos =
                pedido.getProductos()
                        .stream()
                        .map(detalle ->
                                new PedidoResponse.DetalleResponse(
                                        detalle.getIdProducto(),
                                        detalle.getNombre(),
                                        detalle.getCantidad(),
                                        detalle.getPrecioUnitario()
                                )
                        )
                        .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getNumero(),
                pedido.getFecha(),
                pedido.getClienteId(),
                pedido.getNombreCliente(),
                pedido.getTelefono(),
                pedido.getModalidad(),
                pedido.getDireccion(),
                productos,
                pedido.getTotal(),
                pedido.getEstado()
        );
    }
}