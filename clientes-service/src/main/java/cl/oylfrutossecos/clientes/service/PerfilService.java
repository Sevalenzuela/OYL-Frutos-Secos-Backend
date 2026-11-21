package cl.oylfrutossecos.clientes.service;

import cl.oylfrutossecos.clientes.dto.ActualizarPerfilRequest;
import cl.oylfrutossecos.clientes.dto.ClienteResponse;

import cl.oylfrutossecos.clientes.model.Cliente;
import cl.oylfrutossecos.clientes.repository.ClienteRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class PerfilService {

    private final ClienteRepository repository;

    public PerfilService(ClienteRepository repository) {
        this.repository = repository;
    }

    // 1. CONSULTAR PERFIL PROPIO
    @Transactional(readOnly = true)
    public ClienteResponse consultarPerfil(Long clienteId) {

        Cliente cliente = obtenerClienteActivo(clienteId);

        return convertirRespuesta(cliente);
    }

    // 2. ACTUALIZAR PERFIL PROPIO
    @Transactional
    public ClienteResponse actualizarPerfil(
            Long clienteId,
            ActualizarPerfilRequest datos
    ) {

        Cliente cliente = obtenerClienteActivo(clienteId);

        String nombre = datos.nombre().trim();

        String usuario = datos.nombreUsuario()
                .trim()
                .toLowerCase(Locale.ROOT);

        String correo = datos.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Validar nombre.
        if (nombre.length() < 3) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre debe tener al menos 3 caracteres"
            );
        }

        // Validar cantidad de digitos del telefono.
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

        // Comprobar usuario duplicado.
        // Se permite conservar el usuario actual.
        if (
                !cliente.getNombreUsuario().equalsIgnoreCase(usuario)
                        && repository.existsByNombreUsuarioIgnoreCase(usuario)
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El nombre de usuario ya esta registrado"
            );
        }

        // Comprobar correo duplicado.
        // Se permite conservar el correo actual.
        if (
                !cliente.getCorreo().equalsIgnoreCase(correo)
                        && repository.existsByCorreoIgnoreCase(correo)
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya esta registrado"
            );
        }

        // Actualizar solamente los campos permitidos.
        cliente.setNombre(nombre);
        cliente.setNombreUsuario(usuario);
        cliente.setCorreo(correo);
        cliente.setTelefono(datos.telefono().trim());

        cliente.setDireccion(
                datos.direccion() == null
                        ? ""
                        : datos.direccion().trim()
        );

        try {

            Cliente actualizado =
                    repository.saveAndFlush(cliente);

            return convertirRespuesta(actualizado);

        } catch (DataIntegrityViolationException error) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo actualizar: conflicto de datos"
            );
        }
    }

    // 3. COMPROBAR QUE LA CUENTA ESTE ACTIVA
    private Cliente obtenerClienteActivo(Long clienteId) {

        Cliente cliente = repository
                .findById(clienteId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Cuenta no disponible"
                        )
                );

        if (!Boolean.TRUE.equals(cliente.getActivo())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Cuenta no disponible"
            );
        }

        return cliente;
    }

    // 4. CONVERTIR ENTIDAD A DTO SEGURO
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