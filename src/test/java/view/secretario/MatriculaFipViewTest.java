package view.secretario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import controller.secretario.SecretarioController;
import model.fip.FIPDTO;
import model.fip.InscripcionFipPersistenciaTest;
import util.Database;
import util.RelojSistema;

/**
 * Pruebas de la pestaña «Matrícula FIP» de Secretaría (HU 37046), con la fecha del sistema fijada en 2026-11-23 y los
 * datos de InscripcionFipPersistenciaTest: FIP001 (responsable P001, abierta hasta el 2026-11-30, sin inscritos) y
 * FIP002 (ya empezada, con AL0001 inscrito). AL0001 tiene contrato y AL0002 no.
 */
public class MatriculaFipViewTest {
	private static final String LUCIA = "UO0001 - Lucia Pascual Santos";
	private static final String PABLO = "UO0002 - Pablo Garcia Ruiz";

	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private SecretarioView view;
	private MatriculaFipPanel panel;

	@BeforeAll
	public static void botonesEnEspanol() {
		DialogosPrueba.botonesEnEspanol();
	}

	@BeforeEach
	public void setUp() throws Exception {
		RelojSistema.getInstance().fijarFecha(LocalDate.of(2026, 11, 23));
		db.createDatabase(false);
		InscripcionFipPersistenciaTest.loadCleanDatabase(db);
		// con máximo de plazas en las dos FIP, tenga o no el esquema la columna
		InscripcionFipPersistenciaTest.fijarPlazas(db, "FIP001", 30);
		InscripcionFipPersistenciaTest.fijarPlazas(db, "FIP002", 30);
		SwingUtilities.invokeAndWait(() -> {
			view = new SecretarioView();
			new SecretarioController(view, db).initController();
			panel = view.getMatriculaFipPanel();
		});
	}

	@AfterEach
	public void tearDown() throws Exception {
		RelojSistema.getInstance().restablecerFechaReal();
		SwingUtilities.invokeAndWait(() -> view.getFrame().dispose());
		new InscripcionFipPersistenciaTest().tearDown();
	}

	// //////////////////////////////////////////////
	// Consulta
	// //////////////////////////////////////////////

	/** Al abrir queda elegida la FIP que empieza más tarde, con el plazo en el que se puede matricular */
	@Test
	public void testAlAbrir() {
		assertEquals(2, panel.getCbFip().getItemCount());
		assertEquals("FIP001", ((FIPDTO) panel.getCbFip().getSelectedItem()).getFipId());
		assertEquals("Inscritos en FIP001 - Metodologias Docentes", panel.getLblTitulo().getText());
		assertEquals("0 inscritos de 30 plazas · matrícula abierta hasta el 2026-11-30", panel.getLblResultados().getText());
	}

	/** Los inscritos de la FIP elegida, con su tipo y resultado */
	@Test
	public void testInscritos() throws Exception {
		elegirFip("FIP002");
		assertEquals(1, panel.getTabInscritos().getRowCount());
		assertEquals(List.of("IF00001", "AL0001 - Lucia Pascual Santos", "Alumno", "2026-10-05", "PENDIENTE"), fila(0));
		assertEquals("1 inscrito de 30 plazas · la FIP ya ha empezado: no admite altas",
				panel.getLblResultados().getText());
	}

	/** El plazo depende de la fecha del sistema, que se puede simular: el día de inicio ya está cerrado */
	@Test
	public void testPlazoConFechaSimulada() throws Exception {
		RelojSistema.getInstance().fijarFecha(LocalDate.of(2026, 12, 1));
		elegirFip("FIP001");
		assertEquals("0 inscritos de 30 plazas · la FIP ya ha empezado: no admite altas",
				panel.getLblResultados().getText());
		RelojSistema.getInstance().fijarFecha(LocalDate.of(2026, 11, 30));
		elegirFip("FIP001");
		assertEquals("0 inscritos de 30 plazas · matrícula abierta hasta el 2026-11-30", panel.getLblResultados().getText());
	}

	/** Con una fecha simulada anterior a la creación de la FIP, la matrícula todavía no está abierta */
	@Test
	public void testPlazoAntesDeCrearLaFip() throws Exception {
		RelojSistema.getInstance().fijarFecha(LocalDate.of(2026, 10, 31));
		elegirFip("FIP001");
		assertEquals("0 inscritos de 30 plazas · la matrícula se abre el 2026-11-01", panel.getLblResultados().getText());
	}

	/** El texto de ayuda cabe en la columna del formulario: se parte en varias líneas en vez de cortarse */
	@Test
	public void testAyudaCabe() {
		assertTrue(panel.getAyuda().getPreferredSize().width <= 270, "ancho " + panel.getAyuda().getPreferredSize().width);
	}

	// //////////////////////////////////////////////
	// Matricular
	// //////////////////////////////////////////////

	/** Alumno con contrato: se avisa y aparece como PENDIENTE con la fecha del sistema */
	@Test
	public void testMatricularAlumno() throws Exception {
		elegirUsuario(MatriculaFipPanel.TIPO_ALUMNO, LUCIA);
		List<String> dialogos = DialogosPrueba.pulsarYResponder(panel.getBtnMatricular(), "Sí", "Aceptar");
		assertEquals(2, dialogos.size());
		assertEquals("Matricular en FIP: ¿Desea matricular a " + LUCIA + " en Metodologias Docentes?\n"
				+ "La matrícula no se puede anular.", dialogos.get(0));
		assertTrue(dialogos.get(1).startsWith(
				"Matrícula registrada: " + LUCIA + " matriculado en Metodologias Docentes (IF00002)"), dialogos.get(1));
		assertEquals(List.of("IF00002", "AL0001 - Lucia Pascual Santos", "Alumno", "2026-11-23", "PENDIENTE"), fila(0));
		assertEquals("1 inscrito de 30 plazas · matrícula abierta hasta el 2026-11-30", panel.getLblResultados().getText());
	}

	/** Si no se confirma no se matricula */
	@Test
	public void testMatricularSinConfirmar() throws Exception {
		elegirUsuario(MatriculaFipPanel.TIPO_ALUMNO, LUCIA);
		List<String> dialogos = DialogosPrueba.pulsarYResponder(panel.getBtnMatricular(), "No");
		assertEquals(1, dialogos.size());
		assertEquals(0, panel.getTabInscritos().getRowCount());
	}

	/** Las matrículas en FIP no se pueden anular (confirmado por el PO): no hay ningún botón para hacerlo */
	@Test
	public void testNoSePuedeAnular() {
		assertEquals(List.of("Matricular"), DialogosPrueba.textosBotones(panel.getPanel()));
	}

	/** Un profesor se matricula directamente, sin contrato */
	@Test
	public void testMatricularProfesor() throws Exception {
		elegirUsuario(MatriculaFipPanel.TIPO_PROFESOR, "P002");
		DialogosPrueba.pulsarYResponder(panel.getBtnMatricular(), "Sí", "Aceptar");
		assertEquals(List.of("IF00002", "P002 - Jorge Santana Perez", "Profesor", "2026-11-23", "PENDIENTE"), fila(0));
	}

	/** Sin contrato vigente al inicio de la FIP se bloquea la matrícula */
	@Test
	public void testAlumnoSinContrato() throws Exception {
		elegirUsuario(MatriculaFipPanel.TIPO_ALUMNO, PABLO);
		List<String> dialogos = DialogosPrueba.pulsarYResponder(panel.getBtnMatricular(), "Sí", "ACEPTAR");
		assertEquals("Informacion: El alumno AL0002 no tiene ningún contrato vigente el 2026-12-01 "
				+ "(fecha de inicio de la FIP)", dialogos.get(1));
		assertEquals(0, panel.getTabInscritos().getRowCount());
	}

	/** El contrato registrado en la otra pestaña vale en el momento: las dos HU enlazadas */
	@Test
	public void testRegistrarContratoYMatricular() throws Exception {
		ContratosAlumnoPanel contratos = view.getContratosPanel();
		SwingUtilities.invokeAndWait(() -> {
			contratos.getCbAlumno().setSelectedItem(PABLO);
			contratos.getTxtDescripcion().setText("Becario");
			contratos.getTxtFechaInicio().setText("2026-11-01");
			contratos.getTxtFechaFin().setText("2027-01-31");
		});
		DialogosPrueba.pulsarYResponder(contratos.getBtnRegistrar(), "Aceptar");
		elegirUsuario(MatriculaFipPanel.TIPO_ALUMNO, PABLO);
		DialogosPrueba.pulsarYResponder(panel.getBtnMatricular(), "Sí", "Aceptar");
		assertEquals("AL0002 - Pablo Garcia Ruiz", fila(0).get(1));
	}

	/** El responsable de la FIP no se matricula en ella */
	@Test
	public void testResponsable() throws Exception {
		elegirUsuario(MatriculaFipPanel.TIPO_PROFESOR, "P001");
		List<String> dialogos = DialogosPrueba.pulsarYResponder(panel.getBtnMatricular(), "Sí", "ACEPTAR");
		assertEquals(2, dialogos.size());
		assertTrue(dialogos.get(1).contains("responsable"), dialogos.get(1));
		assertEquals(0, panel.getTabInscritos().getRowCount());
	}

	/** Con la FIP empezada (fecha simulada) no se matricula */
	@Test
	public void testFipEmpezada() throws Exception {
		RelojSistema.getInstance().fijarFecha(LocalDate.of(2026, 12, 1));
		elegirUsuario(MatriculaFipPanel.TIPO_ALUMNO, LUCIA);
		List<String> dialogos = DialogosPrueba.pulsarYResponder(panel.getBtnMatricular(), "Sí", "ACEPTAR");
		assertEquals("Informacion: El plazo de matrícula terminó el día anterior al inicio de la FIP (2026-11-30)",
				dialogos.get(1));
		assertEquals(0, panel.getTabInscritos().getRowCount());
	}

	/** Con máximo de plazas se ven las ocupadas, y al llenarse la FIP ya no se matricula */
	@Test
	public void testPlazas() throws Exception {
		InscripcionFipPersistenciaTest.fijarPlazas(db, "FIP001", 2);
		matricularALucia("1 inscrito de 2 plazas · matrícula abierta hasta el 2026-11-30");
		elegirUsuario(MatriculaFipPanel.TIPO_PROFESOR, "P002");
		DialogosPrueba.pulsarYResponder(panel.getBtnMatricular(), "Sí", "Aceptar");
		assertEquals("2 inscritos de 2 plazas · la FIP está completa", panel.getLblResultados().getText());

		db.executeUpdate("UPDATE Fip SET profesorId = NULL, formadorExternoId = 'FE001' WHERE fipId = 'FIP001'");
		elegirUsuario(MatriculaFipPanel.TIPO_PROFESOR, "P001");
		List<String> dialogos = DialogosPrueba.pulsarYResponder(panel.getBtnMatricular(), "Sí", "ACEPTAR");
		assertEquals("Informacion: No quedan plazas disponibles en la FIP (máximo 2 plazas cubiertas)", dialogos.get(1));
		assertEquals(2, panel.getTabInscritos().getRowCount());
	}

	private void matricularALucia(String resultado) throws Exception {
		elegirUsuario(MatriculaFipPanel.TIPO_ALUMNO, LUCIA);
		DialogosPrueba.pulsarYResponder(panel.getBtnMatricular(), "Sí", "Aceptar");
		assertEquals(resultado, panel.getLblResultados().getText());
	}

	/** Un texto que no es ningún usuario no se acepta */
	@Test
	public void testUsuarioNoValido() throws Exception {
		elegirUsuario(MatriculaFipPanel.TIPO_ALUMNO, "no existe");
		List<String> dialogos = DialogosPrueba.pulsarYResponder(panel.getBtnMatricular(), "ACEPTAR");
		assertEquals(List.of("Informacion: Escriba o seleccione el usuario que se va a matricular"), dialogos);
	}

	// //////////////////////////////////////////////
	// Auxiliares
	// //////////////////////////////////////////////

	private void elegirFip(String fipId) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (int i = 0; i < panel.getCbFip().getItemCount(); i++) {
				if (((FIPDTO) panel.getCbFip().getItemAt(i)).getFipId().equals(fipId)) {
					panel.getCbFip().setSelectedIndex(i);
				}
			}
		});
	}

	/** Elige la opción que empieza por el texto; si no hay ninguna, lo deja escrito tal cual */
	private void elegirUsuario(String tipo, String texto) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			panel.getCbTipo().setSelectedItem(tipo);
			Object opcion = texto;
			for (int i = 0; i < panel.getCbUsuario().getItemCount(); i++) {
				if (panel.getCbUsuario().getItemAt(i).toString().startsWith(texto)) {
					opcion = panel.getCbUsuario().getItemAt(i);
				}
			}
			panel.getCbUsuario().setSelectedItem(opcion);
		});
	}

	private List<Object> fila(int i) {
		JTable t = panel.getTabInscritos();
		return List.of(t.getValueAt(i, 0), t.getValueAt(i, 1), t.getValueAt(i, 2), t.getValueAt(i, 3), t.getValueAt(i, 4));
	}
}
