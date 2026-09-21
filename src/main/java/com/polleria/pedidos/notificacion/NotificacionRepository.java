package com.polleria.pedidos.notificacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByPedidoIdOrderByFechaEnvioDesc(Long pedidoId);

    List<Notificacion> findAllByOrderByFechaEnvioDesc();
}
