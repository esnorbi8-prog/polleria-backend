package com.polleria.pedidos.ubicacion;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Última posición GPS conocida del repartidor para un pedido en camino (RF-D02).
 *
 * Se guarda UNA fila por pedido y se va sobreescribiendo con cada envío del
 * celular del repartidor: para el seguimiento en vivo solo importa dónde está
 * ahora, no todo el recorrido.
 */
@Entity
@Table(name = "ubicacion_repartidor")
public class UbicacionRepartidor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long pedidoId;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    /** Precisión reportada por el GPS, en metros (puede venir vacía). */
    private Double precisionMetros;

    /** Email de la cuenta del repartidor que envió la posición. */
    private String repartidorEmail;

    @Column(nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    /** true cuando ya se le avisó al cliente "tu motorizado está llegando" (para no repetirlo). */
    private Boolean avisoLlegadaEnviado = false;

    public UbicacionRepartidor() {
    }

    public Long getId() { return id; }
    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }
    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
    public Double getPrecisionMetros() { return precisionMetros; }
    public void setPrecisionMetros(Double precisionMetros) { this.precisionMetros = precisionMetros; }
    public String getRepartidorEmail() { return repartidorEmail; }
    public void setRepartidorEmail(String repartidorEmail) { this.repartidorEmail = repartidorEmail; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
    public boolean isAvisoLlegadaEnviado() { return Boolean.TRUE.equals(avisoLlegadaEnviado); }
    public void setAvisoLlegadaEnviado(boolean avisoLlegadaEnviado) { this.avisoLlegadaEnviado = avisoLlegadaEnviado; }
}
