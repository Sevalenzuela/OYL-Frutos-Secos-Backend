package cl.oylfrutossecos.clientes.controller;

import cl.oylfrutossecos.clientes.dto.ClienteResponse;
import cl.oylfrutossecos.clientes.model.RolUsuario;
import cl.oylfrutossecos.clientes.service.PerfilService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PerfilService perfilService;

    public AdminController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    // GET /api/admin/acceso
    @GetMapping("/acceso")
    public ResponseEntity<Map<String, Object>> verificarAcceso(
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long clienteId = Long.valueOf(jwt.getSubject());

        ClienteResponse cliente =
                perfilService.consultarPerfil(clienteId);

        // Verificar tambien el rol actual de la cuenta.
        if (cliente.rol() != RolUsuario.ADMINISTRADOR) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acceso administrativo denegado"
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "autorizado", true,
                        "clienteId", cliente.id(),
                        "nombre", cliente.nombre(),
                        "rol", cliente.rol().name()
                )
        );
    }
}