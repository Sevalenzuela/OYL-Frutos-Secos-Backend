package cl.oylfrutossecos.clientes.dto;

import jakarta.validation.constraints.*;

public record RegistroClienteRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 120)
        String nombre,

        @NotBlank(message = "El usuario es obligatorio")
        @Pattern(
                regexp = "[a-zA-Z0-9._-]{3,80}",
                message = "El nombre de usuario no es valido"
        )
        String nombreUsuario,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        @Size(max = 150)
        String correo,

        @NotBlank(message = "El telefono es obligatorio")
        @Size(max = 30)
        @Pattern(
                regexp = "[+0-9 ()-]+",
                message = "El telefono no es valido"
        )
        String telefono,

        @Size(max = 250)
        String direccion,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(
                min = 8,
                max = 72,
                message = "La contrasena debe tener entre 8 y 72 caracteres"
        )
        String contrasena

) {
}