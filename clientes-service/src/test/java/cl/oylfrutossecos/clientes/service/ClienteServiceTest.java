package cl.oylfrutossecos.clientes.service;

import cl.oylfrutossecos.clientes.dto.ClienteResponse;
import cl.oylfrutossecos.clientes.dto.LoginRequest;
import cl.oylfrutossecos.clientes.dto.RegistroClienteRequest;

import cl.oylfrutossecos.clientes.model.Cliente;
import cl.oylfrutossecos.clientes.model.RolUsuario;
import cl.oylfrutossecos.clientes.repository.ClienteRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClienteService service;

    // DATOS FICTICIOS REUTILIZABLES

    private RegistroClienteRequest datosRegistro() {

        return new RegistroClienteRequest(
                "Cliente Prueba",
                "cliente_prueba",
                "cliente@example.com",
                "912345678",
                "Direccion de prueba",
                "ClavePrueba123!"
        );
    }

    private Cliente cliente() {

        Cliente cliente = new Cliente();

        cliente.setId(2L);
        cliente.setNombre("Cliente Prueba");
        cliente.setNombreUsuario("cliente_prueba");
        cliente.setCorreo("cliente@example.com");
        cliente.setTelefono("912345678");
        cliente.setDireccion("Direccion de prueba");
        cliente.setPasswordHash("HASH_FICTICIO");
        cliente.setRol(RolUsuario.CLIENTE);
        cliente.setActivo(true);

        return cliente;
    }

    // PRUEBA 1: Registro correcto

    @Test
    void registrarClienteCorrectamente() {

        when(passwordEncoder.encode("ClavePrueba123!"))
                .thenReturn("HASH_FICTICIO");

        when(repository.saveAndFlush(any(Cliente.class)))
                .thenAnswer(invocacion -> {

                    Cliente guardado = invocacion.getArgument(0);
                    guardado.setId(2L);

                    return guardado;
                });

        ClienteResponse resultado =
                service.registrar(datosRegistro());

        assertEquals(2L, resultado.id());
        assertEquals("cliente_prueba", resultado.nombreUsuario());
        assertEquals(RolUsuario.CLIENTE, resultado.rol());
        assertTrue(resultado.activo());

        verify(passwordEncoder).encode("ClavePrueba123!");

        verify(repository).saveAndFlush(
                argThat(cliente ->
                        "HASH_FICTICIO".equals(
                                cliente.getPasswordHash()
                        )
                                && cliente.getRol() == RolUsuario.CLIENTE
                                && Boolean.TRUE.equals(cliente.getActivo())
                )
        );
    }

    // PRUEBA 2: Nombre demasiado corto

    @Test
    void rechazarNombreInvalido() {

        RegistroClienteRequest datos =
                new RegistroClienteRequest(
                        " A ",
                        "cliente_prueba",
                        "cliente@example.com",
                        "912345678",
                        "",
                        "ClavePrueba123!"
                );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.registrar(datos)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        verifyNoInteractions(repository, passwordEncoder);
    }

    // PRUEBA 3: Telefono invalido

    @Test
    void rechazarTelefonoInvalido() {

        RegistroClienteRequest datos =
                new RegistroClienteRequest(
                        "Cliente Prueba",
                        "cliente_prueba",
                        "cliente@example.com",
                        "123",
                        "",
                        "ClavePrueba123!"
                );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.registrar(datos)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        verifyNoInteractions(repository, passwordEncoder);
    }

    // PRUEBA 4: Contrasena supera limite BCrypt

    @Test
    void rechazarContrasenaDemasiadoLarga() {

        RegistroClienteRequest datos =
                new RegistroClienteRequest(
                        "Cliente Prueba",
                        "cliente_prueba",
                        "cliente@example.com",
                        "912345678",
                        "",
                        "a".repeat(73)
                );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.registrar(datos)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        verifyNoInteractions(repository, passwordEncoder);
    }

    // PRUEBA 5: Usuario duplicado

    @Test
    void rechazarUsuarioDuplicado() {

        when(repository.existsByNombreUsuarioIgnoreCase(
                "cliente_prueba"
        )).thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.registrar(datosRegistro())
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());

        verifyNoInteractions(passwordEncoder);

        verify(repository, never())
                .saveAndFlush(any(Cliente.class));
    }

    // PRUEBA 6: Correo duplicado

    @Test
    void rechazarCorreoDuplicado() {

        when(repository.existsByCorreoIgnoreCase(
                "cliente@example.com"
        )).thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.registrar(datosRegistro())
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());

        verifyNoInteractions(passwordEncoder);

        verify(repository, never())
                .saveAndFlush(any(Cliente.class));
    }

    // PRUEBA 7: Conflicto en base de datos

    @Test
    void manejarConflictoDeBaseDeDatos() {

        when(passwordEncoder.encode("ClavePrueba123!"))
                .thenReturn("HASH_FICTICIO");

        when(repository.saveAndFlush(any(Cliente.class)))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "Registro duplicado"
                        )
                );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.registrar(datosRegistro())
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    // PRUEBA 8: Login correcto

    @Test
    void autenticarClienteCorrectamente() {

        when(repository.findByNombreUsuarioIgnoreCase(
                "cliente_prueba"
        )).thenReturn(Optional.of(cliente()));

        when(passwordEncoder.matches(
                "ClavePrueba123!",
                "HASH_FICTICIO"
        )).thenReturn(true);

        ClienteResponse resultado = service.autenticar(
                new LoginRequest(
                        "cliente_prueba",
                        "ClavePrueba123!"
                )
        );

        assertEquals(2L, resultado.id());
        assertEquals(RolUsuario.CLIENTE, resultado.rol());
        assertTrue(resultado.activo());
    }

    // PRUEBA 9: Contrasena incorrecta

    @Test
    void rechazarContrasenaIncorrecta() {

        when(repository.findByNombreUsuarioIgnoreCase(
                "cliente_prueba"
        )).thenReturn(Optional.of(cliente()));

        when(passwordEncoder.matches(
                "Incorrecta123",
                "HASH_FICTICIO"
        )).thenReturn(false);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.autenticar(
                        new LoginRequest(
                                "cliente_prueba",
                                "Incorrecta123"
                        )
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    // PRUEBA 10: Usuario inexistente

    @Test
    void rechazarUsuarioInexistente() {

        when(repository.findByNombreUsuarioIgnoreCase(
                "desconocido"
        )).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.autenticar(
                        new LoginRequest(
                                "desconocido",
                                "ClavePrueba123!"
                        )
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());

        verifyNoInteractions(passwordEncoder);
    }

    // PRUEBA 11: Cuenta inactiva

    @Test
    void rechazarCuentaInactiva() {

        Cliente cliente = cliente();
        cliente.setActivo(false);

        when(repository.findByNombreUsuarioIgnoreCase(
                "cliente_prueba"
        )).thenReturn(Optional.of(cliente));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.autenticar(
                        new LoginRequest(
                                "cliente_prueba",
                                "ClavePrueba123!"
                        )
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());

        verifyNoInteractions(passwordEncoder);
    }

    // PRUEBA 12: Contrasena demasiado larga en login

    @Test
    void rechazarContrasenaLargaEnLogin() {

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.autenticar(
                        new LoginRequest(
                                "cliente_prueba",
                                "a".repeat(73)
                        )
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());

        verifyNoInteractions(repository, passwordEncoder);
    }
}