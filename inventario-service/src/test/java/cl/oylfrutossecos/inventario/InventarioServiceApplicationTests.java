package cl.oylfrutossecos.inventario;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {

		"spring.datasource.url=jdbc:h2:mem:inventario_test;DB_CLOSE_DELAY=-1",

		"spring.jpa.hibernate.ddl-auto=create-drop",

		"oyl.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",

		"oyl.inventario.secret=YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk="

})
class InventarioServiceApplicationTests {

	@Test
	void contextLoads() {
	}
}