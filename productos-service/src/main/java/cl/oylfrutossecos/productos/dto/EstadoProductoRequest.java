package cl.oylfrutossecos.productos.dto;

import jakarta.validation.constraints.NotNull;

// Datos para activar o desactivar un producto.
public record EstadoProductoRequest(

        @NotNull(message = "El estado es obligatorio")
        Boolean activo

) {
}
