package cl.oylfrutossecos.pedidos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
		properties = "oyl.recuperacion.enabled=false"
)
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:pedidos_test;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.h2.console.enabled=false",
		"oyl.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"oyl.inventario.secret=YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk="
})
class PedidosServiceApplicationTests {

	@Test
	void contextLoads() {
	}
}