package com.polleria.pedidos.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByTokenVerificacion(String tokenVerificacion);

    boolean existsByEmail(String email);
}
