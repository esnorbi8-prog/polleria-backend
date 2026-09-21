package com.polleria.pedidos.pago.config;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configura el SDK de Mercado Pago con el Access Token al arrancar la app.
 *
 * El token NUNCA se escribe en application.properties: se lee de la variable
 * de entorno MERCADOPAGO_ACCESS_TOKEN (ver mercadopago.access-token). Si no
 * está configurada, la app igual levanta (para no romper el resto de
 * endpoints), pero avisa por consola que los endpoints de pago fallarán.
 */
@Component
public class MercadoPagoConfiguracion {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoConfiguracion.class);

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @PostConstruct
    public void configurar() {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("No se configuró MERCADOPAGO_ACCESS_TOKEN. Los endpoints de /api/pedidos/{{id}}/pago " +
                    "fallarán hasta que definas esa variable de entorno con tu Access Token de prueba (TEST-...).");
            return;
        }
        MercadoPagoConfig.setAccessToken(accessToken);
        log.info("SDK de Mercado Pago configurado correctamente.");
    }
}
