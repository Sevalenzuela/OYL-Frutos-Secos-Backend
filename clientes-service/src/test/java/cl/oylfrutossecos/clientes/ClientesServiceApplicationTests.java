package cl.oylfrutossecos.clientes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {

		"spring.datasource.url=jdbc:h2:mem:clientes_test;DB_CLOSE_DELAY=-1",

		"spring.jpa.hibernate.ddl-auto=create-drop",

		"oyl.admin.bootstrap=false",

		"oyl.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="

})
class ClientesServiceApplicationTests {

	@Test
	void contextLoads() {
	}
}