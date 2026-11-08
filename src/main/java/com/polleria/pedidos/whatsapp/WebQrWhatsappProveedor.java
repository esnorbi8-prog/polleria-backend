package com.polleria.pedidos.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WhatsApp vinculado por QR: el backend le pide al servicio local
 * "whatsapp-service" (Node.js + whatsapp-web.js) que envíe el mensaje desde el
 * número que se vinculó escaneando el QR.
 *
 * Integración NO oficial (prueba de concepto): usar un chip secundario.
 */
@Component
public class WebQrWhatsappProveedor implements WhatsappProveedor {

    private final RestClient restClient;
    private final String url;
    private final String apiKey;

    public WebQrWhatsappProveedor(@Value("${webqr.url:http://localhost:3001}") String url,
                                  @Value("${webqr.api-key:polleria-dev-key}") String apiKey) {
        this.url = url == null ? "" : url.trim();
        this.apiKey = apiKey;

        SimpleClientHttpRequestFactory tiempos = new SimpleClientHttpRequestFactory();
        tiempos.setConnectTimeout(3000);   // si el servicio está apagado, falla rápido
        tiempos.setReadTimeout(45000);     // enviar puede tardar unos segundos
        this.restClient = RestClient.builder()
                .baseUrl(this.url.isEmpty() ? "http://localhost:3001" : this.url)
                .requestFactory(tiempos)
                .build();
    }

    @Override
    public String nombre() {
        return "webqr";
    }

    @Override
    public boolean configurado() {
        return !url.isEmpty();
    }

    @Override
    public String descripcion() {
        return "WhatsApp vinculado por QR (servicio en " + url + ")";
    }

    @Override
    public String enviar(String destino, String nombreCliente, String texto) {
        try {
            Map<?, ?> respuesta = restClient.post()
                    .uri("/enviar")
                    .header("X-Api-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("telefono", destino, "texto", texto))
                    .retrieve()
                    .body(Map.class);
            return respuesta != null && respuesta.get("id") != null ? respuesta.get("id").toString() : "sin id";
        } catch (RestClientResponseException e) {
            throw new ProveedorWhatsappException("Servicio WhatsApp HTTP " + e.getStatusCode().value() + ": "
                    + e.getResponseBodyAsString());
        }
    }

    /**
     * Estado del servicio de WhatsApp (para el panel del admin): si está listo,
     * con qué número, o la imagen del QR que hay que escanear.
     */
    public Map<String, Object> estadoServicio() {
        try {
            Map<?, ?> r = restClient.get()
                    .uri("/estado")
                    .header("X-Api-Key", apiKey)
                    .retrieve()
                    .body(Map.class);
            Map<String, Object> estado = new LinkedHashMap<>();
            estado.put("disponible", true);
            if (r != null) r.forEach((k, v) -> estado.put(String.valueOf(k), v));
            return estado;
        } catch (RestClientResponseException e) {
            return Map.of("disponible", false, "estado", "error",
                    "error", "El servicio respondió HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            return Map.of("disponible", false, "estado", "apagado",
                    "error", "El servicio de WhatsApp no responde en " + url + ". ¿Está corriendo `npm start` en whatsapp-service?");
        }
    }

    /** Desvincula el número actual; el servicio mostrará un QR nuevo. */
    public Map<?, ?> cerrarSesion() {
        try {
            return restClient.post()
                    .uri("/cerrar-sesion")
                    .header("X-Api-Key", apiKey)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            throw new ProveedorWhatsappException("No se pudo desvincular: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new ProveedorWhatsappException("El servicio de WhatsApp no responde en " + url);
        }
    }
}
