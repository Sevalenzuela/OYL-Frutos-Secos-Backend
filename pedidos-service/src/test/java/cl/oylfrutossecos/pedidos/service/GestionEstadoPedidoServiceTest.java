package cl.oylfrutossecos.pedidos.service;

import cl.oylfrutossecos.pedidos.client.InventarioClient;
import cl.oylfrutossecos.pedidos.client.ReservaRemota;
import cl.oylfrutossecos.pedidos.model.*;
import cl.oylfrutossecos.pedidos.repository.PedidoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class GestionEstadoPedidoServiceTest {

    @Mock
    private PedidoRepository repository;

    @Mock
    private PedidoService pedidoService;

    @Mock
    private InventarioClient inventarioClient;

    @Mock
    private PlatformTransactionManager transactionManager;

    private GestionEstadoPedidoService service;

    private Pedido pedido;

    private static final String NUMERO = "OYL-PRUEBA-001";

    @BeforeEach
    void prepararPruebas() {

        service = new GestionEstadoPedidoService(
                repository,
                pedidoService,
                inventarioClient,
                transactionManager
        );

        pedido = new Pedido();

        pedido.setNumero(NUMERO);
        pedido.setClienteId(2L);
        pedido.setModalidad(ModalidadPedido.RETIRO);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setFaseCompra(FaseCompra.REGISTRADA);

        pedido.agregarProducto(
                new DetallePedido(
                        1L,
                        "Almendras naturales",
                        2,
                        4500
                )
        );

        when(transactionManager.getTransaction(any()))
                .thenAnswer(
                        invocacion -> new SimpleTransactionStatus()
                );

        when(repository.buscarConBloqueo(NUMERO))
                .thenReturn(Optional.of(pedido));
    }

    private String operacion(Long productoId) {

        return RegistroCompraService.generarOperacionId(
                NUMERO,
                productoId
        );
    }

    private ReservaRemota reserva(
            Long productoId,
            Integer cantidad,
            String estado
    ) {

        return new ReservaRemota(
                operacion(productoId),
                productoId,
                cantidad,
                estado
        );
    }

    // PRUEBA 1: CANCELAR UN PEDIDO

    @Test
    void cancelarPedidoLiberaReservas() {

        when(inventarioClient.cancelarReserva(
                operacion(1L)
        )).thenReturn(
                reserva(1L, 2, "CANCELADA")
        );

        service.cambiarEstado(
                NUMERO,
                EstadoPedido.CANCELADO
        );

        assertEquals(
                EstadoPedido.CANCELADO,
                pedido.getEstado()
        );

        assertNull(pedido.getEstadoObjetivo());

        verify(inventarioClient)
                .cancelarReserva(operacion(1L));
    }

    // PRUEBA 2: ENTREGAR UN PEDIDO

    @Test
    void entregarPedidoConfirmaReservas() {

        pedido.setEstado(
                EstadoPedido.LISTO_PARA_RETIRO
        );

        when(inventarioClient.confirmarReserva(
                operacion(1L)
        )).thenReturn(
                reserva(1L, 2, "CONFIRMADA")
        );

        service.cambiarEstado(
                NUMERO,
                EstadoPedido.ENTREGADO
        );

        assertEquals(
                EstadoPedido.ENTREGADO,
                pedido.getEstado()
        );

        assertNull(pedido.getEstadoObjetivo());

        verify(inventarioClient)
                .confirmarReserva(operacion(1L));
    }

    // PRUEBA 3: PREPARAR SIN MODIFICAR STOCK

    @Test
    void prepararPedidoNoModificaInventario() {

        service.cambiarEstado(
                NUMERO,
                EstadoPedido.EN_PREPARACION
        );

        assertEquals(
                EstadoPedido.EN_PREPARACION,
                pedido.getEstado()
        );

        assertNull(pedido.getEstadoObjetivo());

        verifyNoInteractions(inventarioClient);
    }

    // PRUEBA 4: EVITAR CANCELACIONES DUPLICADAS

    @Test
    void repetirCancelacionNoModificaInventario() {

        pedido.setEstado(EstadoPedido.CANCELADO);

        service.cambiarEstado(
                NUMERO,
                EstadoPedido.CANCELADO
        );

        assertEquals(
                EstadoPedido.CANCELADO,
                pedido.getEstado()
        );

        verifyNoInteractions(inventarioClient);
    }

    // PRUEBA 5: REANUDAR CANCELACION INTERRUMPIDA

    @Test
    void recuperarCancelacionInterrumpida() {

        pedido.agregarProducto(
                new DetallePedido(
                        2L,
                        "Nueces mariposa",
                        1,
                        5200
                )
        );

        when(inventarioClient.cancelarReserva(
                operacion(1L)
        )).thenReturn(
                reserva(1L, 2, "CANCELADA")
        );

        when(inventarioClient.cancelarReserva(
                operacion(2L)
        )).thenThrow(
                new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Inventario no disponible"
                )
        ).thenReturn(
                reserva(2L, 1, "CANCELADA")
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.cambiarEstado(
                        NUMERO,
                        EstadoPedido.CANCELADO
                )
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                error.getStatusCode()
        );

        assertEquals(
                EstadoPedido.PENDIENTE,
                pedido.getEstado()
        );

        assertEquals(
                EstadoPedido.CANCELADO,
                pedido.getEstadoObjetivo()
        );

        // Repetir la misma operacion.

        service.cambiarEstado(
                NUMERO,
                EstadoPedido.CANCELADO
        );

        assertEquals(
                EstadoPedido.CANCELADO,
                pedido.getEstado()
        );

        assertNull(pedido.getEstadoObjetivo());

        verify(inventarioClient, times(2))
                .cancelarReserva(operacion(1L));

        verify(inventarioClient, times(2))
                .cancelarReserva(operacion(2L));
    }

    // PRUEBA 6: IMPEDIR OPERACIONES CONTRADICTORIAS

    @Test
    void rechazarOtroCambioMientrasCancela() {

        pedido.setEstadoObjetivo(
                EstadoPedido.CANCELADO
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.cambiarEstado(
                        NUMERO,
                        EstadoPedido.EN_PREPARACION
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        verifyNoInteractions(inventarioClient);
    }

    // PRUEBA 7: IMPEDIR ENTREGA DIRECTA

    @Test
    void rechazarEntregaDesdePendiente() {

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.cambiarEstado(
                        NUMERO,
                        EstadoPedido.ENTREGADO
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        verifyNoInteractions(inventarioClient);
    }
}