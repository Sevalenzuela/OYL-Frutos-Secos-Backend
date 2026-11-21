package cl.oylfrutossecos.pedidos.controller;

import cl.oylfrutossecos.pedidos.client.ClienteRemoto;
import cl.oylfrutossecos.pedidos.client.ClientesClient;

import cl.oylfrutossecos.pedidos.dto.CambiarEstadoRequest;
import cl.oylfrutossecos.pedidos.dto.CrearPedidoRequest;
import cl.oylfrutossecos.pedidos.dto.PedidoResponse;

import cl.oylfrutossecos.pedidos.service.PedidoService;
import cl.oylfrutossecos.pedidos.service.GestionEstadoPedidoService;
import cl.oylfrutossecos.pedidos.service.RegistroCompraService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService service;
    private final GestionEstadoPedidoService gestionEstados;
    private final ClientesClient clientesClient;
    private final RegistroCompraService registroCompra;

    public PedidoController(
            PedidoService service,
            GestionEstadoPedidoService gestionEstados,
            ClientesClient clientesClient,
            RegistroCompraService registroCompra
    ) {

        this.service = service;
        this.gestionEstados = gestionEstados;
        this.clientesClient = clientesClient;
        this.registroCompra = registroCompra;
    }

    // 1. LISTAR TODOS LOS PEDIDOS

    @GetMapping
    public List<PedidoResponse> listar(
            @AuthenticationPrincipal Jwt jwt
    ) {

        comprobarAdministrador(jwt);

        return service.listarPedidos();
    }

    // 2. CONSULTAR HISTORIAL PROPIO

    @GetMapping("/mis-pedidos")
    public List<PedidoResponse> misPedidos(
            @AuthenticationPrincipal Jwt jwt
    ) {

        ClienteRemoto cliente =
                clientesClient.consultarPerfil(jwt);

        return service.buscarPorCliente(
                cliente.id()
        );
    }

    // 3. BUSCAR PEDIDO POR NUMERO

    @GetMapping("/{numero}")
    public ResponseEntity<PedidoResponse> buscarPorNumero(
            @PathVariable String numero,
            @AuthenticationPrincipal Jwt jwt
    ) {

        ClienteRemoto cliente =
                clientesClient.consultarPerfil(jwt);

        PedidoResponse pedido =
                service.buscarPorNumero(numero);

        boolean administrador =
                "ADMINISTRADOR".equals(cliente.rol());

        boolean propietario =
                cliente.id().equals(pedido.clienteId());

        if (!administrador && !propietario) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Pedido no encontrado"
            );
        }

        return ResponseEntity.ok(pedido);
    }

    // 4. REGISTRAR UNA COMPRA

    @PostMapping
    public ResponseEntity<PedidoResponse> comprar(
            @Valid @RequestBody CrearPedidoRequest datos,
            @AuthenticationPrincipal Jwt jwt
    ) {

        PedidoResponse pedido =
                registroCompra.comprar(jwt, datos);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(pedido);
    }

    // 5. CAMBIAR ESTADO

    @PatchMapping("/{numero}/estado")
    public ResponseEntity<PedidoResponse> cambiarEstado(
            @PathVariable String numero,
            @Valid @RequestBody CambiarEstadoRequest datos,
            @AuthenticationPrincipal Jwt jwt
    ) {

        comprobarAdministrador(jwt);

        return ResponseEntity.ok(
                gestionEstados.cambiarEstado(
                        numero,
                        datos.estado()
                )
        );
    }

    // 6. COMPROBAR PERMISOS ADMINISTRATIVOS

    private void comprobarAdministrador(Jwt jwt) {

        ClienteRemoto cliente =
                clientesClient.consultarPerfil(jwt);

        if (!"ADMINISTRADOR".equals(cliente.rol())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acceso administrativo denegado"
            );
        }
    }
}