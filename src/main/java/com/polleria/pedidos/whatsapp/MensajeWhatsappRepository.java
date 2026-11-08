package com.polleria.pedidos.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeWhatsappRepository extends JpaRepository<MensajeWhatsapp, Long> {

    List<MensajeWhatsapp> findTop50ByOrderByFechaDesc();

    List<MensajeWhatsapp> findByPedidoIdOrderByFechaAsc(Long pedidoId);
}
