package cl.oylfrutossecos.inventario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventario")
public class Inventario {

    // Identificador del producto registrado
    // en el microservicio de Productos.
    @Id
    @Column(name = "producto_id")
    private Long productoId;

    // Unidades disponibles para vender.
    @Column(name = "stock_disponible", nullable = false)
    private Integer stockDisponible = 0;

    // Unidades reservadas para pedidos.
    @Column(name = "stock_reservado", nullable = false)
    private Integer stockReservado = 0;

    // Constructor requerido por JPA
    public Inventario() {
    }

    public Inventario(
            Long productoId,
            Integer stockDisponible,
            Integer stockReservado
    ) {
        this.productoId = productoId;
        this.stockDisponible = stockDisponible;
        this.stockReservado = stockReservado;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Integer getStockDisponible() {
        return stockDisponible;
    }

    public void setStockDisponible(Integer stockDisponible) {
        this.stockDisponible = stockDisponible;
    }

    public Integer getStockReservado() {
        return stockReservado;
    }

    public void setStockReservado(Integer stockReservado) {
        this.stockReservado = stockReservado;
    }
}
