package com.polleria.pedidos.cliente.dto;

public class ActualizarPerfilClienteRequest {

    private String nombre;
    private String telefono;
    private String direccion;
    private String password;

    public ActualizarPerfilClienteRequest() {
    }

    public ActualizarPerfilClienteRequest(String nombre, String telefono, String direccion, String password) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.direccion = direccion;
        this.password = password;
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

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
