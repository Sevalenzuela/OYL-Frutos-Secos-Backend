package cl.oylfrutossecos.inventario.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AltaPendiente {
    private final RestClient productos;

    public AltaPendiente(@Value("${oyl.productos.url}") String urlProductos) {
        var conexiones = new SimpleClientHttpRequestFactory();
        conexiones.setConnectTimeout(1000);
        conexiones.setReadTimeout(1500);
        this.productos = RestClient.builder().baseUrl(urlProductos).requestFactory(conexiones).build();
    }

    public int consultarStock(Long id, UUID clave, String autorizacion) {
        try {
            StockPendiente alta = productos.get().uri("/api/productos/{id}/alta/{clave}", id, clave)
                    .header(HttpHeaders.AUTHORIZATION, autorizacion).retrieve().body(StockPendiente.class);
            if (alta != null) {
                return alta.stock();
            }
        } catch (RestClientResponseException fallo) {
            if (fallo.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alta pendiente no encontrada", fallo);
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo verificar el alta", fallo);
        } catch (RestClientException fallo) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo verificar el alta", fallo);
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se recibió el stock inicial");
    }

    public record StockPendiente(int stock) {}
}
