package view.secretario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import util.Database;
import util.RelojSistema;

/**
 * Pruebas de la pestaña «Contratos de alumnos» de Secretaría (HU 37045), con la fecha del sistema fijada en
 * 2026-11-23 y datos propios: AL0001 con dos contratos (CT0001 terminado y CT0002 vigente) y AL0002 sin ninguno.
 */
public class ContratosAlumnoViewTest {
	private static final String LUCIA = "UO0001 - Lucia Pascual Santos";
	private static final String PABLO = "UO0002 - Pablo Garcia Ruiz";

	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private SecretarioView view;
	private ContratosAlumnoPanel panel;

	@BeforeAll
	public static void botonesEnEspanol() {
		DialogosPrueba.botonesEnEspanol();
	}

	@BeforeEach
	public void setUp() throws Exception {
		RelojSistema.getInstance().fijarFecha(LocalDate.of(2026, 11, 23));
		db.createDatabase(false);
		db.executeBatch(new String[] {
				"INSERT INTO Alumno VALUES ('AL0001','10000001A','Lucia','Pascual Santos','lpascual@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL0002','10000002B','Pablo','Garcia Ruiz','pgarcia@alumno.es')",
				"INSERT INTO ContratoAlumno (contratoId, alumnoId, descripcion, fechaInicio, fechaFin) "
						+ "VALUES ('CT0001','AL0001','Practicas de verano','2026-06-01','2026-08-31')",
				"INSERT INTO ContratoAlumno (contratoId, alumnoId, descripcion, fechaInicio, fechaFin) "
						+ "VALUES ('CT0002','AL0001','Becaria del departamento','2026-09-01','2027-06-30')" });
		SwingUtilities.invokeAndWait(() -> {
			view = new SecretarioView();
			new SecretarioController(view, db).initController();
			panel = view.getContratosPanel();
		});
	}

	@AfterEach
	public void tearDown() throws Exception {
		RelojSistema.getInstance().restablecerFechaReal();
		SwingUtilities.invokeAndWait(() -> view.getFrame().dispose());
		db.executeBatch(new String[] { "DELETE FROM ContratoAlumno" });
	}

	/** El historial sale del contrato que empieza más tarde al que empezó antes, con su situación a día de hoy */
	@Test
	public void testHistorial() throws Exception {
		verContratos(LUCIA);
		assertEquals(2, panel.getTabContratos().getRowCount());
		assertEquals(List.of("CT0002", "Becaria del departamento", "2026-09-01", "2027-06-30", "Vigente"), fila(0));
		assertEquals(List.of("CT0001", "Practicas de verano", "2026-06-01", "2026-08-31", "Terminado"), fila(1));
		assertEquals("Historial de contratos de " + LUCIA, panel.getLblTitulo().getText());
		assertEquals("2 contratos registrados", panel.getLblResultados().getText());
	}

	/** La situación se calcula con la fecha del sistema, que se puede simular en la pantalla de inicio */
	@Test
	public void testSituacionConFechaSimulada() throws Exception {
		RelojSistema.getInstance().fijarFecha(LocalDate.of(2026, 7, 1));
		verContratos(LUCIA);
		assertEquals("Futuro", fila(0).get(4));
		assertEquals("Vigente", fila(1).get(4));
	}

	/** Un contrato está vigente también el día que empieza y el día que termina */
	@Test
	public void testSituacionEnLosLimites() throws Exception {
		RelojSistema.getInstance().fijarFecha(LocalDate.of(2026, 9, 1));
		verContratos(LUCIA);
		assertEquals("Vigente", fila(0).get(4));
		RelojSistema.getInstance().fijarFecha(LocalDate.of(2027, 6, 30));
		verContratos(LUCIA);
		assertEquals("Vigente", fila(0).get(4));
		RelojSistema.getInstance().fijarFecha(LocalDate.of(2027, 7, 1));
		verContratos(LUCIA);
		assertEquals("Terminado", fila(0).get(4));
	}

	@Test
	public void testAlumnoSinContratos() throws Exception {
		verContratos(PABLO);
		assertEquals(0, panel.getTabContratos().getRowCount());
		assertEquals("El alumno no tiene ningún contrato registrado", panel.getLblResultados().getText());
	}

	/** Al registrar se avisa, se vacía el formulario y el contrato aparece en el historial */
	@Test
	public void testRegistrar() throws Exception {
		rellenar(PABLO, "Colaborador", "2027-01-10", "2027-03-31");
		List<String> dialogos = DialogosPrueba.pulsarYResponder(panel.getBtnRegistrar(), "Aceptar");
		assertEquals(List.of("Contrato registrado: Contrato CT0003 registrado para " + PABLO + "."), dialogos);
		assertEquals(List.of("CT0003", "Colaborador", "2027-01-10", "2027-03-31", "Futuro"), fila(0));
		assertEquals("1 contrato registrado", panel.getLblResultados().getText());
		assertEquals("", panel.getTxtDescripcion().getText());
		assertEquals("", panel.getTxtFechaInicio().getText());
		assertEquals("", panel.getTxtFechaFin().getText());
	}

	/** Si las fechas no son válidas se avisa, no se guarda nada y no se pierde lo escrito */
	@Test
	public void testFinAnteriorAlInicio() throws Exception {
		rellenar(PABLO, "Colaborador", "2027-03-31", "2027-01-10");
		List<String> dialogos = DialogosPrueba.pulsarYResponder(panel.getBtnRegistrar(), "ACEPTAR");
		assertEquals(List.of("Informacion: La fecha de fin no puede ser anterior a la de inicio"), dialogos);
		assertEquals("Colaborador", panel.getTxtDescripcion().getText());
		assertEquals(0, contar("AL0002"));
	}

	/** Un texto que no es ningún alumno no se acepta */
	@Test
	public void testAlumnoNoValido() throws Exception {
		rellenar("no existe", "Colaborador", "2027-01-10", "2027-03-31");
		List<String> dialogos = DialogosPrueba.pulsarYResponder(panel.getBtnRegistrar(), "ACEPTAR");
		assertEquals(List.of("Informacion: Escriba o seleccione un alumno de la lista"), dialogos);
		assertEquals(2, contar("AL0001"));
		assertEquals(0, contar("AL0002"));
	}

	/** Solo alta y consulta: la tabla no se puede editar */
	@Test
	public void testHistorialNoEditable() throws Exception {
		verContratos(LUCIA);
		assertTrue(panel.getTabContratos().getRowCount() > 0);
		for (int c = 0; c < panel.getTabContratos().getColumnCount(); c++) {
			assertFalse(panel.getTabContratos().isCellEditable(0, c));
		}
	}

	/** El texto de ayuda cabe en la columna del formulario: se parte en varias líneas en vez de cortarse */
	@Test
	public void testAyudaCabe() {
		assertTrue(panel.getAyuda().getPreferredSize().width <= 270, "ancho " + panel.getAyuda().getPreferredSize().width);
	}

	private void verContratos(String alumno) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			panel.getCbAlumno().setSelectedItem(alumno);
			panel.getBtnVerContratos().doClick();
		});
	}

	private void rellenar(String alumno, String descripcion, String inicio, String fin) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			panel.getCbAlumno().setSelectedItem(alumno);
			panel.getTxtDescripcion().setText(descripcion);
			panel.getTxtFechaInicio().setText(inicio);
			panel.getTxtFechaFin().setText(fin);
		});
	}

	private List<Object> fila(int i) {
		JTable t = panel.getTabContratos();
		return List.of(t.getValueAt(i, 0), t.getValueAt(i, 1), t.getValueAt(i, 2), t.getValueAt(i, 3), t.getValueAt(i, 4));
	}

	private int contar(String alumnoId) {
		return db.executeQueryArray("SELECT 1 FROM ContratoAlumno WHERE alumnoId = ?", alumnoId).size();
	}
}
