package view.estudiante;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import controller.estudiante.MisMatriculasController;
import model.contrato.ContratoAlumnoDTO;
import model.fip.MatriculaFipDisplayDTO;
import model.fixtures.BaseModelTest;
import model.matriculacion.MatriculacionModel;
import view.comun.Estilo;

/**
 * Pruebas de vista de la HU 36967: el alumno con contrato en la universidad consulta
 * sus FIP y sus contratos en la pestaña "FIP" de "Mi Expediente" (MisMatriculasView).
 */
public class MisMatriculasFipViewTest extends BaseModelTest {
	private MisMatriculasView view;
	private MisMatriculasController controller;

	@BeforeEach
	public void setUpView() {
		view = new MisMatriculasView();
		controller = new MisMatriculasController(view, new MatriculacionModel(db), "1", () -> {
		});
		controller.initController();
	}

	@AfterEach
	public void tearDown() {
		if (view != null && view.getFrame() != null) {
			view.getFrame().dispose();
		}
	}

	@Test
	@DisplayName("HU 36967: Alumno sin contrato no ve en ningún caso la pestaña de FIP")
	public void testAlumnoSinContratoNoVePestanaFip() {
		controller.mostrarDatosFip(Collections.emptyList(), Collections.emptyList());

		assertAll(
				() -> assertFalse(view.isPestanaFipVisible(), "La pestaña FIP no debe mostrarse sin contratos"),
				() -> assertEquals(3, view.getTabbedPane().getTabCount(),
						"Solo deben mostrarse las 3 pestañas ordinarias del expediente"));
	}

	@Test
	@DisplayName("HU 36967: Alumno con contrato ve la pestaña FIP a la derecha de Microcredenciales con contratos y resultados")
	public void testAlumnoConContratoVePestanaFipYResultados() {
		List<ContratoAlumnoDTO> contratos = List.of(
				new ContratoAlumnoDTO("CT0002", "1", "Apoyo a la docencia", "2027-01-15", "2027-07-15"),
				new ContratoAlumnoDTO("CT0001", "1", "Becaria del departamento", "2026-09-01", "2027-06-30"));
		List<MatriculaFipDisplayDTO> fips = List.of(
				new MatriculaFipDisplayDTO("FIP003", "Prevencion de Riesgos en Laboratorio", "2026-12-01", "2026-12-10",
						"PENDIENTE"),
				new MatriculaFipDisplayDTO("FIP002", "Proteccion de Datos y Ciberseguridad", "2026-10-05", "2026-10-15",
						"APTO"),
				new MatriculaFipDisplayDTO("FIP001", "Gestion Documental Universitaria", "2026-09-10", "2026-09-20",
						"NO APTO"));

		controller.mostrarDatosFip(contratos, fips);

		assertAll(
				() -> assertTrue(view.isPestanaFipVisible(), "La pestaña FIP debe ser visible al tener contrato"),
				() -> assertEquals(4, view.getTabbedPane().getTabCount(), "Deben existir 4 pestañas en el expediente"),
				() -> assertEquals("  Microcredenciales  ", view.getTabbedPane().getTitleAt(2)),
				() -> assertEquals(MisMatriculasView.TAB_FIP_TITULO, view.getTabbedPane().getTitleAt(3),
						"La pestaña FIP debe situarse inmediatamente a la derecha de Microcredenciales"),
				() -> assertEquals(2, view.getTableModelContratos().getRowCount(), "Deben listarse los 2 contratos"),
				() -> assertEquals("CT0002", view.getTableModelContratos().getValueAt(0, 0)),
				() -> assertEquals("Apoyo a la docencia", view.getTableModelContratos().getValueAt(0, 1)),
				() -> assertEquals(3, view.getTableModelFip().getRowCount(), "Deben listarse las 3 FIP"),
				() -> assertEquals("PENDIENTE", view.getTableModelFip().getValueAt(0, 4)),
				() -> assertEquals("APTO", view.getTableModelFip().getValueAt(1, 4)),
				() -> assertEquals("NO APTO", view.getTableModelFip().getValueAt(2, 4)));
	}

	@Test
	@DisplayName("HU 36967: Si el alumno pasa a no tener contratos, la pestaña FIP se oculta dinámicamente")
	public void testOcultarPestanaFipDinamicamente() {
		controller.mostrarDatosFip(
				List.of(new ContratoAlumnoDTO("CT0001", "1", "Becario", "2026-09-01", "2027-06-30")),
				List.of(new MatriculaFipDisplayDTO("FIP001", "Curso Interno", "2026-10-01", "2026-10-05", null)));
		assertTrue(view.isPestanaFipVisible());
		assertEquals("PENDIENTE", view.getTableModelFip().getValueAt(0, 4));

		view.getTabbedPane().setSelectedIndex(3);
		controller.mostrarDatosFip(Collections.emptyList(), Collections.emptyList());

		assertAll(
				() -> assertFalse(view.isPestanaFipVisible()),
				() -> assertEquals(3, view.getTabbedPane().getTabCount()),
				() -> assertEquals(0, view.getTabbedPane().getSelectedIndex()));
	}

	@Test
	@DisplayName("HU 36967: Estilo asigna colores diferenciados a PENDIENTE, APTO y NO APTO")
	public void testColoresResultadoFip() {
		Color[] pendiente = Estilo.coloresEstado("PENDIENTE");
		Color[] apto = Estilo.coloresEstado("APTO");
		Color[] noApto = Estilo.coloresEstado("NO APTO");

		assertAll(
				() -> assertEquals(Estilo.coloresEstado("SOLICITADA")[0], pendiente[0]),
				() -> assertEquals(Estilo.coloresEstado("APROBADO")[0], apto[0]),
				() -> assertEquals(Estilo.coloresEstado("SUSPENSO")[0], noApto[0]),
				() -> assertNotEquals(apto[0], noApto[0]),
				() -> assertNotEquals(pendiente[0], apto[0]));
	}
}
