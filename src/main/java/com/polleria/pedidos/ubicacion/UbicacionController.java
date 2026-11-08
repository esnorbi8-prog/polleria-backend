package com.polleria.pedidos.ubicacion;

import com.polleria.pedidos.ubicacion.dto.UbicacionRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de geolocalización del repartidor.
 *
 * POST /api/pedidos/{id}/ubicacion — el repartidor manda su posición (REPARTIDOR/ADMINISTRADOR).
 * GET  /api/pedidos/{id}/ubicacion — última posición conocida (cualquiera logueado):
 *      200 con la posición, o 204 si el repartidor todavía no envió ninguna.
 */
@RestController
@RequestMapping("/api/pedidos/{id}/ubicacion")
public class UbicacionController {

    private final UbicacionService ubicacionService;

    public UbicacionController(UbicacionService ubicacionService) {
        this.ubicacionService = ubicacionService;
    }

    @PostMapping
    public UbicacionRepartidor actualizar(@PathVariable Long id,
                                          @Valid @RequestBody UbicacionRequest request,
                                          Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return ubicacionService.actualizar(id, request, email);
    }

    @GetMapping
    public ResponseEntity<UbicacionRepartidor> obtener(@PathVariable Long id) {
        return ubicacionService.obtener(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
