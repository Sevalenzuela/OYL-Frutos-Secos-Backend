
package cl.oylfrutossecos.productos.service;

import cl.oylfrutossecos.productos.dto.ProductoRequest;
import cl.oylfrutossecos.productos.model.Producto;
import cl.oylfrutossecos.productos.model.FotoProducto;
import cl.oylfrutossecos.productos.repository.ProductoRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public Producto prepararAlta(String clave, int stock, ProductoRequest datos, FotoProducto foto) {
        return repository.findByClaveAlta(clave).orElseGet(() -> {
            Producto producto = new Producto();
            copiarDatos(producto, datos);
            producto.prepararAlta(clave, stock);
            repository.saveAndFlush(producto);
            guardarFoto(producto, foto);
            return producto;
        });
    }

    public int consultarStockPendiente(Long id, String clave) {
        return repository.findByClaveAlta(clave)
                .filter(producto -> producto.getId().equals(id))
                .map(Producto::stockPendiente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alta pendiente no encontrada"));
    }

    @Transactional
    public Producto completarAlta(Long id) {
        Producto producto = repository.findById(id).orElseThrow();
        if (producto.stockPendiente() != null) {
            producto.completarAlta();
        }
        return producto;
    }

    public Optional<FotoProducto> buscarFoto(Long id, String version) {
        return buscarPorId(id).flatMap(producto ->
                Optional.ofNullable(entityManager.find(FotoProducto.class, id))
                        .filter(foto -> foto.version().equals(version)
                                && foto.ruta().equals(producto.getImagenUri())));
    }

    private void guardarFoto(Producto producto, FotoProducto foto) {
        if (foto != null) {
            foto.asignarProducto(producto.getId());
            entityManager.merge(foto);
            producto.setImagenUri(foto.ruta());
        }
    }

    // 1. LISTAR TODOS LOS PRODUCTOS
    public List<Producto> listarProductos() {

        return repository.findAllByStockPendienteIsNull(
                Sort.by(Sort.Direction.ASC, "id")
        );
    }

    // 2. BUSCAR PRODUCTO POR ID
    public Optional<Producto> buscarPorId(Long id) {

        return repository.findById(id).filter(producto -> producto.stockPendiente() == null);
    }

    // Buscar un producto obligatorio.
    // Si no existe, responder con HTTP 404.
    private Producto obtenerProducto(Long id) {

        return buscarPorId(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Producto no encontrado: " + id
                        )
                );
    }

    // 3. CREAR PRODUCTO
    @Transactional
    public Producto crearProducto(ProductoRequest datos) {

        Producto producto = new Producto();

        copiarDatos(producto, datos);

        producto.setActivo(true);

        return repository.save(producto);
    }

    // 4. MODIFICAR PRODUCTO
    @Transactional
    public Producto actualizarProducto(
            Long id,
            ProductoRequest datos
    ) {

        return actualizarProducto(id, datos, null);
    }

    @Transactional
    public Producto actualizarProducto(Long id, ProductoRequest datos, FotoProducto foto) {
        Producto producto = obtenerProducto(id);
        String fotoAnterior = producto.getImagenUri();
        copiarDatos(producto, datos);
        guardarFoto(producto, foto);
        if (foto == null && fotoAnterior != null && fotoAnterior.startsWith("/api/productos/")
                && !fotoAnterior.equals(producto.getImagenUri())) {
            FotoProducto anterior = entityManager.find(FotoProducto.class, id);
            if (anterior != null) {
                entityManager.remove(anterior);
            }
        }
        return repository.save(producto);
    }

    // 5. ACTIVAR O DESACTIVAR PRODUCTO
    @Transactional
    public Producto cambiarEstado(
            Long id,
            Boolean activo
    ) {

        Producto producto = obtenerProducto(id);

        producto.setActivo(activo);

        return repository.save(producto);
    }

    // 6. ELIMINACION LOGICA
    @Transactional
    public void eliminarLogicamente(Long id) {

        Producto producto = obtenerProducto(id);

        producto.setActivo(false);

        repository.save(producto);
    }

    // Reutilizar la asignacion de datos.
    // Evitamos repetirla en crear y editar.
    private void copiarDatos(
            Producto producto,
            ProductoRequest datos
    ) {

        producto.setNombre(datos.nombre().trim());
        producto.setCategoria(datos.categoria().trim());
        producto.setPrecio(datos.precio());
        producto.setDescripcion(datos.descripcion().trim());
        producto.setPresentacion(datos.presentacion().trim());
        String referencia = datos.imagenUri();
        if (referencia != null && referencia.stripLeading().matches("(?is)^(content|file):.*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adjunta la foto como archivo");
        }
        producto.setImagenUri(referencia);
    }
}
