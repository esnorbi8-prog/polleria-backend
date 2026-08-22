package com.polleria.pedidos.historial;

import com.polleria.pedidos.pedido.EstadoPedido;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Registro histórico de cada transición de estado de un pedido.
 * Es la "fuente de verdad" que permite reconstruir el seguimiento del pedido
 * (RF-C09) y auditar el flujo completo de estados (RF-A04/RF-A05).
 */
@Entity
@Table(name = "historial_estado_pedido")
public class HistorialEstadoPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoPedido estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estadoNuevo;

    @Column(nullable = false)
    private LocalDateTime fechaCambio = LocalDateTime.now();

    private String observacion;

    public HistorialEstadoPedido() {
    }

    public HistorialEstadoPedido(Long pedidoId, EstadoPedido estadoAnterior, EstadoPedido estadoNuevo, String observacion) {
        this.pedidoId = pedidoId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.observacion = observacion;
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

    public EstadoPedido getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(EstadoPedido estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoPedido getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(EstadoPedido estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(LocalDateTime fechaCambio) {
        this.fechaCambio = fechaCambio;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
