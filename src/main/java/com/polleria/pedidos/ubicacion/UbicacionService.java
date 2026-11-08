package com.polleria.pedidos.ubicacion;

import com.polleria.pedidos.evento.RepartidorCercaEvent;
import com.polleria.pedidos.pedido.EstadoPedido;
import com.polleria.pedidos.pedido.Pedido;
import com.polleria.pedidos.pedido.PedidoService;
import com.polleria.pedidos.ubicacion.dto.UbicacionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Geolocalización del repartidor (RF-D02) y seguimiento en vivo (RF-C09).
 *
 * No usa ningún servicio de mapas: solo guarda coordenadas. El dibujo del mapa
 * lo hace el frontend con Leaflet + OpenStreetMap, que son gratuitos.
 */
@Service
public class UbicacionService {

    private final UbicacionRepartidorRepository repository;
    private final PedidoService pedidoService;
    private final ApplicationEventPublisher eventPublisher;
    private final double distanciaAvisoLlegada;

    public UbicacionService(UbicacionRepartidorRepository repository, PedidoService pedidoService,
                            ApplicationEventPublisher eventPublisher,
                            @Value("${whatsapp.distancia-aviso-llegada-m:150}") double distanciaAvisoLlegada) {
        this.repository = repository;
        this.pedidoService = pedidoService;
        this.eventPublisher = eventPublisher;
        this.distanciaAvisoLlegada = distanciaAvisoLlegada;
    }

    /** Guarda (o sobreescribe) la posición actual del repartidor para ese pedido. */
    @Transactional
    public UbicacionRepartidor actualizar(Long pedidoId, UbicacionRequest request, String repartidorEmail) {
        Pedido pedido = pedidoService.buscarPorId(pedidoId); // 404 si no existe
        if (pedido.getEstado() != EstadoPedido.EN_CAMINO) {
            // Solo tiene sentido rastrear mientras el pedido va en camino.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede enviar la ubicación de un pedido EN_CAMINO (estado actual: " + pedido.getEstado() + ")");
        }

        UbicacionRepartidor ubicacion = repository.findByPedidoId(pedidoId).orElseGet(() -> {
            UbicacionRepartidor nueva = new UbicacionRepartidor();
            nueva.setPedidoId(pedidoId);
            return nueva;
        });
        ubicacion.setLatitud(request.getLatitud());
        ubicacion.setLongitud(request.getLongitud());
        ubicacion.setPrecisionMetros(request.getPrecisionMetros());
        ubicacion.setRepartidorEmail(repartidorEmail);
        ubicacion.setFechaActualizacion(LocalDateTime.now());

        // ¿El motorizado ya está llegando? Se avisa una sola vez por pedido.
        if (!ubicacion.isAvisoLlegadaEnviado()) {
            double[] destino = Geo.coordenadasDeDireccion(pedido.getDireccionEntrega());
            if (destino != null) {
                double metros = Geo.distanciaMetros(request.getLatitud(), request.getLongitud(), destino[0], destino[1]);
                if (metros <= distanciaAvisoLlegada) {
                    ubicacion.setAvisoLlegadaEnviado(true);
                    eventPublisher.publishEvent(new RepartidorCercaEvent(pedidoId, Math.round(metros)));
                }
            }
        }

        return repository.save(ubicacion);
    }

    /** Última posición conocida del repartidor para ese pedido (vacío si todavía no mandó ninguna). */
    @Transactional(readOnly = true)
    public Optional<UbicacionRepartidor> obtener(Long pedidoId) {
        pedidoService.buscarPorId(pedidoId); // 404 si no existe
        return repository.findByPedidoId(pedidoId);
    }
}
