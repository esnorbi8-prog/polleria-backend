package com.polleria.pedidos.usuario.exception;

public class TokenVerificacionInvalidoException extends RuntimeException {
    public TokenVerificacionInvalidoException() {
        super("El token de verificación es inválido o ya fue usado");
    }
}
