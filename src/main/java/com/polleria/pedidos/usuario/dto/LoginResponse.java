package com.polleria.pedidos.usuario.dto;

import com.polleria.pedidos.usuario.Rol;

/** Respuesta del login: el token que hay que mandar en el header Authorization. */
public class LoginResponse {

    private final String token;
    private final String tipo = "Bearer";
    private final Long usuarioId;
    private final String nombreCompleto;
    private final String email;
    private final Rol rol;

    public LoginResponse(String token, Long usuarioId, String nombreCompleto, String email, Rol rol) {
        this.token = token;
        this.usuarioId = usuarioId;
        this.nombreCompleto = nombreCompleto;
        this.email = email;
        this.rol = rol;
    }

    public String getToken() {
        return token;
    }

    public String getTipo() {
        return tipo;
    }

    public Long getUsuarioId() {
        return usuarioId;
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
}
