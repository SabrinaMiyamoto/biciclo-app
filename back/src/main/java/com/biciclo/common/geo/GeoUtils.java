package com.biciclo.common.geo;

/**
 * Cálculos geográficos compartilhados entre os domínios.
 */
public final class GeoUtils {

    private static final double RAIO_TERRA_METROS = 6_371_000.0;

    private GeoUtils() {
    }

    /**
     * Distância em metros entre duas coordenadas usando a fórmula de haversine.
     */
    public static double distanciaMetros(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RAIO_TERRA_METROS * c;
    }
}
