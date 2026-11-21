package cl.oylfrutossecos.pedidos.domain;

import cl.oylfrutossecos.pedidos.model.DetallePedido;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static cl.oylfrutossecos.pedidos.model.EstadoPedido.*;
import static cl.oylfrutossecos.pedidos.model.ModalidadPedido.*;

class ReglasPedidoTest {

    // DATOS FICTICIOS

    private DetallePedido almendras() {

        return new DetallePedido(
                1L,
                "Almendras naturales",
                2,
                4500
        );
    }

    private DetallePedido nueces() {

        return new DetallePedido(
                2L,
                "Nueces mariposa",
                1,
                5200
        );
    }

    // PRUEBA 1: Calcular total correctamente

    @Test
    void calcularTotalCorrectamente() {

        int total = ReglasCreacionPedido.calcularTotal(
                List.of(almendras(), nueces())
        );

        assertEquals(14200, total);
    }

    // PRUEBA 2: Aceptar el limite maximo permitido

    @Test
    void aceptarTotalMaximo() {

        DetallePedido producto = new DetallePedido(
                1L,
                "Producto de prueba",
                1,
                Integer.MAX_VALUE
        );

        assertEquals(
                Integer.MAX_VALUE,
                ReglasCreacionPedido.calcularTotal(
                        List.of(producto)
                )
        );
    }

    // PRUEBA 3: Evitar desbordamiento del total

    @Test
    void rechazarTotalConDesbordamiento() {

        DetallePedido producto = new DetallePedido(
                1L,
                "Producto de prueba",
                2,
                Integer.MAX_VALUE
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ReglasCreacionPedido.calcularTotal(
                        List.of(producto)
                )
        );
    }

    // PRUEBA 4: Rechazar pedidos vacios

    @Test
    void rechazarPedidoSinProductos() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ReglasCreacionPedido.calcularTotal(
                        List.of()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ReglasCreacionPedido.calcularTotal(null)
        );
    }

    // PRUEBA 5: Identificadores invalidos

    @Test
    void rechazarProductoConIdentificadorInvalido() {

        DetallePedido producto = almendras();

        producto.setIdProducto(0L);

        assertThrows(
                IllegalArgumentException.class,
                () -> ReglasCreacionPedido.calcularTotal(
                        List.of(producto)
                )
        );

        producto.setIdProducto(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> ReglasCreacionPedido.calcularTotal(
                        List.of(producto)
                )
        );

        List<DetallePedido> productos =
                Collections.singletonList(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> ReglasCreacionPedido.calcularTotal(
                        productos
                )
        );
    }

    // PRUEBA 6: Cantidades invalidas

    @Test
    void rechazarCantidadesInvalidas() {

        DetallePedido producto = almendras();

        for (Integer cantidad : new Integer[]{0, -5, null}) {

            producto.setCantidad(cantidad);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReglasCreacionPedido.calcularTotal(
                            List.of(producto)
                    )
            );
        }
    }

    // PRUEBA 7: Precios invalidos

    @Test
    void rechazarPreciosInvalidos() {

        DetallePedido producto = almendras();

        for (Integer precio : new Integer[]{0, -100, null}) {

            producto.setPrecioUnitario(precio);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReglasCreacionPedido.calcularTotal(
                            List.of(producto)
                    )
            );
        }
    }

    // PRUEBA 8: Nombre de producto invalido

    @Test
    void rechazarNombreProductoInvalido() {

        DetallePedido producto = almendras();

        for (String nombre : new String[]{"", " ", null, "a".repeat(121)}) {

            producto.setNombre(nombre);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReglasCreacionPedido.calcularTotal(
                            List.of(producto)
                    )
            );
        }
    }

    // PRUEBA 9: Productos duplicados

    @Test
    void rechazarProductosDuplicados() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ReglasCreacionPedido.calcularTotal(
                        List.of(almendras(), almendras())
                )
        );
    }

    // PRUEBA 10: Transiciones desde pendiente

    @Test
    void validarEstadosDesdePendiente() {

        assertTrue(
                ReglasEstadoPedido.puedeCambiarEstado(
                        PENDIENTE,
                        EN_PREPARACION,
                        RETIRO
                )
        );

        assertTrue(
                ReglasEstadoPedido.puedeCambiarEstado(
                        PENDIENTE,
                        CANCELADO,
                        DESPACHO
                )
        );

        assertFalse(
                ReglasEstadoPedido.puedeCambiarEstado(
                        PENDIENTE,
                        ENTREGADO,
                        RETIRO
                )
        );
    }

    // PRUEBA 11: Diferenciar retiro y despacho

    @Test
    void validarEstadosSegunModalidad() {

        assertEquals(
                List.of(LISTO_PARA_RETIRO, CANCELADO),
                ReglasEstadoPedido.estadosSiguientes(
                        EN_PREPARACION,
                        RETIRO
                )
        );

        assertEquals(
                List.of(EN_DESPACHO, CANCELADO),
                ReglasEstadoPedido.estadosSiguientes(
                        EN_PREPARACION,
                        DESPACHO
                )
        );

        assertFalse(
                ReglasEstadoPedido.puedeCambiarEstado(
                        EN_PREPARACION,
                        EN_DESPACHO,
                        RETIRO
                )
        );

        assertFalse(
                ReglasEstadoPedido.puedeCambiarEstado(
                        EN_PREPARACION,
                        LISTO_PARA_RETIRO,
                        DESPACHO
                )
        );
    }

    // PRUEBA 12: Entrega y estados finales

    @Test
    void validarEntregaYEstadosFinales() {

        assertTrue(
                ReglasEstadoPedido.puedeCambiarEstado(
                        LISTO_PARA_RETIRO,
                        ENTREGADO,
                        RETIRO
                )
        );

        assertTrue(
                ReglasEstadoPedido.puedeCambiarEstado(
                        EN_DESPACHO,
                        ENTREGADO,
                        DESPACHO
                )
        );

        assertTrue(
                ReglasEstadoPedido.estadosSiguientes(
                        ENTREGADO,
                        RETIRO
                ).isEmpty()
        );

        assertTrue(
                ReglasEstadoPedido.estadosSiguientes(
                        CANCELADO,
                        DESPACHO
                ).isEmpty()
        );
    }

    // PRUEBA 13: Estados o modalidades nulas

    @Test
    void rechazarTransicionesConDatosNulos() {

        assertFalse(
                ReglasEstadoPedido.puedeCambiarEstado(
                        null,
                        ENTREGADO,
                        RETIRO
                )
        );

        assertFalse(
                ReglasEstadoPedido.puedeCambiarEstado(
                        PENDIENTE,
                        null,
                        RETIRO
                )
        );

        assertFalse(
                ReglasEstadoPedido.puedeCambiarEstado(
                        PENDIENTE,
                        EN_PREPARACION,
                        null
                )
        );
    }
}