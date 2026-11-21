package cl.oylfrutossecos.pedidos.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.stereotype.Component;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Component
public class ClientesClient {

    private final RestClient http;

    public ClientesClient(
            @Value("${oyl.clientes.base-url}") String baseUrl
    ) {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    // CONSULTAR USUARIO AUTENTICADO

    public ClienteRemoto consultarPerfil(Jwt jwt) {

        if (jwt == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Autenticacion requerida"
            );
        }

        ClienteRemoto cliente;

        try {

            cliente = http.get()
                    .uri("/api/clientes/me")
                    .headers(headers ->
                            headers.setBearerAuth(
                                    jwt.getTokenValue()
                            )
                    )
                    .retrieve()
                    .body(ClienteRemoto.class);

        } catch (HttpClientErrorException error) {

            if (
                    error.getStatusCode().value() == 401
                            || error.getStatusCode().value() == 403
            ) {

                throw new ResponseStatusException(
                        error.getStatusCode(),
                        "Cuenta no autorizada"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo consultar al cliente"
            );

        } catch (ResourceAccessException error) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El microservicio Clientes no esta disponible"
            );

        } catch (RestClientException error) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Error de comunicacion con Clientes"
            );
        }

        // VALIDAR IDENTIDAD RECIBIDA

        if (
                cliente == null
                        || cliente.id() == null
                        || cliente.id() <= 0
                        || !cliente.id().toString().equals(jwt.getSubject())
                        || cliente.rol() == null
                        || cliente.activo() == null
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Clientes devolvio datos invalidos"
            );
        }

        // COMPROBAR ESTADO ACTUAL DE LA CUENTA

        if (!Boolean.TRUE.equals(cliente.activo())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "La cuenta no esta activa"
            );
        }

        // COMPROBAR QUE EL ROL NO HAYA CAMBIADO

        if (
                !cliente.rol().equals(
                        jwt.getClaimAsString("rol")
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Los permisos de la cuenta han cambiado"
            );
        }

        return cliente;
    }
}