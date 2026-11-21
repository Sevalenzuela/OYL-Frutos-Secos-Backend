package cl.oylfrutossecos.pedidos.integration;

import cl.oylfrutossecos.pedidos.dto.PedidoResponse;

import cl.oylfrutossecos.pedidos.model.DetallePedido;
import cl.oylfrutossecos.pedidos.model.Pedido;

import cl.oylfrutossecos.pedidos.repository.PedidoRepository;

import cl.oylfrutossecos.pedidos.service.PedidoService;
import cl.oylfrutossecos.pedidos.service.GestionEstadoPedidoService;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static cl.oylfrutossecos.pedidos.model.EstadoPedido.*;
import static cl.oylfrutossecos.pedidos.model.ModalidadPedido.*;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = "oyl.recuperacion.enabled=false"
)

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pedidos_integracion_test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.h2.console.enabled=false",
        "oyl.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "oyl.inventario.secret=YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk="
})
@Transactional
class PedidoIntegracionTest {

    @Autowired
    private PedidoService service;

    @Autowired
    private GestionEstadoPedidoService gestionEstados;

    @Autowired
    private PedidoRepository repository;

    @Autowired
    private EntityManager entityManager;

    // DATOS FICTICIOS REUTILIZABLES

    private List<DetallePedido> productos() {

        return List.of(

                new DetallePedido(
                        1L,
                        "Almendras naturales",
                        2,
                        4500
                ),

                new DetallePedido(
                        2L,
                        "Nueces mariposa",
                        1,
                        5200
                )
        );
    }

    private PedidoResponse crearPedidoRetiro() {

        return service.registrarPedido(
                2L,
                "Cliente Prueba",
                "912345678",
                RETIRO,
                "",
                productos()
        );
    }

    // PRUEBA 1: Registrar pedido de retiro

    @Test
    void registrarPedidoRetiroCorrectamente() {

        PedidoResponse resultado =
                crearPedidoRetiro();

        assertNotNull(resultado.id());
        assertTrue(resultado.numero().startsWith("OYL-"));
        assertEquals(2L, resultado.clienteId());
        assertEquals(RETIRO, resultado.modalidad());
        assertEquals(PENDIENTE, resultado.estado());
        assertEquals(14200, resultado.total());
        assertEquals(2, resultado.productos().size());
        assertEquals("", resultado.direccion());

        // Limpiar el contexto de persistencia para consultar
        // nuevamente los datos guardados en H2.

        entityManager.flush();
        entityManager.clear();

        Pedido guardado = repository
                .findByNumero(resultado.numero())
                .orElseThrow();

        assertEquals(14200, guardado.getTotal());
        assertEquals(2, guardado.getProductos().size());

        for (DetallePedido detalle : guardado.getProductos()) {

            assertNotNull(detalle.getId());

            assertEquals(
                    guardado.getId(),
                    detalle.getPedido().getId()
            );
        }
    }

    // PRUEBA 2: Registrar pedido de despacho

    @Test
    void registrarPedidoDespachoCorrectamente() {

        PedidoResponse resultado =
                service.registrarPedido(
                        2L,
                        "Cliente Prueba",
                        "912345678",
                        DESPACHO,
                        "Direccion de despacho 123",
                        productos()
                );

        assertEquals(DESPACHO, resultado.modalidad());

        assertEquals(
                "Direccion de despacho 123",
                resultado.direccion()
        );

        assertEquals(14200, resultado.total());
        assertEquals(PENDIENTE, resultado.estado());
    }

    // PRUEBA 3: Consultar pedidos e historial

    @Test
    void consultarPedidosCorrectamente() {

        PedidoResponse creado = crearPedidoRetiro();

        PedidoResponse otroCliente =
                service.registrarPedido(
                        3L,
                        "Otro Cliente",
                        "987654321",
                        RETIRO,
                        "",
                        productos()
                );

        List<PedidoResponse> todos =
                service.listarPedidos();

        assertTrue(
                todos.stream().anyMatch(
                        pedido -> pedido.numero().equals(
                                creado.numero()
                        )
                )
        );

        PedidoResponse encontrado =
                service.buscarPorNumero(creado.numero());

        assertEquals(creado.id(), encontrado.id());

        List<PedidoResponse> historial =
                service.buscarPorCliente(2L);

        assertEquals(1, historial.size());

        assertEquals(
                creado.numero(),
                historial.get(0).numero()
        );

        assertFalse(
                historial.stream().anyMatch(
                        pedido -> pedido.numero().equals(
                                otroCliente.numero()
                        )
                )
        );
    }

    // PRUEBA 4: Pedido inexistente

    @Test
    void buscarPedidoInexistenteDevuelve404() {

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.buscarPorNumero("NO-EXISTE")
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );
    }

    // PRUEBA 5: Identificador de cliente invalido

    @Test
    void rechazarClienteInvalido() {

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.registrarPedido(
                        0L,
                        "Cliente Prueba",
                        "912345678",
                        RETIRO,
                        "",
                        productos()
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );
    }

    // PRUEBA 6: Nombre invalido

    @Test
    void rechazarNombreInvalido() {

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.registrarPedido(
                        2L,
                        "A",
                        "912345678",
                        RETIRO,
                        "",
                        productos()
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );
    }

    // PRUEBA 7: Telefono invalido

    @Test
    void rechazarTelefonoInvalido() {

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.registrarPedido(
                        2L,
                        "Cliente Prueba",
                        "123",
                        RETIRO,
                        "",
                        productos()
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );
    }

    // PRUEBA 8: Modalidad invalida

    @Test
    void rechazarModalidadNula() {

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.registrarPedido(
                        2L,
                        "Cliente Prueba",
                        "912345678",
                        null,
                        "",
                        productos()
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );
    }

    // PRUEBA 9: Despacho sin direccion valida

    @Test
    void rechazarDespachoSinDireccion() {

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.registrarPedido(
                        2L,
                        "Cliente Prueba",
                        "912345678",
                        DESPACHO,
                        "",
                        productos()
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );
    }

    // PRUEBA 10: Pedido sin productos

    @Test
    void rechazarPedidoVacio() {

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.registrarPedido(
                        2L,
                        "Cliente Prueba",
                        "912345678",
                        RETIRO,
                        "",
                        List.of()
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );
    }

    // PRUEBA 11: Completar flujo de retiro

    @Test
    void completarPedidoRetiroCorrectamente() {

        PedidoResponse pedido = crearPedidoRetiro();

        String numero = pedido.numero();

        PedidoResponse preparacion =
                gestionEstados.cambiarEstado(
                        numero,
                        EN_PREPARACION
                );

        assertEquals(
                EN_PREPARACION,
                preparacion.estado()
        );

        PedidoResponse listo =
                gestionEstados.cambiarEstado(
                        numero,
                        LISTO_PARA_RETIRO
                );

        assertEquals(
                LISTO_PARA_RETIRO,
                listo.estado()
        );

        PedidoResponse entregado =
                gestionEstados.cambiarEstado(
                        numero,
                        ENTREGADO
                );

        assertEquals(ENTREGADO, entregado.estado());
        assertEquals(14200, entregado.total());
        assertEquals(2, entregado.productos().size());

        entityManager.flush();
        entityManager.clear();

        PedidoResponse guardado =
                service.buscarPorNumero(numero);

        assertEquals(ENTREGADO, guardado.estado());
        assertEquals(14200, guardado.total());
    }

    // PRUEBA 12: Transicion no permitida

    @Test
    void rechazarTransicionInvalida() {

        PedidoResponse pedido = crearPedidoRetiro();

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> gestionEstados.cambiarEstado(
                        pedido.numero(),
                        ENTREGADO
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );
    }

    // PRUEBA 13: Estado nulo

    @Test
    void rechazarNuevoEstadoNulo() {

        PedidoResponse pedido = crearPedidoRetiro();

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> gestionEstados.cambiarEstado(
                        pedido.numero(),
                        null
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );
    }

    // PRUEBA 14: Cambiar estado de pedido inexistente

    @Test
    void rechazarCambioDePedidoInexistente() {

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> gestionEstados.cambiarEstado(
                        "NO-EXISTE",
                        EN_PREPARACION
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );
    }
}