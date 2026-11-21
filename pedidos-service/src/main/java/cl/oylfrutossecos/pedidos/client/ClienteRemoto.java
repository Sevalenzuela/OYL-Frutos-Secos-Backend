package cl.oylfrutossecos.pedidos.client;

public record ClienteRemoto(

        Long id,
        String nombre,
        String nombreUsuario,
        String correo,
        String telefono,
        String direccion,
        String rol,
        Boolean activo

) {
}