package com.polleria.pedidos.common;

import com.polleria.pedidos.pago.exception.PagoInvalidoException;
import com.polleria.pedidos.pedido.exception.StockInsuficienteException;
import com.polleria.pedidos.pedido.exception.TransicionEstadoInvalidaException;
import com.polleria.pedidos.usuario.exception.CredencialesInvalidasException;
import com.polleria.pedidos.usuario.exception.EmailYaRegistradoException;
import com.polleria.pedidos.usuario.exception.TokenVerificacionInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Traduce las excepciones de negocio a respuestas HTTP claras para el frontend. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleTransicionInvalida(TransicionEstadoInvalidaException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> handleStockInsuficiente(StockInsuficienteException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PagoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handlePagoInvalido(PagoInvalidoException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EmailYaRegistradoException.class)
    public ResponseEntity<Map<String, Object>> handleEmailYaRegistrado(EmailYaRegistradoException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(TokenVerificacionInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleTokenInvalido(TokenVerificacionInvalidoException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, Object>> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::toString)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "Datos inválidos: " + detalle);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
