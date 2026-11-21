package cl.oylfrutossecos.pedidos.service;

import cl.oylfrutossecos.pedidos.client.*;
import cl.oylfrutossecos.pedidos.dto.CrearPedidoRequest;
import cl.oylfrutossecos.pedidos.dto.ProductoPedidoRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cl.oylfrutossecos.pedidos.model.ModalidadPedido.*;

@ExtendWith(MockitoExtension.class)
class PreparadorCompraServiceTest {

    @Mock
    private ClientesClient clientesClient;

    @Mock
    private ProductosClient productosClient;

    @Mock
    private InventarioClient inventarioClient;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private PreparadorCompraService service;

    // DATOS FICTICIOS REUTILIZABLES

    private ClienteRemoto cliente(String rol) {

        return new ClienteRemoto(
                2L,
                "Cliente Prueba",
                "cliente_prueba",
                "cliente@example.com",
                "912345678",
                "",
                rol,
                true
        );
    }

    private ProductoRemoto almendras() {

        return new ProductoRemoto(
                1L,
                "Almendras naturales",
                "Frutos secos",
                4500,
                "Producto de prueba",
                "Bolsa de 250 g",
                null,
                true
        );
    }

    private ProductoRemoto nueces() {

        return new ProductoRemoto(
                2L,
                "Nueces mariposa",
                "Frutos secos",
                5200,
                "Producto de prueba",
                "Bolsa de 250 g",
                null,
                true
        );
    }

    private CrearPedidoRequest solicitud(
            List<ProductoPedidoRequest> productos
    ) {

        return new CrearPedidoRequest(
                UUID.randomUUID(),
                "Cliente Prueba",
                "912345678",
                RETIRO,
                "",
                productos
        );
    }

    // PRUEBA 1: Preparar una compra correctamente

    @Test
    void prepararCompraCorrectamente() {

        when(clientesClient.consultarPerfil(jwt))
                .thenReturn(cliente("CLIENTE"));

        when(productosClient.consultarProductoActivo(1L))
                .thenReturn(almendras());

        when(productosClient.consultarProductoActivo(2L))
                .thenReturn(nueces());

        when(inventarioClient.consultarInventario(1L))
                .thenReturn(
                        new InventarioRemoto(1L, 16, 0)
                );

        when(inventarioClient.consultarInventario(2L))
                .thenReturn(
                        new InventarioRemoto(2L, 18, 0)
                );

        CrearPedidoRequest datos = solicitud(
                List.of(
                        new ProductoPedidoRequest(1L, 2),
                        new ProductoPedidoRequest(2L, 1)
                )
        );

        var resultado = service.preparar(jwt, datos);

        assertEquals(datos.solicitudId(), resultado.solicitudId());
        assertEquals(2L, resultado.clienteId());
        assertEquals(14200, resultado.total());
        assertEquals(2, resultado.productos().size());
        assertEquals(4500, resultado.productos().get(0).getPrecioUnitario());
        assertEquals(5200, resultado.productos().get(1).getPrecioUnitario());

        // Preparar no debe modificar el inventario.
        verify(inventarioClient).consultarInventario(1L);
        verify(inventarioClient).consultarInventario(2L);
        verifyNoMoreInteractions(inventarioClient);
    }

    // PRUEBA 2: Rechazar stock insuficiente

    @Test
    void rechazarStockInsuficiente() {

        when(clientesClient.consultarPerfil(jwt))
                .thenReturn(cliente("CLIENTE"));

        when(productosClient.consultarProductoActivo(1L))
                .thenReturn(almendras());

        when(inventarioClient.consultarInventario(1L))
                .thenReturn(
                        new InventarioRemoto(1L, 3, 0)
                );

        CrearPedidoRequest datos = solicitud(
                List.of(
                        new ProductoPedidoRequest(1L, 5)
                )
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.preparar(jwt, datos)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );
    }

    // PRUEBA 3: Rechazar productos duplicados

    @Test
    void rechazarProductosDuplicados() {

        CrearPedidoRequest datos = solicitud(
                List.of(
                        new ProductoPedidoRequest(1L, 2),
                        new ProductoPedidoRequest(1L, 3)
                )
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.preparar(jwt, datos)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );

        verifyNoInteractions(
                clientesClient,
                productosClient,
                inventarioClient
        );
    }

    // PRUEBA 4: Rechazar carrito vacio

    @Test
    void rechazarCarritoVacio() {

        CrearPedidoRequest datos =
                solicitud(List.of());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.preparar(jwt, datos)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );

        verifyNoInteractions(
                clientesClient,
                productosClient,
                inventarioClient
        );
    }

    // PRUEBA 5: Impedir compras administrativas

    @Test
    void rechazarCompraDeAdministrador() {

        when(clientesClient.consultarPerfil(jwt))
                .thenReturn(cliente("ADMINISTRADOR"));

        CrearPedidoRequest datos = solicitud(
                List.of(
                        new ProductoPedidoRequest(1L, 1)
                )
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.preparar(jwt, datos)
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                error.getStatusCode()
        );

        verifyNoInteractions(
                productosClient,
                inventarioClient
        );
    }

    // PRUEBA 6: Producto inexistente

    @Test
    void rechazarProductoInexistente() {

        when(clientesClient.consultarPerfil(jwt))
                .thenReturn(cliente("CLIENTE"));

        when(productosClient.consultarProductoActivo(999L))
                .thenThrow(
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Producto no encontrado"
                        )
                );

        CrearPedidoRequest datos = solicitud(
                List.of(
                        new ProductoPedidoRequest(999L, 1)
                )
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.preparar(jwt, datos)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );

        verifyNoInteractions(inventarioClient);
    }

    // PRUEBA 7: Rechazar despacho sin direccion

    @Test
    void rechazarDespachoSinDireccion() {

        CrearPedidoRequest datos =
                new CrearPedidoRequest(
                        UUID.randomUUID(),
                        "Cliente Prueba",
                        "912345678",
                        DESPACHO,
                        "",
                        List.of(
                                new ProductoPedidoRequest(1L, 1)
                        )
                );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.preparar(jwt, datos)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );

        verifyNoInteractions(
                clientesClient,
                productosClient,
                inventarioClient
        );
    }
}