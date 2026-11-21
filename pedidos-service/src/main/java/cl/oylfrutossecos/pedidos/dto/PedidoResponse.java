package cl.oylfrutossecos.pedidos.dto;

import cl.oylfrutossecos.pedidos.model.EstadoPedido;
import cl.oylfrutossecos.pedidos.model.ModalidadPedido;

import java.util.List;

public record PedidoResponse(

        Long id,
        String numero,
        Long fecha,
        Long clienteId,
        String nombreCliente,
        String telefono,
        ModalidadPedido modalidad,
        String direccion,
        List<DetalleResponse> productos,
        Integer total,
        EstadoPedido estado

) {

    public record DetalleResponse(

            Long idProducto,
            String nombre,
            Integer cantidad,
            Integer precioUnitario

    ) {
    }
}