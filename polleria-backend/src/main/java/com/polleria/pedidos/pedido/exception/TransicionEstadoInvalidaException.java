package com.polleria.pedidos.pedido.exception;

import com.polleria.pedidos.pedido.EstadoPedido;

/** Se lanza cuando se intenta un cambio de estado que la máquina de estados no permite. */
public class TransicionEstadoInvalidaException extends RuntimeException {

    public TransicionEstadoInvalidaException(EstadoPedido actual, EstadoPedido destino) {
        super("No se puede pasar el pedido de estado %s a %s".formatted(actual, destino));
    }
}
