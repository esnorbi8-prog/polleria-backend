package com.polleria.pedidos.usuario;

import com.polleria.pedidos.seguridad.JwtService;
import com.polleria.pedidos.usuario.dto.LoginRequest;
import com.polleria.pedidos.usuario.dto.LoginResponse;
import com.polleria.pedidos.usuario.dto.RegistroRequest;
import com.polleria.pedidos.usuario.dto.UsuarioResponse;
import com.polleria.pedidos.usuario.exception.CredencialesInvalidasException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints públicos de cuenta: registro de clientes, login y verificación
 * de email (RF-C01, RF-C02). Base: /api/auth
 */
@RestController
@RequestMapping("/api/auth")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public UsuarioController(UsuarioService usuarioService, AuthenticationManager authenticationManager,
                              JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /** POST /api/auth/registro — auto-registro de un cliente nuevo (queda con el email sin verificar). */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest request) {
        Usuario usuario = usuarioService.registrarCliente(request);
        return new UsuarioResponse(usuario);
    }

    /**
     * POST /api/auth/verificar-email?token=... — confirma la cuenta con el
     * token generado en el registro (en este avance se simula por log; ver
     * la consola del backend justo después de registrarse).
     */
    @PostMapping("/verificar-email")
    public UsuarioResponse verificarEmail(@RequestParam String token) {
        Usuario usuario = usuarioService.verificarEmail(token);
        return new UsuarioResponse(usuario);
    }

    /** POST /api/auth/login — devuelve el JWT a usar en el header Authorization de los demás endpoints. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos");
        } catch (DisabledException e) {
            throw new CredencialesInvalidasException("Debes verificar tu correo antes de iniciar sesión");
        } catch (LockedException e) {
            throw new CredencialesInvalidasException("Esta cuenta está desactivada; contacta a un administrador");
        }

        Usuario usuario = usuarioService.buscarPorEmail(request.getEmail());
        String token = jwtService.generarToken(usuario.getEmail(), usuario.getRol().name(), usuario.getId());

        return new LoginResponse(token, usuario.getId(), usuario.getNombreCompleto(), usuario.getEmail(), usuario.getRol());
    }

    /** GET /api/auth/me — datos de la cuenta que está usando el token actual. */
    @GetMapping("/me")
    public UsuarioResponse yo(@AuthenticationPrincipal Usuario usuario) {
        return new UsuarioResponse(usuario);
    }
}
