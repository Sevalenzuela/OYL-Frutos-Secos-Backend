package cl.oylfrutossecos.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReservaStockRequest(

        @NotBlank(message = "El identificador es obligatorio")
        @Size(max = 80)
        String operacionId,

        @NotNull(message = "El producto es obligatorio")
        @Positive(message = "El ID debe ser positivo")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        Integer cantidad

) {
}