package cl.oylfrutossecos.pedidos.client;

public record ReservaRemota(
        String operacionId,
        Long productoId,
        Integer cantidad,
        String estado
) {
}