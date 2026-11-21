package cl.oylfrutossecos.productos.config;

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

import java.time.Instant;

import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(

        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,

        properties = {

                "spring.datasource.url=jdbc:h2:mem:productos_seguridad;DB_CLOSE_DELAY=-1",

                "spring.jpa.hibernate.ddl-auto=create-drop",

                "oyl.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="

        }
)
class SeguridadProductosTest {

    @LocalServerPort
    private int puerto;

    private final HttpClient clienteHttp =
            HttpClient.newHttpClient();

    private static final String CLAVE_TEST =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    // 1. CONSTRUIR URL

    private String url(String ruta) {

        return "http://127.0.0.1:"
                + puerto
                + "/api/productos"
                + ruta;
    }

    // 2. GENERAR TOKENS FICTICIOS PARA TESTING

    private String generarToken(
            String rol,
            long segundosExpiracion,
            byte[] clave
    ) throws Exception {

        Instant ahora = Instant.now();

        Instant expiracion =
                ahora.plusSeconds(segundosExpiracion);

        Instant emision =
                segundosExpiracion > 0
                        ? ahora
                        : expiracion.minusSeconds(60);

        JWTClaimsSet claims =
                new JWTClaimsSet.Builder()

                        .issuer("oyl-clientes")

                        .subject("2")

                        .claim("rol", rol)

                        .issueTime(Date.from(emision))

                        .expirationTime(Date.from(expiracion))

                        .build();

        SignedJWT jwt = new SignedJWT(

                new JWSHeader(JWSAlgorithm.HS256),

                claims
        );

        jwt.sign(new MACSigner(clave));

        return jwt.serialize();
    }

    private String tokenValido(String rol)
            throws Exception {

        return generarToken(
                rol,
                600,
                Base64.getDecoder().decode(CLAVE_TEST)
        );
    }

    // 3. ENVIAR SOLICITUD PATCH

    private HttpResponse<String> cambiarEstado(
            String token
    ) throws Exception {

        var solicitud = HttpRequest.newBuilder()

                .uri(URI.create(url("/1/estado")))

                .header(
                        "Content-Type",
                        "application/json"
                )

                .method(
                        "PATCH",
                        HttpRequest.BodyPublishers.ofString(
                                "{\"activo\":true}"
                        )
                );

        if (token != null) {

            solicitud.header(
                    "Authorization",
                    "Bearer " + token
            );
        }

        return clienteHttp.send(

                solicitud.build(),

                HttpResponse.BodyHandlers.ofString()
        );
    }

    // PRUEBA 1: CONSULTAR CATALOGO PUBLICO

    @Test
    void consultarCatalogoSinToken()
            throws Exception {

        HttpRequest solicitud =
                HttpRequest.newBuilder()

                        .uri(URI.create(url("")))

                        .GET()

                        .build();

        var respuesta = clienteHttp.send(

                solicitud,

                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(
                200,
                respuesta.statusCode()
        );
    }

    // PRUEBA 2: RECHAZAR MODIFICACION SIN TOKEN

    @Test
    void rechazarModificacionSinToken()
            throws Exception {

        var respuesta = cambiarEstado(null);

        assertEquals(
                401,
                respuesta.statusCode()
        );
    }

    // PRUEBA 3: RECHAZAR MODIFICACION POR CLIENTE

    @Test
    void rechazarModificacionPorCliente()
            throws Exception {

        String token = tokenValido("CLIENTE");

        var respuesta = cambiarEstado(token);

        assertEquals(
                403,
                respuesta.statusCode()
        );
    }

    // PRUEBA 4: PERMITIR MODIFICACION ADMINISTRATIVA

    @Test
    void permitirModificacionPorAdministrador()
            throws Exception {

        String token =
                tokenValido("ADMINISTRADOR");

        var respuesta = cambiarEstado(token);

        assertEquals(
                200,
                respuesta.statusCode()
        );
    }

    // PRUEBA 5: RECHAZAR TOKEN CON FIRMA INCORRECTA

    @Test
    void rechazarTokenConFirmaIncorrecta()
            throws Exception {

        byte[] claveIncorrecta = new byte[32];

        String token = generarToken(

                "ADMINISTRADOR",

                600,

                claveIncorrecta
        );

        var respuesta = cambiarEstado(token);

        assertEquals(
                401,
                respuesta.statusCode()
        );
    }

    // PRUEBA 6: RECHAZAR TOKEN EXPIRADO

    @Test
    void rechazarTokenExpirado()
            throws Exception {

        String token = generarToken(

                "ADMINISTRADOR",

                -600,

                Base64.getDecoder().decode(CLAVE_TEST)
        );

        var respuesta = cambiarEstado(token);

        assertEquals(
                401,
                respuesta.statusCode()
        );
    }
}