package cl.oylfrutossecos.inventario.repository;

import cl.oylfrutossecos.inventario.model.ReservaStock;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReservaStockRepository
        extends JpaRepository<ReservaStock, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM ReservaStock r
        WHERE r.operacionId = :operacionId
        """)
    Optional<ReservaStock> buscarConBloqueo(
            @Param("operacionId") String operacionId
    );
}