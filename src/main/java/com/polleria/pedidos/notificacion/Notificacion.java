package com.polleria.pedidos.notificacion;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Aviso generado cada vez que un pedido cambia de estado.
 *
 * Para este primer avance el "canal" es PANTALLA: el aviso queda guardado y
 * se consulta vía endpoint para que el frontend (Angular) lo muestre.
 * Más adelante, un servicio de notificaciones independiente (WhatsApp, email)
 * podrá escuchar el mismo evento de cambio de estado y agregar más canales,
 * sin tocar la lógica de pedidos (separación pedida en la casuística).
 */
@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(nullable = false, length = 20)
    private String canal = "PANTALLA";

    @Column(nullable = false)
    private LocalDateTime fechaEnvio = LocalDateTime.now();

    @Column(nullable = false)
    private boolean leida = false;

    public Notificacion() {
    }

    public Notificacion(Long pedidoId, String mensaje) {
        this.pedidoId = pedidoId;
        this.mensaje = mensaje;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }
}
