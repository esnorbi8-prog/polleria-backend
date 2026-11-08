package com.polleria.pedidos.whatsapp;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Registro de cada mensaje de WhatsApp que el sistema intentó enviar.
 * Sirve para que el administrador supervise el bot (RF-A10) y para depurar
 * por qué un mensaje no llegó (columna "detalle").
 */
@Entity
@Table(name = "mensajes_whatsapp")
public class MensajeWhatsapp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Pedido al que se refiere el mensaje (null en los mensajes de prueba). */
    private Long pedidoId;

    /** Número en formato internacional sin "+", ej. 51987654321. */
    private String destinatario;

    /** Motivo del mensaje: PAGADO, EN_CAMINO, REPARTIDOR_CERCA, PRUEBA... */
    @Column(length = 30)
    private String motivo;

    @Column(length = 2000)
    private String texto;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private EstadoEnvio estado;

    /** Id que devuelve Meta si se aceptó, o el error si falló. */
    @Column(length = 1000)
    private String detalle;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    public MensajeWhatsapp() {
    }

    public MensajeWhatsapp(Long pedidoId, String destinatario, String motivo, String texto,
                           EstadoEnvio estado, String detalle) {
        this.pedidoId = pedidoId;
        this.destinatario = destinatario;
        this.motivo = motivo;
        this.texto = texto;
        this.estado = estado;
        this.detalle = detalle;
    }

    public Long getId() { return id; }
    public Long getPedidoId() { return pedidoId; }
    public String getDestinatario() { return destinatario; }
    public String getMotivo() { return motivo; }
    public String getTexto() { return texto; }
    public EstadoEnvio getEstado() { return estado; }
    public String getDetalle() { return detalle; }
    public LocalDateTime getFecha() { return fecha; }
}
