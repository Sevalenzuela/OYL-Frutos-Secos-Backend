package cl.oylfrutossecos.pedidos.config;

import cl.oylfrutossecos.pedidos.model.FaseCompra;
import cl.oylfrutossecos.pedidos.model.Pedido;

import cl.oylfrutossecos.pedidos.repository.PedidoRepository;

import cl.oylfrutossecos.pedidos.service.RegistroCompraService;
import cl.oylfrutossecos.pedidos.service.GestionEstadoPedidoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.context.annotation.Configuration;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(
        name = "oyl.recuperacion.enabled",
        havingValue = "true"
)
public class RecuperacionPedidos {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    RecuperacionPedidos.class
            );

    private final PedidoRepository repository;

    private final RegistroCompraService registroCompra;

    private final GestionEstadoPedidoService gestionEstados;

    public RecuperacionPedidos(
            PedidoRepository repository,
            RegistroCompraService registroCompra,
            GestionEstadoPedidoService gestionEstados
    ) {

        this.repository = repository;
        this.registroCompra = registroCompra;
        this.gestionEstados = gestionEstados;
    }

    // 1. EJECUTAR RECUPERACION PERIODICAMENTE

    @Scheduled(
            initialDelayString = "${oyl.recuperacion.intervalo-ms:30000}",
            fixedDelayString = "${oyl.recuperacion.intervalo-ms:30000}"
    )
    public void recuperarPendientes() {

        recuperarCompras();

        recuperarCambiosDeEstado();
    }

    // 2. RECUPERAR COMPRAS INTERRUMPIDAS

    private void recuperarCompras() {

        List<Pedido> pendientes =
                repository.findByFaseCompraInOrderByFechaAsc(
                        List.of(
                                FaseCompra.RESERVANDO,
                                FaseCompra.CANCELANDO
                        )
                );

        for (Pedido pedido : pendientes) {

            try {

                logger.info(
                        "Recuperando compra {}",
                        pedido.getNumero()
                );

                registroCompra.recuperarCompra(
                        pedido.getNumero()
                );

                logger.info(
                        "Recuperacion de compra procesada: {}",
                        pedido.getNumero()
                );

            } catch (ResponseStatusException error) {

                logger.warn(
                        "Compra {} pendiente. HTTP {}",
                        pedido.getNumero(),
                        error.getStatusCode().value()
                );

            } catch (RuntimeException error) {

                logger.error(
                        "Error recuperando compra {}: {}",
                        pedido.getNumero(),
                        error.getClass().getSimpleName()
                );
            }
        }
    }

    // 3. RECUPERAR ENTREGAS Y CANCELACIONES

    private void recuperarCambiosDeEstado() {

        List<Pedido> pendientes =
                repository
                        .findByEstadoObjetivoIsNotNullOrderByFechaAsc();

        for (Pedido pedido : pendientes) {

            try {

                logger.info(
                        "Recuperando cambio de estado {} -> {}",
                        pedido.getNumero(),
                        pedido.getEstadoObjetivo()
                );

                gestionEstados.cambiarEstado(
                        pedido.getNumero(),
                        pedido.getEstadoObjetivo()
                );

                logger.info(
                        "Cambio de estado recuperado: {}",
                        pedido.getNumero()
                );

            } catch (ResponseStatusException error) {

                logger.warn(
                        "Cambio de estado de {} pendiente. HTTP {}",
                        pedido.getNumero(),
                        error.getStatusCode().value()
                );

            } catch (RuntimeException error) {

                logger.error(
                        "Error recuperando estado de {}: {}",
                        pedido.getNumero(),
                        error.getClass().getSimpleName()
                );
            }
        }
    }
}