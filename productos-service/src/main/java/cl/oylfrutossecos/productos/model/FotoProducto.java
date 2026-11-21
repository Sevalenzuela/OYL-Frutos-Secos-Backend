package cl.oylfrutossecos.productos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Entity
public class FotoProducto {
    @Id
    private Long productoId;
    private String tipo;
    private String version;
    @Lob
    private byte[] contenido;

    protected FotoProducto() {}

    public FotoProducto(String tipo, byte[] contenido) {
        this.tipo = tipo;
        this.contenido = contenido;
        try {
            version = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException fallo) {
            throw new IllegalStateException(fallo);
        }
    }

    public void asignarProducto(Long id) { productoId = id; }
    public String version() { return version; }
    public String tipo() { return tipo; }
    public byte[] contenido() { return contenido; }
    public String ruta() { return "/api/productos/" + productoId + "/foto/" + version; }
}
