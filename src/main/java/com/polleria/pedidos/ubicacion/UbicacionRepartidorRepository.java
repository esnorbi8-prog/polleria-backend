package com.polleria.pedidos.ubicacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UbicacionRepartidorRepository extends JpaRepository<UbicacionRepartidor, Long> {

    Optional<UbicacionRepartidor> findByPedidoId(Long pedidoId);
}
