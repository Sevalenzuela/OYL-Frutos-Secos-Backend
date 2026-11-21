package cl.oylfrutossecos.pedidos.service;

import cl.oylfrutossecos.pedidos.client.*;
import cl.oylfrutossecos.pedidos.domain.ReglasCreacionPedido;
import cl.oylfrutossecos.pedidos.dto.CrearPedidoRequest;
import cl.oylfrutossecos.pedidos.dto.ProductoPedidoRequest;
import cl.oylfrutossecos.pedidos.model.DetallePedido;
import cl.oylfrutossecos.pedidos.model.ModalidadPedido;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PreparadorCompraService {

    private final ClientesClient clientesClient;
    private final ProductosClient productosClient;
    private final InventarioClient inventarioClient;

    public PreparadorCompraService(
            ClientesClient clientesClient,
            ProductosClient productosClient,
            InventarioClient inventarioClient
    ) {

        this.clientesClient = clientesClient;
        this.productosClient = productosClient;
        this.inventarioClient = inventarioClient;
    }

    // RESULTADO INTERNO DE LA PREPARACION

    public record CompraPreparada(
            UUID solicitudId,
            Long clienteId,
            String nombreCliente,
            String telefono,
            ModalidadPedido modalidad,
            String direccion,
            List<DetallePedido> productos,
            int total
    ) {
    }

    // 1. PREPARAR COMPRA

    public CompraPreparada preparar(
            Jwt jwt,
            CrearPedidoRequest datos
    ) {

        validarSolicitud(datos);

        // Consultar la identidad real del comprador.

        ClienteRemoto cliente =
                clientesClient.consultarPerfil(jwt);

        if (!"CLIENTE".equals(cliente.rol())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo los clientes pueden realizar compras"
            );
        }

        List<DetallePedido> detalles =
                new ArrayList<>();

        // Consultar cada producto y sus existencias.

        for (ProductoPedidoRequest solicitado
                : datos.productos()) {

            ProductoRemoto producto =
                    productosClient.consultarProductoActivo(
                            solicitado.idProducto()
                    );

            InventarioRemoto inventario =
                    inventarioClient.consultarInventario(
                            solicitado.idProducto()
                    );

            if (
                    inventario.stockDisponible()
                            < solicitado.cantidad()
            ) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stock insuficiente para "
                                + producto.nombre()
                );
            }

            // Utilizar exclusivamente los precios oficiales.

            DetallePedido detalle = new DetallePedido(
                    producto.id(),
                    producto.nombre(),
                    solicitado.cantidad(),
                    producto.precio()
            );

            detalles.add(detalle);
        }

        // Calcular el total mediante nuestras reglas.

        int total;

        try {

            total = ReglasCreacionPedido.calcularTotal(
                    detalles
            );

        } catch (IllegalArgumentException error) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    error.getMessage()
            );
        }

        // Preparar los datos para el registro definitivo.

        return new CompraPreparada(
                datos.solicitudId(),
                cliente.id(),
                datos.nombreCliente().trim(),
                datos.telefono().trim(),
                datos.modalidad(),

                datos.modalidad() == ModalidadPedido.RETIRO
                        ? ""
                        : datos.direccion().trim(),

                List.copyOf(detalles),
                total
        );
    }

    // 2. VALIDAR SOLICITUD ANTES DE CONSULTAR SERVICIOS

    public void validarSolicitud(
            CrearPedidoRequest datos
    ) {

        if (
                datos == null
                        || datos.solicitudId() == null
        ) {

            throw solicitudInvalida(
                    "El identificador de solicitud es obligatorio"
            );
        }

        if (
                datos.nombreCliente() == null
                        || datos.nombreCliente().trim().length() < 3
                        || datos.nombreCliente().trim().length() > 120
        ) {

            throw solicitudInvalida(
                    "El nombre del cliente no es valido"
            );
        }

        if (
                datos.telefono() == null
                        || datos.telefono().length() > 30
                        || !datos.telefono().matches("[+0-9 ()-]+")
                        || datos.telefono().chars()
                        .filter(c -> c >= '0' && c <= '9')
                        .count() < 9
        ) {

            throw solicitudInvalida(
                    "El telefono no es valido"
            );
        }

        if (datos.modalidad() == null) {

            throw solicitudInvalida(
                    "La modalidad es obligatoria"
            );
        }

        if (
                datos.modalidad() == ModalidadPedido.DESPACHO
                        && (
                        datos.direccion() == null
                                || datos.direccion().trim().length() < 10
                                || datos.direccion().trim().length() > 250
                )
        ) {

            throw solicitudInvalida(
                    "La direccion de despacho no es valida"
            );
        }

        if (
                datos.productos() == null
                        || datos.productos().isEmpty()
                        || datos.productos().size() > 50
        ) {

            throw solicitudInvalida(
                    "La cantidad de productos no es valida"
            );
        }

        Set<Long> identificadores = new HashSet<>();

        for (ProductoPedidoRequest producto
                : datos.productos()) {

            if (
                    producto == null
                            || producto.idProducto() == null
                            || producto.idProducto() <= 0
                            || producto.cantidad() == null
                            || producto.cantidad() <= 0
            ) {

                throw solicitudInvalida(
                        "Producto o cantidad invalida"
                );
            }

            if (!identificadores.add(
                    producto.idProducto()
            )) {

                throw solicitudInvalida(
                        "La compra contiene productos duplicados"
                );
            }
        }
    }

    // 3. CENTRALIZAR ERRORES DE VALIDACION

    private ResponseStatusException solicitudInvalida(
            String mensaje
    ) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                mensaje
        );
    }
}