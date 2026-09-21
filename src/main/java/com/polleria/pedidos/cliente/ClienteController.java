package com.polleria.pedidos.cliente;

import com.polleria.pedidos.common.RecursoNoEncontradoException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteRepository clienteRepository;

    public ClienteController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @GetMapping
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    @GetMapping("/me")
    public Cliente obtenerMiPerfil(@org.springframework.security.core.annotation.AuthenticationPrincipal com.polleria.pedidos.usuario.Usuario usuario) {
        return clienteRepository.findAll().stream()
                .filter(c -> usuario.getId().equals(c.getUsuarioId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el perfil de cliente asociado a esta cuenta."));
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
