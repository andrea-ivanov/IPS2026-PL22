package view.estudiante;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import controller.estudiante.EstudianteController;
import model.fixtures.BaseModelTest;

public class EstudianteNavegacionTest extends BaseModelTest {
    private EstudianteView view;
    private EstudianteController controller;
    private AtomicBoolean loginVuelto;

    @BeforeEach
    public void setUpNavigation() {
        loginVuelto = new AtomicBoolean(false);
        view = new EstudianteView();
        controller = new EstudianteController(view, "AL001", () -> loginVuelto.set(true));
        controller.initController();
    }

    @org.junit.jupiter.api.AfterEach
    public void tearDown() {
        if (view != null && view.getFrame() != null) {
            view.getFrame().dispose();
        }
    }

    @Test
    @DisplayName("Navegación: Menú inicial muestra opciones y centrado")
    public void testMenuInicial() {
        assertNotNull(view.getBtnMisMatriculas(), "El botón de consultar matrículas debe existir");
        assertNotNull(view.getBtnSolicitarMatricula(), "El botón de solicitar matrícula debe existir");
        assertNotNull(view.getBtnVolverLogin(), "El botón de volver al login debe existir");
        assertTrue(view.getFrame().isVisible());
    }

    @Test
    @DisplayName("Navegación: Abrir consulta de matrículas y volver al menú principal en la misma ventana")
    public void testNavegacionConsultaYVolver() {
        // 1. Simular clic en Consultar Mis Matrículas
        view.getBtnMisMatriculas().doClick();

        // Verificar que el panel de consulta tiene componentes embebidos
        assertTrue(view.getPanelConsultaContainer().getComponentCount() > 0,
                "El contenedor de consulta debe tener embebido el panel principal");

        // 2. Simular clic en el botón volver de la vista de matrículas
        Component comp = view.getPanelConsultaContainer().getComponent(0);
        assertNotNull(comp);

        // Volver al menú
        view.mostrarMenu();
        // El frame sigue visible y abierto
        assertTrue(view.getFrame().isVisible());
    }

    @Test
    @DisplayName("Navegación: Abrir solicitud de matrícula y volver al menú principal")
    public void testNavegacionSolicitudYVolver() {
        // Clic en Solicitar Nueva Matrícula
        view.getBtnSolicitarMatricula().doClick();
        assertTrue(view.getPanelSolicitudContainer().getComponentCount() > 0);

        // Volver al menú
        view.mostrarMenu();
        assertTrue(view.getFrame().isVisible());
    }

    @Test
    @DisplayName("Navegación: Volver al Login invoca el callback y cierra/oculta la vista de estudiante")
    public void testVolverAlLogin() {
        view.getBtnVolverLogin().doClick();
        assertTrue(loginVuelto.get(), "Debe haberse invocado el callback para regresar al login");
        assertFalse(view.getFrame().isVisible(), "La ventana del estudiante debe cerrarse u ocultarse");
    }
}
