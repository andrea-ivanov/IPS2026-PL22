package controller.secretario;

import java.time.LocalDate;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
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
import util.SwingUtil;
import util.exception.ValidationException;
import view.secretario.MatriculaFipPanel;

/**
 * Controlador de la pestaña «Matrícula FIP» de Secretaría (HU 37046): elegir la FIP, ver sus inscritos,
 * matricular a un alumno o profesor y anular una inscripción. Las reglas están en InscripcionFIPModel.
 */
public class MatriculaFipController {
	private static final int COLUMNA_INSCRIPCION = 0;

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
		panel.getBtnAnular().addActionListener(e -> SwingUtil.exceptionWrapper(this::anular));
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
		boolean abierta = LocalDate.parse(hoy()).isBefore(LocalDate.parse(fip.getFechaInicio()));
		panel.getLblResultados().setText(inscritos.size() + " inscritos · "
				+ (abierta ? "matrícula abierta hasta el " + LocalDate.parse(fip.getFechaInicio()).minusDays(1)
						: "la FIP ya ha empezado: no admite altas ni anulaciones"));
	}

	private void matricular() {
		FIPDTO fip = (FIPDTO) panel.getCbFip().getSelectedItem();
		if (fip == null) {
			throw new ValidationException("Seleccione la FIP en la que matricular");
		}
		Object usuario = ComboBuscable.getSeleccion(panel.getCbUsuario());
		if (usuario == null) {
			throw new ValidationException("Escriba o seleccione el usuario que se va a matricular");
		}
		String inscripcionId = model.matricular(fip.getFipId(), usuarioId(usuario), tipoSeleccionado(), hoy());
		JOptionPane.showMessageDialog(panel.getPanel(),
				usuario + " matriculado en " + fip.getNombre() + " (" + inscripcionId + ").\n"
						+ "La matrícula es gratuita y queda PENDIENTE hasta que califique el responsable.",
				"Matrícula registrada", JOptionPane.INFORMATION_MESSAGE);
		cargarFipsManteniendo(fip.getFipId());
	}

	/** Pide confirmación: la inscripción anulada desaparece de la FIP */
	private void anular() {
		JTable tabla = panel.getTabInscritos();
		int fila = tabla.getSelectedRow();
		if (fila < 0) {
			throw new ValidationException("Seleccione en la tabla la inscripción que quiere anular");
		}
		int filaModelo = tabla.convertRowIndexToModel(fila);
		String inscripcionId = String.valueOf(tabla.getModel().getValueAt(filaModelo, COLUMNA_INSCRIPCION));
		String usuario = String.valueOf(tabla.getModel().getValueAt(filaModelo, 1));
		int confirm = JOptionPane.showConfirmDialog(panel.getPanel(),
				"¿Desea anular la inscripción " + inscripcionId + " de " + usuario + "?", "Anular inscripción",
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}
		model.anularInscripcion(inscripcionId, hoy());
		JOptionPane.showMessageDialog(panel.getPanel(), "Inscripción " + inscripcionId + " anulada.",
				"Inscripción anulada", JOptionPane.INFORMATION_MESSAGE);
		FIPDTO fip = (FIPDTO) panel.getCbFip().getSelectedItem();
		cargarFipsManteniendo(fip == null ? null : fip.getFipId());
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
		return LocalDate.now().toString();
	}
}
