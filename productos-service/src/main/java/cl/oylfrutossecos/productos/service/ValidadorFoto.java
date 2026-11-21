package cl.oylfrutossecos.productos.service;

import cl.oylfrutossecos.productos.model.FotoProducto;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ValidadorFoto {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_PIXELES = 4_000_000;

    private ValidadorFoto() {}

    public static FotoProducto leer(MultipartFile archivo) {
        if (archivo == null) {
            return null;
        }
        if (archivo.isEmpty() || archivo.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Foto de hasta 5 MB");
        }
        String formato = switch (String.valueOf(archivo.getContentType())) {
            case "image/jpeg" -> "JPEG";
            case "image/png" -> "PNG";
            default -> throw invalida();
        };

        try (var entrada = new MemoryCacheImageInputStream(archivo.getInputStream())) {
            var lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) {
                throw invalida();
            }
            var lector = lectores.next();
            try {
                lector.setInput(entrada);
                if (!formato.equalsIgnoreCase(lector.getFormatName())
                        || (long) lector.getWidth(0) * lector.getHeight(0) > MAX_PIXELES) {
                    throw invalida();
                }
                var salida = new ByteArrayOutputStream();
                if (!ImageIO.write(lector.read(0), formato, salida) || salida.size() > MAX_BYTES) {
                    throw invalida();
                }
                return new FotoProducto(archivo.getContentType(), salida.toByteArray());
            } finally {
                lector.dispose();
            }
        } catch (IOException | IllegalArgumentException fallo) {
            throw invalida();
        }
    }

    private static ResponseStatusException invalida() {
        return new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Selecciona una imagen JPEG o PNG válida");
    }
}
