package cl.oylfrutossecos.pedidos.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String numero;

    @Column(nullable = false)
    private Long fecha;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "nombre_cliente", nullable = false, length = 120)
    private String nombreCliente;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModalidadPedido modalidad;

    @Column(nullable = false, length = 250)
    private String direccion = "";

    @Column(nullable = false)
    private Integer total = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    // FASE TECNICA DEL REGISTRO DE COMPRA

    @Enumerated(EnumType.STRING)
    @Column(name = "fase_compra", length = 20)
    private FaseCompra faseCompra;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_objetivo", length = 30)
    private EstadoPedido estadoObjetivo;

    @OneToMany(
            mappedBy = "pedido",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DetallePedido> productos = new ArrayList<>();

    public Pedido() {
    }

    // AGREGAR DETALLE

    public void agregarProducto(DetallePedido detalle) {

        productos.add(detalle);
        detalle.setPedido(this);
    }

    // DETERMINAR SI EL PEDIDO PUEDE MOSTRARSE

    public boolean esVisible() {

        return faseCompra == null
                || faseCompra == FaseCompra.REGISTRADA;
    }

    // GETTERS Y SETTERS

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Long getFecha() {
        return fecha;
    }

    public void setFecha(Long fecha) {
        this.fecha = fecha;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public ModalidadPedido getModalidad() {
        return modalidad;
    }

    public void setModalidad(ModalidadPedido modalidad) {
        this.modalidad = modalidad;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public FaseCompra getFaseCompra() {
        return faseCompra;
    }

    public void setFaseCompra(FaseCompra faseCompra) {
        this.faseCompra = faseCompra;
    }

    public List<DetallePedido> getProductos() {
        return productos;
    }

    public EstadoPedido getEstadoObjetivo() {
        return estadoObjetivo;
    }

    public void setEstadoObjetivo(EstadoPedido estadoObjetivo) {
        this.estadoObjetivo = estadoObjetivo;
    }
}