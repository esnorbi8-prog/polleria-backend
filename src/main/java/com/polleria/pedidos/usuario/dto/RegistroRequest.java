package com.polleria.pedidos.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para el auto-registro público (RF-C01). Siempre crea una cuenta con
 * rol CLIENTE — para dar de alta personal (cocinero, repartidor, cajera,
 * administrador) existe un endpoint aparte que solo puede usar un
 * administrador (ver {@link AltaPersonalRequest}), porque esos roles no se
 * auto-asignan.
 */
public class RegistroRequest {

    @NotBlank
    private String nombreCompleto;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank
    private String telefono;

    private String direccion;

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
}
