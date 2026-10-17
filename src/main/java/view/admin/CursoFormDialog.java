package view.admin;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * Formulario modal para añadir un curso a un grado (HU 35223).
 */
public class CursoFormDialog {
    private JDialog dialog;
    private JTextField txtNombre;
    private JComboBox<String> cbCoordinador;
    private JTextField txtItinerario;
    private JTextField txtCreditos;
    private JTextField txtAnio;
    private JButton btnGuardar;
    private JButton btnCancelar;

    public CursoFormDialog(Frame owner) {
        dialog = new JDialog(owner, "Añadir curso", true);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        formPanel.add(new JLabel("Nombre:"));
        txtNombre = new JTextField(20);
        formPanel.add(txtNombre);

        formPanel.add(new JLabel("Coordinador:"));
        cbCoordinador = new JComboBox<>();
        formPanel.add(cbCoordinador);

        formPanel.add(new JLabel("Itinerario:"));
        txtItinerario = new JTextField(20);
        formPanel.add(txtItinerario);

        formPanel.add(new JLabel("Créditos:"));
        txtCreditos = new JTextField(20);
        formPanel.add(txtCreditos);

        formPanel.add(new JLabel("Año (1-6):"));
        txtAnio = new JTextField(20);
        formPanel.add(txtAnio);

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

    public void mostrar() {
        dialog.pack();
        dialog.setVisible(true);
    }

    public void cerrar() {
        dialog.dispose();
    }

    public JTextField getTxtNombre() {
        return txtNombre;
    }

    public JComboBox<String> getCbCoordinador() {
        return cbCoordinador;
    }

    public JTextField getTxtItinerario() {
        return txtItinerario;
    }

    public JTextField getTxtCreditos() {
        return txtCreditos;
    }

    public JTextField getTxtAnio() {
        return txtAnio;
    }

    public JButton getBtnGuardar() {
        return btnGuardar;
    }
}
