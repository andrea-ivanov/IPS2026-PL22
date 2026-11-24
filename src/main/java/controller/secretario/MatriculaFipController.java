package controller.secretario;

import java.time.LocalDate;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import model.alumno.AlumnoModel;
import model.fip.FIPDTO;
import model.fip.InscripcionFIPModel;
import model.fip.InscritoFipDTO;
import model.fip.TipoUsuarioFIP;
import model.profesor.ProfesorDTO;
import model.profesor.ProfesorModel;
import util.ComboBuscable;
import util.Database;
import util.RelojSistema;
import util.SwingUtil;
import util.exception.ValidationException;
import view.secretario.MatriculaFipPanel;

/**
 * Controlador de la pestaña «Matrícula FIP» de Secretaría (HU 37046): elegir la FIP, ver sus inscritos,
 * y matricular a un alumno o profesor. Las reglas están en InscripcionFIPModel.
 */
public class MatriculaFipController {
	private final MatriculaFipPanel panel;
	private final InscripcionFIPModel model;
	private final AlumnoModel alumnoModel;
	private final ProfesorModel profesorModel;

	public MatriculaFipController(MatriculaFipPanel panel, Database db) {
		this.panel = panel;
		this.model = new InscripcionFIPModel(db);
		this.alumnoModel = new AlumnoModel(db);
		this.profesorModel = new ProfesorModel(db);
	}

	public void initController() {
		panel.getCbTipo().addActionListener(e -> SwingUtil.exceptionWrapper(this::cargarUsuarios));
		panel.getCbFip().addActionListener(e -> SwingUtil.exceptionWrapper(this::cargarInscritos));
		panel.getBtnMatricular().addActionListener(e -> SwingUtil.exceptionWrapper(this::matricular));
		SwingUtil.exceptionWrapper(() -> {
			cargarUsuarios();
			cargarFips();
		});
	}

	/** Todas las FIP; queda elegida la primera (la que empieza más tarde) */
	private void cargarFips() {
		List<FIPDTO> fips = model.getFips();
		panel.getCbFip().setModel(new DefaultComboBoxModel<>(fips.toArray(new Object[0])));
		if (fips.isEmpty()) {
			panel.getLblResultados().setText("No hay ninguna FIP creada");
		}
		cargarInscritos();
	}

	/** El desplegable de usuarios cambia con el tipo de asistente: alumnos o profesores de la plantilla */
	private void cargarUsuarios() {
		if (tipoSeleccionado() == TipoUsuarioFIP.PROFESOR) {
			panel.getCbUsuario().setModel(new DefaultComboBoxModel<>(profesorModel.getProfesores().toArray(new Object[0])));
		} else {
			panel.getCbUsuario().setModel(SwingUtil.getComboModelFromList(alumnoModel.getListaAlumnos()));
		}
		ComboBuscable.hacerBuscable(panel.getCbUsuario());
	}

	private void cargarInscritos() {
		FIPDTO fip = (FIPDTO) panel.getCbFip().getSelectedItem();
		DefaultTableModel tabla = (DefaultTableModel) panel.getTabInscritos().getModel();
		tabla.setRowCount(0);
		if (fip == null) {
			return;
		}
		List<InscritoFipDTO> inscritos = model.getInscritos(fip.getFipId());
		for (InscritoFipDTO i : inscritos) {
			tabla.addRow(new Object[] { i.getInscripcionId(), i.getUsuarioId() + " - " + i.getUsuario(),
					"ALUMNO".equals(i.getTipo()) ? "Alumno" : "Profesor", i.getFechaInscripcion(),
					"NO_APTO".equals(i.getResultado()) ? "NO APTO" : i.getResultado() });
		}
		panel.getLblTitulo().setText("Inscritos en " + fip.getFipId() + " - " + fip.getNombre());
		panel.getLblResultados().setText(inscritos.size() + (inscritos.size() == 1 ? " inscrito" : " inscritos")
				+ (fip.getPlazasMaximas() > 0 ? " de " + fip.getPlazasMaximas() + " plazas" : "") + " · " + plazo(fip));
	}

	/** En qué punto está la matrícula: se abre al crear la FIP y se cierra el día anterior a su inicio */
	private static String plazo(FIPDTO fip) {
		LocalDate hoy = LocalDate.parse(hoy());
		LocalDate inicio = LocalDate.parse(fip.getFechaInicio());
		if (!hoy.isBefore(inicio)) {
			return "la FIP ya ha empezado: no admite altas";
		}
		if (hoy.isBefore(LocalDate.parse(fip.getFechaCreacion()))) {
			return "la matrícula se abre el " + fip.getFechaCreacion();
		}
		if (!fip.hayPlazasDisponibles()) {
			return "la FIP está completa";
		}
		return "matrícula abierta hasta el " + inicio.minusDays(1);
	}

	/** Pide confirmación, porque la matrícula no se puede anular */
	private void matricular() {
		FIPDTO fip = (FIPDTO) panel.getCbFip().getSelectedItem();
		if (fip == null) {
			throw new ValidationException("Seleccione la FIP en la que matricular");
		}
		Object usuario = ComboBuscable.getSeleccion(panel.getCbUsuario());
		if (usuario == null) {
			throw new ValidationException("Escriba o seleccione el usuario que se va a matricular");
		}
		int confirm = JOptionPane.showConfirmDialog(panel.getPanel(),
				"¿Desea matricular a " + usuario + " en " + fip.getNombre() + "?\nLa matrícula no se puede anular.",
				"Matricular en FIP", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}
		String inscripcionId = model.matricular(fip.getFipId(), usuarioId(usuario), tipoSeleccionado(), hoy());
		JOptionPane.showMessageDialog(panel.getPanel(),
				usuario + " matriculado en " + fip.getNombre() + " (" + inscripcionId + ").\n"
						+ "La matrícula es gratuita y queda PENDIENTE hasta que califique el responsable.",
				"Matrícula registrada", JOptionPane.INFORMATION_MESSAGE);
		cargarFipsManteniendo(fip.getFipId());
	}

	/** Recarga las FIP (cambian las plazas ocupadas) sin perder la FIP elegida */
	private void cargarFipsManteniendo(String fipId) {
		cargarFips();
		for (int i = 0; i < panel.getCbFip().getItemCount(); i++) {
			if (((FIPDTO) panel.getCbFip().getItemAt(i)).getFipId().equals(fipId)) {
				panel.getCbFip().setSelectedIndex(i);
			}
		}
	}

	private TipoUsuarioFIP tipoSeleccionado() {
		return MatriculaFipPanel.TIPO_PROFESOR.equals(panel.getCbTipo().getSelectedItem()) ? TipoUsuarioFIP.PROFESOR
				: TipoUsuarioFIP.ALUMNO;
	}

	/** Identificador del usuario elegido: el id del ProfesorDTO, o el id real del alumno a partir de «UO0001 - Nombre» */
	private String usuarioId(Object seleccion) {
		if (seleccion instanceof ProfesorDTO) {
			return ((ProfesorDTO) seleccion).getProfesorId();
		}
		return alumnoModel.resolverAlumnoId(String.valueOf(seleccion));
	}

	private static String hoy() {
		return RelojSistema.getInstance().hoyIso();
	}
}
