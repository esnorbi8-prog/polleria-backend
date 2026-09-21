package com.polleria.pedidos.common;

/** Excepción genérica de "no encontrado" (pedido, producto o cliente inexistente). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
