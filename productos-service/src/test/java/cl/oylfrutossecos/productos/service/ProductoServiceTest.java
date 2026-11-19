package cl.oylfrutossecos.productos.service;

import cl.oylfrutossecos.productos.dto.ProductoRequest;
import cl.oylfrutossecos.productos.model.Producto;
import cl.oylfrutossecos.productos.repository.ProductoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository repository;

    @InjectMocks
    private ProductoService service;

    // Producto ficticio reutilizable.
    private Producto crearProductoPrueba() {

        Producto producto = new Producto();

        producto.setId(1L);
        producto.setNombre("Almendras naturales");
        producto.setCategoria("Frutos secos");
        producto.setPrecio(4500);
        producto.setDescripcion("Almendras de prueba");
        producto.setPresentacion("Bolsa 250 g");
        producto.setActivo(true);

        return producto;
    }

    // Datos ficticios para crear y editar.
    private ProductoRequest crearDatosPrueba() {

        return new ProductoRequest(
                "Almendras premium",
                "Frutos secos",
                5500,
                "Almendras premium de prueba",
                "Bolsa 250 g",
                null
        );
    }

    // PRUEBA 1: Listar productos
    @Test
    void listarProductosCorrectamente() {

        Producto producto = crearProductoPrueba();

        when(repository.findAll(any(Sort.class)))
                .thenReturn(List.of(producto));

        List<Producto> resultado =
                service.listarProductos();

        assertEquals(1, resultado.size());
        assertEquals(
                "Almendras naturales",
                resultado.get(0).getNombre()
        );

        verify(repository).findAll(
                Sort.by(Sort.Direction.ASC, "id")
        );
    }

    // PRUEBA 2: Buscar producto existente
    @Test
    void buscarProductoExistente() {

        Producto producto = crearProductoPrueba();

        when(repository.findById(1L))
                .thenReturn(Optional.of(producto));

        Optional<Producto> resultado =
                service.buscarPorId(1L);

        assertTrue(resultado.isPresent());
        assertEquals(1L, resultado.get().getId());
    }

    // PRUEBA 3: Buscar producto inexistente
    @Test
    void buscarProductoInexistente() {

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        Optional<Producto> resultado =
                service.buscarPorId(999L);

        assertTrue(resultado.isEmpty());
    }

    // PRUEBA 4: Crear producto
    @Test
    void crearProductoCorrectamente() {

        when(repository.save(any(Producto.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0)
                );

        Producto resultado =
                service.crearProducto(crearDatosPrueba());

        assertEquals(
                "Almendras premium",
                resultado.getNombre()
        );

        assertEquals(5500, resultado.getPrecio());
        assertTrue(resultado.getActivo());

        verify(repository).save(any(Producto.class));
    }

    // PRUEBA 5: Editar producto
    @Test
    void actualizarProductoCorrectamente() {

        Producto producto = crearProductoPrueba();

        when(repository.findById(1L))
                .thenReturn(Optional.of(producto));

        when(repository.save(any(Producto.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0)
                );

        Producto resultado = service.actualizarProducto(
                1L,
                crearDatosPrueba()
        );

        assertEquals("Almendras premium", resultado.getNombre());
        assertEquals(5500, resultado.getPrecio());
        assertEquals(1L, resultado.getId());

        verify(repository).save(producto);
    }

    // PRUEBA 6: Desactivar producto
    @Test
    void desactivarProductoCorrectamente() {

        Producto producto = crearProductoPrueba();

        when(repository.findById(1L))
                .thenReturn(Optional.of(producto));

        when(repository.save(any(Producto.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0)
                );

        Producto resultado =
                service.cambiarEstado(1L, false);

        assertFalse(resultado.getActivo());

        verify(repository).save(producto);
    }

    // PRUEBA 7: Reactivar producto
    @Test
    void reactivarProductoCorrectamente() {

        Producto producto = crearProductoPrueba();
        producto.setActivo(false);

        when(repository.findById(1L))
                .thenReturn(Optional.of(producto));

        when(repository.save(any(Producto.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0)
                );

        Producto resultado =
                service.cambiarEstado(1L, true);

        assertTrue(resultado.getActivo());

        verify(repository).save(producto);
    }

    // PRUEBA 8: Eliminacion logica
    @Test
    void eliminarProductoLogicamente() {

        Producto producto = crearProductoPrueba();

        when(repository.findById(1L))
                .thenReturn(Optional.of(producto));

        service.eliminarLogicamente(1L);

        assertFalse(producto.getActivo());

        verify(repository).save(producto);

        // No debe realizar eliminacion fisica.
        verify(repository, never()).deleteById(1L);
    }

    // PRUEBA 9: Editar ID inexistente
    @Test
    void errorAlEditarProductoInexistente() {

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.actualizarProducto(
                                999L,
                                crearDatosPrueba()
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );

        verify(repository, never())
                .save(any(Producto.class));
    }

    // PRUEBA 10: Cambiar estado de ID inexistente
    @Test
    void errorAlCambiarEstadoInexistente() {

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.cambiarEstado(999L, false)
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );

        verify(repository, never())
                .save(any(Producto.class));
    }

    // PRUEBA 11: Eliminar ID inexistente
    @Test
    void errorAlEliminarProductoInexistente() {

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.eliminarLogicamente(999L)
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );

        verify(repository, never())
                .save(any(Producto.class));
    }
}
