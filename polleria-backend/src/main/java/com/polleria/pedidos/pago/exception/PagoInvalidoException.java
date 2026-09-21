package com.polleria.pedidos.pago.exception;

/**
 * Se lanza cuando algo del flujo de pago no es válido: el pedido no está en
 * el estado correcto para pagar, Mercado Pago rechaza la solicitud, o el pago
 * consultado todavía no está aprobado.
 */
public class PagoInvalidoException extends RuntimeException {

    public PagoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
