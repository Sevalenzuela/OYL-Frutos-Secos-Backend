package cl.oylfrutossecos.inventario.service;

import cl.oylfrutossecos.inventario.dto.AjusteStockRequest;
import cl.oylfrutossecos.inventario.dto.ReservaStockRequest;
import cl.oylfrutossecos.inventario.dto.TipoMovimiento;

import cl.oylfrutossecos.inventario.model.EstadoReserva;
import cl.oylfrutossecos.inventario.model.Inventario;
import cl.oylfrutossecos.inventario.model.ReservaStock;

import cl.oylfrutossecos.inventario.repository.InventarioRepository;
import cl.oylfrutossecos.inventario.repository.ReservaStockRepository;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final ReservaStockRepository reservaRepository;

    public InventarioService(
            InventarioRepository inventarioRepository,
            ReservaStockRepository reservaRepository
    ) {
        this.inventarioRepository = inventarioRepository;
        this.reservaRepository = reservaRepository;
    }

    // 1. CONSULTAR INVENTARIO
    public List<Inventario> listarInventario() {

        return inventarioRepository.findAll(
                Sort.by(Sort.Direction.ASC, "productoId")
        );
    }

    public Optional<Inventario> buscarPorProductoId(
            Long productoId
    ) {
        return inventarioRepository.findById(productoId);
    }

    // 2. ENTRADAS Y SALIDAS
    @Transactional
    public Inventario ajustarStock(
            Long productoId,
            AjusteStockRequest datos
    ) {
        Inventario inventario = obtenerInventarioBloqueado(
                productoId
        );

        int disponible = inventario.getStockDisponible();
        int cantidad = datos.cantidad();

        if (datos.tipo() == TipoMovimiento.ENTRADA) {

            if (disponible > Integer.MAX_VALUE - cantidad) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La cantidad supera el limite permitido"
                );
            }

            inventario.setStockDisponible(
                    disponible + cantidad
            );

        } else if (datos.tipo() == TipoMovimiento.SALIDA) {

            if (cantidad > disponible) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stock insuficiente. Disponible: "
                                + disponible
                );
            }

            inventario.setStockDisponible(
                    disponible - cantidad
            );
        }

        return inventarioRepository.save(inventario);
    }

    // 3. RESERVAR STOCK
    @Transactional
    public ReservaStock reservar(ReservaStockRequest datos) {

        String operacionId = datos.operacionId();
        Long productoId = datos.productoId();
        int cantidad = datos.cantidad();

        // Si ya existe, nunca descontar nuevamente.
        Optional<ReservaStock> existente =
                reservaRepository.findById(operacionId);

        if (existente.isPresent()) {
            return comprobarReintento(existente.get(), datos);
        }

        // Bloquear el inventario del producto.
        Inventario inventario =
                obtenerInventarioBloqueado(productoId);

        // Comprobar otra vez después del bloqueo,
        // por si otra solicitud creó esta reserva.
        existente = reservaRepository.findById(operacionId);

        if (existente.isPresent()) {
            return comprobarReintento(existente.get(), datos);
        }

        int disponible = inventario.getStockDisponible();
        int reservado = inventario.getStockReservado();

        if (cantidad > disponible) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Stock insuficiente para reservar"
            );
        }

        if (reservado > Integer.MAX_VALUE - cantidad) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La reserva supera el limite permitido"
            );
        }

        // Apartar las unidades.
        inventario.setStockDisponible(
                disponible - cantidad
        );

        inventario.setStockReservado(
                reservado + cantidad
        );

        ReservaStock reserva = new ReservaStock(
                operacionId,
                productoId,
                cantidad
        );

        inventarioRepository.save(inventario);

        return reservaRepository.save(reserva);
    }

    // 4. CONFIRMAR RESERVA
    @Transactional
    public ReservaStock confirmarReserva(String operacionId) {

        ReservaStock reserva =
                obtenerReservaBloqueada(operacionId);

        // Repetir una confirmación no descuenta dos veces.
        if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            return reserva;
        }

        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede confirmar una reserva cancelada"
            );
        }

        Inventario inventario = obtenerInventarioBloqueado(
                reserva.getProductoId()
        );

        int reservado = inventario.getStockReservado();
        int cantidad = reserva.getCantidad();

        if (reservado < cantidad) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inconsistencia en el stock reservado"
            );
        }

        // Ya se descontó del disponible al reservar.
        // Ahora solo se libera del contador reservado.
        inventario.setStockReservado(
                reservado - cantidad
        );

        reserva.setEstado(EstadoReserva.CONFIRMADA);

        inventarioRepository.save(inventario);

        return reservaRepository.save(reserva);
    }

    // 5. CANCELAR RESERVA
    @Transactional
    public ReservaStock cancelarReserva(String operacionId) {

        ReservaStock reserva =
                obtenerReservaBloqueada(operacionId);

        // Cancelar dos veces no devuelve stock duplicado.
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            return reserva;
        }

        if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede cancelar una reserva confirmada"
            );
        }

        Inventario inventario = obtenerInventarioBloqueado(
                reserva.getProductoId()
        );

        int reservado = inventario.getStockReservado();
        int disponible = inventario.getStockDisponible();
        int cantidad = reserva.getCantidad();

        if (reservado < cantidad) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inconsistencia en el stock reservado"
            );
        }

        if (disponible > Integer.MAX_VALUE - cantidad) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede devolver el stock"
            );
        }

        // Devolver las unidades al disponible.
        inventario.setStockDisponible(
                disponible + cantidad
        );

        inventario.setStockReservado(
                reservado - cantidad
        );

        reserva.setEstado(EstadoReserva.CANCELADA);

        inventarioRepository.save(inventario);

        return reservaRepository.save(reserva);
    }

    // 6. CONSULTAR RESERVA
    public Optional<ReservaStock> buscarReserva(
            String operacionId
    ) {
        return reservaRepository.findById(operacionId);
    }

    // METODOS AUXILIARES
    private Inventario obtenerInventarioBloqueado(
            Long productoId
    ) {
        return inventarioRepository
                .buscarConBloqueo(productoId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "No existe inventario para producto "
                                        + productoId
                        )
                );
    }

    private ReservaStock obtenerReservaBloqueada(
            String operacionId
    ) {
        return reservaRepository
                .buscarConBloqueo(operacionId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Reserva no encontrada"
                        )
                );
    }

    private ReservaStock comprobarReintento(
            ReservaStock existente,
            ReservaStockRequest datos
    ) {
        if (
                !existente.getProductoId().equals(datos.productoId())
                        || !existente.getCantidad().equals(datos.cantidad())
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El identificador ya pertenece a otra reserva"
            );
        }

        return existente;
    }
}
