
package cl.oylfrutossecos.pedidos.controller;

import cl.oylfrutossecos.pedidos.client.ProductoRemoto;
import cl.oylfrutossecos.pedidos.client.ProductosClient;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integracion/productos")
public class IntegracionProductoController {

    private final ProductosClient productosClient;

    public IntegracionProductoController(
            ProductosClient productosClient
    ) {
        this.productosClient = productosClient;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoRemoto> consultar(
            @PathVariable Long id
    ) {

        ProductoRemoto producto =
                productosClient.consultarProductoActivo(id);

        return ResponseEntity.ok(producto);
    }
}
