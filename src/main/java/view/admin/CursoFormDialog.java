package view.admin;

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

import view.comun.Estilo;

/**
 * Formulario modal para añadir o modificar un curso de un grado (HU 35223).
 */
public class CursoFormDialog {
    private JDialog dialog;
    private JTextField txtNombre;
    private JComboBox<String> cbCoordinador;
    private JTextField txtItinerario;
    private JTextField txtCreditos;
    private JTextField txtAnio;
    private JLabel lblAvisoCreditos;
    private JButton btnGuardar;
    private JButton btnCancelar;

    public CursoFormDialog(Frame owner) {
        this(owner, "Añadir curso");
    }

    public CursoFormDialog(Frame owner, String titulo) {
        dialog = new JDialog(owner, titulo, true);
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

        lblAvisoCreditos = new JLabel("Este curso está en varios grados: los créditos cambiarán en todos ellos.");
        lblAvisoCreditos.setBorder(new EmptyBorder(8, 4, 0, 4));
        lblAvisoCreditos.setFont(Estilo.ETIQUETA);
        lblAvisoCreditos.setForeground(Estilo.AVISO);
        lblAvisoCreditos.setVisible(false);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        btnGuardar = Estilo.botonPrimario("Guardar");
        btnCancelar = Estilo.botonSecundario("Cancelar");
        btnCancelar.addActionListener(e -> cerrar());
        btnPanel.add(btnGuardar);
        btnPanel.add(btnCancelar);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(lblAvisoCreditos, BorderLayout.NORTH);
        southPanel.add(btnPanel, BorderLayout.CENTER);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(southPanel, BorderLayout.SOUTH);
        Estilo.estilizarFormulario(dialog, formPanel, btnPanel, southPanel);
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

    /** Deja visibles pero no editables el nombre y el coordinador. */
    public void deshabilitarNoEditables() {
        txtNombre.setEnabled(false);
        cbCoordinador.setEnabled(false);
    }

    public void mostrarAvisoCreditos() {
        lblAvisoCreditos.setVisible(true);
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
