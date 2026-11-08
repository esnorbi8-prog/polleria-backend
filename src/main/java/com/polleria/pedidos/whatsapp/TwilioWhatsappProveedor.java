package com.polleria.pedidos.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * WhatsApp a través de Twilio (ideal para pruebas con el Sandbox).
 *
 * Dos formas de enviar:
 * - Texto libre (por defecto): parámetro Body. Twilio solo lo acepta si el
 *   cliente le escribió al número en las últimas 24 h (en el Sandbox: el "join").
 * - Plantilla de contenido (si se configura twilio.content-sid = HX...): se
 *   manda ContentSid + ContentVariables {"1": nombre, "2": aviso}. Es lo que
 *   Twilio exige cuando responde el error 21654 "ContentSid Required".
 */
@Component
public class TwilioWhatsappProveedor implements WhatsappProveedor {

    private static final Logger log = LoggerFactory.getLogger(TwilioWhatsappProveedor.class);

    private final RestClient restClient = RestClient.create("https://api.twilio.com/2010-04-01");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String accountSid;
    private final String authToken;
    private final String numeroOrigen;
    private final String contentSid;

    public TwilioWhatsappProveedor(@Value("${twilio.account-sid:}") String accountSid,
                                   @Value("${twilio.auth-token:}") String authToken,
                                   @Value("${twilio.whatsapp-from:+14155238886}") String numeroOrigen,
                                   @Value("${twilio.content-sid:}") String contentSid) {
        this.accountSid = accountSid == null ? "" : accountSid.trim();
        this.authToken = authToken == null ? "" : authToken.trim();
        String n = numeroOrigen == null ? "" : numeroOrigen.trim().replace("whatsapp:", "").replace(" ", "");
        this.numeroOrigen = n.startsWith("+") ? n : "+" + n;
        this.contentSid = contentSid == null ? "" : contentSid.trim();
    }

    @Override
    public String nombre() {
        return "twilio";
    }

    @Override
    public boolean configurado() {
        return !accountSid.isEmpty() && !authToken.isEmpty();
    }

    @Override
    public String descripcion() {
        return "Twilio (desde " + numeroOrigen + ", " + (usaPlantilla() ? "plantilla " + contentSid : "texto libre") + ")";
    }

    private boolean usaPlantilla() {
        return !contentSid.isEmpty();
    }

    @Override
    public String enviar(String destino, String nombreCliente, String texto) {
        // Se arma el formulario a mano (x-www-form-urlencoded) para que los
        // parámetros lleguen a Twilio exactamente con estos nombres.
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("From", "whatsapp:" + numeroOrigen);
        parametros.put("To", "whatsapp:+" + destino);

        if (usaPlantilla()) {
            String nombre = (nombreCliente == null || nombreCliente.isBlank()) ? "cliente" : nombreCliente.trim();
            // Las variables de plantilla de WhatsApp no admiten saltos de línea.
            String aviso = texto.replaceAll("\\s*\\n+\\s*", " · ").replaceAll("\\s{2,}", " ").trim();
            parametros.put("ContentSid", contentSid);
            parametros.put("ContentVariables", json(Map.of(
                    "1", WhatsappClient.recortar(nombre, 60),
                    "2", WhatsappClient.recortar(aviso, 900))));
        } else {
            parametros.put("Body", WhatsappClient.recortar(texto, 1500));
        }

        String formulario = parametros.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        log.debug("[TWILIO] Enviando a {} con parámetros {}", destino, parametros.keySet());

        String credenciales = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        try {
            Map<?, ?> respuesta = restClient.post()
                    .uri("/Accounts/{sid}/Messages.json", accountSid)
                    .header("Authorization", "Basic " + credenciales)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formulario)
                    .retrieve()
                    .body(Map.class);
            return respuesta != null && respuesta.get("sid") != null ? respuesta.get("sid").toString() : "sin id";
        } catch (RestClientResponseException e) {
            throw new ProveedorWhatsappException("Twilio HTTP " + e.getStatusCode().value() + ": "
                    + e.getResponseBodyAsString() + " [parámetros enviados: " + parametros.keySet() + "]");
        }
    }

    private String json(Map<String, String> valores) {
        try {
            return objectMapper.writeValueAsString(valores);
        } catch (JsonProcessingException e) {
            throw new ProveedorWhatsappException("No se pudieron armar las variables de la plantilla: " + e.getMessage());
        }
    }
}
