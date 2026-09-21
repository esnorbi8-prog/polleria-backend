package com.polleria.pedidos.cliente;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Cliente que realiza pedidos. Sigue guardando solo datos de contacto: el
 * login/contraseña vive en {@link com.polleria.pedidos.usuario.Usuario}
 * (RF-C01/RF-C02), enlazado acá por {@code usuarioId} cuando el cliente se
 * registró él mismo. Un Cliente puede no tener usuario asociado si lo creó
 * caja/admin a mano (pedido telefónico, sin cuenta).
 */
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nombre;

    @NotBlank
    @Column(nullable = false)
    private String telefono;

    @Email
    private String email;

    private String direccion;

    /**
     * Enlaza este Cliente con su cuenta de acceso (Usuario con rol CLIENTE).
     * Nullable porque los clientes cargados antes de este avance (o creados
     * a mano por caja/admin para pedidos telefónicos) no tienen login.
     */
    @Column(name = "usuario_id")
    private Long usuarioId;

    public Cliente() {
    }

    public Cliente(String nombre, String telefono, String email, String direccion) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }
}
