package com.polleria.pedidos.cliente;

import com.polleria.pedidos.cliente.dto.ActualizarPerfilClienteRequest;
import com.polleria.pedidos.common.RecursoNoEncontradoException;
import com.polleria.pedidos.usuario.Usuario;
import com.polleria.pedidos.usuario.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteRepository clienteRepository;
    private final UsuarioService usuarioService;

    public ClienteController(ClienteRepository clienteRepository, UsuarioService usuarioService) {
        this.clienteRepository = clienteRepository;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    @GetMapping("/me")
    public Cliente obtenerMiPerfil(@AuthenticationPrincipal Usuario usuario) {
        return clienteRepository.findAll().stream()
                .filter(c -> usuario.getId().equals(c.getUsuarioId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el perfil de cliente asociado a esta cuenta."));
    }

    @PutMapping("/me")
    public Cliente actualizarMiPerfil(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody ActualizarPerfilClienteRequest request) {
        return usuarioService.actualizarPerfilCliente(usuario.getId(), request);
    }

    @GetMapping("/{id}")
    public Cliente obtener(@PathVariable Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Cliente crear(@Valid @RequestBody Cliente cliente) {
        cliente.setId(null);
        return clienteRepository.save(cliente);
    }
}
