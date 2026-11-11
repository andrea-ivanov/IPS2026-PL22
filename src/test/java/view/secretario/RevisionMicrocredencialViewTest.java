package view.secretario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import controller.secretario.RevisionMicrocredencialController;
import model.microcredencial.RevisionMicrocredencialModelTest;
import util.Database;

/**
 * Pruebas de la pestaña "Propuestas de Microcredenciales" de Secretaría (HU 35994): se pulsan los botones de la
 * vista y se responden los diálogos como lo haría el secretario.
 * Abre ventanas, así que no se ejecuta en mvn test (ver docs/HISTORIAS.md).
 * Datos de RevisionMicrocredencialModelTest: MC002 y MC001 PROPUESTAS (en ese orden), MC003 ACEPTADA, MC004 RECHAZADA
 */
public class RevisionMicrocredencialViewTest {
	private static final long ESPERA_MAXIMA_MS = 15000;

	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private SecretarioView view;

	/** Un diálogo que se espera: se escribe el motivo (si no es nulo) y se pulsa el botón */
	private static class Paso {
		final String motivo;
		final String boton;

		Paso(String motivo, String boton) {
			this.motivo = motivo;
			this.boton = boton;
		}
	}

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
		RevisionMicrocredencialModelTest.loadCleanDatabase(db);
		SwingUtilities.invokeAndWait(() -> {
			view = new SecretarioView();
			new RevisionMicrocredencialController(view, db).initController();
		});
	}

	@AfterEach
	public void tearDown() throws Exception {
		SwingUtilities.invokeAndWait(() -> view.getFrame().dispose());
	}

	// //////////////////////////////////////////////
	// Listado
	// //////////////////////////////////////////////

	/** Al abrir salen las PROPUESTAS con los datos clave, la más antigua primero */
	@Test
	public void testListadoAlAbrir() {
		JTable tabla = view.getTabPropuestas();
		assertEquals(2, tabla.getRowCount());
		assertEquals("MC002", tabla.getValueAt(0, 0));
		assertEquals("MC001", tabla.getValueAt(1, 0));
		assertEquals("Docker Basico", tabla.getValueAt(1, 1));
		assertEquals("Ana Garcia", tabla.getValueAt(1, 2));
		assertEquals(3.0, tabla.getValueAt(1, 3));
		assertEquals("2027-02-22 - 2027-04-16", tabla.getValueAt(1, 4));
		assertEquals("2 propuestas pendientes de revisión", view.getLblResultadosPropuestas().getText());
	}

	/** "Actualizar listado" recoge los cambios hechos fuera de la pantalla */
	@Test
	public void testActualizarListado() throws Exception {
		db.executeUpdate("UPDATE Microcredencial SET estado = 'ACEPTADA', fechaResolucion = ? WHERE estado = 'PROPUESTA'",
				hoy());
		SwingUtilities.invokeAndWait(() -> view.getBtnRecargarPropuestas().doClick());
		assertEquals(0, view.getTabPropuestas().getRowCount());
		assertEquals("No hay propuestas pendientes de revisión", view.getLblResultadosPropuestas().getText());
	}

	// //////////////////////////////////////////////
	// Aceptar
	// //////////////////////////////////////////////

	/** Aceptar con confirmación: pasa a ACEPTADA y desaparece de la lista */
	@Test
	public void testAceptar() throws Exception {
		seleccionar("MC001");
		List<String> dialogos = pulsarYResponder(view.getBtnAceptarPropuesta(),
				new Paso(null, "Sí"), new Paso(null, "Aceptar"));

		assertEquals(List.of("Aceptar Propuesta", "Propuesta Aceptada"), dialogos);
		assertEquals("ACEPTADA", columna("estado", "MC001"));
		assertEquals(hoy(), columna("fechaResolucion", "MC001"));
		assertEquals(1, view.getTabPropuestas().getRowCount());
		assertEquals("MC002", view.getTabPropuestas().getValueAt(0, 0));
	}

	/** Si no se confirma no cambia nada */
	@Test
	public void testAceptarSinConfirmar() throws Exception {
		seleccionar("MC001");
		List<String> dialogos = pulsarYResponder(view.getBtnAceptarPropuesta(), new Paso(null, "No"));

		assertEquals(List.of("Aceptar Propuesta"), dialogos);
		assertEquals("PROPUESTA", columna("estado", "MC001"));
		assertEquals(2, view.getTabPropuestas().getRowCount());
	}

	/** Sin fila seleccionada se avisa y no cambia nada */
	@Test
	public void testAceptarSinSeleccion() throws Exception {
		List<String> dialogos = pulsarYResponder(view.getBtnAceptarPropuesta(), new Paso(null, "ACEPTAR"));

		assertEquals(List.of("Informacion"), dialogos);
		assertEquals("PROPUESTA", columna("estado", "MC001"));
		assertEquals("PROPUESTA", columna("estado", "MC002"));
	}

	// //////////////////////////////////////////////
	// Rechazar
	// //////////////////////////////////////////////

	/** Rechazar con motivo: pasa a RECHAZADA con el motivo y desaparece de la lista */
	@Test
	public void testRechazar() throws Exception {
		seleccionar("MC001");
		List<String> dialogos = pulsarYResponder(view.getBtnRechazarPropuesta(),
				new Paso("Contenido solapado con otra asignatura", "Aceptar"), new Paso(null, "Aceptar"));

		assertEquals(List.of("Rechazar Propuesta", "Propuesta Rechazada"), dialogos);
		assertEquals("RECHAZADA", columna("estado", "MC001"));
		assertEquals("Contenido solapado con otra asignatura", columna("motivoRechazo", "MC001"));
		assertEquals(hoy(), columna("fechaResolucion", "MC001"));
		assertEquals(1, view.getTabPropuestas().getRowCount());
		assertEquals("1 propuestas pendientes de revisión", view.getLblResultadosPropuestas().getText());
	}

	/** Sin motivo no se rechaza: se avisa y el diálogo vuelve a abrirse hasta que se escribe uno */
	@Test
	public void testRechazarExigeMotivo() throws Exception {
		seleccionar("MC001");
		List<String> dialogos = pulsarYResponder(view.getBtnRechazarPropuesta(),
				new Paso("   ", "Aceptar"), new Paso(null, "Aceptar"),
				new Paso("Objetivos poco definidos", "Aceptar"), new Paso(null, "Aceptar"));

		assertEquals(List.of("Rechazar Propuesta", "Falta el motivo del rechazo", "Rechazar Propuesta",
				"Propuesta Rechazada"), dialogos);
		assertEquals("RECHAZADA", columna("estado", "MC001"));
		assertEquals("Objetivos poco definidos", columna("motivoRechazo", "MC001"));
	}

	/** Lo escrito no se pierde cuando el motivo no es válido (aquí, demasiado largo) */
	@Test
	public void testMotivoNoSePierde() throws Exception {
		seleccionar("MC001");
		String largo = "x".repeat(501);
		List<String> textoAlReabrir = new ArrayList<>();
		List<String> dialogos = pulsarYResponder(view.getBtnRechazarPropuesta(), textoAlReabrir,
				new Paso(largo, "Aceptar"), new Paso(null, "Aceptar"), new Paso(null, "Cancelar"));

		assertEquals(List.of("Rechazar Propuesta", "Falta el motivo del rechazo", "Rechazar Propuesta"), dialogos);
		assertEquals(largo, textoAlReabrir.get(2));
		assertEquals("PROPUESTA", columna("estado", "MC001"));
	}

	/** Cancelar el rechazo no cambia nada */
	@Test
	public void testRechazarCancelado() throws Exception {
		seleccionar("MC001");
		List<String> dialogos = pulsarYResponder(view.getBtnRechazarPropuesta(), new Paso("Da igual", "Cancelar"));

		assertEquals(List.of("Rechazar Propuesta"), dialogos);
		assertEquals("PROPUESTA", columna("estado", "MC001"));
		assertNull(columna("motivoRechazo", "MC001"));
		assertEquals(2, view.getTabPropuestas().getRowCount());
	}

	// //////////////////////////////////////////////
	// Auxiliares
	// //////////////////////////////////////////////

	private void seleccionar(String microcredencialId) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JTable tabla = view.getTabPropuestas();
			for (int i = 0; i < tabla.getRowCount(); i++) {
				if (microcredencialId.equals(tabla.getValueAt(i, 0))) {
					tabla.setRowSelectionInterval(i, i);
				}
			}
		});
	}

	private List<String> pulsarYResponder(JButton boton, Paso... pasos) throws Exception {
		return pulsarYResponder(boton, new ArrayList<>(), pasos);
	}

	/**
	 * Pulsa el botón y responde en orden a los diálogos que se abren.
	 * @param textos recibe el texto del motivo que había en cada diálogo al abrirse (o null si no tiene cuadro)
	 * @return los títulos de los diálogos que se han abierto
	 */
	private List<String> pulsarYResponder(JButton boton, List<String> textos, Paso... pasos) throws Exception {
		List<String> titulos = Collections.synchronizedList(new ArrayList<>());
		Thread respondedor = new Thread(() -> responder(titulos, textos, pasos));
		respondedor.setDaemon(true);
		respondedor.start();
		// doClick vuelve cuando se han cerrado todos los diálogos modales que abre
		SwingUtilities.invokeAndWait(boton::doClick);
		respondedor.join(ESPERA_MAXIMA_MS);
		return new ArrayList<>(titulos);
	}

	private void responder(List<String> titulos, List<String> textos, Paso[] pasos) {
		Set<Window> atendidos = Collections.newSetFromMap(new IdentityHashMap<>());
		long limite = System.currentTimeMillis() + ESPERA_MAXIMA_MS;
		int i = 0;
		while (i < pasos.length && System.currentTimeMillis() < limite) {
			JDialog dialogo = dialogoNuevo(atendidos);
			if (dialogo == null) {
				dormir(100);
				continue;
			}
			atendidos.add(dialogo);
			dormir(150);
			Paso paso = pasos[i++];
			titulos.add(dialogo.getTitle());
			try {
				SwingUtilities.invokeAndWait(() -> {
					JTextArea area = buscar(dialogo.getContentPane(), JTextArea.class, null);
					textos.add(area == null ? null : area.getText());
					if (paso.motivo != null) {
						assertNotNull(area, "El diálogo " + dialogo.getTitle() + " no tiene cuadro para el motivo");
						area.setText(paso.motivo);
					}
					JButton b = buscar(dialogo.getContentPane(), JButton.class, paso.boton);
					assertNotNull(b, "El diálogo " + dialogo.getTitle() + " no tiene el botón " + paso.boton);
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

	/** Primer componente del tipo indicado; si texto no es nulo, el botón con ese texto */
	private static <T extends Component> T buscar(Container contenedor, Class<T> tipo, String texto) {
		for (Component c : contenedor.getComponents()) {
			if (tipo.isInstance(c) && (texto == null || texto.equals(((JButton) c).getText()))) {
				return tipo.cast(c);
			}
			if (c instanceof Container) {
				T encontrado = buscar((Container) c, tipo, texto);
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

	private String columna(String nombre, String microcredencialId) {
		Object valor = db.executeQueryArray("SELECT " + nombre + " FROM Microcredencial WHERE microcredencialId = ?",
				microcredencialId).get(0)[0];
		return valor == null ? null : valor.toString();
	}

	private static String hoy() {
		return LocalDate.now().toString();
	}
}
