package com.polleria.pedidos.usuario;

import com.polleria.pedidos.usuario.dto.AltaPersonalRequest;
import com.polleria.pedidos.usuario.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestión de cuentas de personal (RF-A01): solo un ADMINISTRADOR puede dar
 * de alta cocineros, repartidores, cajeras u otros administradores, y
 * activar/desactivar cuentas. La autorización por rol la aplica
 * {@code SecurityConfig} sobre "/api/usuarios/**"; este controller no vuelve
 * a chequear el rol.
 *
 * Base: /api/usuarios
 */
@RestController
@RequestMapping("/api/usuarios")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    public AdminUsuarioController(UsuarioService usuarioService, UsuarioRepository usuarioRepository) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    /** GET /api/usuarios — lista todas las cuentas (para el panel de administración). */
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::new).toList();
    }

    /** POST /api/usuarios — da de alta una cuenta de personal con el rol indicado. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody AltaPersonalRequest request) {
        Usuario usuario = usuarioService.crearPersonal(request);
        return new UsuarioResponse(usuario);
    }

    /** PATCH /api/usuarios/{id}/estado?activo=false — activa o desactiva una cuenta. */
    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable Long id, @RequestParam boolean activo) {
        Usuario usuario = usuarioService.cambiarActivo(id, activo);
        return new UsuarioResponse(usuario);
    }

    /** PUT /api/usuarios/{id} — edita una cuenta de personal. */
    @PutMapping("/{id}")
    public UsuarioResponse editar(@PathVariable Long id, @Valid @RequestBody com.polleria.pedidos.usuario.dto.EdicionPersonalRequest request) {
        Usuario usuario = usuarioService.editarPersonal(id, request);
        return new UsuarioResponse(usuario);
    }

    /** DELETE /api/usuarios/{id} — elimina permanentemente una cuenta. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
    }
}
