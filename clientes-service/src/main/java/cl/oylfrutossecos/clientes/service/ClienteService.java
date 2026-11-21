package cl.oylfrutossecos.clientes.service;

import cl.oylfrutossecos.clientes.dto.ClienteResponse;
import cl.oylfrutossecos.clientes.dto.RegistroClienteRequest;
import cl.oylfrutossecos.clientes.dto.LoginRequest;

import cl.oylfrutossecos.clientes.model.Cliente;
import cl.oylfrutossecos.clientes.model.RolUsuario;

import cl.oylfrutossecos.clientes.repository.ClienteRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class ClienteService {

    private final ClienteRepository repository;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(
            ClienteRepository repository,
            PasswordEncoder passwordEncoder
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. REGISTRAR UN NUEVO CLIENTE
    @Transactional
    public ClienteResponse registrar(
            RegistroClienteRequest datos
    ) {

        String nombre = datos.nombre().trim();

        String usuario = datos.nombreUsuario()
                .trim()
                .toLowerCase(Locale.ROOT);

        String correo = datos.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Validar longitud del nombre.
        if (nombre.length() < 3) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre debe tener al menos 3 caracteres"
            );
        }

        // Validar telefono.
        long digitos = datos.telefono()
                .chars()
                .filter(c -> c >= '0' && c <= '9')
                .count();

        if (digitos < 9) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El telefono debe tener al menos 9 digitos"
            );
        }

        // Validar longitud admitida por BCrypt.
        int bytesContrasena = datos.contrasena()
                .getBytes(StandardCharsets.UTF_8)
                .length;

        if (bytesContrasena > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contrasena supera el limite permitido"
            );
        }

        // Comprobar usuario duplicado.
        if (repository.existsByNombreUsuarioIgnoreCase(usuario)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El nombre de usuario ya esta registrado"
            );
        }

        // Comprobar correo duplicado.
        if (repository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya esta registrado"
            );
        }

        // Crear cliente.
        Cliente cliente = new Cliente();

        cliente.setNombre(nombre);
        cliente.setNombreUsuario(usuario);
        cliente.setCorreo(correo);
        cliente.setTelefono(datos.telefono().trim());

        cliente.setDireccion(
                datos.direccion() == null
                        ? ""
                        : datos.direccion().trim()
        );

        // Generar hash de la contrasena.
        cliente.setPasswordHash(
                passwordEncoder.encode(datos.contrasena())
        );

        cliente.setRol(RolUsuario.CLIENTE);
        cliente.setActivo(true);

        try {

            Cliente guardado =
                    repository.saveAndFlush(cliente);

            return convertirRespuesta(guardado);

        } catch (DataIntegrityViolationException error) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo registrar: conflicto de datos"
            );
        }
    }

    // 2. AUTENTICAR USUARIO
    @Transactional(readOnly = true)
    public ClienteResponse autenticar(LoginRequest datos) {

        String usuario = datos.nombreUsuario()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Comprobar longitud admitida por BCrypt.
        if (
                datos.contrasena()
                        .getBytes(StandardCharsets.UTF_8)
                        .length > 72
        ) {
            throw credencialesInvalidas();
        }

        // Buscar usuario en la base de datos.
        Cliente cliente = repository
                .findByNombreUsuarioIgnoreCase(usuario)
                .orElseThrow(this::credencialesInvalidas);

        // Comprobar que la cuenta este activa.
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw credencialesInvalidas();
        }

        // Comparar contrasena con el hash almacenado.
        boolean contrasenaCorrecta =
                passwordEncoder.matches(
                        datos.contrasena(),
                        cliente.getPasswordHash()
                );

        if (!contrasenaCorrecta) {
            throw credencialesInvalidas();
        }

        // Devolver datos seguros del cliente.
        return convertirRespuesta(cliente);
    }

    // ERROR GENERICO DE AUTENTICACION
    private ResponseStatusException credencialesInvalidas() {

        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Credenciales invalidas"
        );
    }

    // CONVERTIR ENTIDAD A DTO
    private ClienteResponse convertirRespuesta(
            Cliente cliente
    ) {

        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getNombreUsuario(),
                cliente.getCorreo(),
                cliente.getTelefono(),
                cliente.getDireccion(),
                cliente.getRol(),
                cliente.getActivo()
        );
    }
}
