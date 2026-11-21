package cl.oylfrutossecos.pedidos.client;

import jakarta.validation.constraints.*;

public record ReservaInventarioRequest(

        @NotBlank
        @Pattern(regexp = "[a-zA-Z0-9_-]{1,80}")
        String operacionId,

        @NotNull
        @Positive
        Long productoId,

        @NotNull
        @Positive
        Integer cantidad

) {
}