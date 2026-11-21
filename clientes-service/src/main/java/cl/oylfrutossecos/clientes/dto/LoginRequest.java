package cl.oylfrutossecos.clientes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 80)
        String nombreUsuario,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(max = 72)
        String contrasena

) {
}