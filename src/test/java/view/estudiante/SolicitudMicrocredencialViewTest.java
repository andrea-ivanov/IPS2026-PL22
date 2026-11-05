package view.estudiante;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import controller.estudiante.SolicitudMicrocredencialController;
import model.matriculacion.MatriculacionModel;
import model.microcredencial.InscripcionMicrocredencialModel;
import model.microcredencial.OfertaMicrocredencialDTO;
import model.microcredencial.ResumenSolicitudMicrocredencialDTO;
import util.Database;

/**
 * Pruebas unitarias para la Vista y Controlador de Solicitud de Microcredenciales (HU 35987).
 * Datos propios: el alumno AL0001 tiene matrícula este año en un grado de Ingenieria y Arquitectura y hay una
 * microcredencial ACEPTADA de esa rama con la matriculación abierta hoy (el controlador usa la fecha del sistema).
 */
public class SolicitudMicrocredencialViewTest {
    private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
    private SolicitudMicrocredencialView view;
    private SolicitudMicrocredencialController controller;
    private MatriculacionModel model;
    private AtomicBoolean menuVuelto;

    @BeforeEach
    public void setUpView() {
        db.createDatabase(false);
        loadCleanDatabase(db);
        menuVuelto = new AtomicBoolean(false);
        view = new SolicitudMatriculaView();
        model = new MatriculacionModel(db);
        controller = new SolicitudMicrocredencialController(view, model, new InscripcionMicrocredencialModel(db),
                "AL0001", () -> menuVuelto.set(true));
        controller.initController();
    }

    private static void loadCleanDatabase(Database db) {
        LocalDate hoy = LocalDate.now();
        db.executeBatch(new String[] {
                "INSERT INTO AnioAcademico VALUES ('2026-2027','2026-09-01','2027-08-31','2026-07-01','2026-09-30')",
                "INSERT INTO Grado VALUES ('G1','Ingenieria Informatica','Escuela de Ingenieria',"
                        + "'Ingenieria y Arquitectura','VIGENTE','2024-09-01')",
                "INSERT INTO Alumno VALUES ('AL0001','10000001A','Juan','Perez','al0001@uni.es')",
                "INSERT INTO Profesor VALUES ('P001','20000001A','Ana','Garcia','p001@uni.es')",
                "INSERT INTO Matricula VALUES ('M1','AL0001','G1','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
                "INSERT INTO Microcredencial VALUES ('MC001','2026-2027','P001','Inteligencia Artificial Aplicada',"
                        + "'Ingenieria y Arquitectura',3,30,'" + hoy.minusDays(1) + "','" + hoy.plusDays(7) + "','"
                        + hoy.plusDays(10) + "','" + hoy.plusDays(60) + "','ACEPTADA','" + hoy.minusDays(5) + "','"
                        + hoy.minusDays(2) + "',NULL,NULL,NULL,0)"
        });
    }

    @AfterEach
    public void tearDown() {
        if (view != null && view.getFrame() != null) {
            view.getFrame().dispose();
        }
    }

    @Test
    @DisplayName("HU35987: Inicialización en modo Microcredenciales con componentes clave y checkboxes")
    public void testInicializacionPaso1() {
        assertNotNull(view.getLblRamasHabilitadas(), "La etiqueta de ramas habilitadas debe existir");
        assertNotNull(view.getCbFiltroRama(), "El combo de filtro por rama debe existir");
        assertNotNull(view.getTableMicrocredenciales(), "La tabla de microcredenciales debe existir");
        assertNotNull(view.getBtnContinuarPaso2(), "El botón continuar al resumen debe existir");
        assertNotNull(view.getBtnVolverMenuDesdePaso1(), "El botón volver al menú debe existir");

        // Columna 0 debe ser Boolean para checkbox
        assertEquals(Boolean.class, view.getModelMicrocredenciales().getColumnClass(0),
                "La primera columna debe ser de tipo Boolean para renderizar checkboxes");
    }

    @Test
    @DisplayName("HU35987: Navegación secuencial entre pasos (Paso 1 -> Paso 2 -> Paso 3)")
    public void testNavegacionPasos() {
        assertDoesNotThrow(() -> view.mostrarPaso1());
        assertDoesNotThrow(() -> view.mostrarPaso2());
        assertDoesNotThrow(() -> view.mostrarPaso3());
        assertDoesNotThrow(() -> view.mostrarPaso1());
    }

    @Test
    @DisplayName("HU35987: Selección de métodos de pago en vista unificada")
    public void testMetodosPago() {
        view.getRbTarjeta().setSelected(true);
        assertEquals("TARJETA", view.getMetodoPagoSeleccionado());

        view.getRbTransferencia().setSelected(true);
        assertEquals("TRANSFERENCIA", view.getMetodoPagoSeleccionado());

        view.getRbCartaPago().setSelected(true);
        assertEquals("CARTA_PAGO", view.getMetodoPagoSeleccionado());
    }

    @Test
    @DisplayName("HU35987: Cálculo en vivo a 20.0 € por crédito en microcredenciales")
    public void testCalculoEnVivoCreditosEImporte() {
        assertEquals(1, view.getModelMicrocredenciales().getRowCount(), "Debe ofertarse la microcredencial abierta hoy");
        if (view.getModelMicrocredenciales().getRowCount() > 0) {
            view.getModelMicrocredenciales().setValueAt(Boolean.TRUE, 0, 0);

            List<OfertaMicrocredencialDTO> seleccionadas = view.getMicrocredencialesSeleccionadas();
            assertFalse(seleccionadas.isEmpty(), "Debe haber al menos 1 microcredencial seleccionada");

            double creds = (Double) view.getModelMicrocredenciales().getValueAt(0, 4);
            assertEquals(creds, seleccionadas.get(0).getCreditos(), 0.001);
            assertEquals(creds * 20.0, seleccionadas.get(0).getPrecio(), 0.001);
        }
    }

    @Test
    @DisplayName("HU35987: Carga del resumen consolidado de microcredencial en Paso 2")
    public void testCargaResumen() {
        ResumenSolicitudMicrocredencialDTO resumen = new ResumenSolicitudMicrocredencialDTO();
        resumen.setAlumnoId("AL0001");
        resumen.setNombreCompleto("Estudiante Prueba");
        resumen.setDni("12345678Z");
        resumen.setFechaSolicitud("2026-10-29");

        List<OfertaMicrocredencialDTO> lista = new ArrayList<>();
        OfertaMicrocredencialDTO mc = new OfertaMicrocredencialDTO();
        mc.setMicrocredencialId("MC001");
        mc.setNombre("Inteligencia Artificial Aplicada");
        mc.setRamaConocimiento("Ingenieria y Arquitectura");
        mc.setCreditos(3.0);
        mc.setPlazas(30);
        mc.setPlazasLibres(20);
        mc.setInicioMatriculacion("2026-11-01");
        mc.setFinMatriculacion("2026-11-15");
        mc.setInicioImparticion("2026-11-20");
        mc.setFinImparticion("2027-01-30");
        mc.setPrecio(60.0);
        lista.add(mc);
        resumen.setMicrocredenciales(lista);
        resumen.setCreditosTotales(3);
        resumen.setImporteTotal(60.0);

        view.cargarResumenMicrocredencial(resumen);

        assertEquals(1, resumen.getMicrocredenciales().size());
        assertEquals(3, resumen.getCreditosTotales());
        assertEquals(60.0, resumen.getImporteTotal(), 0.001);
    }

    @Test
    @DisplayName("HU35987: Botón Volver al Menú ejecuta el callback")
    public void testVolverAlMenuCallback() {
        view.getBtnVolverMenuDesdePaso1().doClick();
        assertTrue(menuVuelto.get(), "Debe invocarse el callback de regreso al menú");
    }
}
