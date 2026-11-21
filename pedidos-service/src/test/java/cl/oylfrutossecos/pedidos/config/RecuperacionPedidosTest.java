package cl.oylfrutossecos.pedidos.config;

import cl.oylfrutossecos.pedidos.model.EstadoPedido;
import cl.oylfrutossecos.pedidos.model.FaseCompra;
import cl.oylfrutossecos.pedidos.model.Pedido;

import cl.oylfrutossecos.pedidos.repository.PedidoRepository;

import cl.oylfrutossecos.pedidos.service.RegistroCompraService;
import cl.oylfrutossecos.pedidos.service.GestionEstadoPedidoService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecuperacionPedidosTest {

    @Mock
    private PedidoRepository repository;

    @Mock
    private RegistroCompraService registroCompra;

    @Mock
    private GestionEstadoPedidoService gestionEstados;

    @InjectMocks
    private RecuperacionPedidos recuperacion;

    private static final List<FaseCompra> FASES_PENDIENTES =
            List.of(
                    FaseCompra.RESERVANDO,
                    FaseCompra.CANCELANDO
            );

    private Pedido pedido(
            String numero,
            FaseCompra fase
    ) {

        Pedido pedido = new Pedido();

        pedido.setNumero(numero);
        pedido.setFaseCompra(fase);
        pedido.setEstado(EstadoPedido.PENDIENTE);

        return pedido;
    }

    // PRUEBA 1: RECUPERAR COMPRAS INTERRUMPIDAS

    @Test
    void recuperarComprasPendientes() {

        Pedido reservando = pedido(
                "OYL-001",
                FaseCompra.RESERVANDO
        );

        Pedido cancelando = pedido(
                "OYL-002",
                FaseCompra.CANCELANDO
        );

        when(repository.findByFaseCompraInOrderByFechaAsc(
                FASES_PENDIENTES
        )).thenReturn(
                List.of(reservando, cancelando)
        );

        recuperacion.recuperarPendientes();

        verify(registroCompra)
                .recuperarCompra("OYL-001");

        verify(registroCompra)
                .recuperarCompra("OYL-002");

        verifyNoInteractions(gestionEstados);
    }

    // PRUEBA 2: RECUPERAR CAMBIOS DE ESTADO

    @Test
    void recuperarCambiosDeEstadoPendientes() {

        Pedido cancelando = pedido(
                "OYL-003",
                FaseCompra.REGISTRADA
        );

        cancelando.setEstadoObjetivo(
                EstadoPedido.CANCELADO
        );

        Pedido entregando = pedido(
                "OYL-004",
                FaseCompra.REGISTRADA
        );

        entregando.setEstado(
                EstadoPedido.LISTO_PARA_RETIRO
        );

        entregando.setEstadoObjetivo(
                EstadoPedido.ENTREGADO
        );

        when(
                repository
                        .findByEstadoObjetivoIsNotNullOrderByFechaAsc()
        ).thenReturn(
                List.of(cancelando, entregando)
        );

        recuperacion.recuperarPendientes();

        verify(gestionEstados).cambiarEstado(
                "OYL-003",
                EstadoPedido.CANCELADO
        );

        verify(gestionEstados).cambiarEstado(
                "OYL-004",
                EstadoPedido.ENTREGADO
        );

        verifyNoInteractions(registroCompra);
    }

    // PRUEBA 3: CONTINUAR SI UNA COMPRA FALLA

    @Test
    void continuarRecuperacionSiUnaCompraFalla() {

        Pedido primero = pedido(
                "OYL-005",
                FaseCompra.RESERVANDO
        );

        Pedido segundo = pedido(
                "OYL-006",
                FaseCompra.RESERVANDO
        );

        when(repository.findByFaseCompraInOrderByFechaAsc(
                FASES_PENDIENTES
        )).thenReturn(
                List.of(primero, segundo)
        );

        doThrow(
                new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE
                )
        ).when(registroCompra)
                .recuperarCompra("OYL-005");

        assertDoesNotThrow(
                () -> recuperacion.recuperarPendientes()
        );

        verify(registroCompra)
                .recuperarCompra("OYL-005");

        verify(registroCompra)
                .recuperarCompra("OYL-006");
    }

    // PRUEBA 4: CONTINUAR SI UNA CANCELACION FALLA

    @Test
    void continuarSiUnCambioDeEstadoFalla() {

        Pedido primero = pedido(
                "OYL-007",
                FaseCompra.REGISTRADA
        );

        primero.setEstadoObjetivo(
                EstadoPedido.CANCELADO
        );

        Pedido segundo = pedido(
                "OYL-008",
                FaseCompra.REGISTRADA
        );

        segundo.setEstadoObjetivo(
                EstadoPedido.CANCELADO
        );

        when(
                repository
                        .findByEstadoObjetivoIsNotNullOrderByFechaAsc()
        ).thenReturn(
                List.of(primero, segundo)
        );

        when(gestionEstados.cambiarEstado(
                "OYL-007",
                EstadoPedido.CANCELADO
        )).thenThrow(
                new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE
                )
        );

        assertDoesNotThrow(
                () -> recuperacion.recuperarPendientes()
        );

        verify(gestionEstados).cambiarEstado(
                "OYL-007",
                EstadoPedido.CANCELADO
        );

        verify(gestionEstados).cambiarEstado(
                "OYL-008",
                EstadoPedido.CANCELADO
        );
    }

    // PRUEBA 5: NO EXISTEN OPERACIONES PENDIENTES

    @Test
    void noProcesarSiNoExistenPendientes() {

        when(repository.findByFaseCompraInOrderByFechaAsc(
                FASES_PENDIENTES
        )).thenReturn(List.of());

        when(
                repository
                        .findByEstadoObjetivoIsNotNullOrderByFechaAsc()
        ).thenReturn(List.of());

        recuperacion.recuperarPendientes();

        verifyNoInteractions(
                registroCompra,
                gestionEstados
        );
    }
}