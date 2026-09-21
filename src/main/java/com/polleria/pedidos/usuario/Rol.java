package com.polleria.pedidos.usuario;

/**
 * Roles del sistema (RF-A01). Cada uno corresponde a un actor de la
 * casuística: Cliente, Cocinero, Repartidor, Cajera y Administrador.
 *
 * Se usa tal cual como el "ROLE_x" de Spring Security (ver
 * {@link Usuario#getAuthorities()}), así que los controllers protegen sus
 * endpoints con {@code hasRole("CLIENTE")}, {@code hasRole("ADMINISTRADOR")}, etc.
 */
public enum Rol {
    CLIENTE,
    COCINERO,
    REPARTIDOR,
    CAJERA,
    ADMINISTRADOR
}
