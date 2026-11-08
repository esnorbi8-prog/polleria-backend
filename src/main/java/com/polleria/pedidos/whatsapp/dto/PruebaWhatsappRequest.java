package com.polleria.pedidos.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de POST /api/whatsapp/prueba: { "telefono": "987654321", "texto": "Hola desde la pollería" }. */
public class PruebaWhatsappRequest {

    @NotBlank
    private String telefono;

    private String texto;

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
}
