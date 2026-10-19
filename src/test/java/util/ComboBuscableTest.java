package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

/**
 * Pruebas del combo en el que se puede escribir para buscar (profesores y alumnos del login).
 */
class ComboBuscableTest {

	private static JComboBox<Object> combo(Object... opciones) {
		return new JComboBox<>(new DefaultComboBoxModel<>(opciones));
	}

	/** Escribe el texto en el combo como si lo tecleara el usuario (el filtro se hace al soltar la tecla) */
	private static void escribir(JComboBox<Object> combo, String texto) {
		JTextField editor = (JTextField) combo.getEditor().getEditorComponent();
		editor.setText(texto);
		KeyEvent soltar = new KeyEvent(editor, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_A, 'a');
		for (KeyListener l : editor.getKeyListeners()) {
			l.keyReleased(soltar);
		}
	}

	@Test
	void alEscribirSoloQuedanLasOpcionesQueContienenElTexto() {
		JComboBox<Object> cb = combo("UO001 - Alejandro Garcia Lopez", "UO002 - Alejandro Perez Diaz",
				"UO003 - Lucia Rodriguez Garcia");
		ComboBuscable.hacerBuscable(cb);

		escribir(cb, "alejandro");
		assertEquals(2, cb.getModel().getSize());
		// Tambien busca por apellidos y por UO, sin distinguir mayusculas
		escribir(cb, "GARCIA");
		assertEquals(2, cb.getModel().getSize());
		escribir(cb, "uo003");
		assertEquals(1, cb.getModel().getSize());
	}

	@Test
	void laSeleccionEsLaOpcionUnicaQueCoincideONulaSiHayVariasONinguna() {
		JComboBox<Object> cb = combo("UO001 - Alejandro Garcia Lopez", "UO002 - Alejandro Perez Diaz");
		ComboBuscable.hacerBuscable(cb);

		escribir(cb, "alejandro");
		assertNull(ComboBuscable.getSeleccion(cb));
		escribir(cb, "perez");
		assertEquals("UO002 - Alejandro Perez Diaz", ComboBuscable.getSeleccion(cb));
		escribir(cb, "nadie");
		assertNull(ComboBuscable.getSeleccion(cb));
		escribir(cb, "UO001 - Alejandro Garcia Lopez");
		assertEquals("UO001 - Alejandro Garcia Lopez", ComboBuscable.getSeleccion(cb));
	}

	@Test
	void volverAHacerloBuscableActualizaLasOpcionesSinDuplicarElBuscador() {
		JComboBox<Object> cb = combo("UO001 - Alejandro Garcia Lopez");
		ComboBuscable.hacerBuscable(cb);
		JTextField editor = (JTextField) cb.getEditor().getEditorComponent();
		int listeners = editor.getKeyListeners().length;

		// Se recargan las opciones (p. ej. al recargar la base de datos) y se vuelve a llamar
		cb.setModel(new DefaultComboBoxModel<>(new Object[] { "UO005 - Marta Suarez Alvarez",
				"UO006 - Marta Diaz Vega" }));
		ComboBuscable.hacerBuscable(cb);

		assertEquals(listeners, editor.getKeyListeners().length);
		escribir(cb, "marta");
		assertEquals(2, cb.getModel().getSize());
		escribir(cb, "alejandro");
		assertEquals(0, cb.getModel().getSize());
		escribir(cb, "vega");
		assertEquals("UO006 - Marta Diaz Vega", ComboBuscable.getSeleccion(cb));
	}
}
