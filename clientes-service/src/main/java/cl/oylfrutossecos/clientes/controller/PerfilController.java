package cl.oylfrutossecos.clientes.controller;

import cl.oylfrutossecos.clientes.dto.ActualizarPerfilRequest;
import cl.oylfrutossecos.clientes.dto.ClienteResponse;

import cl.oylfrutossecos.clientes.service.PerfilService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
public class PerfilController {

    private final PerfilService service;

    public PerfilController(PerfilService service) {
        this.service = service;
    }

    // GET /api/clientes/me
    @GetMapping("/me")
    public ResponseEntity<ClienteResponse> consultarPerfil(
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long clienteId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(
                service.consultarPerfil(clienteId)
        );
    }

    // PUT /api/clientes/me
    @PutMapping("/me")
    public ResponseEntity<ClienteResponse> actualizarPerfil(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ActualizarPerfilRequest datos
    ) {

        Long clienteId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(
                service.actualizarPerfil(clienteId, datos)
        );
    }
}