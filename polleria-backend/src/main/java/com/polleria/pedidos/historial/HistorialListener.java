package com.polleria.pedidos.historial;

import com.polleria.pedidos.evento.CambioEstadoPedidoEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escucha los cambios de estado y los deja registrados en el historial,
 * de forma totalmente independiente de PedidoService.
 */
@Component
public class HistorialListener {

    private final HistorialEstadoPedidoRepository historialRepository;

    public HistorialListener(HistorialEstadoPedidoRepository historialRepository) {
        this.historialRepository = historialRepository;
    }

    @EventListener
    @Transactional
    public void alCambiarEstado(CambioEstadoPedidoEvent evento) {
        HistorialEstadoPedido registro = new HistorialEstadoPedido(
                evento.getPedidoId(),
                evento.getEstadoAnterior(),
                evento.getEstadoNuevo(),
                evento.getObservacion()
        );
        historialRepository.save(registro);
    }
}
