package cl.oylfrutossecos.pedidos.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;

@Component
public class InventarioClient {

    private final RestClient http;

    public InventarioClient(
            @Value("${oyl.inventario.base-url}")
            String baseUrl,

            @Value("${oyl.inventario.secret}")
            String secretoInterno
    ) {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeaders(headers ->
                        headers.setBasicAuth(
                                "pedidos-service",
                                secretoInterno
                        )
                )
                .build();
    }

    // 1. CONSULTAR INVENTARIO

    public InventarioRemoto consultarInventario(
            Long productoId
    ) {

        if (productoId == null || productoId <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El ID del producto no es valido"
            );
        }

        InventarioRemoto inventario = ejecutar(() ->
                http.get()
                        .uri(
                                "/api/inventario/{productoId}",
                                productoId
                        )
                        .retrieve()
                        .body(InventarioRemoto.class)
        );

        if (
                inventario == null
                        || !productoId.equals(inventario.productoId())
                        || inventario.stockDisponible() == null
                        || inventario.stockReservado() == null
                        || inventario.stockDisponible() < 0
                        || inventario.stockReservado() < 0
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Inventario devolvio datos invalidos"
            );
        }

        return inventario;
    }

    // 2. RESERVAR PRODUCTOS

    public ReservaRemota reservar(
            String operacionId,
            Long productoId,
            Integer cantidad
    ) {

        validarOperacionId(operacionId);

        if (
                productoId == null
                        || productoId <= 0
                        || cantidad == null
                        || cantidad <= 0
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los datos de la reserva no son validos"
            );
        }

        ReservaInventarioRequest datos =
                new ReservaInventarioRequest(
                        operacionId,
                        productoId,
                        cantidad
                );

        ReservaRemota reserva = ejecutar(() ->
                http.post()
                        .uri("/api/inventario/reservas")
                        .body(datos)
                        .retrieve()
                        .body(ReservaRemota.class)
        );

        validarReserva(reserva, operacionId);

        if (
                !productoId.equals(reserva.productoId())
                        || !cantidad.equals(reserva.cantidad())
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Inventario devolvio una reserva diferente"
            );
        }

        return reserva;
    }

    // 3. CONSULTAR RESERVA

    public ReservaRemota consultarReserva(
            String operacionId
    ) {

        validarOperacionId(operacionId);

        ReservaRemota reserva = ejecutar(() ->
                http.get()
                        .uri(
                                "/api/inventario/reservas/{operacionId}",
                                operacionId
                        )
                        .retrieve()
                        .body(ReservaRemota.class)
        );

        validarReserva(reserva, operacionId);

        return reserva;
    }

    // 4. CONFIRMAR RESERVA

    public ReservaRemota confirmarReserva(
            String operacionId
    ) {

        validarOperacionId(operacionId);

        ReservaRemota reserva = ejecutar(() ->
                http.post()
                        .uri(
                                "/api/inventario/reservas/{operacionId}/confirmar",
                                operacionId
                        )
                        .retrieve()
                        .body(ReservaRemota.class)
        );

        validarReserva(reserva, operacionId);

        if (!"CONFIRMADA".equals(reserva.estado())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "La reserva no fue confirmada correctamente"
            );
        }

        return reserva;
    }

    // 5. CANCELAR RESERVA

    public ReservaRemota cancelarReserva(
            String operacionId
    ) {

        validarOperacionId(operacionId);

        ReservaRemota reserva = ejecutar(() ->
                http.post()
                        .uri(
                                "/api/inventario/reservas/{operacionId}/cancelar",
                                operacionId
                        )
                        .retrieve()
                        .body(ReservaRemota.class)
        );

        validarReserva(reserva, operacionId);

        if (!"CANCELADA".equals(reserva.estado())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "La reserva no fue cancelada correctamente"
            );
        }

        return reserva;
    }

    // 6. VALIDAR IDENTIFICADOR DE OPERACION

    private void validarOperacionId(String operacionId) {

        if (
                operacionId == null
                        || !operacionId.matches("[a-zA-Z0-9_-]{1,80}")
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador de operacion no es valido"
            );
        }
    }

    // 7. VALIDAR RESPUESTA DE INVENTARIO

    private void validarReserva(
            ReservaRemota reserva,
            String operacionId
    ) {

        if (
                reserva == null
                        || !operacionId.equals(reserva.operacionId())
                        || reserva.productoId() == null
                        || reserva.productoId() <= 0
                        || reserva.cantidad() == null
                        || reserva.cantidad() <= 0
                        || reserva.estado() == null
                        || !Set.of(
                        "RESERVADA",
                        "CONFIRMADA",
                        "CANCELADA"
                ).contains(reserva.estado())
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Inventario devolvio una reserva invalida"
            );
        }
    }

    // 8. EJECUTAR SOLICITUD Y CONTROLAR ERRORES

    private <T> T ejecutar(Supplier<T> solicitud) {

        try {

            return solicitud.get();

        } catch (HttpClientErrorException error) {

            throw switch (error.getStatusCode().value()) {

                case 400 -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Solicitud de inventario invalida"
                );

                case 404 -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Inventario o reserva no encontrada"
                );

                case 409 -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stock insuficiente o conflicto de reserva"
                );

                default -> new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Inventario rechazo la solicitud"
                );
            };

        } catch (ResourceAccessException error) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El microservicio Inventario no esta disponible"
            );

        } catch (RestClientException error) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Error de comunicacion con Inventario"
            );
        }
    }
}