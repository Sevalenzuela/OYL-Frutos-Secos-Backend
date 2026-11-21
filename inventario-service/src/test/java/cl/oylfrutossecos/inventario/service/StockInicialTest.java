package cl.oylfrutossecos.inventario.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:stock_inicial;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class StockInicialTest {
    @DynamicPropertySource
    static void configurarSecretosDePrueba(DynamicPropertyRegistry propiedades) {
        propiedades.add("oyl.jwt.secret", StockInicialTest::generarSecreto);
        propiedades.add("oyl.inventario.secret", StockInicialTest::generarSecreto);
    }

    private static String generarSecreto() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Autowired
    private InventarioService inventario;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void reintentarNoRestableceExistenciasNiReservas() {
        long id = 650L;
        String clave = java.util.UUID.randomUUID().toString();
        assertEquals(8, inventario.inicializar(id, 8, clave).getStockDisponible());
        jdbc.update("UPDATE inventario SET stock_disponible = 5, stock_reservado = 3 WHERE producto_id = ?", id);

        var reintento = inventario.inicializar(id, 8, clave);
        assertEquals(5, reintento.getStockDisponible());
        assertEquals(3, reintento.getStockReservado());
        var conflicto = assertThrows(ResponseStatusException.class,
                () -> inventario.inicializar(id, 15, java.util.UUID.randomUUID().toString()));
        assertEquals(409, conflicto.getStatusCode().value());
    }
}
