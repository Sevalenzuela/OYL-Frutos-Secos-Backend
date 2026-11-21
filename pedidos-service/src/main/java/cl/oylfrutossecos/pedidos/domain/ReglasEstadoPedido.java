package cl.oylfrutossecos.pedidos.domain;

import cl.oylfrutossecos.pedidos.model.EstadoPedido;
import cl.oylfrutossecos.pedidos.model.ModalidadPedido;

import java.util.List;

public final class ReglasEstadoPedido {

    private ReglasEstadoPedido() {
    }

    // 1. OBTENER LOS ESTADOS PERMITIDOS

    public static List<EstadoPedido> estadosSiguientes(
            EstadoPedido estadoActual,
            ModalidadPedido modalidad
    ) {

        if (estadoActual == null || modalidad == null) {
            return List.of();
        }

        return switch (estadoActual) {

            case PENDIENTE -> List.of(
                    EstadoPedido.EN_PREPARACION,
                    EstadoPedido.CANCELADO
            );

            case EN_PREPARACION -> {

                if (modalidad == ModalidadPedido.RETIRO) {

                    yield List.of(
                            EstadoPedido.LISTO_PARA_RETIRO,
                            EstadoPedido.CANCELADO
                    );

                } else {

                    yield List.of(
                            EstadoPedido.EN_DESPACHO,
                            EstadoPedido.CANCELADO
                    );
                }
            }

            case LISTO_PARA_RETIRO, EN_DESPACHO ->
                    List.of(EstadoPedido.ENTREGADO);

            case ENTREGADO, CANCELADO ->
                    List.of();
        };
    }

    // 2. VALIDAR UNA TRANSICION

    public static boolean puedeCambiarEstado(
            EstadoPedido estadoActual,
            EstadoPedido nuevoEstado,
            ModalidadPedido modalidad
    ) {

        if (nuevoEstado == null) {
            return false;
        }

        return estadosSiguientes(
                estadoActual,
                modalidad
        ).contains(nuevoEstado);
    }
}