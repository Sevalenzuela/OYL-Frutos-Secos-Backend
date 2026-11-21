package cl.oylfrutossecos.clientes.service.integracion;

import cl.oylfrutossecos.clientes.model.Cliente;
import cl.oylfrutossecos.clientes.repository.ClienteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.address=127.0.0.1",
                "spring.datasource.url=jdbc:h2:mem:seguridad_test;DB_CLOSE_DELAY=-1",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.h2.console.enabled=false",
                "oyl.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "oyl.admin.bootstrap=true",
                "oyl.admin.password=AdminTest12345!"
        }
)
class SeguridadIntegracionTest {

    @LocalServerPort
    private int puerto;

    @Autowired
    private ClienteRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RestClient clienteHttp;

    private static final String CLAVE_CLIENTE =
            "ClienteTest123!";

    private static final String CLAVE_ADMIN =
            "AdminTest12345!";

    @BeforeEach
    void configurarClienteHttp() {

        clienteHttp = RestClient.builder()
                .baseUrl("http://127.0.0.1:" + puerto)
                .build();
    }

    // METODOS AUXILIARES

    private String generarUsuario() {

        return "test_" + UUID.randomUUID()
                .toString()
                .substring(0, 8);
    }

    private ResponseEntity<Map> registrar(String usuario) {

        return clienteHttp.post()
                .uri("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "nombre", "Cliente Integracion",
                        "nombreUsuario", usuario,
                        "correo", usuario + "@example.com",
                        "telefono", "912345678",
                        "direccion", "Direccion de prueba",
                        "contrasena", CLAVE_CLIENTE
                ))
                .retrieve()
                .toEntity(Map.class);
    }

    private String registrarClientePrueba() {

        String usuario = generarUsuario();

        ResponseEntity<Map> respuesta =
                registrar(usuario);

        assertEquals(
                HttpStatus.CREATED,
                respuesta.getStatusCode()
        );

        return usuario;
    }

    private Map<?, ?> iniciarSesion(
            String usuario,
            String contrasena
    ) {

        ResponseEntity<Map> respuesta =
                clienteHttp.post()
                        .uri("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "nombreUsuario", usuario,
                                "contrasena", contrasena
                        ))
                        .retrieve()
                        .toEntity(Map.class);

        assertEquals(
                HttpStatus.OK,
                respuesta.getStatusCode()
        );

        assertNotNull(respuesta.getBody());

        return respuesta.getBody();
    }

    private String obtenerToken(
            String usuario,
            String contrasena
    ) {

        Map<?, ?> respuesta =
                iniciarSesion(usuario, contrasena);

        String token = (String) respuesta.get("token");

        assertNotNull(token);
        assertFalse(token.isBlank());

        return token;
    }

    // PRUEBA 1: Registro real y BCrypt

    @Test
    void registrarClienteConContrasenaProtegida() {

        String usuario = generarUsuario();

        ResponseEntity<Map> respuesta =
                registrar(usuario);

        assertEquals(
                HttpStatus.CREATED,
                respuesta.getStatusCode()
        );

        Map<?, ?> cuerpo = respuesta.getBody();

        assertNotNull(cuerpo);

        assertEquals(usuario, cuerpo.get("nombreUsuario"));
        assertEquals("CLIENTE", cuerpo.get("rol"));
        assertEquals(true, cuerpo.get("activo"));

        // La respuesta no debe exponer credenciales.
        assertFalse(cuerpo.containsKey("contrasena"));
        assertFalse(cuerpo.containsKey("passwordHash"));

        // Consultar el registro real de H2.
        Cliente guardado = repository
                .findByNombreUsuarioIgnoreCase(usuario)
                .orElseThrow();

        String hash = guardado.getPasswordHash();

        // Comprobar que no se guardo texto plano.
        assertNotEquals(CLAVE_CLIENTE, hash);

        // Comprobar el formato de BCrypt.
        assertEquals(60, hash.length());
        assertTrue(hash.startsWith("$2a$12$"));

        // Verificar la contrasena utilizando BCrypt.
        assertTrue(
                passwordEncoder.matches(
                        CLAVE_CLIENTE,
                        hash
                )
        );
    }

    // PRUEBA 2: Login y perfil con JWT valido

    @Test
    void iniciarSesionYConsultarPerfil() {

        String usuario = registrarClientePrueba();

        Map<?, ?> login =
                iniciarSesion(usuario, CLAVE_CLIENTE);

        String token = (String) login.get("token");

        assertNotNull(token);
        assertFalse(token.isBlank());

        assertEquals("Bearer", login.get("tipo"));

        assertEquals(
                1800L,
                ((Number) login.get("expiraEn")).longValue()
        );

        // Acceder al perfil mediante el token.
        ResponseEntity<Map> respuesta =
                clienteHttp.get()
                        .uri("/api/clientes/me")
                        .headers(headers ->
                                headers.setBearerAuth(token)
                        )
                        .retrieve()
                        .toEntity(Map.class);

        assertEquals(
                HttpStatus.OK,
                respuesta.getStatusCode()
        );

        Map<?, ?> perfil = respuesta.getBody();

        assertNotNull(perfil);
        assertEquals(usuario, perfil.get("nombreUsuario"));
        assertEquals("CLIENTE", perfil.get("rol"));

        assertFalse(perfil.containsKey("passwordHash"));
    }

    // PRUEBA 3: Contrasena incorrecta

    @Test
    void rechazarContrasenaIncorrecta() {

        String usuario = registrarClientePrueba();

        RestClientResponseException error = assertThrows(
                RestClientResponseException.class,
                () -> clienteHttp.post()
                        .uri("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "nombreUsuario", usuario,
                                "contrasena", "Incorrecta123!"
                        ))
                        .retrieve()
                        .toEntity(Map.class)
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                error.getStatusCode()
        );
    }

    // PRUEBA 4: Acceso sin token

    @Test
    void rechazarSolicitudSinToken() {

        RestClientResponseException error = assertThrows(
                RestClientResponseException.class,
                () -> clienteHttp.get()
                        .uri("/api/clientes/me")
                        .retrieve()
                        .toEntity(Map.class)
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                error.getStatusCode()
        );
    }

    // PRUEBA 5: Acceso con token falso

    @Test
    void rechazarTokenFalso() {

        RestClientResponseException error = assertThrows(
                RestClientResponseException.class,
                () -> clienteHttp.get()
                        .uri("/api/clientes/me")
                        .headers(headers ->
                                headers.setBearerAuth("token-falso")
                        )
                        .retrieve()
                        .toEntity(Map.class)
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                error.getStatusCode()
        );
    }

    // PRUEBA 6: Cliente sin permisos administrativos

    @Test
    void impedirAccesoAdministrativoACliente() {

        String usuario = registrarClientePrueba();

        String token =
                obtenerToken(usuario, CLAVE_CLIENTE);

        RestClientResponseException error = assertThrows(
                RestClientResponseException.class,
                () -> clienteHttp.get()
                        .uri("/api/admin/acceso")
                        .headers(headers ->
                                headers.setBearerAuth(token)
                        )
                        .retrieve()
                        .toEntity(Map.class)
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                error.getStatusCode()
        );
    }

    // PRUEBA 7: Administrador autorizado

    @Test
    void permitirAccesoAdministrativo() {

        String token = obtenerToken(
                "admin_oyl",
                CLAVE_ADMIN
        );

        ResponseEntity<Map> respuesta =
                clienteHttp.get()
                        .uri("/api/admin/acceso")
                        .headers(headers ->
                                headers.setBearerAuth(token)
                        )
                        .retrieve()
                        .toEntity(Map.class);

        assertEquals(
                HttpStatus.OK,
                respuesta.getStatusCode()
        );

        Map<?, ?> cuerpo = respuesta.getBody();

        assertNotNull(cuerpo);

        assertEquals(true, cuerpo.get("autorizado"));

        assertEquals(
                "ADMINISTRADOR",
                cuerpo.get("rol")
        );
    }
}