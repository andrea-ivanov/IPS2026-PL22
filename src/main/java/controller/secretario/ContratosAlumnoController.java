package controller.secretario;

import java.time.LocalDate;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import model.alumno.AlumnoModel;
import model.contrato.ContratoAlumnoDTO;
import model.contrato.ContratoAlumnoModel;
import util.ComboBuscable;
import util.Database;
import util.SwingUtil;
import util.exception.ValidationException;
import view.secretario.ContratosAlumnoPanel;

/**
 * Controlador de la pestaña «Contratos de alumnos» de Secretaría (HU 37045):
 * historial de contratos del alumno elegido y alta de contratos nuevos.
 */
public class ContratosAlumnoController {
	private final ContratosAlumnoPanel panel;
	private final ContratoAlumnoModel model;
	private final AlumnoModel alumnoModel;

	public ContratosAlumnoController(ContratosAlumnoPanel panel, Database db) {
		this.panel = panel;
		this.model = new ContratoAlumnoModel(db);
		this.alumnoModel = new AlumnoModel(db);
	}

	public void initController() {
		panel.getBtnVerContratos().addActionListener(e -> SwingUtil.exceptionWrapper(this::cargarHistorial));
		panel.getBtnRegistrar().addActionListener(e -> SwingUtil.exceptionWrapper(this::registrarContrato));
		SwingUtil.exceptionWrapper(this::cargarAlumnos);
	}

	/** Mismo desplegable buscable que el de la pantalla de inicio: «AL0001 - Nombre Apellidos» */
	private void cargarAlumnos() {
		panel.getCbAlumno().setModel(SwingUtil.getComboModelFromList(alumnoModel.getListaAlumnos()));
		ComboBuscable.hacerBuscable(panel.getCbAlumno());
	}

	private void cargarHistorial() {
		String alumno = alumnoSeleccionado();
		List<ContratoAlumnoDTO> contratos = model.getContratos(alumnoId(alumno));
		DefaultTableModel tabla = (DefaultTableModel) panel.getTabContratos().getModel();
		tabla.setRowCount(0);
		LocalDate hoy = LocalDate.now();
		for (ContratoAlumnoDTO c : contratos) {
			tabla.addRow(new Object[] { c.getContratoId(), c.getDescripcion(), c.getFechaInicio(), c.getFechaFin(),
					situacion(c, hoy) });
		}
		panel.getLblTitulo().setText("Historial de contratos de " + alumno);
		panel.getLblResultados().setText(contratos.isEmpty()
				? "El alumno no tiene ningún contrato registrado"
				: contratos.size() + " contratos registrados");
	}

	private void registrarContrato() {
		String alumno = alumnoSeleccionado();
		String contratoId = model.registrarContrato(alumnoId(alumno), panel.getTxtDescripcion().getText(),
				panel.getTxtFechaInicio().getText(), panel.getTxtFechaFin().getText());
		JOptionPane.showMessageDialog(panel.getPanel(),
				"Contrato " + contratoId + " registrado para " + alumno + ".", "Contrato registrado",
				JOptionPane.INFORMATION_MESSAGE);
		panel.getTxtDescripcion().setText("");
		panel.getTxtFechaInicio().setText("");
		panel.getTxtFechaFin().setText("");
		cargarHistorial();
	}

	/** Vigente, terminado o futuro respecto a hoy (solo informativo) */
	private static String situacion(ContratoAlumnoDTO c, LocalDate hoy) {
		if (hoy.isBefore(LocalDate.parse(c.getFechaInicio()))) {
			return "Futuro";
		}
		return hoy.isAfter(LocalDate.parse(c.getFechaFin())) ? "Terminado" : "Vigente";
	}

	private String alumnoSeleccionado() {
		Object seleccion = ComboBuscable.getSeleccion(panel.getCbAlumno());
		if (seleccion == null) {
			throw new ValidationException("Escriba o seleccione un alumno de la lista");
		}
		return seleccion.toString().trim();
	}

	/** «AL0001 - Lucia Pascual» -> «AL0001» */
	private static String alumnoId(String alumno) {
		int separador = alumno.indexOf(" - ");
		return separador > 0 ? alumno.substring(0, separador).trim() : alumno;
	}
}
