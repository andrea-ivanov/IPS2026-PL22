package view.profesor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import controller.profesor.ProfesorController;
import model.fixtures.BaseModelTest;
import model.profesor.ProfesorDTO;

/**
 * El profesor se elige en la pantalla de inicio: el panel de profesor se abre ya cargado con sus datos.
 */
public class ProfesorAccesoTest extends BaseModelTest {
    private ProfesorView view;

    @BeforeEach
    public void setUpProfesor() {
        db.executeBatch(new String[] { "INSERT INTO Profesor VALUES ('P001','10000001A','Ana','Garcia','p001@uni.es')" });
        view = new ProfesorView();
        ProfesorController controller = new ProfesorController(view, db, new ProfesorDTO("P001", "Ana", "Garcia"));
        controller.initController();
    }

    @AfterEach
    public void tearDown() {
        if (view != null && view.getFrame() != null) {
            view.getFrame().dispose();
        }
    }

    @Test
    @DisplayName("El panel muestra el profesor que ha entrado y carga sus pestañas sin pulsar nada")
    public void testPanelCargadoConElProfesorDelLogin() {
        assertEquals("Ana Garcia (P001)", view.getLblProfesor().getText());
        assertTrue(view.getBtnEnviarPropuesta().isEnabled(), "Las propuestas se cargan al abrir");
        assertTrue(view.getBtnProponerMicrocredencial().isEnabled(), "Las microcredenciales se cargan al abrir");
        assertEquals("Microcredenciales de P001 - Ana Garcia: todavía no ha propuesto ninguna",
                view.getLblProfesorMicrocredencial().getText());
    }
}
