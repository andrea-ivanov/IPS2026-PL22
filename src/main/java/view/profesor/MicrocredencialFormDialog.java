package view.profesor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import model.microcredencial.MicrocredencialModel;
import view.comun.Estilo;

/**
 * Formulario modal para proponer una microcredencial (HU 35988).
 * El fin de la matriculación no se escribe: lo rellena el controlador a partir del inicio.
 */
public class MicrocredencialFormDialog {
	private JDialog dialog;
	private JTextField txtNombre;
	private JComboBox<String> cbRama;
	private JTextField txtPlazas;
	private JTextField txtCreditos;
	private JTextField txtInicioMatriculacion;
	private JTextField txtFinMatriculacion;
	private JTextField txtInicioImparticion;
	private JTextField txtFinImparticion;
	private JButton btnGuardar;
	private JButton btnCancelar;

	public MicrocredencialFormDialog(Frame owner) {
		dialog = new JDialog(owner, "Proponer microcredencial", true);
		dialog.setLayout(new BorderLayout());

		JPanel formPanel = new JPanel(new GridLayout(8, 2, 10, 10));
		formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

		formPanel.add(new JLabel("Nombre:"));
		txtNombre = new JTextField(24);
		formPanel.add(txtNombre);

		formPanel.add(new JLabel("Rama de conocimiento:"));
		cbRama = new JComboBox<>(MicrocredencialModel.RAMAS.toArray(new String[0]));
		cbRama.setSelectedIndex(-1);
		formPanel.add(cbRama);

		formPanel.add(new JLabel("Plazas:"));
		txtPlazas = new JTextField(24);
		formPanel.add(txtPlazas);

		formPanel.add(new JLabel("Créditos (máx. "
				+ MicrocredencialModel.formatearCreditos(MicrocredencialModel.MAX_CREDITOS_MICROCREDENCIAL) + "):"));
		txtCreditos = new JTextField(24);
		formPanel.add(txtCreditos);

		formPanel.add(new JLabel("Inicio matriculación (AAAA-MM-DD):"));
		txtInicioMatriculacion = new JTextField(24);
		formPanel.add(txtInicioMatriculacion);

		formPanel.add(new JLabel("Fin matriculación (" + MicrocredencialModel.DIAS_MATRICULACION_MICROCREDENCIAL
				+ " días):"));
		txtFinMatriculacion = new JTextField(24);
		txtFinMatriculacion.setEditable(false);
		txtFinMatriculacion.setFocusable(false);
		formPanel.add(txtFinMatriculacion);

		formPanel.add(new JLabel("Inicio impartición (AAAA-MM-DD):"));
		txtInicioImparticion = new JTextField(24);
		formPanel.add(txtInicioImparticion);

		formPanel.add(new JLabel("Fin impartición (AAAA-MM-DD):"));
		txtFinImparticion = new JTextField(24);
		formPanel.add(txtFinImparticion);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
		btnGuardar = Estilo.botonPrimario("Guardar");
		btnCancelar = Estilo.botonSecundario("Cancelar");
		btnCancelar.addActionListener(e -> cerrar());
		btnPanel.add(btnGuardar);
		btnPanel.add(btnCancelar);

		dialog.add(formPanel, BorderLayout.CENTER);
		dialog.add(btnPanel, BorderLayout.SOUTH);
		Estilo.estilizarFormulario(dialog, formPanel, btnPanel);
		dialog.pack();
		dialog.setLocationRelativeTo(owner);
	}

	public void mostrar() {
		dialog.setVisible(true);
	}

	public void cerrar() {
		dialog.dispose();
	}

	public JDialog getDialog() {
		return dialog;
	}

	public JTextField getTxtNombre() {
		return txtNombre;
	}

	public JComboBox<String> getCbRama() {
		return cbRama;
	}

	public JTextField getTxtPlazas() {
		return txtPlazas;
	}

	public JTextField getTxtCreditos() {
		return txtCreditos;
	}

	public JTextField getTxtInicioMatriculacion() {
		return txtInicioMatriculacion;
	}

	public JTextField getTxtFinMatriculacion() {
		return txtFinMatriculacion;
	}

	public JTextField getTxtInicioImparticion() {
		return txtInicioImparticion;
	}

	public JTextField getTxtFinImparticion() {
		return txtFinImparticion;
	}

	public JButton getBtnGuardar() {
		return btnGuardar;
	}
}
