package cl.oylfrutossecos.productos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Datos permitidos para crear y editar productos.
public record ProductoRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120)
        String nombre,

        @NotBlank(message = "La categoria es obligatoria")
        @Size(max = 60)
        String categoria,

        @NotNull(message = "El precio es obligatorio")
        @Min(value = 1, message = "El precio debe ser mayor a cero")
        Integer precio,

        @NotBlank(message = "La descripcion es obligatoria")
        @Size(max = 500)
        String descripcion,

        @NotBlank(message = "La presentacion es obligatoria")
        @Size(max = 100)
        String presentacion,

        @Size(max = 500)
        String imagenUri

) {
}
