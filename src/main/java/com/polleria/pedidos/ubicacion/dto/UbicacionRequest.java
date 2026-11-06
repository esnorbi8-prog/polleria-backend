package com.polleria.pedidos.ubicacion.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** Posición que manda el celular del repartidor: { "latitud": -14.07, "longitud": -75.73, "precisionMetros": 12 }. */
public class UbicacionRequest {

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitud;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitud;

    private Double precisionMetros;

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
    public Double getPrecisionMetros() { return precisionMetros; }
    public void setPrecisionMetros(Double precisionMetros) { this.precisionMetros = precisionMetros; }
}
