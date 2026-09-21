package com.polleria.pedidos.usuario;

import com.polleria.pedidos.cliente.Cliente;
import com.polleria.pedidos.cliente.ClienteRepository;
import com.polleria.pedidos.usuario.dto.AltaPersonalRequest;
import com.polleria.pedidos.usuario.dto.RegistroRequest;
import com.polleria.pedidos.usuario.exception.EmailYaRegistradoException;
import com.polleria.pedidos.usuario.exception.TokenVerificacionInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Alta y verificación de cuentas (RF-C01, RF-C02, RF-A01).
 *
 * No hay todavía un servicio real de envío de correos en el proyecto (queda
 * para el avance del "comprobante/boleta por correo" de la casuística), así
 * que la verificación de email se simula: se genera un token y se loguea
 * como si fuera el contenido del correo. El endpoint de verificación
 * (`/api/auth/verificar-email`) funciona igual que con un correo real; solo
 * cambia de dónde se copia el token durante las pruebas.
 */
@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registro público (siempre rol CLIENTE). Crea el Usuario y, junto con
     * él, el Cliente de contacto (para que los pedidos sigan funcionando
     * exactamente igual que antes: `PedidoRequest.clienteId` no cambió).
     */
    @Transactional
    public Usuario registrarCliente(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EmailYaRegistradoException(request.getEmail());
        }

        Usuario usuario = new Usuario(request.getNombreCompleto(), request.getEmail(),
                passwordEncoder.encode(request.getPassword()), Rol.CLIENTE);
        usuario.setTokenVerificacion(UUID.randomUUID().toString());
        usuario = usuarioRepository.save(usuario);

        Cliente cliente = new Cliente(request.getNombreCompleto(), request.getTelefono(),
                request.getEmail(), request.getDireccion());
        cliente.setUsuarioId(usuario.getId());
        clienteRepository.save(cliente);

        log.info("[EMAIL SIMULADO] Verifica tu cuenta Pollería con este token: {} (usuario #{}, {})",
                usuario.getTokenVerificacion(), usuario.getId(), usuario.getEmail());

        return usuario;
    }

    /**
     * Alta de personal por un administrador (RF-A01). A diferencia del
     * auto-registro, estas cuentas quedan verificadas de inmediato: el
     * administrador ya confirmó la identidad de la persona al crearlas.
     */
    @Transactional
    public Usuario crearPersonal(AltaPersonalRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EmailYaRegistradoException(request.getEmail());
        }

        Usuario usuario = new Usuario(request.getNombreCompleto(), request.getEmail(),
                passwordEncoder.encode(request.getPassword()), request.getRol());
        usuario.setEmailVerificado(true);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario verificarEmail(String token) {
        Usuario usuario = usuarioRepository.findByTokenVerificacion(token)
                .orElseThrow(TokenVerificacionInvalidoException::new);

        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacion(null);
        return usuarioRepository.save(usuario);
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "No existe una cuenta con el correo: " + email));
    }

    @Transactional
    public Usuario cambiarActivo(Long usuarioId, boolean activo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new com.polleria.pedidos.common.RecursoNoEncontradoException(
                        "Usuario no encontrado: " + usuarioId));
        usuario.setActivo(activo);
        return usuarioRepository.save(usuario);
    }
}
