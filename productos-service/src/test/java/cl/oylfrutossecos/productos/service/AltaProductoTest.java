package cl.oylfrutossecos.productos.service;

import cl.oylfrutossecos.productos.dto.ProductoRequest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:alta_producto;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AltaProductoTest {
    @DynamicPropertySource
    static void configurarSecretosDePrueba(DynamicPropertyRegistry propiedades) {
        propiedades.add("oyl.jwt.secret", AltaProductoTest::generarSecreto);
    }

    private static String generarSecreto() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Autowired
    private ProductoService productos;

    @Test
    void reintentaElAltaSinDuplicarYConservaLaFoto() throws Exception {
        var respuestas = new AtomicInteger();
        var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/inventario/", peticion -> {
            try (peticion) {
                peticion.getRequestBody().readAllBytes();
                peticion.sendResponseHeaders(respuestas.incrementAndGet() == 1 ? 503 : 200, -1);
            }
        });
        servidor.start();
        try {
            var alta = new AltaProducto(productos, "http://127.0.0.1:" + servidor.getAddress().getPort());
            var clave = UUID.randomUUID();
            var datos = new ProductoRequest("Producto alta", "Frutos secos", 1000, "Descripción", "500 g", null);
            var salida = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB), "JPEG", salida);
            var archivo = new MockMultipartFile("foto", "foto.jpg", "image/jpeg", salida.toByteArray());

            assertThrows(ResponseStatusException.class,
                    () -> alta.crear(clave, 12, datos, ValidadorFoto.leer(archivo), "Bearer prueba"));
            assertTrue(productos.listarProductos().stream().noneMatch(p -> p.getNombre().equals(datos.nombre())));

            var creado = alta.crear(clave, 12, datos, ValidadorFoto.leer(archivo), "Bearer prueba");
            assertTrue(creado.getActivo());
            assertEquals(creado.getId(),
                    alta.crear(clave, 12, datos, ValidadorFoto.leer(archivo), "Bearer prueba").getId());
            assertEquals(2, respuestas.get());

            var ruta = creado.getImagenUri();
            var version = ruta.substring(ruta.lastIndexOf('/') + 1);
            assertEquals("image/jpeg", productos.buscarFoto(creado.getId(), version).orElseThrow().tipo());
            productos.actualizarProducto(creado.getId(), datos);
            assertTrue(productos.buscarFoto(creado.getId(), version).isEmpty());
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void rechazaContenidoQueNoCoincideConElMime() {
        var archivo = new MockMultipartFile("foto", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});
        var fallo = assertThrows(ResponseStatusException.class, () -> ValidadorFoto.leer(archivo));
        assertEquals(415, fallo.getStatusCode().value());
    }
}
