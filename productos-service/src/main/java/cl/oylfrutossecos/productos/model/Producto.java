
package cl.oylfrutossecos.productos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(nullable = false)
    private Integer precio;

    @Column(length = 500)
    private String descripcion;

    @Column(length = 100)
    private String presentacion;

    @Column(name = "imagen_uri", length = 500)
    private String imagenUri;

    @Column(nullable = false)
    private Boolean activo = true;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(unique = true, length = 36)
    private String claveAlta;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer stockPendiente;

    public Integer stockPendiente() { return stockPendiente; }

    public void prepararAlta(String clave, int stock) {
        claveAlta = clave;
        stockPendiente = stock;
        activo = false;
    }

    public void completarAlta() {
        stockPendiente = null;
        activo = true;
    }

    // Constructor requerido por JPA
    public Producto() {
    }

    // Getters y setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Integer getPrecio() {
        return precio;
    }

    public void setPrecio(Integer precio) {
        this.precio = precio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getPresentacion() {
        return presentacion;
    }

    public void setPresentacion(String presentacion) {
        this.presentacion = presentacion;
    }

    public String getImagenUri() {
        return imagenUri;
    }

    public void setImagenUri(String imagenUri) {
        this.imagenUri = imagenUri;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
