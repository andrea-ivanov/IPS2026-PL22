package view.secretario;

import static view.comun.Estilo.*;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Pestaña «Contratos de alumnos» de Secretaría (HU 37045): buscar un alumno, ver el historial de sus contratos
 * y registrar uno nuevo. Va en su propia clase para no mezclarse con el resto de SecretarioView.
 */
public class ContratosAlumnoPanel {
	public static final int COLUMNA_ID = 0;

	private final JPanel panel;
	private JComboBox<Object> cbAlumno;
	private JButton btnVerContratos;
	private JTextField txtDescripcion;
	private JTextField txtFechaInicio;
	private JTextField txtFechaFin;
	private JButton btnRegistrar;
	private JTable tabContratos;
	private JLabel lblTitulo;
	private JLabel lblResultados;
	private JLabel ayuda;

	public ContratosAlumnoPanel() {
		panel = new JPanel(new BorderLayout(12, 0));
		panel.setBackground(FONDO);
		panel.setBorder(new EmptyBorder(12, 2, 2, 2));
		panel.add(envolverFiltros(crearFormulario(), 300), BorderLayout.WEST);
		panel.add(crearHistorial(), BorderLayout.CENTER);
	}

	/** Columna izquierda: elegir el alumno y dar de alta un contrato */
	private JPanel crearFormulario() {
		JPanel form = panelFiltros("Alumno");
		cbAlumno = new JComboBox<>();
		addFiltro(form, "Alumno (escriba para buscar)", cbAlumno);
		btnVerContratos = botonSecundario("Ver contratos");
		addBoton(form, btnVerContratos);

		form.add(Box.createVerticalStrut(18));
		JLabel tituloNuevo = tituloTarjeta("Nuevo contrato");
		tituloNuevo.setAlignmentX(Component.LEFT_ALIGNMENT);
		form.add(tituloNuevo);
		form.add(Box.createVerticalStrut(10));
		txtDescripcion = new JTextField();
		addFiltro(form, "Descripción (ej. «soy becario»)", txtDescripcion);
		txtFechaInicio = new JTextField();
		addFiltro(form, "Fecha de inicio (AAAA-MM-DD)", txtFechaInicio);
		txtFechaFin = new JTextField();
		addFiltro(form, "Fecha de fin (AAAA-MM-DD)", txtFechaFin);
		btnRegistrar = botonPrimario("Registrar contrato");
		addBoton(form, btnRegistrar);

		form.add(Box.createVerticalStrut(12));
		ayuda = textoSecundario("<html><div style='width:200px'>Un alumno puede tener varios contratos, aunque se solapen. "
				+ "Se admiten contratos pasados y futuros. Los contratos no se modifican ni se borran.</div></html>");
		ayuda.setAlignmentX(Component.LEFT_ALIGNMENT);
		form.add(ayuda);
		return form;
	}

	/** Zona derecha: historial de contratos del alumno elegido */
	private JPanel crearHistorial() {
		String[] columnas = { "Código", "Descripción", "Fecha inicio", "Fecha fin", "Situación" };
		tabContratos = new JTable(new DefaultTableModel(columnas, 0) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		});
		estilizarTabla(tabContratos);
		tabContratos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JPanel card = tarjeta(new BorderLayout(0, 10));
		lblTitulo = tituloTarjeta("Historial de contratos");
		card.add(lblTitulo, BorderLayout.NORTH);
		card.add(scrollTabla(tabContratos), BorderLayout.CENTER);
		lblResultados = textoSecundario("Seleccione un alumno y pulse «Ver contratos»");
		card.add(lblResultados, BorderLayout.SOUTH);
		return card;
	}

	private static void addBoton(JPanel form, JButton boton) {
		boton.setAlignmentX(Component.LEFT_ALIGNMENT);
		boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
		form.add(boton);
		form.add(Box.createVerticalStrut(8));
	}

	public JPanel getPanel() {
		return panel;
	}

	public JComboBox<Object> getCbAlumno() {
		return cbAlumno;
	}

	public JButton getBtnVerContratos() {
		return btnVerContratos;
	}

	public JTextField getTxtDescripcion() {
		return txtDescripcion;
	}

	public JTextField getTxtFechaInicio() {
		return txtFechaInicio;
	}

	public JTextField getTxtFechaFin() {
		return txtFechaFin;
	}

	public JButton getBtnRegistrar() {
		return btnRegistrar;
	}

	public JTable getTabContratos() {
		return tabContratos;
	}

	public JLabel getLblTitulo() {
		return lblTitulo;
	}

	public JLabel getAyuda() {
		return ayuda;
	}

	public JLabel getLblResultados() {
		return lblResultados;
	}
}
