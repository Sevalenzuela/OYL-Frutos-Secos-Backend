package cl.oylfrutossecos.clientes.config;

import cl.oylfrutossecos.clientes.model.Cliente;
import cl.oylfrutossecos.clientes.model.RolUsuario;
import cl.oylfrutossecos.clientes.repository.ClienteRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;

@Configuration
public class InicializadorAdministrador {

    @Bean
    @ConditionalOnProperty(
            name = "oyl.admin.bootstrap",
            havingValue = "true"
    )
    public ApplicationRunner crearAdministrador(
            ClienteRepository repository,
            PasswordEncoder passwordEncoder,
            @Value("${oyl.admin.password:}") String contrasena
    ) {

        return args -> {

            String usuario = "admin_oyl";
            String correo = "admin-oyl@example.com";

            var existente = repository
                    .findByNombreUsuarioIgnoreCase(usuario);

            // No volver a crear ni modificar al administrador.
            if (existente.isPresent()) {

                if (existente.get().getRol()
                        != RolUsuario.ADMINISTRADOR) {

                    throw new IllegalStateException(
                            "El usuario administrativo ya esta ocupado"
                    );
                }

                return;
            }

            // Validar la contrasena administrativa.
            if (
                    contrasena.isBlank()
                            || contrasena.length() < 12
                            || contrasena.getBytes(
                            StandardCharsets.UTF_8
                    ).length > 72
            ) {

                throw new IllegalStateException(
                        "Configura una contrasena administrativa valida"
                );
            }

            if (repository.existsByCorreoIgnoreCase(correo)) {

                throw new IllegalStateException(
                        "El correo administrativo ya esta ocupado"
                );
            }

            Cliente administrador = new Cliente();

            administrador.setNombre("Administrador OYL");
            administrador.setNombreUsuario(usuario);
            administrador.setCorreo(correo);
            administrador.setTelefono("900000001");
            administrador.setDireccion("");

            administrador.setPasswordHash(
                    passwordEncoder.encode(contrasena)
            );

            administrador.setRol(RolUsuario.ADMINISTRADOR);
            administrador.setActivo(true);

            repository.saveAndFlush(administrador);

            System.out.println(
                    "Administrador OYL creado correctamente"
            );
        };
    }
}