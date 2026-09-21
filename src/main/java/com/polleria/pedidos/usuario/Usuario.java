package com.polleria.pedidos.usuario;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Cuenta de acceso al sistema. Implementa {@link UserDetails} directamente
 * para que Spring Security la use sin necesidad de una clase adaptadora
 * aparte: más simple de mantener en un proyecto de este tamaño.
 *
 * Un Usuario con rol CLIENTE queda enlazado 1 a 1 con un
 * {@link com.polleria.pedidos.cliente.Cliente} (los datos de contacto/pedidos
 * siguen viviendo en esa tabla, que ya existía desde el primer avance).
 */
@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nombreCompleto;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    /** Hash BCrypt de la contraseña. Nunca se guarda ni se devuelve en texto plano. */
    @NotBlank
    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    /** RF-C02: mientras esté en false, el login queda bloqueado. */
    @Column(nullable = false)
    private boolean emailVerificado = false;

    /** Token de un solo uso enviado (simulado) por correo para verificar la cuenta. */
    @Column(length = 100)
    private String tokenVerificacion;

    /** Permite al administrador dar de baja una cuenta sin borrarla (auditoría). */
    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public Usuario() {
    }

    public Usuario(String nombreCompleto, String email, String passwordHash, Rol rol) {
        this.nombreCompleto = nombreCompleto;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    // ---- getters/setters de negocio ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public boolean isEmailVerificado() {
        return emailVerificado;
    }

    public void setEmailVerificado(boolean emailVerificado) {
        this.emailVerificado = emailVerificado;
    }

    public String getTokenVerificacion() {
        return tokenVerificacion;
    }

    public void setTokenVerificacion(String tokenVerificacion) {
        this.tokenVerificacion = tokenVerificacion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    // ---- contrato UserDetails (lo que usa Spring Security) ----

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return activo;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo && emailVerificado;
    }
}
