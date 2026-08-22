package com.polleria.pedidos.pedido.exception;

/** Se lanza cuando un producto no tiene stock suficiente (o está agotado) al armar un pedido. */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String nombreProducto, int disponible, int solicitado) {
        super("Stock insuficiente para '%s': disponible %d, solicitado %d"
                .formatted(nombreProducto, disponible, solicitado));
    }
}
