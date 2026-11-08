package com.polleria.pedidos.evento;

/**
 * Se publica UNA sola vez por pedido, cuando el GPS del repartidor entra en el
 * radio de llegada de la dirección del cliente (por defecto 150 m).
 * Lo escucha el servicio de WhatsApp para mandar "tu motorizado está llegando".
 */
public class RepartidorCercaEvent {

    private final Long pedidoId;
    private final long distanciaMetros;

    public RepartidorCercaEvent(Long pedidoId, long distanciaMetros) {
        this.pedidoId = pedidoId;
        this.distanciaMetros = distanciaMetros;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public long getDistanciaMetros() {
        return distanciaMetros;
    }
}
