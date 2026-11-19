
package cl.oylfrutossecos.productos.controller;

import cl.oylfrutossecos.productos.dto.EstadoProductoRequest;
import cl.oylfrutossecos.productos.dto.ProductoRequest;
import cl.oylfrutossecos.productos.model.Producto;
import cl.oylfrutossecos.productos.service.ProductoService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    // GET /api/productos
    @GetMapping
    public List<Producto> listar() {

        return service.listarProductos();
    }

    // GET /api/productos/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Producto> buscarPorId(
            @PathVariable Long id
    ) {

        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    // POST /api/productos
    @PostMapping
    public ResponseEntity<Producto> crear(
            @Valid @RequestBody ProductoRequest datos
    ) {

        Producto creado = service.crearProducto(datos);

        URI ubicacion = URI.create(
                "/api/productos/" + creado.getId()
        );

        return ResponseEntity
                .created(ubicacion)
                .body(creado);
    }

    // PUT /api/productos/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest datos
    ) {

        Producto actualizado =
                service.actualizarProducto(id, datos);

        return ResponseEntity.ok(actualizado);
    }

    // PATCH /api/productos/{id}/estado
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Producto> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody EstadoProductoRequest datos
    ) {

        Producto actualizado = service.cambiarEstado(
                id,
                datos.activo()
        );

        return ResponseEntity.ok(actualizado);
    }

    // DELETE /api/productos/{id}
    // No borra la fila: deja activo = false.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id
    ) {

        service.eliminarLogicamente(id);

        return ResponseEntity.noContent().build();
    }
}
