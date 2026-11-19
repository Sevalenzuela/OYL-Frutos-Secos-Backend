
package cl.oylfrutossecos.productos.service;

import cl.oylfrutossecos.productos.dto.ProductoRequest;
import cl.oylfrutossecos.productos.model.Producto;
import cl.oylfrutossecos.productos.repository.ProductoRepository;

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

    // 1. LISTAR TODOS LOS PRODUCTOS
    public List<Producto> listarProductos() {

        return repository.findAll(
                Sort.by(Sort.Direction.ASC, "id")
        );
    }

    // 2. BUSCAR PRODUCTO POR ID
    public Optional<Producto> buscarPorId(Long id) {

        return repository.findById(id);
    }

    // Buscar un producto obligatorio.
    // Si no existe, responder con HTTP 404.
    private Producto obtenerProducto(Long id) {

        return repository.findById(id)
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

        Producto producto = obtenerProducto(id);

        copiarDatos(producto, datos);

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
        producto.setImagenUri(datos.imagenUri());
    }
}
