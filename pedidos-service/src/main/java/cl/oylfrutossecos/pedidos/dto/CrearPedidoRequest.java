package cl.oylfrutossecos.pedidos.dto;

import cl.oylfrutossecos.pedidos.model.ModalidadPedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record CrearPedidoRequest(

        @NotNull
        UUID solicitudId,

        @NotBlank
        @Size(min = 3, max = 120)
        String nombreCliente,

        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "[+0-9 ()-]+")
        String telefono,

        @NotNull
        ModalidadPedido modalidad,

        @Size(max = 250)
        String direccion,

        @NotEmpty
        @Size(max = 50)
        List<@NotNull @Valid ProductoPedidoRequest> productos

) {
}