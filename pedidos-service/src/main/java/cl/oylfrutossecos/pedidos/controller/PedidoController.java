package cl.oylfrutossecos.pedidos.controller;

import cl.oylfrutossecos.pedidos.dto.CambiarEstadoRequest;
import cl.oylfrutossecos.pedidos.dto.PedidoResponse;

import cl.oylfrutossecos.pedidos.service.PedidoService;
import cl.oylfrutossecos.pedidos.service.GestionEstadoPedidoService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService service;
    private final GestionEstadoPedidoService gestionEstados;

    public PedidoController(
            PedidoService service,
            GestionEstadoPedidoService gestionEstados
    ) {

        this.service = service;
        this.gestionEstados = gestionEstados;
    }

    // 1. LISTAR TODOS LOS PEDIDOS

    @GetMapping
    public List<PedidoResponse> listar() {

        return service.listarPedidos();
    }

    // 2. BUSCAR PEDIDO POR NUMERO

    @GetMapping("/{numero}")
    public ResponseEntity<PedidoResponse> buscarPorNumero(
            @PathVariable String numero
    ) {

        return ResponseEntity.ok(
                service.buscarPorNumero(numero)
        );
    }

    // 3. CAMBIAR ESTADO DE UN PEDIDO

    @PatchMapping("/{numero}/estado")
    public ResponseEntity<PedidoResponse> cambiarEstado(
            @PathVariable String numero,
            @Valid @RequestBody CambiarEstadoRequest datos
    ) {

        return ResponseEntity.ok(
                gestionEstados.cambiarEstado(
                        numero,
                        datos.estado()
                )
        );
    }
}