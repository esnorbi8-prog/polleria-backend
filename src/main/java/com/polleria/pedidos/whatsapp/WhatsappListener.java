package com.polleria.pedidos.whatsapp;

import com.polleria.pedidos.evento.CambioEstadoPedidoEvent;
import com.polleria.pedidos.evento.RepartidorCercaEvent;
import com.polleria.pedidos.pedido.EstadoPedido;
import com.polleria.pedidos.pedido.Pedido;
import com.polleria.pedidos.pedido.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Servicio de notificaciones por WhatsApp (casuística, prueba de concepto):
 * escucha los cambios de estado del pedido y manda el mensaje que corresponde.
 *
 * Igual que HistorialListener y NotificacionListener, PedidoService no sabe
 * que esta clase existe: solo reacciona al evento de dominio.
 *
 * - AFTER_COMMIT: solo avisa si el cambio de estado realmente se guardó.
 * - @Async: el envío corre en segundo plano y nunca frena ni rompe el pedido.
 */
@Component
public class WhatsappListener {

    private static final Logger log = LoggerFactory.getLogger(WhatsappListener.class);

    private final PedidoRepository pedidoRepository;
    private final WhatsappMensajes mensajes;
    private final WhatsappClient client;
    private final Set<EstadoPedido> estadosNotificados;

    public WhatsappListener(PedidoRepository pedidoRepository,
                            WhatsappMensajes mensajes,
                            WhatsappClient client,
                            @Value("${whatsapp.estados-notificados:PAGADO,EN_PREPARACION,LISTO,EN_CAMINO,ENTREGADO,CANCELADO}") String estados) {
        this.pedidoRepository = pedidoRepository;
        this.mensajes = mensajes;
        this.client = client;
        this.estadosNotificados = EnumSet.noneOf(EstadoPedido.class);
        Arrays.stream(estados.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(s -> estadosNotificados.add(EstadoPedido.valueOf(s.toUpperCase())));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alCambiarEstado(CambioEstadoPedidoEvent evento) {
        if (!estadosNotificados.contains(evento.getEstadoNuevo())) return;

        Pedido pedido = pedidoRepository.findById(evento.getPedidoId()).orElse(null);
        if (pedido == null || pedido.getCliente() == null) return;

        String texto = mensajes.porEstado(pedido, evento.getEstadoNuevo(), evento.getObservacion());
        if (texto == null) return;

        client.enviar(pedido.getId(), pedido.getCliente().getTelefono(), pedido.getCliente().getNombre(),
                evento.getEstadoNuevo().name(), texto);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alAcercarseRepartidor(RepartidorCercaEvent evento) {
        Pedido pedido = pedidoRepository.findById(evento.getPedidoId()).orElse(null);
        if (pedido == null || pedido.getCliente() == null) return;

        log.info("[WHATSAPP] Repartidor a {} m del destino del pedido #{}", evento.getDistanciaMetros(), pedido.getId());
        client.enviar(pedido.getId(), pedido.getCliente().getTelefono(), pedido.getCliente().getNombre(),
                "REPARTIDOR_CERCA", mensajes.repartidorCerca(pedido, evento.getDistanciaMetros()));
    }
}
