package cl.oylfrutossecos.clientes.controller;

import cl.oylfrutossecos.clientes.dto.ClienteResponse;
import cl.oylfrutossecos.clientes.dto.RegistroClienteRequest;
import cl.oylfrutossecos.clientes.dto.LoginRequest;
import cl.oylfrutossecos.clientes.dto.LoginResponse;

import cl.oylfrutossecos.clientes.service.ClienteService;
import cl.oylfrutossecos.clientes.service.JwtService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ClienteService service;
    private final JwtService jwtService;

    public AuthController(
            ClienteService service,
            JwtService jwtService
    ) {
        this.service = service;
        this.jwtService = jwtService;
    }

    // 1. REGISTRAR CLIENTE
    @PostMapping("/registro")
    public ResponseEntity<ClienteResponse> registrar(
            @Valid @RequestBody RegistroClienteRequest datos
    ) {

        ClienteResponse cliente = service.registrar(datos);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cliente);
    }

    // 2. INICIAR SESION Y GENERAR JWT
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest datos
    ) {

        // Verificar las credenciales.
        ClienteResponse cliente =
                service.autenticar(datos);

        // Generar token firmado.
        String token =
                jwtService.generarToken(cliente);

        LoginResponse respuesta = new LoginResponse(
                token,
                "Bearer",
                jwtService.obtenerDuracionSegundos(),
                cliente
        );

        return ResponseEntity.ok(respuesta);
    }

    // 3. CONSULTAR SESION AUTENTICADA
    @GetMapping("/sesion")
    public ResponseEntity<Map<String, Object>> consultarSesion(
            @AuthenticationPrincipal Jwt jwt
    ) {

        return ResponseEntity.ok(
                Map.of(
                        "autenticado", true,
                        "clienteId", Long.valueOf(jwt.getSubject()),
                        "rol", jwt.getClaimAsString("rol")
                )
        );
    }
}