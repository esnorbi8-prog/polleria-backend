package com.polleria.pedidos.usuario.dto;  
  
import com.polleria.pedidos.usuario.Rol;  
import jakarta.validation.constraints.Email;  
import jakarta.validation.constraints.NotBlank;  
import jakarta.validation.constraints.NotNull;  
  
public class EdicionPersonalRequest {  
    @NotBlank private String nombreCompleto;  
    @NotBlank @Email private String email;  
    @NotNull private Rol rol;  
    private String password; // opcional  
    public String getNombreCompleto() { return nombreCompleto; }  
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }  
    public String getEmail() { return email; }  
    public void setEmail(String email) { this.email = email; }  
    public Rol getRol() { return rol; }  
    public void setRol(Rol rol) { this.rol = rol; }  
    public String getPassword() { return password; }  
    public void setPassword(String password) { this.password = password; }  
} 
