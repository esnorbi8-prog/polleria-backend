package com.polleria.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de Pedidos de la Pollería.
 *
 * Primer avance del sistema (Caso 2 - Pollería):
 *  - Persistencia de pedidos (entidades) vía JPA.
 *  - Control de estados del pedido con validación de transiciones.
 *  - Aviso de cada cambio de estado (historial + notificación consultable por pantalla).
 *  - Endpoints REST para que el frontend Angular (y otros clientes) interactúen con el sistema.
 */
@SpringBootApplication
public class PolleriaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PolleriaApplication.class, args);
    }
}
