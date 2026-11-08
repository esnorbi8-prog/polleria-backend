package com.polleria.pedidos.whatsapp;

import com.polleria.pedidos.pedido.DetallePedido;
import com.polleria.pedidos.pedido.EstadoPedido;
import com.polleria.pedidos.pedido.Pedido;
import com.polleria.pedidos.pedido.TipoEntrega;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Redacta los mensajes que el bot manda al cliente. Está separado del envío
 * para poder cambiar los textos sin tocar la integración con Meta.
 * WhatsApp interpreta *texto* como negrita.
 */
@Component
public class WhatsappMensajes {

    private final int minutosPreparacion;
    private final int minutosReparto;

    public WhatsappMensajes(@Value("${whatsapp.tiempo-preparacion-min:25}") int minutosPreparacion,
                            @Value("${whatsapp.tiempo-reparto-min:20}") int minutosReparto) {
        this.minutosPreparacion = minutosPreparacion;
        this.minutosReparto = minutosReparto;
    }

    /** Mensaje para un cambio de estado. Devuelve null si ese estado no lleva aviso. */
    public String porEstado(Pedido pedido, EstadoPedido estado, String observacion) {
        String nombre = primerNombre(pedido);
        Long id = pedido.getId();
        boolean delivery = pedido.getTipoEntrega() == TipoEntrega.DELIVERY;

        return switch (estado) {
            case RECIBIDO -> """
                    ¡Hola %s! 🍗 Recibimos tu pedido *#%d*.
                    Total: *S/ %s*
                    Te confirmaremos apenas se registre tu pago.""".formatted(nombre, id, soles(pedido.getTotal()));

            case PAGADO -> """
                    ✅ ¡Pago confirmado, %s! Tu pedido *#%d* ya entró a cocina.

                    %s
                    Total: *S/ %s*

                    ⏱️ Tiempo estimado: *%d min* (%s).""".formatted(
                    nombre, id, detalle(pedido), soles(pedido.getTotal()),
                    delivery ? minutosPreparacion + minutosReparto : minutosPreparacion,
                    delivery ? "delivery a " + direccion(pedido) : "recojo en el local");

            case EN_PREPARACION -> "👨‍🍳 Tu pedido *#%d* se está preparando. ¡Ya casi!".formatted(id);

            case LISTO -> delivery
                    ? "📦 Tu pedido *#%d* está listo y en unos minutos sale a reparto.".formatted(id)
                    : "🛍️ Tu pedido *#%d* está listo. ¡Ya puedes recogerlo en el local!".formatted(id);

            case EN_CAMINO -> """
                    🛵 ¡Tu pedido *#%d* salió a reparto!
                    Llega en aproximadamente *%d min* a %s.""".formatted(id, minutosReparto, direccion(pedido));

            case ENTREGADO -> "🎉 Pedido *#%d* entregado. ¡Gracias por tu compra, %s! Buen provecho 🍗".formatted(id, nombre);

            case CANCELADO -> "❌ Tu pedido *#%d* fue cancelado.%s Si tienes dudas, respóndenos por aquí.".formatted(
                    id, (observacion == null || observacion.isBlank()) ? "" : " Motivo: " + observacion + ".");
        };
    }

    /** Aviso de llegada, cuando el GPS del motorizado entra al radio de la dirección. */
    public String repartidorCerca(Pedido pedido, long distanciaMetros) {
        return """
                📍 ¡%s, tu motorizado está llegando!
                Está a unos *%d m* de tu dirección con tu pedido *#%d*. Ten todo listo para recibirlo 🙌""".formatted(
                primerNombre(pedido), Math.max(10, Math.round(distanciaMetros / 10.0) * 10), pedido.getId());
    }

    private String detalle(Pedido pedido) {
        StringBuilder sb = new StringBuilder();
        for (DetallePedido item : pedido.getItems()) {
            sb.append("• ").append(item.getCantidad()).append(" x ").append(item.getProducto().getNombre())
              .append(" — S/ ").append(soles(item.getSubtotal())).append('\n');
        }
        return sb.toString().trim();
    }

    private String primerNombre(Pedido pedido) {
        String nombre = pedido.getCliente() != null ? pedido.getCliente().getNombre() : null;
        if (nombre == null || nombre.isBlank()) return "cliente";
        return nombre.trim().split("\\s+")[0];
    }

    /** Dirección sin las coordenadas ocultas ("Calle X |-14.07,-75.72" -> "Calle X"). */
    private String direccion(Pedido pedido) {
        String d = pedido.getDireccionEntrega();
        if (d == null || d.isBlank()) return "tu dirección";
        return d.split("\\|")[0].trim();
    }

    private String soles(BigDecimal monto) {
        return monto == null ? "0.00" : monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
