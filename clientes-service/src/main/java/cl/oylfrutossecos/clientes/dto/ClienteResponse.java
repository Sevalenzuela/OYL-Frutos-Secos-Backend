package cl.oylfrutossecos.clientes.dto;

import cl.oylfrutossecos.clientes.model.RolUsuario;

public record ClienteResponse(

        Long id,
        String nombre,
        String nombreUsuario,
        String correo,
        String telefono,
        String direccion,
        RolUsuario rol,
        Boolean activo

) {
}