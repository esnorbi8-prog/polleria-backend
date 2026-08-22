package com.polleria.pedidos.notificacion;

import com.polleria.pedidos.common.RecursoNoEncontradoException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de notificaciones ("avisos en pantalla" de cambios de estado).
 * Pensado para que el frontend (Angular) haga polling y muestre los avisos
 * más recientes, tipo campanita de notificaciones.
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionRepository notificacionRepository;

    public NotificacionController(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    /** GET /api/notificaciones — feed global de avisos (el más reciente primero). */
    @GetMapping
    public List<Notificacion> listarTodas() {
        return notificacionRepository.findAllByOrderByFechaEnvioDesc();
    }

    /** GET /api/notificaciones/pedido/{pedidoId} — avisos de un pedido puntual. */
    @GetMapping("/pedido/{pedidoId}")
    public List<Notificacion> listarPorPedido(@PathVariable Long pedidoId) {
        return notificacionRepository.findByPedidoIdOrderByFechaEnvioDesc(pedidoId);
    }

    /** PATCH /api/notificaciones/{id}/leida — marca un aviso como leído en pantalla. */
    @PatchMapping("/{id}/leida")
    public Notificacion marcarLeida(@PathVariable Long id) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificación no encontrada: " + id));
        notificacion.setLeida(true);
        return notificacionRepository.save(notificacion);
    }
}
