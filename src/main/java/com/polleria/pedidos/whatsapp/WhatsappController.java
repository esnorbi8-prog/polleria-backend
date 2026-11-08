package com.polleria.pedidos.whatsapp;

import com.polleria.pedidos.whatsapp.dto.PruebaWhatsappRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Supervisión del bot de WhatsApp (RF-A10). Solo ADMINISTRADOR.
 *
 * GET  /api/whatsapp/estado               — proveedor en uso (meta/twilio) y si está conectado o simulado
 * GET  /api/whatsapp/mensajes             — últimos 50 mensajes enviados
 * GET  /api/whatsapp/mensajes/pedido/{id} — mensajes de un pedido
 * POST /api/whatsapp/prueba               — manda un mensaje de prueba a un número
 * POST /api/whatsapp/webqr/cerrar-sesion  — desvincula el WhatsApp vinculado por QR
 */
@RestController
@RequestMapping("/api/whatsapp")
public class WhatsappController {

    private final WhatsappClient client;
    private final MensajeWhatsappRepository repository;
    private final WebQrWhatsappProveedor webQr;

    public WhatsappController(WhatsappClient client, MensajeWhatsappRepository repository,
                              WebQrWhatsappProveedor webQr) {
        this.client = client;
        this.repository = repository;
        this.webQr = webQr;
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("proveedor", client.getProveedor());
        estado.put("conectado", !client.isSimulado());
        estado.put("modoEnvio", client.getModoEnvio());
        if ("webqr".equals(client.getProveedor())) {
            // Estado real del WhatsApp vinculado: listo, esperando QR (con la imagen) o apagado
            estado.put("servicio", webQr.estadoServicio());
        }
        return estado;
    }

    @GetMapping("/mensajes")
    public List<MensajeWhatsapp> ultimos() {
        return repository.findTop50ByOrderByFechaDesc();
    }

    @GetMapping("/mensajes/pedido/{pedidoId}")
    public List<MensajeWhatsapp> porPedido(@PathVariable Long pedidoId) {
        return repository.findByPedidoIdOrderByFechaAsc(pedidoId);
    }

    @PostMapping("/prueba")
    public MensajeWhatsapp prueba(@Valid @RequestBody PruebaWhatsappRequest request) {
        String texto = (request.getTexto() == null || request.getTexto().isBlank())
                ? "👋 Hola, este es un mensaje de prueba del bot de la Pollería. ¡La conexión con WhatsApp funciona!"
                : request.getTexto();
        return client.enviar(null, request.getTelefono(), "Prueba", "PRUEBA", texto);
    }

    @PostMapping("/webqr/cerrar-sesion")
    public Map<?, ?> cerrarSesionWebQr() {
        if (!"webqr".equals(client.getProveedor())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El bot no está usando el proveedor webqr");
        }
        try {
            return webQr.cerrarSesion();
        } catch (ProveedorWhatsappException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }
}
