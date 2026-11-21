
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

@Component
public class ProductosClient {

    private final RestClient http;

    public ProductosClient(
            @Value("${oyl.productos.base-url}") String baseUrl
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

    public ProductoRemoto consultarProductoActivo(
            Long productoId
    ) {

        if (productoId == null || productoId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El ID del producto no es valido"
            );
        }

        ProductoRemoto producto;

        try {

            // Solicitar datos directamente a Productos.
            producto = http.get()
                    .uri("/api/productos/{id}", productoId)
                    .retrieve()
                    .body(ProductoRemoto.class);

        } catch (HttpClientErrorException.NotFound error) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El producto no existe"
            );

        } catch (ResourceAccessException error) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El microservicio Productos no esta disponible"
            );

        } catch (RestClientException error) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Error al consultar el microservicio Productos"
            );
        }

        if (
                producto == null
                        || !productoId.equals(producto.id())
                        || producto.nombre() == null
                        || producto.nombre().isBlank()
                        || producto.precio() == null
                        || producto.precio() <= 0
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Productos devolvio datos invalidos"
            );
        }

        if (!Boolean.TRUE.equals(producto.activo())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El producto esta desactivado"
            );
        }

        return producto;
    }
}
