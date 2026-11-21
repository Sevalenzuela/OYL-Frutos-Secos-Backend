package cl.oylfrutossecos.inventario.service;

import cl.oylfrutossecos.inventario.dto.AjusteStockRequest;
import cl.oylfrutossecos.inventario.dto.ReservaStockRequest;
import cl.oylfrutossecos.inventario.dto.TipoMovimiento;

import cl.oylfrutossecos.inventario.model.EstadoReserva;
import cl.oylfrutossecos.inventario.model.Inventario;
import cl.oylfrutossecos.inventario.model.ReservaStock;

import cl.oylfrutossecos.inventario.repository.InventarioRepository;
import cl.oylfrutossecos.inventario.repository.ReservaStockRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock
    private InventarioRepository inventarioRepository;

    @Mock
    private ReservaStockRepository reservaRepository;

    @InjectMocks
    private InventarioService service;

    // DATOS FICTICIOS
    private Inventario inventario() {
        return new Inventario(1L, 27, 0);
    }

    private ReservaStock reserva() {
        return new ReservaStock("op-1", 1L, 5);
    }

    private void bloquearInventario(Inventario inventario) {
        when(inventarioRepository.buscarConBloqueo(
                inventario.getProductoId()
        )).thenReturn(Optional.of(inventario));
    }

    private void guardarInventario() {
        when(inventarioRepository.save(any(Inventario.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0)
                );
    }

    private void guardarReserva() {
        when(reservaRepository.save(any(ReservaStock.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0)
                );
    }

    // PRUEBA 1
    @Test
    void listarInventarioCorrectamente() {

        Inventario inventario = inventario();

        when(inventarioRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(inventario));

        List<Inventario> resultado =
                service.listarInventario();

        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getProductoId());

        verify(inventarioRepository).findAll(
                Sort.by(Sort.Direction.ASC, "productoId")
        );
    }

    // PRUEBA 2
    @Test
    void buscarInventarioExistente() {

        when(inventarioRepository.findById(1L))
                .thenReturn(Optional.of(inventario()));

        Optional<Inventario> resultado =
                service.buscarPorProductoId(1L);

        assertTrue(resultado.isPresent());
        assertEquals(27, resultado.get().getStockDisponible());
    }

    // PRUEBA 3
    @Test
    void buscarInventarioInexistente() {

        when(inventarioRepository.findById(999L))
                .thenReturn(Optional.empty());

        Optional<Inventario> resultado =
                service.buscarPorProductoId(999L);

        assertTrue(resultado.isEmpty());
    }

    // PRUEBA 4
    @Test
    void entradaAumentaStock() {

        Inventario inventario = inventario();
        bloquearInventario(inventario);
        guardarInventario();

        Inventario resultado = service.ajustarStock(
                1L,
                new AjusteStockRequest(TipoMovimiento.ENTRADA, 5)
        );

        assertEquals(32, resultado.getStockDisponible());
        assertEquals(0, resultado.getStockReservado());

        verify(inventarioRepository).save(inventario);
    }

    // PRUEBA 5
    @Test
    void salidaDisminuyeStock() {

        Inventario inventario = inventario();
        bloquearInventario(inventario);
        guardarInventario();

        Inventario resultado = service.ajustarStock(
                1L,
                new AjusteStockRequest(TipoMovimiento.SALIDA, 3)
        );

        assertEquals(24, resultado.getStockDisponible());
        assertEquals(0, resultado.getStockReservado());
    }

    // PRUEBA 6
    @Test
    void salidaSinStockDevuelve409() {

        Inventario inventario = inventario();
        bloquearInventario(inventario);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.ajustarStock(
                        1L,
                        new AjusteStockRequest(TipoMovimiento.SALIDA, 999)
                )
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(27, inventario.getStockDisponible());

        verify(inventarioRepository, never())
                .save(any(Inventario.class));
    }

    // PRUEBA 7
    @Test
    void entradaConDesbordamientoDevuelve400() {

        Inventario inventario = new Inventario(
                1L,
                Integer.MAX_VALUE,
                0
        );

        bloquearInventario(inventario);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.ajustarStock(
                        1L,
                        new AjusteStockRequest(TipoMovimiento.ENTRADA, 1)
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        verify(inventarioRepository, never())
                .save(any(Inventario.class));
    }

    // PRUEBA 8
    @Test
    void ajusteDeProductoInexistenteDevuelve404() {

        when(inventarioRepository.buscarConBloqueo(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.ajustarStock(
                        999L,
                        new AjusteStockRequest(TipoMovimiento.ENTRADA, 5)
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    // PRUEBA 9
    @Test
    void reservarDescuentaDisponibleYAumentaReservado() {

        Inventario inventario = inventario();

        when(reservaRepository.findById("op-1"))
                .thenReturn(Optional.empty());

        bloquearInventario(inventario);
        guardarReserva();

        ReservaStock resultado = service.reservar(
                new ReservaStockRequest("op-1", 1L, 5)
        );

        assertEquals(EstadoReserva.RESERVADA, resultado.getEstado());
        assertEquals(22, inventario.getStockDisponible());
        assertEquals(5, inventario.getStockReservado());

        verify(inventarioRepository).save(inventario);
        verify(reservaRepository).save(any(ReservaStock.class));
    }

    // PRUEBA 10
    @Test
    void repetirReservaNoDescuentaDosVeces() {

        ReservaStock existente = reserva();

        when(reservaRepository.findById("op-1"))
                .thenReturn(Optional.of(existente));

        ReservaStock resultado = service.reservar(
                new ReservaStockRequest("op-1", 1L, 5)
        );

        assertSame(existente, resultado);
        assertEquals(EstadoReserva.RESERVADA, resultado.getEstado());

        verifyNoInteractions(inventarioRepository);
        verify(reservaRepository, never())
                .save(any(ReservaStock.class));
    }

    // PRUEBA 11
    @Test
    void reutilizarIdConOtraCantidadDevuelve409() {

        when(reservaRepository.findById("op-1"))
                .thenReturn(Optional.of(reserva()));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.reservar(
                        new ReservaStockRequest("op-1", 1L, 8)
                )
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());

        verifyNoInteractions(inventarioRepository);
    }

    // PRUEBA 12
    @Test
    void reservarMasDelDisponibleDevuelve409() {

        Inventario inventario = inventario();

        when(reservaRepository.findById("op-1"))
                .thenReturn(Optional.empty());

        bloquearInventario(inventario);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.reservar(
                        new ReservaStockRequest("op-1", 1L, 999)
                )
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(27, inventario.getStockDisponible());
        assertEquals(0, inventario.getStockReservado());

        verify(inventarioRepository, never())
                .save(any(Inventario.class));

        verify(reservaRepository, never())
                .save(any(ReservaStock.class));
    }

    // PRUEBA 13
    @Test
    void confirmarReservaLiberaContadorReservado() {

        Inventario inventario = new Inventario(1L, 22, 5);
        ReservaStock reserva = reserva();

        when(reservaRepository.buscarConBloqueo("op-1"))
                .thenReturn(Optional.of(reserva));

        bloquearInventario(inventario);
        guardarReserva();

        ReservaStock resultado =
                service.confirmarReserva("op-1");

        assertEquals(EstadoReserva.CONFIRMADA, resultado.getEstado());
        assertEquals(22, inventario.getStockDisponible());
        assertEquals(0, inventario.getStockReservado());

        verify(inventarioRepository).save(inventario);
    }

    // PRUEBA 14
    @Test
    void confirmarDosVecesNoDescuentaNuevamente() {

        ReservaStock reserva = reserva();
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        when(reservaRepository.buscarConBloqueo("op-1"))
                .thenReturn(Optional.of(reserva));

        ReservaStock resultado =
                service.confirmarReserva("op-1");

        assertEquals(EstadoReserva.CONFIRMADA, resultado.getEstado());

        verifyNoInteractions(inventarioRepository);
        verify(reservaRepository, never())
                .save(any(ReservaStock.class));
    }

    // PRUEBA 15
    @Test
    void noPermiteConfirmarReservaCancelada() {

        ReservaStock reserva = reserva();
        reserva.setEstado(EstadoReserva.CANCELADA);

        when(reservaRepository.buscarConBloqueo("op-1"))
                .thenReturn(Optional.of(reserva));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.confirmarReserva("op-1")
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(inventarioRepository);
    }

    // PRUEBA 16
    @Test
    void cancelarDevuelveStockDisponible() {

        Inventario inventario = new Inventario(1L, 22, 5);
        ReservaStock reserva = reserva();

        when(reservaRepository.buscarConBloqueo("op-1"))
                .thenReturn(Optional.of(reserva));

        bloquearInventario(inventario);
        guardarReserva();

        ReservaStock resultado =
                service.cancelarReserva("op-1");

        assertEquals(EstadoReserva.CANCELADA, resultado.getEstado());
        assertEquals(27, inventario.getStockDisponible());
        assertEquals(0, inventario.getStockReservado());

        verify(inventarioRepository).save(inventario);
    }

    // PRUEBA 17
    @Test
    void cancelarDosVecesNoDevuelveStockDuplicado() {

        ReservaStock reserva = reserva();
        reserva.setEstado(EstadoReserva.CANCELADA);

        when(reservaRepository.buscarConBloqueo("op-1"))
                .thenReturn(Optional.of(reserva));

        ReservaStock resultado =
                service.cancelarReserva("op-1");

        assertEquals(EstadoReserva.CANCELADA, resultado.getEstado());

        verifyNoInteractions(inventarioRepository);
        verify(reservaRepository, never())
                .save(any(ReservaStock.class));
    }

    // PRUEBA 18
    @Test
    void noPermiteCancelarReservaConfirmada() {

        ReservaStock reserva = reserva();
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        when(reservaRepository.buscarConBloqueo("op-1"))
                .thenReturn(Optional.of(reserva));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.cancelarReserva("op-1")
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(inventarioRepository);
    }

    // PRUEBA 19
    @Test
    void confirmarReservaInexistenteDevuelve404() {

        when(reservaRepository.buscarConBloqueo("desconocida"))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.confirmarReserva("desconocida")
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }
}
