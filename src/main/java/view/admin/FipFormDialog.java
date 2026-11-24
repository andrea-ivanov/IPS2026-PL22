package view.admin;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.text.ParseException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.text.MaskFormatter;

import model.profesor.ProfesorDTO;
import view.comun.Estilo;

/**
 * Formulario modal "Crear FIP" del administrador (HU 37165): nombre, profesor y fechas de inicio y fin (dd/mm/aa).
 */
public class FipFormDialog {
    private JDialog dialog;
    private JTextField txtNombre;
    private JComboBox<ProfesorDTO> cbProfesor;
    private JFormattedTextField txtFechaInicio;
    private JFormattedTextField txtFechaFin;
    private JButton btnCrear;
    private JButton btnCancelar;

    public FipFormDialog(Frame owner) {
        dialog = new JDialog(owner, "Crear FIP", true);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        formPanel.add(new JLabel("Nombre:"));
        txtNombre = new JTextField(20);
        formPanel.add(txtNombre);

        formPanel.add(new JLabel("Profesor:"));
        cbProfesor = new JComboBox<>();
        formPanel.add(cbProfesor);

        formPanel.add(new JLabel("Fecha Inicio (dd/mm/aa):"));
        txtFechaInicio = createDateField();
        formPanel.add(txtFechaInicio);

        formPanel.add(new JLabel("Fecha Fin (dd/mm/aa):"));
        txtFechaFin = createDateField();
        formPanel.add(txtFechaFin);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        btnCrear = Estilo.botonPrimario("Crear");
        btnCancelar = Estilo.botonSecundario("Cancelar");
        btnCancelar.addActionListener(e -> cerrar());
        btnPanel.add(btnCrear);
        btnPanel.add(btnCancelar);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        Estilo.estilizarFormulario(dialog, formPanel, btnPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
    }

    private JFormattedTextField createDateField() {
        try {
            MaskFormatter dateMask = new MaskFormatter("##/##/##");
            dateMask.setPlaceholderCharacter('_');
            return new JFormattedTextField(dateMask);
        } catch (ParseException e) {
            return new JFormattedTextField();
        }
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

    public JComboBox<ProfesorDTO> getCbProfesor() {
        return cbProfesor;
    }

    public JFormattedTextField getTxtFechaInicio() {
        return txtFechaInicio;
    }

    public JFormattedTextField getTxtFechaFin() {
        return txtFechaFin;
    }

    public JButton getBtnCrear() {
        return btnCrear;
    }
}
