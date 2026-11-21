package cl.oylfrutossecos.inventario.repository;

import cl.oylfrutossecos.inventario.model.Inventario;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventarioRepository
        extends JpaRepository<Inventario, Long> {

    // Bloquea el registro durante una modificación
    // para evitar que dos operaciones simultáneas
    // descuenten las mismas unidades.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i
        FROM Inventario i
        WHERE i.productoId = :productoId
        """)
    Optional<Inventario> buscarConBloqueo(
            @Param("productoId") Long productoId
    );
}
