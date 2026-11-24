package view.secretario;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Auxiliar de las pruebas de vista de las HU 37045 y 37046: pulsa un botón y responde a los diálogos que se abren
 * como lo haría el secretario, anotando su título y su mensaje.
 */
final class DialogosPrueba {
	private static final long ESPERA_MAXIMA_MS = 15000;

	private DialogosPrueba() {
	}

	/** Igual que MainController, para que los botones no dependan del idioma del sistema */
	static void botonesEnEspanol() {
		UIManager.put("OptionPane.yesButtonText", "Sí");
		UIManager.put("OptionPane.noButtonText", "No");
		UIManager.put("OptionPane.okButtonText", "Aceptar");
		UIManager.put("OptionPane.cancelButtonText", "Cancelar");
	}

	/**
	 * Pulsa el botón y, en cada diálogo que se abre, pulsa por orden el botón indicado.
	 * @return «título: mensaje» de cada diálogo
	 */
	static List<String> pulsarYResponder(JButton boton, String... respuestas) throws Exception {
		List<String> dialogos = Collections.synchronizedList(new ArrayList<>());
		Thread respondedor = new Thread(() -> responder(dialogos, respuestas));
		respondedor.setDaemon(true);
		respondedor.start();
		// doClick vuelve cuando se han cerrado todos los diálogos modales que abre
		SwingUtilities.invokeAndWait(boton::doClick);
		respondedor.join(ESPERA_MAXIMA_MS);
		return new ArrayList<>(dialogos);
	}

	/** Textos de los botones con texto que hay en el contenedor */
	static List<String> textosBotones(Container contenedor) {
		List<String> textos = new ArrayList<>();
		for (Component c : contenedor.getComponents()) {
			if (c instanceof JButton && !((JButton) c).getText().isEmpty()) {
				textos.add(((JButton) c).getText());
			} else if (c instanceof Container) {
				textos.addAll(textosBotones((Container) c));
			}
		}
		return textos;
	}

	private static void responder(List<String> dialogos, String[] respuestas) {
		Set<Window> atendidos = Collections.newSetFromMap(new IdentityHashMap<>());
		long limite = System.currentTimeMillis() + ESPERA_MAXIMA_MS;
		int i = 0;
		while (i < respuestas.length && System.currentTimeMillis() < limite) {
			JDialog dialogo = dialogoNuevo(atendidos);
			if (dialogo == null) {
				dormir(100);
				continue;
			}
			atendidos.add(dialogo);
			dormir(150);
			String respuesta = respuestas[i++];
			JOptionPane pane = buscar(dialogo.getContentPane(), JOptionPane.class, null);
			dialogos.add(dialogo.getTitle() + ": " + (pane == null ? "" : pane.getMessage()));
			try {
				SwingUtilities.invokeAndWait(() -> {
					JButton b = buscar(dialogo.getContentPane(), JButton.class, respuesta);
					if (b == null) {
						dialogos.add("No hay botón " + respuesta);
						dialogo.dispose(); // para que la prueba no se quede bloqueada en el diálogo
					} else {
						b.doClick();
					}
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
}
