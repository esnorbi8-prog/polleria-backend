package com.polleria.pedidos.whatsapp;

/** El proveedor (Meta o Twilio) rechazó el mensaje; el texto trae su respuesta de error. */
public class ProveedorWhatsappException extends RuntimeException {

    public ProveedorWhatsappException(String mensaje) {
        super(mensaje);
    }
}
