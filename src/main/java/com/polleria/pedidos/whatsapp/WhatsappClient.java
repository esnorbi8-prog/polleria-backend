package com.polleria.pedidos.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Punto único de envío del bot de WhatsApp.
 *
 * Elige el proveedor con la propiedad whatsapp.proveedor ("meta" o "twilio").
 * Si ese proveedor no tiene credenciales, queda en MODO SIMULADO: el mensaje
 * solo se muestra en consola, así el proyecto funciona sin ninguna cuenta.
 *
 * Todo intento queda guardado en la tabla mensajes_whatsapp, y nunca lanza
 * excepción: si el envío falla, el pedido sigue su curso igual.
 */
@Component
public class WhatsappClient {

    private static final Logger log = LoggerFactory.getLogger(WhatsappClient.class);

    private final MensajeWhatsappRepository repository;
    private final WhatsappProveedor proveedor;
    private final String codigoPais;

    public WhatsappClient(MensajeWhatsappRepository repository,
                          List<WhatsappProveedor> proveedores,
                          @Value("${whatsapp.proveedor:meta}") String proveedorElegido,
                          @Value("${whatsapp.codigo-pais:51}") String codigoPais) {
        this.repository = repository;
        this.codigoPais = codigoPais;

        String elegido = proveedorElegido == null ? "meta" : proveedorElegido.trim().toLowerCase();
        this.proveedor = proveedores.stream()
                .filter(p -> p.nombre().equals(elegido))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "whatsapp.proveedor='" + proveedorElegido + "' no existe. Usa 'meta' o 'twilio'."));

        if (isSimulado()) {
            log.warn("WhatsApp en MODO SIMULADO: el proveedor '{}' no tiene credenciales. Los mensajes solo se mostrarán en consola.", elegido);
        } else {
            log.info("WhatsApp configurado con {}", proveedor.descripcion());
        }
    }

    public boolean isSimulado() {
        return !proveedor.configurado();
    }

    public String getProveedor() {
        return proveedor.nombre();
    }

    public String getModoEnvio() {
        return isSimulado() ? "simulado" : proveedor.descripcion();
    }

    public MensajeWhatsapp enviar(Long pedidoId, String telefono, String nombreCliente, String motivo, String texto) {
        String destino = normalizarTelefono(telefono);
        if (destino == null) {
            log.info("[WHATSAPP] Pedido #{}: se omite el aviso {}, teléfono no válido ({})", pedidoId, motivo, telefono);
            return repository.save(new MensajeWhatsapp(pedidoId, telefono, motivo, texto, EstadoEnvio.OMITIDO,
                    "Teléfono no válido para WhatsApp"));
        }

        if (isSimulado()) {
            log.info("[WHATSAPP SIMULADO] Para {} (pedido #{}):\n{}", destino, pedidoId, texto);
            return repository.save(new MensajeWhatsapp(pedidoId, destino, motivo, texto, EstadoEnvio.SIMULADO, null));
        }

        try {
            String id = proveedor.enviar(destino, nombreCliente, texto);
            log.info("[WHATSAPP] Enviado por {} a {} (pedido #{}, {}): {}", proveedor.nombre(), destino, pedidoId, motivo, id);
            return repository.save(new MensajeWhatsapp(pedidoId, destino, motivo, texto, EstadoEnvio.ENVIADO, id));
        } catch (ProveedorWhatsappException e) {
            log.error("[WHATSAPP] {} rechazó el mensaje a {} (pedido #{}): {}", proveedor.nombre(), destino, pedidoId, e.getMessage());
            return repository.save(new MensajeWhatsapp(pedidoId, destino, motivo, texto, EstadoEnvio.ERROR, recortar(e.getMessage(), 1000)));
        } catch (Exception e) {
            log.error("[WHATSAPP] No se pudo conectar con {} (pedido #{}): {}", proveedor.nombre(), pedidoId, e.getMessage());
            return repository.save(new MensajeWhatsapp(pedidoId, destino, motivo, texto, EstadoEnvio.ERROR,
                    recortar("Sin conexión: " + e.getMessage(), 1000)));
        }
    }

    /**
     * Convierte el teléfono guardado al formato internacional sin "+".
     * Ej: "987 654 321" -> "51987654321". Devuelve null si no parece un
     * celular (ej. "000000000" del cliente de mostrador).
     */
    String normalizarTelefono(String telefono) {
        if (telefono == null) return null;
        String digitos = telefono.replaceAll("\\D", "");
        if (digitos.isEmpty() || digitos.chars().allMatch(c -> c == '0')) return null;

        if (digitos.startsWith("00")) digitos = digitos.substring(2);
        if ("51".equals(codigoPais) && digitos.length() == 9) {
            // Celulares de Perú: 9 dígitos que empiezan con 9
            return digitos.startsWith("9") ? codigoPais + digitos : null;
        }
        if (digitos.length() < 10 || digitos.length() > 15) {
            return digitos.length() >= 7 ? codigoPais + digitos : null;
        }
        return digitos;
    }

    static String recortar(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
