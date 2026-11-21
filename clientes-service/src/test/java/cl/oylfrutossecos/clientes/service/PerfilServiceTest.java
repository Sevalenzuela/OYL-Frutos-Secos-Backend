package cl.oylfrutossecos.clientes.service;

import cl.oylfrutossecos.clientes.dto.ActualizarPerfilRequest;
import cl.oylfrutossecos.clientes.dto.ClienteResponse;

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
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    @Mock
    private ClienteRepository repository;

    @InjectMocks
    private PerfilService service;

    // DATOS FICTICIOS REUTILIZABLES

    private Cliente cliente() {

        Cliente cliente = new Cliente();

        cliente.setId(2L);
        cliente.setNombre("Cliente Prueba");
        cliente.setNombreUsuario("cliente_prueba");
        cliente.setCorreo("cliente@example.com");
        cliente.setTelefono("912345678");
        cliente.setDireccion("Direccion original");
        cliente.setPasswordHash("HASH_FICTICIO");
        cliente.setRol(RolUsuario.CLIENTE);
        cliente.setActivo(true);

        return cliente;
    }

    private ActualizarPerfilRequest datosPerfil() {

        return new ActualizarPerfilRequest(
                "Cliente Actualizado",
                "cliente_prueba",
                "cliente@example.com",
                "987654321",
                "Nueva direccion"
        );
    }

    private void simularGuardado() {

        when(repository.saveAndFlush(any(Cliente.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0)
                );
    }

    // PRUEBA 1: Consultar perfil existente

    @Test
    void consultarPerfilCorrectamente() {

        when(repository.findById(2L))
                .thenReturn(Optional.of(cliente()));

        ClienteResponse resultado =
                service.consultarPerfil(2L);

        assertEquals(2L, resultado.id());
        assertEquals("Cliente Prueba", resultado.nombre());
        assertEquals("cliente_prueba", resultado.nombreUsuario());
        assertEquals(RolUsuario.CLIENTE, resultado.rol());
    }

    // PRUEBA 2: Cuenta inexistente

    @Test
    void rechazarPerfilInexistente() {

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.consultarPerfil(999L)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    // PRUEBA 3: Cuenta inactiva

    @Test
    void rechazarPerfilInactivo() {

        Cliente cliente = cliente();
        cliente.setActivo(false);

        when(repository.findById(2L))
                .thenReturn(Optional.of(cliente));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.consultarPerfil(2L)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    // PRUEBA 4: Actualizar datos personales

    @Test
    void actualizarPerfilCorrectamente() {

        Cliente cliente = cliente();

        when(repository.findById(2L))
                .thenReturn(Optional.of(cliente));

        simularGuardado();

        ClienteResponse resultado = service.actualizarPerfil(
                2L,
                datosPerfil()
        );

        assertEquals("Cliente Actualizado", resultado.nombre());
        assertEquals("987654321", resultado.telefono());
        assertEquals("Nueva direccion", resultado.direccion());

        // No modificar campos protegidos.
        assertEquals("HASH_FICTICIO", cliente.getPasswordHash());
        assertEquals(RolUsuario.CLIENTE, cliente.getRol());
        assertTrue(cliente.getActivo());

        verify(repository).saveAndFlush(cliente);
    }

    // PRUEBA 5: Cambiar usuario y correo

    @Test
    void cambiarUsuarioYCorreoCorrectamente() {

        Cliente cliente = cliente();

        when(repository.findById(2L))
                .thenReturn(Optional.of(cliente));

        simularGuardado();

        ActualizarPerfilRequest datos =
                new ActualizarPerfilRequest(
                        "Cliente Actualizado",
                        "NUEVO_USUARIO",
                        "NUEVO@EXAMPLE.COM",
                        "987654321",
                        null
                );

        ClienteResponse resultado =
                service.actualizarPerfil(2L, datos);

        assertEquals("nuevo_usuario", resultado.nombreUsuario());
        assertEquals("nuevo@example.com", resultado.correo());
        assertEquals("", resultado.direccion());
    }

    // PRUEBA 6: Usuario duplicado

    @Test
    void rechazarUsuarioDuplicado() {

        when(repository.findById(2L))
                .thenReturn(Optional.of(cliente()));

        when(repository.existsByNombreUsuarioIgnoreCase(
                "usuario_ocupado"
        )).thenReturn(true);

        ActualizarPerfilRequest datos =
                new ActualizarPerfilRequest(
                        "Cliente Actualizado",
                        "usuario_ocupado",
                        "cliente@example.com",
                        "987654321",
                        ""
                );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.actualizarPerfil(2L, datos)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());

        verify(repository, never())
                .saveAndFlush(any(Cliente.class));
    }

    // PRUEBA 7: Correo duplicado

    @Test
    void rechazarCorreoDuplicado() {

        when(repository.findById(2L))
                .thenReturn(Optional.of(cliente()));

        when(repository.existsByCorreoIgnoreCase(
                "ocupado@example.com"
        )).thenReturn(true);

        ActualizarPerfilRequest datos =
                new ActualizarPerfilRequest(
                        "Cliente Actualizado",
                        "cliente_prueba",
                        "ocupado@example.com",
                        "987654321",
                        ""
                );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.actualizarPerfil(2L, datos)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());

        verify(repository, never())
                .saveAndFlush(any(Cliente.class));
    }

    // PRUEBA 8: Nombre invalido

    @Test
    void rechazarNombreInvalido() {

        when(repository.findById(2L))
                .thenReturn(Optional.of(cliente()));

        ActualizarPerfilRequest datos =
                new ActualizarPerfilRequest(
                        " A ",
                        "cliente_prueba",
                        "cliente@example.com",
                        "987654321",
                        ""
                );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.actualizarPerfil(2L, datos)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        verify(repository, never())
                .saveAndFlush(any(Cliente.class));
    }

    // PRUEBA 9: Telefono invalido

    @Test
    void rechazarTelefonoInvalido() {

        when(repository.findById(2L))
                .thenReturn(Optional.of(cliente()));

        ActualizarPerfilRequest datos =
                new ActualizarPerfilRequest(
                        "Cliente Actualizado",
                        "cliente_prueba",
                        "cliente@example.com",
                        "123",
                        ""
                );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.actualizarPerfil(2L, datos)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        verify(repository, never())
                .saveAndFlush(any(Cliente.class));
    }

    // PRUEBA 10: Conflicto en base de datos

    @Test
    void manejarConflictoAlActualizar() {

        when(repository.findById(2L))
                .thenReturn(Optional.of(cliente()));

        when(repository.saveAndFlush(any(Cliente.class)))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "Conflicto de datos"
                        )
                );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.actualizarPerfil(2L, datosPerfil())
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }
}