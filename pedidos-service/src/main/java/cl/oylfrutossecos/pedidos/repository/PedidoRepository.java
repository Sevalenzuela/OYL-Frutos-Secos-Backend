package cl.oylfrutossecos.pedidos.repository;

import cl.oylfrutossecos.pedidos.model.Pedido;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository
        extends JpaRepository<Pedido, Long> {

    // Buscar pedido por numero.
    Optional<Pedido> findByNumero(String numero);

    // Historial de pedidos de un cliente.
    List<Pedido> findByClienteIdOrderByFechaDesc(
            Long clienteId
    );

    // Bloquear el pedido durante una modificacion.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM Pedido p
        WHERE p.numero = :numero
        """)
    Optional<Pedido> buscarConBloqueo(
            @Param("numero") String numero
    );
}