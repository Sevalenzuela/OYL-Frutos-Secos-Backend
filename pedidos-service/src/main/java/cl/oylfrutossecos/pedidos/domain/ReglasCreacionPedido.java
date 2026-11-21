package cl.oylfrutossecos.pedidos.domain;

import cl.oylfrutossecos.pedidos.model.DetallePedido;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ReglasCreacionPedido {

    private ReglasCreacionPedido() {
    }

    // VALIDAR LOS PRODUCTOS DE UN PEDIDO

    public static void validarProductos(
            List<DetallePedido> productos
    ) {

        if (productos == null || productos.isEmpty()) {

            throw new IllegalArgumentException(
                    "El pedido debe contener productos"
            );
        }

        Set<Long> identificadores = new HashSet<>();

        for (DetallePedido producto : productos) {

            if (
                    producto == null
                            || producto.getIdProducto() == null
                            || producto.getIdProducto() <= 0
            ) {

                throw new IllegalArgumentException(
                        "El identificador del producto no es valido"
                );
            }

            if (
                    producto.getCantidad() == null
                            || producto.getCantidad() <= 0
            ) {

                throw new IllegalArgumentException(
                        "La cantidad debe ser mayor a cero"
                );
            }

            if (
                    producto.getPrecioUnitario() == null
                            || producto.getPrecioUnitario() <= 0
            ) {

                throw new IllegalArgumentException(
                        "El precio debe ser mayor a cero"
                );
            }

            if (
                    producto.getNombre() == null
                            || producto.getNombre().isBlank()
                            || producto.getNombre().length() > 120
            ) {

                throw new IllegalArgumentException(
                        "El nombre del producto no es valido"
                );
            }

            if (!identificadores.add(
                    producto.getIdProducto()
            )) {

                throw new IllegalArgumentException(
                        "El pedido contiene productos duplicados"
                );
            }
        }
    }

    // CALCULAR EL TOTAL DEL PEDIDO

    public static int calcularTotal(
            List<DetallePedido> productos
    ) {

        validarProductos(productos);

        long total = 0;

        for (DetallePedido producto : productos) {

            long subtotal =
                    (long) producto.getCantidad()
                            * producto.getPrecioUnitario();

            // Evitar desbordamiento del total.
            if (subtotal > Integer.MAX_VALUE - total) {

                throw new IllegalArgumentException(
                        "El total supera el monto permitido"
                );
            }

            total += subtotal;
        }

        return (int) total;
    }
}