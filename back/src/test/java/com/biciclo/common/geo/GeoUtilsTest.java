package com.biciclo.common.geo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeoUtilsTest {

    @Test
    void distanciaEntreOMesmoPontoEhZero() {
        assertEquals(0.0, GeoUtils.distanciaMetros(-27.59, -48.55, -27.59, -48.55), 0.001);
    }

    @Test
    void calculaDistanciaDeUmDecimoDeGrauDeLatitude() {
        double metros = GeoUtils.distanciaMetros(-27.59, -48.55, -27.60, -48.55);

        // 0,01° de latitude ≈ 1,11 km
        assertEquals(1111.9, metros, 2.0);
    }
}
