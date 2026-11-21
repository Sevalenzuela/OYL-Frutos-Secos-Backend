package cl.oylfrutossecos.pedidos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductoPedidoRequest(

        @NotNull
        @Positive
        Long idProducto,

        @NotNull
        @Positive
        Integer cantidad

) {
}