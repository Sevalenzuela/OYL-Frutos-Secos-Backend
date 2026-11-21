package cl.oylfrutossecos.inventario.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reservas_stock")
public class ReservaStock {

    @Id
    @Column(name = "operacion_id", length = 80)
    private String operacionId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(nullable = false)
    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    public ReservaStock() {
    }

    public ReservaStock(
            String operacionId,
            Long productoId,
            Integer cantidad
    ) {
        this.operacionId = operacionId;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.estado = EstadoReserva.RESERVADA;
    }

    public String getOperacionId() {
        return operacionId;
    }

    public Long getProductoId() {
        return productoId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }
}
