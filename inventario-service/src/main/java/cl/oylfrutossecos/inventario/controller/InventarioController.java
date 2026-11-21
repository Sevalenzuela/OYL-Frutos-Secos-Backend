package cl.oylfrutossecos.inventario.controller;

import cl.oylfrutossecos.inventario.dto.AjusteStockRequest;
import cl.oylfrutossecos.inventario.dto.ReservaStockRequest;

import cl.oylfrutossecos.inventario.model.Inventario;
import cl.oylfrutossecos.inventario.model.ReservaStock;

import cl.oylfrutossecos.inventario.service.InventarioService;
import cl.oylfrutossecos.inventario.service.AltaPendiente;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService service;

    private final AltaPendiente altas;

    public InventarioController(InventarioService service,
            AltaPendiente altas) {
        this.service = service;
        this.altas = altas;
    }

    @PutMapping("/{productoId}/inicial")
    public Inventario inicializar(@PathVariable Long productoId,
            @RequestHeader("Authorization") String autorizacion,
            @Valid @RequestBody Inicializacion datos) {
        int stock = altas.consultarStock(productoId, datos.clave(), autorizacion);
        return service.inicializar(productoId, stock, datos.clave().toString());
    }

    public record Inicializacion(@NotNull UUID clave) {}

    // LISTAR INVENTARIO
    @GetMapping
    public List<Inventario> listar() {
        return service.listarInventario();
    }

    // CONSULTAR STOCK POR PRODUCTO
    @GetMapping("/{productoId}")
    public ResponseEntity<Inventario> buscarPorProductoId(
            @PathVariable Long productoId
    ) {
        return service.buscarPorProductoId(productoId)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    // REGISTRAR ENTRADA O SALIDA
    @PostMapping("/{productoId}/ajustes")
    public ResponseEntity<Inventario> ajustarStock(
            @PathVariable Long productoId,
            @Valid @RequestBody AjusteStockRequest datos
    ) {
        return ResponseEntity.ok(
                service.ajustarStock(productoId, datos)
        );
    }

    // CREAR RESERVA
    @PostMapping("/reservas")
    public ResponseEntity<ReservaStock> reservar(
            @Valid @RequestBody ReservaStockRequest datos
    ) {
        return ResponseEntity.ok(
                service.reservar(datos)
        );
    }

    // CONSULTAR RESERVA
    @GetMapping("/reservas/{operacionId}")
    public ResponseEntity<ReservaStock> buscarReserva(
            @PathVariable String operacionId
    ) {
        return service.buscarReserva(operacionId)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    // CONFIRMAR RESERVA
    @PostMapping("/reservas/{operacionId}/confirmar")
    public ResponseEntity<ReservaStock> confirmar(
            @PathVariable String operacionId
    ) {
        return ResponseEntity.ok(
                service.confirmarReserva(operacionId)
        );
    }

    // CANCELAR RESERVA
    @PostMapping("/reservas/{operacionId}/cancelar")
    public ResponseEntity<ReservaStock> cancelar(
            @PathVariable String operacionId
    ) {
        return ResponseEntity.ok(
                service.cancelarReserva(operacionId)
        );
    }
}
