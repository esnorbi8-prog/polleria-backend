package com.polleria.pedidos.usuario.dto;

import com.polleria.pedidos.usuario.Rol;
import com.polleria.pedidos.usuario.Usuario;

import java.time.LocalDateTime;

/**
 * Vista pública de un Usuario para respuestas de la API: nunca incluye el
 * hash de la contraseña ni el token de verificación.
 */
public class UsuarioResponse {

    private final Long id;
    private final String nombreCompleto;
    private final String email;
    private final Rol rol;
    private final boolean emailVerificado;
    private final boolean activo;
    private final LocalDateTime fechaCreacion;
    private final String tokenVerificacion; // Para la demo en el frontend

    public UsuarioResponse(Usuario u) {
        this.id = u.getId();
        this.nombreCompleto = u.getNombreCompleto();
        this.email = u.getEmail();
        this.rol = u.getRol();
        this.emailVerificado = u.isEmailVerificado();
        this.activo = u.isActivo();
        this.fechaCreacion = u.getFechaCreacion();
        this.tokenVerificacion = u.getTokenVerificacion();
    }

    public Long getId() {
        return id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getEmail() {
        return email;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean isEmailVerificado() {
        return emailVerificado;
    }

    public boolean isActivo() {
        return activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public String getTokenVerificacion() {
        return tokenVerificacion;
    }
}
