package com.polleria.pedidos.whatsapp;

/**
 * Proveedor que realmente entrega el mensaje de WhatsApp (Meta o Twilio).
 * Se elige con la propiedad whatsapp.proveedor; el resto del bot (listener,
 * textos, aviso de llegada) no cambia según el proveedor.
 */
public interface WhatsappProveedor {

    /** Nombre con el que se elige en whatsapp.proveedor: "meta" o "twilio". */
    String nombre();

    /** true si tiene sus credenciales configuradas; si no, el bot queda en modo simulado. */
    boolean configurado();

    /** Descripción corta para logs y para GET /api/whatsapp/estado. */
    String descripcion();

    /**
     * Envía el mensaje y devuelve el id que asigna el proveedor.
     * Lanza ProveedorWhatsappException si el proveedor lo rechaza.
     */
    String enviar(String destino, String nombreCliente, String texto);
}
