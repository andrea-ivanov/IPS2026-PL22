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
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Pestaña «Matrícula FIP» de Secretaría (HU 37046): elegir una FIP, buscar al profesor o alumno y matricularlo,
 * y ver los inscritos. Las matrículas no se pueden anular. Va en su propia clase para no mezclarse con
 * el resto de SecretarioView.
 */
public class MatriculaFipPanel {
	public static final String TIPO_ALUMNO = "Alumno";
	public static final String TIPO_PROFESOR = "Profesor";

	private final JPanel panel;
	private JComboBox<Object> cbFip;
	private JComboBox<String> cbTipo;
	private JComboBox<Object> cbUsuario;
	private JButton btnMatricular;
	private JTable tabInscritos;
	private JLabel lblTitulo;
	private JLabel lblResultados;
	private JLabel ayuda;

	public MatriculaFipPanel() {
		panel = new JPanel(new BorderLayout(12, 0));
		panel.setBackground(FONDO);
		panel.setBorder(new EmptyBorder(12, 2, 2, 2));
		panel.add(envolverFiltros(crearFormulario(), 300), BorderLayout.WEST);
		panel.add(crearInscritos(), BorderLayout.CENTER);
	}

	/** Columna izquierda: FIP, tipo de asistente, usuario y acciones */
	private JPanel crearFormulario() {
		JPanel form = panelFiltros("Matricular en una FIP");
		cbFip = new JComboBox<>();
		addFiltro(form, "FIP", cbFip);
		cbTipo = new JComboBox<>(new String[] { TIPO_ALUMNO, TIPO_PROFESOR });
		addFiltro(form, "Tipo de asistente", cbTipo);
		cbUsuario = new JComboBox<>();
		addFiltro(form, "Usuario (escriba para buscar)", cbUsuario);
		btnMatricular = botonPrimario("Matricular");
		addBoton(form, btnMatricular);

		form.add(Box.createVerticalStrut(12));
		ayuda = textoSecundario("<html><div style='width:200px'>Se matricula hasta el día anterior al inicio de la FIP, "
				+ "mientras queden plazas. Un alumno necesita "
				+ "un contrato vigente en la fecha de inicio. Los formadores externos y el responsable de la FIP no se "
				+ "matriculan. La matrícula es gratuita, no se puede anular y queda PENDIENTE hasta que califique el responsable.</div></html>");
		ayuda.setAlignmentX(Component.LEFT_ALIGNMENT);
		form.add(ayuda);
		return form;
	}

	/** Zona derecha: inscritos en la FIP elegida */
	private JPanel crearInscritos() {
		String[] columnas = { "Inscripción", "Usuario", "Tipo", "Fecha inscripción", "Resultado" };
		tabInscritos = new JTable(new DefaultTableModel(columnas, 0) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		});
		estilizarTabla(tabInscritos);
		tabInscritos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JPanel card = tarjeta(new BorderLayout(0, 10));
		lblTitulo = tituloTarjeta("Inscritos en la FIP");
		card.add(lblTitulo, BorderLayout.NORTH);
		card.add(scrollTabla(tabInscritos), BorderLayout.CENTER);
		lblResultados = textoSecundario("Seleccione una FIP");
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

	public JComboBox<Object> getCbFip() {
		return cbFip;
	}

	public JComboBox<String> getCbTipo() {
		return cbTipo;
	}

	public JComboBox<Object> getCbUsuario() {
		return cbUsuario;
	}

	public JButton getBtnMatricular() {
		return btnMatricular;
	}

	public JTable getTabInscritos() {
		return tabInscritos;
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
