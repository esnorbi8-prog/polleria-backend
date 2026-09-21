package com.polleria.pedidos.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Genera y valida los JWT que identifican al usuario logueado en cada
 * request (autenticación sin sesión de servidor: el token trae todo lo
 * necesario y se manda en el header {@code Authorization: Bearer <token>}).
 */
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(String email, String rol, Long usuarioId) {
        Date ahora = new Date();
        Date expira = new Date(ahora.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("rol", rol)
                .claim("usuarioId", usuarioId)
                .issuedAt(ahora)
                .expiration(expira)
                .signWith(key())
                .compact();
    }

    /** Devuelve el email (subject) del token, o null si es inválido/expiró. */
    public String extraerEmail(String token) {
        Claims claims = parsearClaims(token);
        return claims == null ? null : claims.getSubject();
    }

    public boolean esValido(String token) {
        return parsearClaims(token) != null;
    }

    private Claims parsearClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
