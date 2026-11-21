package cl.oylfrutossecos.pedidos.controller;

import cl.oylfrutossecos.pedidos.client.InventarioClient;
import cl.oylfrutossecos.pedidos.client.InventarioRemoto;
import cl.oylfrutossecos.pedidos.client.ReservaRemota;
import cl.oylfrutossecos.pedidos.client.ReservaInventarioRequest;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integracion/inventario")
public class IntegracionInventarioController {

    private final InventarioClient inventarioClient;

    public IntegracionInventarioController(
            InventarioClient inventarioClient
    ) {

        this.inventarioClient = inventarioClient;
    }

    // 1. CONSULTAR INVENTARIO

    @GetMapping("/{productoId}")
    public ResponseEntity<InventarioRemoto> consultar(
            @PathVariable Long productoId
    ) {

        return ResponseEntity.ok(
                inventarioClient.consultarInventario(productoId)
        );
    }

    // 2. CREAR RESERVA

    @PostMapping("/reservas")
    public ResponseEntity<ReservaRemota> reservar(
            @Valid @RequestBody ReservaInventarioRequest datos
    ) {

        return ResponseEntity.ok(
                inventarioClient.reservar(
                        datos.operacionId(),
                        datos.productoId(),
                        datos.cantidad()
                )
        );
    }

    // 3. CONSULTAR RESERVA

    @GetMapping("/reservas/{operacionId}")
    public ResponseEntity<ReservaRemota> consultarReserva(
            @PathVariable String operacionId
    ) {

        return ResponseEntity.ok(
                inventarioClient.consultarReserva(operacionId)
        );
    }

    // 4. CONFIRMAR RESERVA

    @PostMapping("/reservas/{operacionId}/confirmar")
    public ResponseEntity<ReservaRemota> confirmar(
            @PathVariable String operacionId
    ) {

        return ResponseEntity.ok(
                inventarioClient.confirmarReserva(operacionId)
        );
    }

    // 5. CANCELAR RESERVA

    @PostMapping("/reservas/{operacionId}/cancelar")
    public ResponseEntity<ReservaRemota> cancelar(
            @PathVariable String operacionId
    ) {

        return ResponseEntity.ok(
                inventarioClient.cancelarReserva(operacionId)
        );
    }
}