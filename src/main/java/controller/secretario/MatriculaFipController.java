package controller.secretario;

import javax.swing.DefaultComboBoxModel;

import model.alumno.AlumnoModel;
import model.profesor.ProfesorDTO;
import model.profesor.ProfesorModel;
import util.ComboBuscable;
import util.Database;
import util.SwingUtil;
import util.exception.ValidationException;
import view.secretario.MatriculaFipPanel;

/**
 * Controlador de la pestaña «Matrícula FIP» de Secretaría (HU 37046).
 * De momento carga los usuarios según el tipo de asistente y comprueba lo que se puede sin la tabla Fip.
 * Pendiente de la tabla Fip e InscripcionFip (las añade otro miembro del equipo): cargar las FIP y sus inscritos,
 * y guardar o anular la inscripción con las reglas de InscripcionFIPModel.
 */
public class MatriculaFipController {
	private final MatriculaFipPanel panel;
	private final AlumnoModel alumnoModel;
	private final ProfesorModel profesorModel;

	public MatriculaFipController(MatriculaFipPanel panel, Database db) {
		this.panel = panel;
		this.alumnoModel = new AlumnoModel(db);
		this.profesorModel = new ProfesorModel(db);
	}

	public void initController() {
		panel.getCbTipo().addActionListener(e -> SwingUtil.exceptionWrapper(this::cargarUsuarios));
		panel.getBtnMatricular().addActionListener(e -> SwingUtil.exceptionWrapper(this::matricular));
		panel.getBtnAnular().addActionListener(e -> SwingUtil.exceptionWrapper(this::anular));
		SwingUtil.exceptionWrapper(this::cargarUsuarios);
		panel.getLblResultados().setText("Todavía no hay FIP: pendiente de la tabla de FIP");
	}

	/** El desplegable de usuarios cambia con el tipo de asistente: alumnos o profesores de la plantilla */
	private void cargarUsuarios() {
		if (MatriculaFipPanel.TIPO_PROFESOR.equals(panel.getCbTipo().getSelectedItem())) {
			panel.getCbUsuario().setModel(new DefaultComboBoxModel<>(profesorModel.getProfesores().toArray(new Object[0])));
		} else {
			panel.getCbUsuario().setModel(SwingUtil.getComboModelFromList(alumnoModel.getListaAlumnos()));
		}
		ComboBuscable.hacerBuscable(panel.getCbUsuario());
	}

	private void matricular() {
		if (panel.getCbFip().getSelectedItem() == null) {
			throw new ValidationException("Seleccione la FIP en la que matricular");
		}
		if (ComboBuscable.getSeleccion(panel.getCbUsuario()) == null) {
			throw new ValidationException("Escriba o seleccione el usuario que se va a matricular");
		}
		// pendiente de la tabla Fip: validar con InscripcionFIPModel.validarMatricula y guardar la inscripción
	}

	private void anular() {
		if (panel.getTabInscritos().getSelectedRow() < 0) {
			throw new ValidationException("Seleccione en la tabla la inscripción que quiere anular");
		}
		// pendiente de la tabla InscripcionFip: comprobar el plazo con InscripcionFIPModel y anular la inscripción
	}

	/** Identificador del usuario elegido: «AL0001 - Nombre» para alumnos y el id del ProfesorDTO para profesores */
	static String usuarioId(Object seleccion) {
		if (seleccion instanceof ProfesorDTO) {
			return ((ProfesorDTO) seleccion).getProfesorId();
		}
		String texto = String.valueOf(seleccion).trim();
		int separador = texto.indexOf(" - ");
		return separador > 0 ? texto.substring(0, separador).trim() : texto;
	}
}
