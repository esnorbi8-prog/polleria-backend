package com.polleria.pedidos.whatsapp;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Activa @Async: los mensajes de WhatsApp se envían en segundo plano para que
 * una demora o caída de Meta nunca frene el cambio de estado del pedido.
 */
@Configuration
@EnableAsync
public class WhatsappConfig {
}
