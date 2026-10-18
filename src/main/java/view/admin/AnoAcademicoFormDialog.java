package view.admin;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.text.ParseException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.MaskFormatter;

/**
 * Formulario modal para configurar un nuevo año académico y su periodo de matriculación (HU 35223).
 */
public class AnoAcademicoFormDialog {
    private JDialog dialog;
    private JFormattedTextField txtInicioAnio;
    private JFormattedTextField txtFinAnio;
    private JFormattedTextField txtInicioMatriculacion;
    private JFormattedTextField txtFinMatriculacion;
    private JButton btnGuardar;
    private JButton btnCancelar;

    public AnoAcademicoFormDialog(Frame owner) {
        dialog = new JDialog(owner, "Configurar Nuevo Año Académico", true);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        formPanel.add(new JLabel("Inicio año académico (dd/mm/aa):"));
        txtInicioAnio = createDateField();
        formPanel.add(txtInicioAnio);

        formPanel.add(new JLabel("Fin año académico (dd/mm/aa):"));
        txtFinAnio = createDateField();
        formPanel.add(txtFinAnio);

        formPanel.add(new JLabel("Inicio matriculación (dd/mm/aa):"));
        txtInicioMatriculacion = createDateField();
        formPanel.add(txtInicioMatriculacion);

        formPanel.add(new JLabel("Fin matriculación (dd/mm/aa):"));
        txtFinMatriculacion = createDateField();
        formPanel.add(txtFinMatriculacion);

        JPanel btnPanel = new JPanel();
        btnGuardar = new JButton("Guardar");
        btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> cerrar());
        btnPanel.add(btnGuardar);
        btnPanel.add(btnCancelar);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
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

    public JFormattedTextField getTxtInicioAnio() {
        return txtInicioAnio;
    }

    public JFormattedTextField getTxtFinAnio() {
        return txtFinAnio;
    }

    public JFormattedTextField getTxtInicioMatriculacion() {
        return txtInicioMatriculacion;
    }

    public JFormattedTextField getTxtFinMatriculacion() {
        return txtFinMatriculacion;
    }

    public JButton getBtnGuardar() {
        return btnGuardar;
    }
}
