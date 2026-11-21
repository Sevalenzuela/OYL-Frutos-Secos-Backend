package cl.oylfrutossecos.clientes.repository;

import cl.oylfrutossecos.clientes.model.Cliente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository
        extends JpaRepository<Cliente, Long> {

    // Comprobar si un nombre de usuario ya existe.
    boolean existsByNombreUsuarioIgnoreCase(
            String nombreUsuario
    );

    // Comprobar si un correo ya esta registrado.
    boolean existsByCorreoIgnoreCase(
            String correo
    );

    // Buscar un usuario para el futuro login.
    Optional<Cliente> findByNombreUsuarioIgnoreCase(
            String nombreUsuario
    );
}