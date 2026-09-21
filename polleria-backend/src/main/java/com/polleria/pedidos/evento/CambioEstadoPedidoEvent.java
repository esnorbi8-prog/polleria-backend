package com.polleria.pedidos.evento;

import com.polleria.pedidos.pedido.EstadoPedido;

import java.time.LocalDateTime;

/**
 * Evento de dominio publicado cada vez que un pedido cambia de estado.
 *
 * Es el punto de desacople entre la lógica de pedidos (PedidoService) y todo
 * lo que reacciona a esos cambios: historial, notificaciones en pantalla y,
 * a futuro, WhatsApp / boleta por correo — cada uno como un listener
 * independiente, sin que PedidoService conozca su existencia.
 */
public class CambioEstadoPedidoEvent {

    private final Long pedidoId;
    private final Long clienteId;
    private final String nombreCliente;
    private final EstadoPedido estadoAnterior;
    private final EstadoPedido estadoNuevo;
    private final String observacion;
    private final LocalDateTime fecha;

    public CambioEstadoPedidoEvent(Long pedidoId, Long clienteId, String nombreCliente,
                                    EstadoPedido estadoAnterior, EstadoPedido estadoNuevo,
                                    String observacion) {
        this.pedidoId = pedidoId;
        this.clienteId = clienteId;
        this.nombreCliente = nombreCliente;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.observacion = observacion;
        this.fecha = LocalDateTime.now();
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public EstadoPedido getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoPedido getEstadoNuevo() {
        return estadoNuevo;
    }

    public String getObservacion() {
        return observacion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
