package view.estudiante;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import controller.estudiante.SolicitudMatriculaController;
import model.fixtures.BaseModelTest;
import model.matriculacion.MatriculacionModel;
import model.matriculacion.OfertaCursoDTO;
import model.matriculacion.OfertaGradoDTO;
import model.matriculacion.ResumenSolicitudDTO;

/**
 * Pruebas unitarias para la Vista y Controlador de Solicitud de Matrícula (HU 35235 / Fases 3 y 4).
 */
public class SolicitudMatriculaViewTest extends BaseModelTest {
    private SolicitudMatriculaView view;
    private SolicitudMatriculaController controller;
    private MatriculacionModel model;
    private AtomicBoolean menuVuelto;

    @BeforeEach
    public void setUpView() {
        menuVuelto = new AtomicBoolean(false);
        view = new SolicitudMatriculaView();
        model = new MatriculacionModel(db);
        controller = new SolicitudMatriculaController(view, model, "1", () -> menuVuelto.set(true));
        controller.initController();
    }

    @AfterEach
    public void tearDown() {
        if (view != null && view.getFrame() != null) {
            view.getFrame().dispose();
        }
    }

    @Test
    @DisplayName("Fase 3: La vista se inicializa en el Paso 1 con sus componentes clave")
    public void testInicializacionPaso1() {
        assertNotNull(view.getCbGrados(), "El combo de grados debe estar presente");
        assertNotNull(view.getTableCursos(), "La tabla de asignaturas debe estar presente");
        assertNotNull(view.getBtnContinuarPaso2(), "El botón para continuar al resumen debe estar presente");
        assertNotNull(view.getBtnVolverMenuDesdePaso1(), "El botón para volver al menú debe estar presente");

        // Verificar que la primera columna de la tabla es un checkbox (Boolean)
        assertEquals(Boolean.class, view.getModelCursos().getColumnClass(0),
                "La primera columna debe ser de tipo Boolean para renderizar checkboxes");
    }

    @Test
    @DisplayName("Fase 3: Navegación fluida entre Paso 1, Paso 2 y Paso 3")
    public void testNavegacionPasos() {
        // Inicialmente en Paso 1
        assertDoesNotThrow(() -> view.mostrarPaso1());

        // Navegar a Paso 2 (Resumen)
        assertDoesNotThrow(() -> view.mostrarPaso2());

        // Navegar a Paso 3 (Pago)
        assertDoesNotThrow(() -> view.mostrarPaso3());

        // Volver a Paso 1
        assertDoesNotThrow(() -> view.mostrarPaso1());
    }

    @Test
    @DisplayName("Fase 3: Selección de método de pago y opciones de RadioButton")
    public void testMetodosPago() {
        view.getRbTarjeta().setSelected(true);
        assertEquals("TARJETA", view.getMetodoPagoSeleccionado());

        view.getRbTransferencia().setSelected(true);
        assertEquals("TRANSFERENCIA", view.getMetodoPagoSeleccionado());

        view.getRbCartaPago().setSelected(true);
        assertEquals("CARTA_PAGO", view.getMetodoPagoSeleccionado());
    }

    @Test
    @DisplayName("Fase 3: Carga del resumen consolidado en el Paso 2")
    public void testCargaResumen() {
        ResumenSolicitudDTO resumen = new ResumenSolicitudDTO();
        resumen.setAlumnoId("AL001");
        resumen.setNombreCompleto("Lucia");
        resumen.setDni("10000001A");
        resumen.setGradoId("G001");
        resumen.setNombreGrado("Ingeniería Informática");
        resumen.setAnioAcademicoId("2026-2027");
        resumen.setFechaSolicitud("2026-09-01");

        List<OfertaCursoDTO> cursos = new ArrayList<>();
        cursos.add(new OfertaCursoDTO("C001", "Programación", 1, 6.0, "General"));
        cursos.add(new OfertaCursoDTO("C002", "Bases de Datos", 1, 6.0, "General"));
        resumen.setAsignaturas(cursos);

        view.cargarResumen(resumen);

        assertEquals(2, resumen.getAsignaturas().size());
        assertEquals(12, resumen.getCreditosTotales());
        assertEquals(240.0, resumen.getImporteTotal(), 0.001);
    }

    @Test
    @DisplayName("Fase 4: Botón Volver al Menú invoca el callback")
    public void testVolverAlMenuCallback() {
        view.getBtnVolverMenuDesdePaso1().doClick();
        assertTrue(menuVuelto.get(), "Debe haberse invocado el callback para regresar al menú");
    }
}

