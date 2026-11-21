package cl.oylfrutossecos.pedidos.dto;

import cl.oylfrutossecos.pedidos.model.EstadoPedido;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(

        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoPedido estado

) {
}