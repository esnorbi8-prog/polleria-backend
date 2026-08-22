package com.polleria.pedidos.pedido.dto;

import com.polleria.pedidos.pedido.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public class CambioEstadoRequest {

    @NotNull
    private EstadoPedido nuevoEstado;

    /** Motivo opcional, útil sobre todo para CANCELADO ("cliente abandonó el pago", "producto agotado", etc). */
    private String observacion;

    public EstadoPedido getNuevoEstado() {
        return nuevoEstado;
    }

    public void setNuevoEstado(EstadoPedido nuevoEstado) {
        this.nuevoEstado = nuevoEstado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
