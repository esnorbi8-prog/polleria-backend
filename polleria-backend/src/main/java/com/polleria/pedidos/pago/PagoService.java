package com.polleria.pedidos.pago;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.polleria.pedidos.pago.dto.PreferenciaPagoResponse;
import com.polleria.pedidos.pago.exception.PagoInvalidoException;
import com.polleria.pedidos.pedido.DetallePedido;
import com.polleria.pedidos.pedido.EstadoPedido;
import com.polleria.pedidos.pedido.Pedido;
import com.polleria.pedidos.pedido.PedidoRepository;
import com.polleria.pedidos.pedido.PedidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Prueba de concepto de pasarela de pago con Mercado Pago (Checkout Pro).
 *
 * No reinventa el control de estados: reutiliza PedidoService.cambiarEstado
 * para pasar el pedido a PAGADO, así que el historial y las notificaciones en
 * pantalla del primer avance siguen funcionando exactamente igual, ahora
 * disparadas por un pago real (o de prueba) en vez de un PATCH manual.
 */
@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PedidoRepository pedidoRepository;
    private final PedidoService pedidoService;

    @Value("${mercadopago.notification-url}")
    private String notificationUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public PagoService(PedidoRepository pedidoRepository, PedidoService pedidoService) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoService = pedidoService;
    }

    /**
     * Genera la preferencia de pago en Mercado Pago para un pedido en estado
     * RECIBIDO y devuelve el link (init_point) al que hay que mandar al
     * cliente para que pague (Checkout Pro = checkout redirigido).
     */
    @Transactional
    public PreferenciaPagoResponse crearPreferencia(Long pedidoId) {
        Pedido pedido = pedidoService.buscarPorId(pedidoId);

        if (pedido.getEstado() != EstadoPedido.RECIBIDO) {
            throw new PagoInvalidoException(
                    "Solo se puede iniciar el pago de un pedido en estado RECIBIDO (estado actual: " + pedido.getEstado() + ")");
        }

        List<PreferenceItemRequest> items = new ArrayList<>();
        for (DetallePedido detalle : pedido.getItems()) {
            items.add(PreferenceItemRequest.builder()
                    .id(String.valueOf(detalle.getProducto().getId()))
                    .title(detalle.getProducto().getNombre())
                    .quantity(detalle.getCantidad())
                    .currencyId("PEN")
                    .unitPrice(detalle.getPrecioUnitario())
                    .build());
        }

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(frontendUrl + "/pago/exito?pedido=" + pedidoId)
                .pending(frontendUrl + "/pago/pendiente?pedido=" + pedidoId)
                .failure(frontendUrl + "/pago/error?pedido=" + pedidoId)
                .build();

        PreferenceRequest request = PreferenceRequest.builder()
                .items(items)
                .backUrls(backUrls)
                // NOTA: "auto_return" (volver solo al sitio tras pagar) exige que
                // back_urls.success sea un dominio público real; Mercado Pago lo
                // rechaza con "invalid_auto_return" si es localhost. Como el
                // frontend Angular todavía no está desplegado, se deja sin activar
                // por ahora; el cliente puede volver manualmente con el botón
                // "Volver al sitio" de Mercado Pago. Cuando exista una URL pública,
                // se puede reactivar agregando .autoReturn("approved") acá.
                // external_reference es lo que nos permite, en el webhook, saber a qué
                // Pedido de nuestra BD corresponde el pago que avisa Mercado Pago.
                .externalReference(String.valueOf(pedidoId))
                .notificationUrl(notificationUrl)
                .build();

        try {
            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(request);

            pedido.setReferenciaPago(preference.getId());
            pedidoRepository.save(pedido);

            log.info("Preferencia de pago creada para pedido #{}: {}", pedidoId, preference.getId());

            return new PreferenciaPagoResponse(preference.getId(), preference.getInitPoint(), preference.getSandboxInitPoint());
        } catch (MPApiException e) {
            log.error("Mercado Pago rechazó la creación de preferencia del pedido #{}: {}", pedidoId, e.getApiResponse().getContent());
            throw new PagoInvalidoException("Mercado Pago rechazó la solicitud: " + e.getApiResponse().getContent());
        } catch (MPException e) {
            log.error("Error de comunicación con Mercado Pago (pedido #{})", pedidoId, e);
            throw new PagoInvalidoException("No se pudo conectar con Mercado Pago: " + e.getMessage());
        }
    }

    /**
     * Confirma manualmente un pago contra la API de Mercado Pago (consultando
     * por su id) y, si está aprobado, mueve el pedido a PAGADO. Sirve para
     * probar el flujo completo sin depender de que el webhook llegue: en
     * localhost (sin ngrok/túnel) Mercado Pago no puede alcanzar tu máquina.
     */
    @Transactional
    public Pedido confirmarPago(Long pedidoId, String paymentId) {
        Pedido pedido = pedidoService.buscarPorId(pedidoId);

        Payment payment = obtenerPago(paymentId);

        if (!"approved".equals(payment.getStatus())) {
            throw new PagoInvalidoException(
                    "El pago " + paymentId + " todavía no está aprobado por Mercado Pago (estado: " + payment.getStatus() + ")");
        }

        pedido.setReferenciaPago(String.valueOf(payment.getId()));
        pedidoRepository.save(pedido);

        return pedidoService.cambiarEstado(pedidoId, EstadoPedido.PAGADO,
                "Pago confirmado por Mercado Pago (payment #" + payment.getId() + ")");
    }

    /**
     * Procesa la notificación (webhook) que Mercado Pago envía automáticamente
     * ante cualquier evento de pago. Nunca propaga una excepción hacia el
     * controller: si algo falla, se loguea y listo, para que Mercado Pago no
     * reintente sin fin creyendo que nuestro servidor está caído.
     */
    public void procesarWebhook(String tipo, String dataId) {
        if (dataId == null || dataId.isBlank()) {
            return;
        }
        if (tipo != null && !"payment".equals(tipo)) {
            log.info("Webhook de Mercado Pago ignorado (tipo distinto de 'payment'): {}", tipo);
            return;
        }

        try {
            Payment payment = obtenerPago(dataId);

            String externalReference = payment.getExternalReference();
            if (externalReference == null) {
                log.warn("El pago {} no trae external_reference; no se puede asociar a un pedido", dataId);
                return;
            }

            Long pedidoId = Long.parseLong(externalReference);
            Pedido pedido = pedidoService.buscarPorId(pedidoId);

            if ("approved".equals(payment.getStatus()) && pedido.getEstado() == EstadoPedido.RECIBIDO) {
                pedido.setReferenciaPago(String.valueOf(payment.getId()));
                pedidoRepository.save(pedido);
                pedidoService.cambiarEstado(pedidoId, EstadoPedido.PAGADO,
                        "Pago confirmado por webhook de Mercado Pago (payment #" + payment.getId() + ")");
                log.info("Webhook: pedido #{} pasó a PAGADO (payment #{})", pedidoId, payment.getId());
            } else {
                log.info("Webhook recibido para pago {} / pedido #{}: estado MP={}, estado pedido={} (sin acción)",
                        dataId, pedidoId, payment.getStatus(), pedido.getEstado());
            }
        } catch (Exception e) {
            log.error("Error procesando webhook de Mercado Pago (payment id={})", dataId, e);
        }
    }

    private Payment obtenerPago(String paymentId) {
        try {
            PaymentClient client = new PaymentClient();
            return client.get(Long.parseLong(paymentId));
        } catch (NumberFormatException e) {
            throw new PagoInvalidoException("El id de pago '" + paymentId + "' no es válido");
        } catch (MPApiException e) {
            throw new PagoInvalidoException("Mercado Pago no reconoce el pago " + paymentId + ": " + e.getApiResponse().getContent());
        } catch (MPException e) {
            throw new PagoInvalidoException("No se pudo conectar con Mercado Pago: " + e.getMessage());
        }
    }
}
