package com.polleria.pedidos.pedido;

/**
 * Estados por los que pasa un pedido de comida, según la casuística:
 * recibido, pagado, en preparación, en camino, entregado; más los estados
 * de excepción CANCELADO (cancelación a medio preparar / pago abandonado).
 */
public enum EstadoPedido {

    /** El pedido se registró en el sistema, pendiente de pago. */
    RECIBIDO,

    /** El pago fue confirmado (por Mercado Pago u otro medio). */
    PAGADO,

    /** Cocina está preparando el pedido. */
    EN_PREPARACION,

    /** Cocina terminó de preparar el pedido (listo para despacho/entrega en local). */
    LISTO,

    /** El repartidor salió con el pedido hacia la dirección del cliente. */
    EN_CAMINO,

    /** El pedido fue entregado al cliente (retiro en local o delivery). */
    ENTREGADO,

    /**
     * El pedido se canceló: puede ocurrir antes de pagar, mientras se prepara,
     * o si el cliente abandona el pago a mitad de camino.
     */
    CANCELADO
}
