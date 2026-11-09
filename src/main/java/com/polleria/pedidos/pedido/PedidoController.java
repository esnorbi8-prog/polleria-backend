package com.polleria.pedidos.pedido;

import com.polleria.pedidos.historial.HistorialEstadoPedido;
import com.polleria.pedidos.historial.HistorialEstadoPedidoRepository;
import com.polleria.pedidos.pedido.dto.CambioEstadoRequest;
import com.polleria.pedidos.pedido.dto.PedidoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.polleria.pedidos.usuario.Usuario;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
import java.util.Map;

/**
 * Endpoints del módulo de Pedidos.
 *
 * Base: /api/pedidos
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final HistorialEstadoPedidoRepository historialRepository;

    public PedidoController(PedidoService pedidoService, HistorialEstadoPedidoRepository historialRepository) {
        this.pedidoService = pedidoService;
        this.historialRepository = historialRepository;
    }

    /** POST /api/pedidos — crea un pedido nuevo (queda en estado RECIBIDO). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pedido crear(@Valid @RequestBody PedidoRequest request) {
        return pedidoService.crearPedido(request);
    }

    /** GET /api/pedidos — lista todos los pedidos, opcionalmente filtrando por estado. */
    @GetMapping
    public List<Pedido> listar(@RequestParam(required = false) EstadoPedido estado) {
        return estado == null ? pedidoService.listarTodos() : pedidoService.listarPorEstado(estado);
    }

    /** GET /api/pedidos/{id} — detalle de un pedido. */
    @GetMapping("/{id}")
    public Pedido obtener(@PathVariable Long id) {
        return pedidoService.buscarPorId(id);
    }

    /**
     * PATCH /api/pedidos/{id}/estado — cambia el estado del pedido.
     * Valida la transición y dispara el aviso (historial + notificación en pantalla).
     */
    @PatchMapping("/{id}/estado")
    public Pedido cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest request) {
        return pedidoService.cambiarEstado(id, request.getNuevoEstado(), request.getObservacion());
    }

    /**
     * POST /api/pedidos/{id}/cancelar — cancela un pedido (cliente cancela su propio pedido, o cajera/admin).
     */
    @PostMapping("/{id}/cancelar")
    public Pedido cancelar(@PathVariable Long id,
                           @AuthenticationPrincipal Usuario usuario,
                           @RequestBody(required = false) Map<String, String> body) {
        String motivo = (body != null) ? body.get("motivo") : null;
        return pedidoService.cancelarPedido(id, usuario, motivo);
    }

    /** GET /api/pedidos/{id}/historial — historial de cambios de estado de un pedido (para seguimiento). */
    @GetMapping("/{id}/historial")
    public List<HistorialEstadoPedido> historial(@PathVariable Long id) {
        // valida que el pedido exista
        pedidoService.buscarPorId(id);
        return historialRepository.findByPedidoIdOrderByFechaCambioAsc(id);
    }
}
