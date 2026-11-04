package view.admin;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import view.comun.Estilo;

/**
 * Formulario modal para añadir o modificar un grado (HU 35223).
 */
public class GradoFormDialog {
    private JDialog dialog;
    private JTextField txtNombre;
    private JTextField txtFacultad;
    private JTextField txtRama;
    private JButton btnGuardar;
    private JButton btnCancelar;

    public GradoFormDialog(Frame owner) {
        this(owner, "Añadir grado");
    }

    public GradoFormDialog(Frame owner, String titulo) {
        dialog = new JDialog(owner, titulo, true);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        formPanel.add(new JLabel("Nombre:"));
        txtNombre = new JTextField(20);
        formPanel.add(txtNombre);

        formPanel.add(new JLabel("Facultad:"));
        txtFacultad = new JTextField(20);
        formPanel.add(txtFacultad);

        formPanel.add(new JLabel("Rama de conocimiento:"));
        // TODO: cambiar por un JComboBox con las 5 ramas de ck_grado_rama (schema.sql); con texto libre
        // cualquier errata o tilde ("Ingeniería") la rechaza la BD con un error tecnico
        txtRama = new JTextField(20);
        formPanel.add(txtRama);

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

    public JTextField getTxtNombre() {
        return txtNombre;
    }

    public JTextField getTxtFacultad() {
        return txtFacultad;
    }

    public JTextField getTxtRama() {
        return txtRama;
    }

    public JButton getBtnGuardar() {
        return btnGuardar;
    }
}
