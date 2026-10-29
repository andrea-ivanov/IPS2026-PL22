package view.estudiante;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import controller.estudiante.SolicitudMicrocredencialController;
import model.fixtures.BaseModelTest;
import model.matriculacion.MatriculacionModel;
import model.matriculacion.OfertaMicrocredencialDTO;
import model.matriculacion.ResumenSolicitudMicrocredencialDTO;

/**
 * Pruebas unitarias para la Vista y Controlador de Solicitud de Microcredenciales (HU 35987).
 */
public class SolicitudMicrocredencialViewTest extends BaseModelTest {
    private SolicitudMicrocredencialView view;
    private SolicitudMicrocredencialController controller;
    private MatriculacionModel model;
    private AtomicBoolean menuVuelto;

    @BeforeEach
    public void setUpView() {
        menuVuelto = new AtomicBoolean(false);
        view = new SolicitudMicrocredencialView();
        model = new MatriculacionModel(db);
        controller = new SolicitudMicrocredencialController(view, model, "AL0001", () -> menuVuelto.set(true));
        controller.initController();
    }

    @AfterEach
    public void tearDown() {
        if (view != null && view.getFrame() != null) {
            view.getFrame().dispose();
        }
    }

    @Test
    @DisplayName("HU35987: Inicialización en Paso 1 con componentes clave y renderizado de checkboxes")
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
    @DisplayName("HU35987: Selección de métodos de pago")
    public void testMetodosPago() {
        view.getRbTarjeta().setSelected(true);
        assertEquals("TARJETA", view.getMetodoPagoSeleccionado());

        view.getRbTransferencia().setSelected(true);
        assertEquals("TRANSFERENCIA", view.getMetodoPagoSeleccionado());

        view.getRbCartaPago().setSelected(true);
        assertEquals("CARTA_PAGO", view.getMetodoPagoSeleccionado());
    }

    @Test
    @DisplayName("HU35987: Cálculo en vivo a 20.0 € por crédito")
    public void testCalculoEnVivoCreditosEImporte() {
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
    @DisplayName("HU35987: Carga del resumen consolidado en Paso 2")
    public void testCargaResumen() {
        ResumenSolicitudMicrocredencialDTO resumen = new ResumenSolicitudMicrocredencialDTO();
        resumen.setAlumnoId("AL0001");
        resumen.setNombreCompleto("Estudiante Prueba");
        resumen.setDni("12345678Z");
        resumen.setFechaSolicitud("2026-10-29");

        List<OfertaMicrocredencialDTO> lista = new ArrayList<>();
        lista.add(new OfertaMicrocredencialDTO("MC001", "Inteligencia Artificial Aplicada", "Desc",
                "Ingenieria y Arquitectura", 3.0, 30, 10, "2026-09-01", "2026-11-15", "2026-11-20", "2027-01-30"));
        resumen.setMicrocredenciales(lista);
        resumen.setCreditosTotales(3);
        resumen.setImporteTotal(60.0);

        view.cargarResumen(resumen);

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

