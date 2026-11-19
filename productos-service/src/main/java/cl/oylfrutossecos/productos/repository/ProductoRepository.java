package cl.oylfrutossecos.productos.repository;

import cl.oylfrutossecos.productos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository
        extends JpaRepository<Producto, Long> {
}
