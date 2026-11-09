package model.matriculacion;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import model.fixtures.BaseModelTest;
import util.exception.ValidationException;

/**
 * Pruebas unitarias para las consultas y la persistencia transaccional de microcredenciales (HU 35987).
 */
public class MatriculacionMicrocredencialModelTest extends BaseModelTest {
    private MatriculacionModel model;

    @BeforeEach
    public void setUp() {
        super.setUp();
        model = new MatriculacionModel(db);
    }

    private void insertarMicrocredencialPrueba(String id, String anio, String profesor, String nombre,
                                               String rama, double creditos, int plazas, String estado) {
        String sql = "INSERT INTO Microcredencial ("
                + "    microcredencialId, anioAcademicoId, profesorId, nombre, ramaConocimiento, "
                + "    creditos, plazas, inicioMatriculacion, finMatriculacion, inicioImparticion, finImparticion, "
                + "    estado, fechaPropuesta, fechaResolucion, motivoRechazo, fechaCancelacion, fechaCierreActa, cierreAutomatico"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, '2026-09-01', '2026-11-15', '2026-11-20', '2027-01-30', ?, '2026-08-01', '2026-08-10', NULL, NULL, NULL, 0)";
        db.executeUpdate(sql, id, anio, profesor, nombre, rama, creditos, plazas, estado);
    }

    @Test
    @DisplayName("HU35987: Estudiante matriculado o graduado tiene habilitadas sus ramas de conocimiento")
    public void testRamasHabilitadasPorEstudiante() {
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
        insertarMicrocredencialPrueba("MC901", "1", "1", "Docker para Developers", "Ingenieria y Arquitectura", 3.0, 30, "ACEPTADA");
        insertarMicrocredencialPrueba("MC902", "1", "1", "Biomedicina Molecular", "Ciencias", 4.0, 25, "ACEPTADA");

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

    @Test
    @DisplayName("HU35987: Persistencia transaccional de inscripción con Tarjeta (INSCRITA y pagoValidado = 1)")
    public void testPersistenciaMatriculaTarjeta() {
        insertarMicrocredencialPrueba("MC801", "1", "1", "Testing Avanzado", "Ingenieria y Arquitectura", 3.0, 30, "ACEPTADA");

        OfertaMicrocredencialDTO dto = new OfertaMicrocredencialDTO();
        dto.setMicrocredencialId("MC801");
        dto.setNombre("Testing Avanzado");
        dto.setRamaConocimiento("Ingenieria y Arquitectura");
        dto.setCreditos(3.0);
        dto.setPrecio(60.0);

        ResumenSolicitudMicrocredencialDTO resumen = new ResumenSolicitudMicrocredencialDTO();
        resumen.setAlumnoId("1");
        resumen.setNombreCompleto("Juan Perez");
        resumen.setFechaSolicitud("2026-11-01");
        resumen.setMetodoPago("TARJETA");
        resumen.setMicrocredenciales(List.of(dto));
        resumen.setCreditosTotales(3);
        resumen.setImporteTotal(60.0);

        assertDoesNotThrow(() -> model.registrarSolicitudMicrocredencial(resumen));

        // Verificar registro persistido en la base de datos
        List<Object[]> rows = db.executeQueryArray(
                "SELECT matriculaId, microcredencialId, estado, importe, metodoPago, pagoValidado FROM MatriculaMicrocredencial WHERE microcredencialId = ?",
                "MC801");

        assertNotNull(rows);
        assertEquals(1, rows.size(), "Debe existir exactamente 1 fila en MatriculaMicrocredencial");
        assertEquals("1", rows.get(0)[0].toString()); // matriculaId
        assertEquals("MC801", rows.get(0)[1].toString());
        assertEquals("INSCRITA", rows.get(0)[2].toString());
        assertEquals(60.0, ((Number) rows.get(0)[3]).doubleValue(), 0.001);
        assertEquals("TARJETA", rows.get(0)[4].toString());
        assertEquals(1, ((Number) rows.get(0)[5]).intValue());
    }

    @Test
    @DisplayName("HU35987: Persistencia de inscripción con Transferencia (SOLICITADA y pagoValidado = 0)")
    public void testPersistenciaMatriculaTransferencia() {
        insertarMicrocredencialPrueba("MC802", "1", "1", "Cloud Computing", "Ingenieria y Arquitectura", 4.0, 20, "ACEPTADA");

        OfertaMicrocredencialDTO dto = new OfertaMicrocredencialDTO();
        dto.setMicrocredencialId("MC802");
        dto.setNombre("Cloud Computing");
        dto.setRamaConocimiento("Ingenieria y Arquitectura");
        dto.setCreditos(4.0);
        dto.setPrecio(80.0);

        ResumenSolicitudMicrocredencialDTO resumen = new ResumenSolicitudMicrocredencialDTO();
        resumen.setAlumnoId("1");
        resumen.setFechaSolicitud("2026-11-01");
        resumen.setMetodoPago("TRANSFERENCIA");
        resumen.setMicrocredenciales(List.of(dto));
        resumen.setCreditosTotales(4);
        resumen.setImporteTotal(80.0);

        assertDoesNotThrow(() -> model.registrarSolicitudMicrocredencial(resumen));

        List<Object[]> rows = db.executeQueryArray(
                "SELECT estado, metodoPago, pagoValidado FROM MatriculaMicrocredencial WHERE microcredencialId = ?",
                "MC802");

        assertNotNull(rows);
        assertEquals(1, rows.size());
        assertEquals("SOLICITADA", rows.get(0)[0].toString());
        assertEquals("TRANSFERENCIA", rows.get(0)[1].toString());
        assertEquals(0, ((Number) rows.get(0)[2]).intValue());
    }

    @Test
    @DisplayName("HU35987: Validación impide inscribirse dos veces en la misma microcredencial")
    public void testValidacionImpedirInscripcionDuplicada() {
        insertarMicrocredencialPrueba("MC803", "1", "1", "Git Flow", "Ingenieria y Arquitectura", 2.0, 15, "ACEPTADA");

        OfertaMicrocredencialDTO dto = new OfertaMicrocredencialDTO();
        dto.setMicrocredencialId("MC803");
        dto.setNombre("Git Flow");
        dto.setRamaConocimiento("Ingenieria y Arquitectura");
        dto.setCreditos(2.0);
        dto.setPrecio(40.0);

        ResumenSolicitudMicrocredencialDTO resumen = new ResumenSolicitudMicrocredencialDTO();
        resumen.setAlumnoId("1");
        resumen.setMetodoPago("TARJETA");
        resumen.setMicrocredenciales(List.of(dto));

        // Primera inscripción tiene éxito
        assertDoesNotThrow(() -> model.registrarSolicitudMicrocredencial(resumen));

        // Segunda inscripción debe lanzar ValidationException
        ValidationException ex = assertThrows(ValidationException.class, () -> model.registrarSolicitudMicrocredencial(resumen));
        assertTrue(ex.getMessage().toLowerCase().contains("ya se encuentra matriculado")
                || ex.getMessage().toLowerCase().contains("previa"));
    }

    @Test
    @DisplayName("HU35987: Validación impide inscribirse si no hay plazas libres")
    public void testValidacionSinPlazasDisponibles() {
        // Microcredencial con solo 1 plaza
        insertarMicrocredencialPrueba("MC804", "1", "1", "Seguridad Perimetral", "Ingenieria y Arquitectura", 3.0, 1, "ACEPTADA");

        // Simular que esa 1 plaza ya está ocupada y pagada por otro alumno
        db.executeUpdate("INSERT INTO Matricula (matriculaId, alumnoId, gradoId, anioAcademicoId, fechaSolicitud, estado) "
                + "VALUES ('999', '2', '1', '1', '2026-09-01', 'ACTIVA')");
        db.executeUpdate("INSERT INTO MatriculaMicrocredencial VALUES ('999', '1', 'MC804', '2026-10-01', 'INSCRITA', NULL, NULL, 60, 'TARJETA', '2026-10-01', 1, NULL)");

        OfertaMicrocredencialDTO dto = new OfertaMicrocredencialDTO();
        dto.setMicrocredencialId("MC804");
        dto.setNombre("Seguridad Perimetral");
        dto.setRamaConocimiento("Ingenieria y Arquitectura");
        dto.setCreditos(3.0);
        dto.setPrecio(60.0);

        ResumenSolicitudMicrocredencialDTO resumen = new ResumenSolicitudMicrocredencialDTO();
        resumen.setAlumnoId("1");
        resumen.setMetodoPago("TARJETA");
        resumen.setMicrocredenciales(List.of(dto));

        ValidationException ex = assertThrows(ValidationException.class, () -> model.registrarSolicitudMicrocredencial(resumen));
        assertTrue(ex.getMessage().toLowerCase().contains("no quedan plazas"));
    }

    @Test
    @DisplayName("HU35987: Validación impide inscribirse si el alumno no pertenece a la rama")
    public void testValidacionRamaNoHabilitada() {
        // Microcredencial de Ciencias
        insertarMicrocredencialPrueba("MC805", "1", "1", "Astrofísica", "Ciencias", 3.0, 20, "ACEPTADA");

        OfertaMicrocredencialDTO dto = new OfertaMicrocredencialDTO();
        dto.setMicrocredencialId("MC805");
        dto.setNombre("Astrofísica");
        dto.setRamaConocimiento("Ciencias"); // El alumno 1 es de Ingeniería, no de Ciencias
        dto.setCreditos(3.0);
        dto.setPrecio(60.0);

        ResumenSolicitudMicrocredencialDTO resumen = new ResumenSolicitudMicrocredencialDTO();
        resumen.setAlumnoId("1");
        resumen.setMetodoPago("TARJETA");
        resumen.setMicrocredenciales(List.of(dto));

        ValidationException ex = assertThrows(ValidationException.class, () -> model.registrarSolicitudMicrocredencial(resumen));
        assertTrue(ex.getMessage().toLowerCase().contains("rama")
                || ex.getMessage().toLowerCase().contains("matrícula válida"));
    }
}
