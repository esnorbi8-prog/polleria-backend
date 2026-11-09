package com.polleria.pedidos.pedido;

import com.polleria.pedidos.cliente.Cliente;
import com.polleria.pedidos.cliente.ClienteRepository;
import com.polleria.pedidos.common.RecursoNoEncontradoException;
import com.polleria.pedidos.evento.CambioEstadoPedidoEvent;
import com.polleria.pedidos.pedido.dto.ItemPedidoRequest;
import com.polleria.pedidos.pedido.dto.PedidoRequest;
import com.polleria.pedidos.pedido.exception.StockInsuficienteException;
import com.polleria.pedidos.pedido.exception.TransicionEstadoInvalidaException;
import com.polleria.pedidos.producto.Producto;
import com.polleria.pedidos.producto.ProductoRepository;
import com.polleria.pedidos.usuario.Rol;
import com.polleria.pedidos.usuario.Usuario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lógica de negocio de pedidos: creación, y control de la máquina de
 * estados. Esta clase NO sabe nada de cómo se avisa al cliente/cocina/etc:
 * solo publica un evento de dominio (CambioEstadoPedidoEvent) y quien quiera
 * reaccionar (historial, notificaciones en pantalla, WhatsApp, boleta...) se
 * suscribe a él. Así se separa la lógica de pedidos de la de notificación,
 * tal como pide la casuística.
 */
@Service
public class PedidoService {

    /**
     * Transiciones de estado permitidas. Un pedido "en camino" no puede
     * volver a "recibido", un pedido "entregado" o "cancelado" es terminal, etc.
     */
    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES_PERMITIDAS = new EnumMap<>(EstadoPedido.class);

    static {
        TRANSICIONES_PERMITIDAS.put(EstadoPedido.RECIBIDO, EnumSet.of(EstadoPedido.PAGADO, EstadoPedido.CANCELADO));
        TRANSICIONES_PERMITIDAS.put(EstadoPedido.PAGADO, EnumSet.of(EstadoPedido.EN_PREPARACION, EstadoPedido.CANCELADO));
        TRANSICIONES_PERMITIDAS.put(EstadoPedido.EN_PREPARACION, EnumSet.of(EstadoPedido.LISTO, EstadoPedido.CANCELADO));
        TRANSICIONES_PERMITIDAS.put(EstadoPedido.LISTO, EnumSet.of(EstadoPedido.EN_CAMINO, EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO));
        TRANSICIONES_PERMITIDAS.put(EstadoPedido.EN_CAMINO, EnumSet.of(EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO));
        TRANSICIONES_PERMITIDAS.put(EstadoPedido.ENTREGADO, EnumSet.noneOf(EstadoPedido.class));
        TRANSICIONES_PERMITIDAS.put(EstadoPedido.CANCELADO, EnumSet.noneOf(EstadoPedido.class));
    }

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PedidoService(PedidoRepository pedidoRepository,
                          ClienteRepository clienteRepository,
                          ProductoRepository productoRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Pedido crearPedido(PedidoRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + request.getClienteId()));

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setTipoEntrega(request.getTipoEntrega());
        pedido.setDireccionEntrega(request.getDireccionEntrega());
        pedido.setEstado(EstadoPedido.RECIBIDO);

        for (ItemPedidoRequest itemReq : request.getItems()) {
            Producto producto = productoRepository.findById(itemReq.getProductoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + itemReq.getProductoId()));

            if (producto.isAgotado() || producto.getStock() < itemReq.getCantidad()) {
                throw new StockInsuficienteException(producto.getNombre(), producto.getStock() == null ? 0 : producto.getStock(), itemReq.getCantidad());
            }

            // Se descuenta el stock al crear el pedido (reserva); si el pedido se
            // cancela luego, se repone (ver cambiarEstado).
            producto.setStock(producto.getStock() - itemReq.getCantidad());
            productoRepository.save(producto);

            DetallePedido detalle = new DetallePedido(producto, itemReq.getCantidad(), producto.getPrecio());
            pedido.agregarItem(detalle);
        }

        pedido.recalcularTotal();
        Pedido guardado = pedidoRepository.save(pedido);

        // Aviso del primer estado también, para que quede en el historial/notificaciones desde el inicio.
        publicarCambioEstado(guardado, null, EstadoPedido.RECIBIDO, "Pedido registrado en el sistema");

        return guardado;
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPorEstado(EstadoPedido estado) {
        return pedidoRepository.findByEstado(estado);
    }

    /**
     * Cambia el estado de un pedido, validando que la transición sea válida
     * según la máquina de estados. Publica CambioEstadoPedidoEvent al finalizar,
     * que dispara el registro en historial y la notificación en pantalla.
     */
    @Transactional
    public Pedido cambiarEstado(Long pedidoId, EstadoPedido nuevoEstado, String observacion) {
        Pedido pedido = buscarPorId(pedidoId);
        EstadoPedido estadoActual = pedido.getEstado();

        if (estadoActual == nuevoEstado) {
            throw new TransicionEstadoInvalidaException(estadoActual, nuevoEstado);
        }

        Set<EstadoPedido> permitidos = TRANSICIONES_PERMITIDAS.getOrDefault(estadoActual, EnumSet.noneOf(EstadoPedido.class));
        if (!permitidos.contains(nuevoEstado)) {
            throw new TransicionEstadoInvalidaException(estadoActual, nuevoEstado);
        }

        // Cancelación a medio preparar (o en cualquier punto anterior a entregado):
        // se repone el stock reservado de cada producto del pedido.
        if (nuevoEstado == EstadoPedido.CANCELADO) {
            reponerStock(pedido);
        }

        pedido.setEstado(nuevoEstado);
        Pedido actualizado = pedidoRepository.save(pedido);

        publicarCambioEstado(actualizado, estadoActual, nuevoEstado, observacion);

        return actualizado;
    }

    /**
     * Cancela un pedido solicitado por un cliente o personal del restaurante.
     * Valida permisos de propiedad (si es cliente) y que el pedido no se encuentre
     * en estados avanzados (LISTO, EN_CAMINO, ENTREGADO). Repone el stock y publica eventos.
     */
    @Transactional
    public Pedido cancelarPedido(Long pedidoId, Usuario usuario, String motivo) {
        Pedido pedido = buscarPorId(pedidoId);

        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new TransicionEstadoInvalidaException("El pedido #" + pedidoId + " ya se encuentra cancelado.");
        }

        if (usuario != null && usuario.getRol() == Rol.CLIENTE) {
            if (pedido.getCliente() == null || !usuario.getId().equals(pedido.getCliente().getUsuarioId())) {
                throw new AccessDeniedException("No tienes permiso para cancelar este pedido.");
            }

            // Un cliente solo puede cancelar si está RECIBIDO, PAGADO o EN_PREPARACION
            if (pedido.getEstado() != EstadoPedido.RECIBIDO &&
                pedido.getEstado() != EstadoPedido.PAGADO &&
                pedido.getEstado() != EstadoPedido.EN_PREPARACION) {
                throw new TransicionEstadoInvalidaException(
                    "No es posible cancelar el pedido #" + pedidoId + " porque ya está " +
                    pedido.getEstado().name().toLowerCase() + ". Por favor comunícate con el restaurante.");
            }
        }

        // Cocina solo cancela pedidos en cola (PAGADO) o en fuego (EN_PREPARACION)
        if (usuario != null && usuario.getRol() == Rol.COCINERO) {
            if (pedido.getEstado() != EstadoPedido.PAGADO && pedido.getEstado() != EstadoPedido.EN_PREPARACION) {
                throw new TransicionEstadoInvalidaException(
                    "Cocina solo puede cancelar pedidos en cola o en preparación.");
            }
        }

        // Reparto solo cancela entregas listas para llevar (LISTO) o en camino (EN_CAMINO)
        if (usuario != null && usuario.getRol() == Rol.REPARTIDOR) {
            if (pedido.getEstado() != EstadoPedido.LISTO && pedido.getEstado() != EstadoPedido.EN_CAMINO) {
                throw new TransicionEstadoInvalidaException(
                    "Reparto solo puede cancelar entregas que estén listas para llevar o en camino.");
            }
        }

        String defaultMotivo;
        if (usuario != null) {
            defaultMotivo = switch (usuario.getRol()) {
                case CLIENTE -> "Cancelado por el cliente";
                case COCINERO -> "Cancelado por cocina";
                case REPARTIDOR -> "Cancelado por reparto";
                default -> "Cancelado por el restaurante";
            };
        } else {
            defaultMotivo = "Cancelado";
        }

        String observacion = (motivo != null && !motivo.isBlank()) ? motivo : defaultMotivo;

        return cambiarEstado(pedidoId, EstadoPedido.CANCELADO, observacion);
    }

    private void reponerStock(Pedido pedido) {
        for (DetallePedido item : pedido.getItems()) {
            Producto producto = item.getProducto();
            producto.setStock(producto.getStock() + item.getCantidad());
            productoRepository.save(producto);
        }
    }

    private void publicarCambioEstado(Pedido pedido, EstadoPedido anterior, EstadoPedido nuevo, String observacion) {
        eventPublisher.publishEvent(new CambioEstadoPedidoEvent(
                pedido.getId(),
                pedido.getCliente().getId(),
                pedido.getCliente().getNombre(),
                anterior,
                nuevo,
                observacion
        ));
    }
}
