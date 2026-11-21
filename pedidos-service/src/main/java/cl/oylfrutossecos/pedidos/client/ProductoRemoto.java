
package cl.oylfrutossecos.pedidos.client;

public record ProductoRemoto(
        Long id,
        String nombre,
        String categoria,
        Integer precio,
        String descripcion,
        String presentacion,
        String imagenUri,
        Boolean activo
) {
}
