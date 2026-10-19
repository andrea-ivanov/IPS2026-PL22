package util;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/**
 * Convierte un JComboBox en un combo en el que se puede escribir para buscar:
 * mientras se escribe, la lista solo muestra las opciones que contienen el texto (sin distinguir mayúsculas).
 * Uso: rellenar primero el combo y después llamar a ComboBuscable.hacerBuscable(combo).
 * Para leer la opción elegida usar ComboBuscable.getSeleccion(combo), que descarta textos que no son opciones.
 */
public final class ComboBuscable {
	private static final String OPCIONES = "ComboBuscable.opciones";
	private static final String CLAVES = "ComboBuscable.claves";

	private ComboBuscable() {
	}

	/**
	 * Hace el combo editable y filtra sus opciones al escribir; las opciones son las que tiene en este momento.
	 * Si el combo ya era buscable (p. ej. tras volver a cargar sus opciones), solo se actualizan las opciones
	 * de la búsqueda, sin añadir otro buscador.
	 */
	public static <E> void hacerBuscable(JComboBox<E> combo) {
		List<E> opciones = new ArrayList<>();
		// texto de cada opción en minúsculas, calculado una sola vez y no en cada tecla
		List<String> claves = new ArrayList<>();
		ComboBoxModel<E> model = combo.getModel();
		for (int i = 0; i < model.getSize(); i++) {
			E o = model.getElementAt(i);
			opciones.add(o);
			claves.add(o == null ? "" : o.toString().toLowerCase());
		}
		@SuppressWarnings("unchecked")
		List<E> opcionesActuales = (List<E>) combo.getClientProperty(OPCIONES);
		@SuppressWarnings("unchecked")
		List<String> clavesActuales = (List<String>) combo.getClientProperty(CLAVES);
		if (opcionesActuales != null && clavesActuales != null) {
			// el buscador ya está instalado y usa estas mismas listas: basta con cambiar su contenido
			opcionesActuales.clear();
			opcionesActuales.addAll(opciones);
			clavesActuales.clear();
			clavesActuales.addAll(claves);
			return;
		}
		combo.putClientProperty(OPCIONES, opciones);
		combo.putClientProperty(CLAVES, claves);
		combo.setEditable(true);

		JTextField editor = (JTextField) combo.getEditor().getEditorComponent();
		editor.addKeyListener(new KeyAdapter() {
			// última búsqueda: si el texto no cambia (p. ej. al soltar una tecla que no escribe) no se vuelve a filtrar
			private String ultimoTexto = null;

			@Override
			public void keyReleased(KeyEvent e) {
				String texto = editor.getText();
				if (esTeclaDeNavegacion(e.getKeyCode()) || texto.equals(ultimoTexto)) {
					return;
				}
				ultimoTexto = texto;
				filtrar(combo, opciones, claves, texto);
			}
		});
	}

	/**
	 * Opción elegida en el combo: la seleccionada si es una de las opciones, o la única que coincide con el texto escrito.
	 * @return null si el texto no corresponde a ninguna opción (o a más de una)
	 */
	@SuppressWarnings("unchecked")
	public static <E> E getSeleccion(JComboBox<E> combo) {
		Object seleccionado = combo.isEditable() ? combo.getEditor().getItem() : combo.getSelectedItem();
		List<E> opciones = (List<E>) combo.getClientProperty(OPCIONES);
		if (opciones == null || seleccionado == null) {
			return (E) seleccionado;
		}
		String texto = seleccionado.toString().trim();
		List<E> coincidencias = new ArrayList<>();
		for (E o : opciones) {
			if (o.toString().equalsIgnoreCase(texto)) {
				return o;
			}
			if (contiene(o, texto)) {
				coincidencias.add(o);
			}
		}
		return coincidencias.size() == 1 ? coincidencias.get(0) : null;
	}

	private static <E> void filtrar(JComboBox<E> combo, List<E> opciones, List<String> claves, String texto) {
		String buscado = texto.trim().toLowerCase();
		Vector<E> coincidencias = new Vector<>();
		for (int i = 0; i < opciones.size(); i++) {
			if (claves.get(i).contains(buscado)) {
				coincidencias.add(opciones.get(i));
			}
		}
		DefaultComboBoxModel<E> filtrado = new DefaultComboBoxModel<>(coincidencias);
		// se mantiene lo escrito: si no, el combo pondría en el editor la primera opción de la lista filtrada
		filtrado.setSelectedItem(texto);
		combo.setModel(filtrado);
		JTextField editor = (JTextField) combo.getEditor().getEditorComponent();
		editor.setText(texto);
		if (filtrado.getSize() > 0 && combo.isShowing()) {
			combo.hidePopup(); // se cierra y se abre para que la lista ajuste su tamaño a las opciones filtradas
			combo.showPopup();
		} else {
			combo.hidePopup();
		}
	}

	private static boolean contiene(Object opcion, String texto) {
		return opcion != null && opcion.toString().toLowerCase().contains(texto.trim().toLowerCase());
	}

	private static boolean esTeclaDeNavegacion(int keyCode) {
		return keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_LEFT
				|| keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_ESCAPE
				|| keyCode == KeyEvent.VK_TAB || keyCode == KeyEvent.VK_SHIFT || keyCode == KeyEvent.VK_CONTROL
				|| keyCode == KeyEvent.VK_ALT || keyCode == KeyEvent.VK_HOME || keyCode == KeyEvent.VK_END;
	}
}
