package com.polleria.pedidos.whatsapp;

/** Resultado de cada intento de envío por WhatsApp. */
public enum EstadoEnvio {
    /** Meta aceptó el mensaje (la entrega final la confirma Meta después). */
    ENVIADO,
    /** No hay credenciales configuradas: el mensaje solo se mostró en la consola. */
    SIMULADO,
    /** Meta rechazó el mensaje o no se pudo conectar. */
    ERROR,
    /** No se envió: el cliente no tiene un celular válido (ej. cliente de mostrador). */
    OMITIDO
}
