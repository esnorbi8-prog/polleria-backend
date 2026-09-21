package com.polleria.pedidos.pago;

import com.polleria.pedidos.pago.dto.PreferenciaPagoResponse;
import com.polleria.pedidos.pago.dto.WebhookNotification;
import com.polleria.pedidos.pedido.Pedido;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de pago (prueba de concepto — Mercado Pago Checkout Pro).
 */
@RestController
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    /**
     * POST /api/pedidos/{id}/pago — genera la preferencia de pago en Mercado
     * Pago y devuelve el link (init_point / sandbox_init_point) al que hay
     * que redirigir al cliente para que pague (Checkout Pro).
     */
    @PostMapping("/api/pedidos/{id}/pago")
    public PreferenciaPagoResponse crearPreferencia(@PathVariable Long id) {
        return pagoService.crearPreferencia(id);
    }

    /**
     * POST /api/pedidos/{id}/pago/confirmar?paymentId=123456 — confirma
     * manualmente un pago contra la API de Mercado Pago; si está aprobado,
     * mueve el pedido a PAGADO. Útil para la demo/pruebas en local, donde el
     * webhook real no puede llegar a localhost sin un túnel (ngrok).
     */
    @PostMapping("/api/pedidos/{id}/pago/confirmar")
    @ResponseStatus(HttpStatus.OK)
    public Pedido confirmarPago(@PathVariable Long id, @RequestParam String paymentId) {
        return pagoService.confirmarPago(id, paymentId);
    }

    /**
     * POST /api/pagos/webhook — URL pública que Mercado Pago llama solo
     * cuando cambia el estado de un pago. Acepta tanto el formato nuevo
     * ({ "type": "payment", "data": { "id": "..." } } por body) como el
     * formato por query params (?type=payment&data.id=... o el IPN legacy
     * ?topic=payment&id=...). Siempre responde 200 para que Mercado Pago no
     * reintente sin fin, incluso si el procesamiento interno falla.
     */
    @PostMapping("/api/pagos/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody(required = false) WebhookNotification body,
            @RequestParam(required = false) String type,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String id) {

        String tipoEvento = (body != null && body.getType() != null) ? body.getType() : (type != null ? type : topic);
        String pagoId = (body != null && body.getData() != null) ? body.getData().getId() : (dataId != null ? dataId : id);

        pagoService.procesarWebhook(tipoEvento, pagoId);

        return ResponseEntity.ok().build();
    }
}
