package cl.oylfrutossecos.pedidos.service;

import cl.oylfrutossecos.pedidos.domain.ReglasEstadoPedido;

import cl.oylfrutossecos.pedidos.dto.PedidoResponse;

import cl.oylfrutossecos.pedidos.model.EstadoPedido;
import cl.oylfrutossecos.pedidos.model.Pedido;

import cl.oylfrutossecos.pedidos.repository.PedidoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GestionEstadoPedidoService {

    private final PedidoRepository repository;
    private final PedidoService pedidoService;

    public GestionEstadoPedidoService(
            PedidoRepository repository,
            PedidoService pedidoService
    ) {

        this.repository = repository;
        this.pedidoService = pedidoService;
    }

    // CAMBIAR EL ESTADO DE UN PEDIDO

    @Transactional
    public PedidoResponse cambiarEstado(
            String numero,
            EstadoPedido nuevoEstado
    ) {

        if (nuevoEstado == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nuevo estado es obligatorio"
            );
        }

        // Buscar y bloquear el pedido.
        Pedido pedido = repository
                .buscarConBloqueo(numero)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Pedido no encontrado"
                        )
                );

        // Comprobar que la transicion este permitida.
        boolean permitido =
                ReglasEstadoPedido.puedeCambiarEstado(
                        pedido.getEstado(),
                        nuevoEstado,
                        pedido.getModalidad()
                );

        if (!permitido) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se permite cambiar de "
                            + pedido.getEstado()
                            + " a "
                            + nuevoEstado
            );
        }

        // Actualizar el estado.
        pedido.setEstado(nuevoEstado);

        repository.saveAndFlush(pedido);

        // Reutilizar la conversion a DTO de PedidoService.
        return pedidoService.buscarPorNumero(numero);
    }
}