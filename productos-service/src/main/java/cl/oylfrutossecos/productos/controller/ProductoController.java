
package cl.oylfrutossecos.productos.controller;

import cl.oylfrutossecos.productos.dto.EstadoProductoRequest;
import cl.oylfrutossecos.productos.dto.ProductoRequest;
import cl.oylfrutossecos.productos.model.Producto;
import cl.oylfrutossecos.productos.service.ProductoService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cl.oylfrutossecos.productos.service.AltaProducto;
import cl.oylfrutossecos.productos.service.ValidadorFoto;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService service;

    private final AltaProducto alta;

    public ProductoController(ProductoService service, AltaProducto alta) {
        this.service = service;
        this.alta = alta;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Producto> crearConInventario(
            @RequestHeader("Authorization") String autorizacion,
            @RequestHeader("Idempotency-Key") UUID clave,
            @RequestParam @Min(0) int stock,
            @Valid @RequestPart ProductoRequest datos,
            @RequestPart(required = false) MultipartFile foto) {
        Producto creado = alta.crear(clave, stock, datos, ValidadorFoto.leer(foto), autorizacion);
        return ResponseEntity.created(URI.create("/api/productos/" + creado.getId())).body(creado);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Producto editarConFoto(@PathVariable Long id,
            @Valid @RequestPart ProductoRequest datos,
            @RequestPart(required = false) MultipartFile foto) {
        return service.actualizarProducto(id, datos, ValidadorFoto.leer(foto));
    }

    @GetMapping("/{id}/alta/{clave}")
    public StockPendiente consultarAlta(@PathVariable Long id, @PathVariable UUID clave) {
        return new StockPendiente(service.consultarStockPendiente(id, clave.toString()));
    }

    public record StockPendiente(int stock) {}

    @GetMapping("/{id}/foto/{version}")
    public ResponseEntity<byte[]> foto(@PathVariable Long id, @PathVariable String version) {
        return service.buscarFoto(id, version)
                .map(foto -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(foto.tipo()))
                        .cacheControl(CacheControl.noCache())
                        .body(foto.contenido()))
                .orElseGet(() -> ResponseEntity.notFound().build());
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
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
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
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
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
