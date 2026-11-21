package cl.oylfrutossecos.pedidos.client;

public record InventarioRemoto(
        Long productoId,
        Integer stockDisponible,
        Integer stockReservado
) {
}
