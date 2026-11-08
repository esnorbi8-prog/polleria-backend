package com.polleria.pedidos.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * API oficial de WhatsApp de Meta (WhatsApp Cloud API).
 *
 * Modos (whatsapp.modo-envio):
 * - texto: mensaje libre. Meta solo lo entrega si el cliente le escribió al
 *   número de la pollería en las últimas 24 horas.
 * - plantilla: plantilla aprobada (whatsapp.plantilla.nombre) con 2 variables:
 *   {{1}} = nombre del cliente, {{2}} = el aviso. Funciona siempre.
 */
@Component
public class MetaWhatsappProveedor implements WhatsappProveedor {

    private final RestClient restClient;
    private final String token;
    private final String phoneNumberId;
    private final String modoEnvio;
    private final String plantillaNombre;
    private final String plantillaIdioma;

    public MetaWhatsappProveedor(@Value("${whatsapp.token:}") String token,
                                 @Value("${whatsapp.phone-number-id:}") String phoneNumberId,
                                 @Value("${whatsapp.api-version:v25.0}") String apiVersion,
                                 @Value("${whatsapp.modo-envio:texto}") String modoEnvio,
                                 @Value("${whatsapp.plantilla.nombre:aviso_pedido}") String plantillaNombre,
                                 @Value("${whatsapp.plantilla.idioma:es}") String plantillaIdioma) {
        this.token = token == null ? "" : token.trim();
        this.phoneNumberId = phoneNumberId == null ? "" : phoneNumberId.trim();
        this.modoEnvio = modoEnvio == null ? "texto" : modoEnvio.trim().toLowerCase();
        this.plantillaNombre = plantillaNombre;
        this.plantillaIdioma = plantillaIdioma;
        this.restClient = RestClient.builder()
                .baseUrl("https://graph.facebook.com/" + apiVersion)
                .build();
    }

    @Override
    public String nombre() {
        return "meta";
    }

    @Override
    public boolean configurado() {
        return !token.isEmpty() && !phoneNumberId.isEmpty();
    }

    @Override
    public String descripcion() {
        return "Meta Cloud API (modo " + modoEnvio + ")";
    }

    @Override
    public String enviar(String destino, String nombreCliente, String texto) {
        Map<String, Object> cuerpo = "plantilla".equals(modoEnvio)
                ? cuerpoPlantilla(destino, nombreCliente, texto)
                : cuerpoTexto(destino, texto);
        try {
            Map<?, ?> respuesta = restClient.post()
                    .uri("/{id}/messages", phoneNumberId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(Map.class);

            if (respuesta != null && respuesta.get("messages") instanceof List<?> lista && !lista.isEmpty()
                    && lista.get(0) instanceof Map<?, ?> m && m.get("id") != null) {
                return m.get("id").toString();
            }
            return "sin id";
        } catch (RestClientResponseException e) {
            throw new ProveedorWhatsappException("Meta HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        }
    }

    private Map<String, Object> cuerpoTexto(String destino, String texto) {
        return Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", destino,
                "type", "text",
                "text", Map.of("preview_url", false, "body", WhatsappClient.recortar(texto, 4000))
        );
    }

    /** Las variables de plantilla no admiten saltos de línea (regla de Meta): el aviso se pasa a una línea. */
    private Map<String, Object> cuerpoPlantilla(String destino, String nombreCliente, String texto) {
        String nombre = (nombreCliente == null || nombreCliente.isBlank()) ? "cliente" : nombreCliente.trim();
        String aviso = texto.replaceAll("\\s*\\n+\\s*", " · ").replaceAll("\\s{2,}", " ").trim();

        return Map.of(
                "messaging_product", "whatsapp",
                "to", destino,
                "type", "template",
                "template", Map.of(
                        "name", plantillaNombre,
                        "language", Map.of("code", plantillaIdioma),
                        "components", List.of(Map.of(
                                "type", "body",
                                "parameters", List.of(
                                        Map.of("type", "text", "text", WhatsappClient.recortar(nombre, 60)),
                                        Map.of("type", "text", "text", WhatsappClient.recortar(aviso, 900))
                                )
                        ))
                )
        );
    }
}
