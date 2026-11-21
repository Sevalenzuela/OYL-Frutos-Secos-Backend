package cl.oylfrutossecos.inventario.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;

import java.time.Duration;
import java.time.Instant;

import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(

        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,

        properties = {

                "spring.datasource.url=jdbc:h2:mem:inventario_seguridad;DB_CLOSE_DELAY=-1",

                "spring.jpa.hibernate.ddl-auto=create-drop",

                "oyl.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",

                "oyl.inventario.secret=YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk="

        }
)
class SeguridadInventarioTest {

    @LocalServerPort
    private int puerto;

    private final HttpClient http =
            HttpClient.newHttpClient();

    private static final String CLAVE_JWT =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private static final String CLAVE_INTERNA =
            "YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk=";

    // GENERAR TOKEN JWT DE PRUEBA

    private String token(String rol)
            throws Exception {

        Instant ahora = Instant.now();

        JWTClaimsSet claims =
                new JWTClaimsSet.Builder()

                        .issuer("oyl-clientes")
                        .subject("2")
                        .claim("rol", rol)
                        .issueTime(Date.from(ahora))

                        .expirationTime(
                                Date.from(ahora.plusSeconds(600))
                        )

                        .build();

        SignedJWT jwt = new SignedJWT(

                new JWSHeader(JWSAlgorithm.HS256),

                claims
        );

        jwt.sign(
                new MACSigner(
                        Base64.getDecoder().decode(CLAVE_JWT)
                )
        );

        return "Bearer " + jwt.serialize();
    }

    // GENERAR CREDENCIAL INTERNA

    private String credencialInterna(String clave) {

        String credencial =
                "pedidos-service:" + clave;

        return "Basic " +

                Base64.getEncoder().encodeToString(
                        credencial.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
    }

    // ENVIAR SOLICITUD HTTP

    private HttpResponse<String> solicitud(
            String metodo,
            String ruta,
            String autorizacion,
            String cuerpo
    ) throws Exception {

        var builder = HttpRequest.newBuilder()

                .uri(
                        URI.create(
                                "http://127.0.0.1:"
                                        + puerto
                                        + "/api/inventario"
                                        + ruta
                        )
                )

                .timeout(Duration.ofSeconds(10))

                .header(
                        "Content-Type",
                        "application/json"
                )

                .method(
                        metodo,

                        cuerpo == null

                                ? HttpRequest.BodyPublishers.noBody()

                                : HttpRequest.BodyPublishers.ofString(
                                cuerpo
                        )
                );

        if (autorizacion != null) {

            builder.header(
                    "Authorization",
                    autorizacion
            );
        }

        return http.send(

                builder.build(),

                HttpResponse.BodyHandlers.ofString()
        );
    }

    // CREAR JSON DE UNA RESERVA

    private String datosReserva(String operacionId) {

        return """
            {
                "operacionId": "%s",
                "productoId": 1,
                "cantidad": 1
            }
            """.formatted(operacionId);
    }

    // PRUEBA 1: CONSULTA PUBLICA

    @Test
    void consultarExistenciasSinToken()
            throws Exception {

        var respuesta = solicitud(
                "GET",
                "/1",
                null,
                null
        );

        assertEquals(
                200,
                respuesta.statusCode()
        );
    }

    // PRUEBA 2: IMPEDIR AJUSTES SIN TOKEN

    @Test
    void rechazarAjusteSinToken()
            throws Exception {

        var respuesta = solicitud(

                "POST",
                "/1/ajustes",
                null,

                "{\"tipo\":\"ENTRADA\",\"cantidad\":1}"
        );

        assertEquals(
                401,
                respuesta.statusCode()
        );
    }

    // PRUEBA 3: IMPEDIR AJUSTES POR CLIENTE

    @Test
    void rechazarAjustePorCliente()
            throws Exception {

        var respuesta = solicitud(

                "POST",
                "/1/ajustes",
                token("CLIENTE"),

                "{\"tipo\":\"ENTRADA\",\"cantidad\":1}"
        );

        assertEquals(
                403,
                respuesta.statusCode()
        );
    }

    // PRUEBA 4: PERMITIR AJUSTES ADMINISTRATIVOS

    @Test
    void permitirAjustePorAdministrador()
            throws Exception {

        String autorizacion =
                token("ADMINISTRADOR");

        var entrada = solicitud(

                "POST",
                "/1/ajustes",
                autorizacion,

                "{\"tipo\":\"ENTRADA\",\"cantidad\":1}"
        );

        assertEquals(200, entrada.statusCode());

        var salida = solicitud(

                "POST",
                "/1/ajustes",
                autorizacion,

                "{\"tipo\":\"SALIDA\",\"cantidad\":1}"
        );

        assertEquals(200, salida.statusCode());
    }

    // PRUEBA 5: IMPEDIR RESERVAS ANONIMAS

    @Test
    void rechazarReservaSinCredencial()
            throws Exception {

        var respuesta = solicitud(

                "POST",
                "/reservas",
                null,

                datosReserva(
                        UUID.randomUUID().toString()
                )
        );

        assertEquals(
                401,
                respuesta.statusCode()
        );
    }

    // PRUEBA 6: IMPEDIR RESERVAS CON JWT DE USUARIO

    @Test
    void rechazarReservasDirectasConJwt()
            throws Exception {

        String datos = datosReserva(
                UUID.randomUUID().toString()
        );

        var cliente = solicitud(

                "POST",
                "/reservas",
                token("CLIENTE"),
                datos
        );

        var administrador = solicitud(

                "POST",
                "/reservas",
                token("ADMINISTRADOR"),
                datos
        );

        assertEquals(401, cliente.statusCode());

        assertEquals(
                401,
                administrador.statusCode()
        );
    }

    // PRUEBA 7: RECHAZAR CREDENCIAL INCORRECTA

    @Test
    void rechazarCredencialInternaIncorrecta()
            throws Exception {

        var respuesta = solicitud(

                "POST",
                "/reservas",

                credencialInterna("clave-incorrecta"),

                datosReserva(
                        UUID.randomUUID().toString()
                )
        );

        assertEquals(
                401,
                respuesta.statusCode()
        );
    }

    // PRUEBA 8: RESERVAR Y CANCELAR INTERNAMENTE

    @Test
    void permitirReservaYCancelacionInterna()
            throws Exception {

        String operacionId =
                UUID.randomUUID().toString();

        String autorizacion =
                credencialInterna(CLAVE_INTERNA);

        var reserva = solicitud(

                "POST",
                "/reservas",
                autorizacion,
                datosReserva(operacionId)
        );

        assertEquals(200, reserva.statusCode());

        assertTrue(
                reserva.body().contains("RESERVADA")
        );

        var consultaPublica = solicitud(

                "GET",
                "/reservas/" + operacionId,
                null,
                null
        );

        assertEquals(
                401,
                consultaPublica.statusCode()
        );

        var consultaInterna = solicitud(

                "GET",
                "/reservas/" + operacionId,
                autorizacion,
                null
        );

        assertEquals(
                200,
                consultaInterna.statusCode()
        );

        var cancelacion = solicitud(

                "POST",
                "/reservas/" + operacionId + "/cancelar",
                autorizacion,
                null
        );

        assertEquals(
                200,
                cancelacion.statusCode()
        );

        assertTrue(
                cancelacion.body().contains("CANCELADA")
        );
    }

    // PRUEBA 9: CONFIRMAR INTERNAMENTE

    @Test
    void permitirConfirmacionInterna()
            throws Exception {

        String operacionId =
                UUID.randomUUID().toString();

        String autorizacion =
                credencialInterna(CLAVE_INTERNA);

        var reserva = solicitud(

                "POST",
                "/reservas",
                autorizacion,
                datosReserva(operacionId)
        );

        assertEquals(200, reserva.statusCode());

        var confirmacion = solicitud(

                "POST",
                "/reservas/" + operacionId + "/confirmar",
                autorizacion,
                null
        );

        assertEquals(
                200,
                confirmacion.statusCode()
        );

        assertTrue(
                confirmacion.body().contains("CONFIRMADA")
        );
    }
}