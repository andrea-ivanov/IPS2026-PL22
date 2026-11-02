package model.matriculacion;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import model.fixtures.BaseModelTest;

/**
 * Pruebas unitarias para las consultas y reglas de negocio del modelo de microcredenciales (HU 35987).
 */
public class MatriculacionMicrocredencialModelTest extends BaseModelTest {
    private MatriculacionModel model;

    @BeforeEach
    public void setUp() {
        model = new MatriculacionModel(db);
    }

    @Test
    @DisplayName("HU35987: Estudiante matriculado o graduado tiene habilitadas sus ramas de conocimiento")
    public void testRamasHabilitadasPorEstudiante() {
        // En los datos de prueba, AL0001 (o UO 1) pertenece a un grado de Ingeniería
        List<String> ramas = model.getRamasHabilitadasPorEstudiante("1");
        assertNotNull(ramas);
        assertFalse(ramas.isEmpty(), "El estudiante 1 debe tener al menos una rama habilitada");
        boolean coincide = ramas.stream().anyMatch(r -> r.toLowerCase().contains("ingenier")
                || r.toLowerCase().contains("cienc"));
        assertTrue(coincide, "Debe contener una rama válida de su titulación");
    }

    @Test
    @DisplayName("HU35987: Alumno inexistente o nulo no tiene ramas habilitadas")
    public void testRamasHabilitadasAlumnoNulo() {
        List<String> ramasNulo = model.getRamasHabilitadasPorEstudiante(null);
        assertNotNull(ramasNulo);
        assertTrue(ramasNulo.isEmpty());

        List<String> ramasVacio = model.getRamasHabilitadasPorEstudiante("AL_INEXISTENTE_99999");
        assertNotNull(ramasVacio);
        assertTrue(ramasVacio.isEmpty());
    }

    @Test
    @DisplayName("HU35987: Oferta de microcredenciales solo incluye aquellas de las ramas habilitadas")
    public void testOfertaMicrocredencialesFiltradasPorRama() {
        List<String> ramasSoloIngenieria = List.of("Ingenieria y Arquitectura");
        List<OfertaMicrocredencialDTO> oferta = model.getOfertaMicrocredenciales(ramasSoloIngenieria);

        assertNotNull(oferta);
        assertFalse(oferta.isEmpty(), "Debe haber oferta para Ingeniería y Arquitectura");

        for (OfertaMicrocredencialDTO mc : oferta) {
            assertEquals("Ingenieria y Arquitectura", mc.getRamaConocimiento(),
                    "Todas las microcredenciales ofertadas deben pertenecer a la rama solicitada");
            assertTrue(mc.getCreditos() > 0, "Los créditos deben ser positivos");
            assertEquals(mc.getCreditos() * 20.0, mc.getPrecio(), 0.001,
                    "El precio oficial debe ser siempre créditos * 20.0 euros");
        }
    }

    @Test
    @DisplayName("HU35987: Sin ramas habilitadas la oferta es vacía")
    public void testOfertaSinRamasVacia() {
        List<OfertaMicrocredencialDTO> oferta = model.getOfertaMicrocredenciales(List.of());
        assertNotNull(oferta);
        assertTrue(oferta.isEmpty());
    }
}
