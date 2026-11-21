package cl.oylfrutossecos.productos.service;

import cl.oylfrutossecos.productos.dto.ProductoRequest;
import cl.oylfrutossecos.productos.model.FotoProducto;
import cl.oylfrutossecos.productos.model.Producto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AltaProducto {
    private final ProductoService productos;
    private final RestClient inventario;

    public AltaProducto(ProductoService productos, @Value("${oyl.inventario.url}") String urlInventario) {
        this.productos = productos;
        var conexiones = new SimpleClientHttpRequestFactory();
        conexiones.setConnectTimeout(2000);
        conexiones.setReadTimeout(3000);
        this.inventario = RestClient.builder().baseUrl(urlInventario).requestFactory(conexiones).build();
    }

    public synchronized Producto crear(UUID clave, int stock, ProductoRequest datos,
                                      FotoProducto foto, String autorizacion) {
        Producto producto = productos.prepararAlta(clave.toString(), stock, datos, foto);
        if (producto.stockPendiente() == null) {
            return producto;
        }

        try {
            inventario.put().uri("/api/inventario/{id}/inicial", producto.getId())
                    .header(HttpHeaders.AUTHORIZATION, autorizacion)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(new Inicializacion(clave))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException fallo) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo confirmar el inventario. Reintenta guardar el producto.", fallo);
        }
        return productos.completarAlta(producto.getId());
    }

    private record Inicializacion(UUID clave) {}
}
