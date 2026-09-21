package com.polleria.pedidos.notificacion;

import com.polleria.pedidos.evento.CambioEstadoPedidoEvent;
import com.polleria.pedidos.pedido.EstadoPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de notificaciones (primer avance): escucha CambioEstadoPedidoEvent
 * y genera el aviso correspondiente.
 *
 * Hoy el canal es "PANTALLA": se guarda una Notificacion consultable por
 * endpoint para que Angular la muestre. El mensaje ya está redactado como se
 * lo mostraríamos al cliente ("tu pedido está en camino, llega en ~20 min"),
 * para que más adelante un canal de WhatsApp pueda reusar el mismo texto sin
 * tocar PedidoService ni esta clase.
 */
@Component
public class NotificacionListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionListener.class);

    private final NotificacionRepository notificacionRepository;

    public NotificacionListener(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @EventListener
    @Transactional
    public void alCambiarEstado(CambioEstadoPedidoEvent evento) {
        String mensaje = construirMensaje(evento);

        Notificacion notificacion = new Notificacion(evento.getPedidoId(), mensaje);
        notificacionRepository.save(notificacion);

        // Aviso "aunque sea por pantalla": queda persistido y disponible por
        // GET /api/notificaciones. Este log simula además el canal de consola/servidor.
        log.info("[AVISO PEDIDO #{}] {}", evento.getPedidoId(), mensaje);
    }

    private String construirMensaje(CambioEstadoPedidoEvent evento) {
        String nombre = evento.getNombreCliente();
        EstadoPedido nuevo = evento.getEstadoNuevo();

        String base = switch (nuevo) {
            case RECIBIDO -> "Hola %s, recibimos tu pedido #%d. Te avisamos apenas se confirme el pago.".formatted(nombre, evento.getPedidoId());
            case PAGADO -> "¡Pago confirmado! Tu pedido #%d ya pasó a cocina.".formatted(evento.getPedidoId());
            case EN_PREPARACION -> "Tu pedido #%d está en preparación.".formatted(evento.getPedidoId());
            case LISTO -> "Tu pedido #%d está listo.".formatted(evento.getPedidoId());
            case EN_CAMINO -> "Tu pedido #%d salió a reparto, llega en aproximadamente 20 min.".formatted(evento.getPedidoId());
            case ENTREGADO -> "Tu pedido #%d fue entregado. ¡Gracias por tu compra!".formatted(evento.getPedidoId());
            case CANCELADO -> "Tu pedido #%d fue cancelado.".formatted(evento.getPedidoId());
        };

        if (evento.getObservacion() != null && !evento.getObservacion().isBlank()) {
            base += " (" + evento.getObservacion() + ")";
        }
        return base;
    }
}
