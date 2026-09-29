package com.polleria.pedidos.seguridad;

import com.polleria.pedidos.usuario.Usuario;
import com.polleria.pedidos.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Se ejecuta una vez por request: si viene un header
 * {@code Authorization: Bearer <token>} válido, deja al usuario autenticado
 * en el SecurityContext para el resto del pipeline (igual que si hubiera una
 * sesión, pero sin guardar nada en el servidor).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            String email = jwtService.extraerEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<Usuario> usuario = usuarioRepository.findByEmail(email);

                if (usuario.isPresent() && jwtService.esValido(token)) {
                    Usuario u = usuario.get();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Refrescar el token si el admin o cocina muestran actividad de modificación (creación o edición)
                    String rol = u.getRol().name();
                    if ("ADMIN".equals(rol) || "COCINA".equals(rol)) {
                        String method = request.getMethod();
                        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method)) {
                            String nuevoToken = jwtService.generarToken(u.getEmail(), rol, u.getId());
                            response.setHeader("X-New-Token", nuevoToken);
                            response.setHeader("Access-Control-Expose-Headers", "X-New-Token");
                        }
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
