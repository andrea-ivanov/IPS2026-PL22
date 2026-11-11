package view.profesor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import controller.profesor.ProfesorController;
import model.microcredencial.ActaMicrocredencialModelTest;
import model.profesor.ProfesorDTO;
import util.Database;

/**
 * Pruebas del acta de una microcredencial en la pestaña "Microcredenciales" del profesor (HU 36459): se
 * selecciona la microcredencial, se escriben notas y se pulsan los botones respondiendo a los diálogos.
 * Abre ventanas, así que no se ejecuta en mvn test: mvn test -Dtest=ProfesorActaMicrocredencialViewTest
 * Datos de ActaMicrocredencialModelTest, con las fechas de MC1 (terminó ayer) y MC2 (empieza en 5 días)
 * movidas a hoy para que no dependan del día en que se ejecuta.
 */
public class ProfesorActaMicrocredencialViewTest {
	private static final long ESPERA_MAXIMA_MS = 15000;
	private static final int COLUMNA_NOTA = 4;

	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private ProfesorView view;

	@BeforeAll
	public static void botonesEnEspanol() {
		// Igual que MainController, para que los botones no dependan del idioma del sistema
		UIManager.put("OptionPane.yesButtonText", "Sí");
		UIManager.put("OptionPane.noButtonText", "No");
		UIManager.put("OptionPane.okButtonText", "Aceptar");
		UIManager.put("OptionPane.cancelButtonText", "Cancelar");
	}

	@BeforeEach
	public void setUp() throws Exception {
		db.createDatabase(false);
		ActaMicrocredencialModelTest.loadCleanDatabase(db);
		LocalDate hoy = LocalDate.now();
		db.executeUpdate("UPDATE Microcredencial SET inicioMatriculacion = ?, finMatriculacion = ?, "
				+ "inicioImparticion = ?, finImparticion = ? WHERE microcredencialId = 'MC1'",
				hoy.minusDays(40).toString(), hoy.minusDays(26).toString(), hoy.minusDays(20).toString(),
				hoy.minusDays(1).toString());
		db.executeUpdate("UPDATE Microcredencial SET inicioMatriculacion = ?, finMatriculacion = ?, "
				+ "inicioImparticion = ?, finImparticion = ? WHERE microcredencialId = 'MC2'",
				hoy.minusDays(14).toString(), hoy.toString(), hoy.plusDays(5).toString(), hoy.plusDays(30).toString());
		SwingUtilities.invokeAndWait(() -> {
			view = new ProfesorView();
			new ProfesorController(view, db, new ProfesorDTO("P001", "Ana", "Garcia")).initController();
		});
	}

	@AfterEach
	public void tearDown() throws Exception {
		SwingUtilities.invokeAndWait(() -> view.getFrame().dispose());
	}

	// //////////////////////////////////////////////
	// Consulta
	// //////////////////////////////////////////////

	/** Al elegir la microcredencial sale su acta, solo con las inscripciones pagadas, y se puede calificar y cerrar */
	@Test
	public void testSeleccionarMuestraActaSoloPagadas() throws Exception {
		seleccionar("MC1");
		JTable notas = view.getTabNotasMicrocredencial();
		assertEquals(3, notas.getRowCount());
		assertEquals("M1", notas.getValueAt(0, 0));
		assertEquals("M2", notas.getValueAt(1, 0));
		assertEquals("M5", notas.getValueAt(2, 0));
		assertEquals("8.5", notas.getValueAt(0, COLUMNA_NOTA));
		assertEquals("", notas.getValueAt(1, COLUMNA_NOTA));
		assertTrue(notas.isCellEditable(0, COLUMNA_NOTA));
		assertFalse(notas.isCellEditable(0, 0));
		assertTrue(view.getBtnGuardarNotasMicrocredencial().isEnabled());
		assertTrue(view.getBtnCerrarActaMicrocredencial().isEnabled());
		assertTrue(view.getLblEstadoActaMicrocredencial().getText().startsWith("Acta ABIERTA"),
				view.getLblEstadoActaMicrocredencial().getText());
	}

	/** El profesor solo ve sus microcredenciales: la de otro profesor no está en su tabla */
	@Test
	public void testOtraMicrocredencialNoVisible() {
		assertEquals(-1, fila("MC3"));
		assertTrue(fila("MC1") >= 0);
	}

	/** Antes de que empiece la microcredencial no se ponen notas ni se cierra */
	@Test
	public void testAntesDeEmpezarNoEditable() throws Exception {
		seleccionar("MC2");
		JTable notas = view.getTabNotasMicrocredencial();
		assertEquals(1, notas.getRowCount());
		assertFalse(notas.isCellEditable(0, COLUMNA_NOTA));
		assertFalse(view.getBtnGuardarNotasMicrocredencial().isEnabled());
		assertFalse(view.getBtnCerrarActaMicrocredencial().isEnabled());
		assertTrue(view.getLblEstadoActaMicrocredencial().getText().contains("todavía no se pueden poner notas"),
				view.getLblEstadoActaMicrocredencial().getText());
	}

	/** Un acta cerrada es de solo lectura y muestra NP a quien no tiene nota */
	@Test
	public void testActaCerradaSoloLectura() throws Exception {
		seleccionar("MC4");
		JTable notas = view.getTabNotasMicrocredencial();
		assertEquals("NP", notas.getValueAt(0, COLUMNA_NOTA));
		assertFalse(notas.isCellEditable(0, COLUMNA_NOTA));
		assertFalse(view.getBtnGuardarNotasMicrocredencial().isEnabled());
		assertFalse(view.getBtnCerrarActaMicrocredencial().isEnabled());
		assertEquals("Acta CERRADA el 2026-10-30: solo lectura", view.getLblEstadoActaMicrocredencial().getText());
	}

	// //////////////////////////////////////////////
	// Notas y cierre
	// //////////////////////////////////////////////

	/** Se escribe una nota con coma decimal, se guarda y queda en la BD */
	@Test
	public void testGuardarNotas() throws Exception {
		seleccionar("MC1");
		SwingUtilities.invokeAndWait(() -> view.getTabNotasMicrocredencial().setValueAt("7,5", 1, COLUMNA_NOTA));
		List<String> dialogos = pulsarYResponder(view.getBtnGuardarNotasMicrocredencial(), "Aceptar");

		assertEquals(List.of("Guardar notas"), dialogos);
		assertEquals("7.5", nota("M2"));
		assertEquals("7.5", view.getTabNotasMicrocredencial().getValueAt(1, COLUMNA_NOTA));
	}

	/** Si no se confirma el cierre no cambia nada */
	@Test
	public void testCerrarSinConfirmar() throws Exception {
		seleccionar("MC1");
		List<String> dialogos = pulsarYResponder(view.getBtnCerrarActaMicrocredencial(), "No");

		assertEquals(List.of("Cerrar acta"), dialogos);
		assertEquals("ACEPTADA", columna("estado"));
		assertTrue(view.getBtnCerrarActaMicrocredencial().isEnabled());
	}

	/** Al confirmar se cierra con la fecha de hoy, las notas quedan fijas y quien no tenía nota sale NP */
	@Test
	public void testCerrarConConfirmacion() throws Exception {
		seleccionar("MC1");
		List<String> dialogos = pulsarYResponder(view.getBtnCerrarActaMicrocredencial(), "Sí", "Aceptar");

		assertEquals(List.of("Cerrar acta", "Acta cerrada"), dialogos);
		assertEquals("FINALIZADA", columna("estado"));
		assertEquals(LocalDate.now().toString(), columna("fechaCierreActa"));
		assertEquals("0", columna("cierreAutomatico"));
		assertFalse(view.getBtnCerrarActaMicrocredencial().isEnabled());
		assertFalse(view.getBtnGuardarNotasMicrocredencial().isEnabled());
		assertEquals("NP", view.getTabNotasMicrocredencial().getValueAt(1, COLUMNA_NOTA));
		assertTrue(view.getLblEstadoActaMicrocredencial().getText().startsWith("Acta CERRADA"),
				view.getLblEstadoActaMicrocredencial().getText());
		assertEquals("FINALIZADA", view.getTabMicrocredenciales().getValueAt(fila("MC1"), 11));
	}

	// //////////////////////////////////////////////
	// Auxiliares
	// //////////////////////////////////////////////

	private int fila(String microcredencialId) {
		JTable tabla = view.getTabMicrocredenciales();
		for (int i = 0; i < tabla.getRowCount(); i++) {
			if (microcredencialId.equals(tabla.getValueAt(i, 0))) {
				return i;
			}
		}
		return -1;
	}

	private void seleccionar(String microcredencialId) throws Exception {
		int fila = fila(microcredencialId);
		assertTrue(fila >= 0, "No está " + microcredencialId);
		SwingUtilities.invokeAndWait(() -> view.getTabMicrocredenciales().setRowSelectionInterval(fila, fila));
	}

	/**
	 * Pulsa el botón y responde los diálogos que abre, en orden, pulsando el botón de cada uno.
	 * @return los títulos de los diálogos que se han abierto
	 */
	private List<String> pulsarYResponder(JButton boton, String... botones) throws Exception {
		List<String> titulos = Collections.synchronizedList(new ArrayList<>());
		Thread respondedor = new Thread(() -> responder(titulos, botones));
		respondedor.setDaemon(true);
		respondedor.start();
		// doClick vuelve cuando se han cerrado todos los diálogos modales que abre
		SwingUtilities.invokeAndWait(boton::doClick);
		respondedor.join(ESPERA_MAXIMA_MS);
		return new ArrayList<>(titulos);
	}

	private void responder(List<String> titulos, String[] botones) {
		Set<Window> atendidos = Collections.newSetFromMap(new IdentityHashMap<>());
		long limite = System.currentTimeMillis() + ESPERA_MAXIMA_MS;
		int i = 0;
		while (i < botones.length && System.currentTimeMillis() < limite) {
			JDialog dialogo = dialogoNuevo(atendidos);
			if (dialogo == null) {
				dormir(100);
				continue;
			}
			atendidos.add(dialogo);
			dormir(150);
			String texto = botones[i++];
			titulos.add(dialogo.getTitle());
			try {
				SwingUtilities.invokeAndWait(() -> {
					JButton b = buscarBoton(dialogo.getContentPane(), texto);
					assertNotNull(b, "El diálogo " + dialogo.getTitle() + " no tiene el botón " + texto);
					b.doClick();
				});
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private static JDialog dialogoNuevo(Set<Window> atendidos) {
		for (Window w : Window.getWindows()) {
			if (w instanceof JDialog && w.isShowing() && !atendidos.contains(w)) {
				return (JDialog) w;
			}
		}
		return null;
	}

	private static JButton buscarBoton(Container contenedor, String texto) {
		for (Component c : contenedor.getComponents()) {
			if (c instanceof JButton && texto.equals(((JButton) c).getText())) {
				return (JButton) c;
			}
			if (c instanceof Container) {
				JButton encontrado = buscarBoton((Container) c, texto);
				if (encontrado != null) {
					return encontrado;
				}
			}
		}
		return null;
	}

	private static void dormir(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static String nota(String matriculaId) {
		Object valor = db.executeQueryArray("SELECT CAST(nota AS REAL) FROM MatriculaMicrocredencial "
				+ "WHERE matriculaId = ? AND microcredencialId = 'MC1'", matriculaId).get(0)[0];
		return valor == null ? null : valor.toString();
	}

	private static String columna(String nombre) {
		Object valor = db.executeQueryArray("SELECT " + nombre + " FROM Microcredencial WHERE microcredencialId = 'MC1'")
				.get(0)[0];
		return valor == null ? null : valor.toString();
	}
}
