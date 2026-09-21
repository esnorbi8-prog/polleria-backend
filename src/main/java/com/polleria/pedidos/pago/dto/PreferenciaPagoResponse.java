package com.polleria.pedidos.pago.dto;

/**
 * Respuesta al generar una preferencia de pago en Mercado Pago.
 *
 * initPoint es el link de producción; sandboxInitPoint es el que hay que usar
 * mientras se trabaja con credenciales de prueba (TEST-...): redirige a un
 * Checkout Pro de pruebas donde se puede pagar con las tarjetas de test.
 */
public class PreferenciaPagoResponse {

    private final String preferenceId;
    private final String initPoint;
    private final String sandboxInitPoint;

    public PreferenciaPagoResponse(String preferenceId, String initPoint, String sandboxInitPoint) {
        this.preferenceId = preferenceId;
        this.initPoint = initPoint;
        this.sandboxInitPoint = sandboxInitPoint;
    }

    public String getPreferenceId() {
        return preferenceId;
    }

    public String getInitPoint() {
        return initPoint;
    }

    public String getSandboxInitPoint() {
        return sandboxInitPoint;
    }
}
