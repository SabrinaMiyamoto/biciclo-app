package com.biciclo.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertiesTest {

    @Test
    void corsRecusaCuringa() {
        assertThrows(IllegalStateException.class, () -> new CorsProperties(List.of("*")));
    }

    @Test
    void corsLimpaEspacosEVazios() {
        CorsProperties props = new CorsProperties(List.of(" http://localhost:4200 ", ""));

        assertEquals(List.of("http://localhost:4200"), props.allowedOrigins());
    }

    @Test
    void adminComparaEmailSemDiferenciarMaiusculas() {
        AdminProperties props = new AdminProperties(List.of(" Ana@Biciclo.com "));

        assertTrue(props.isAdmin("ana@biciclo.com"));
        assertFalse(props.isAdmin("bia@biciclo.com"));
        assertFalse(new AdminProperties(null).isAdmin("ana@biciclo.com"));
    }
}
