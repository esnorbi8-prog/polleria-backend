package com.polleria.pedidos.ubicacion;

/** Cálculos geográficos simples, sin depender de ningún servicio de mapas. */
public final class Geo {

    private static final double RADIO_TIERRA_M = 6_371_000;

    private Geo() {
    }

    /** Distancia en metros entre dos coordenadas (fórmula de Haversine). */
    public static double distanciaMetros(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * RADIO_TIERRA_M * Math.asin(Math.sqrt(a));
    }

    /**
     * El checkout guarda las coordenadas exactas al final de la dirección:
     * "Calle Tulipanes 123 |-14.0754,-75.7296". Devuelve {lat, lng} o null.
     */
    public static double[] coordenadasDeDireccion(String direccion) {
        if (direccion == null || !direccion.contains("|")) return null;
        String[] partes = direccion.substring(direccion.lastIndexOf('|') + 1).split(",");
        if (partes.length < 2) return null;
        try {
            return new double[]{Double.parseDouble(partes[0].trim()), Double.parseDouble(partes[1].trim())};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
