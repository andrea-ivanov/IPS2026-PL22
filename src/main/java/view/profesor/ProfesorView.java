package view.profesor;

import javax.swing.*;
import java.awt.*;

public class ProfesorView {
    private JFrame frame;
    private JComboBox<Object> cbProfesores;
    private JButton btnCargarAsignaciones;
    private JTable tabAsignaciones;

    public ProfesorView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Panel de Profesor - Asignaciones Docentes");
        frame.setBounds(150, 150, 650, 400);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        JPanel panelNorte = new JPanel();
        panelNorte.add(new JLabel("Profesor:"));
        cbProfesores = new JComboBox<>();
        panelNorte.add(cbProfesores);

        btnCargarAsignaciones = new JButton("Ver Asignaciones");
        panelNorte.add(btnCargarAsignaciones);
        frame.getContentPane().add(panelNorte, BorderLayout.NORTH);

        tabAsignaciones = new JTable();
        tabAsignaciones.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabAsignaciones.setDefaultEditor(Object.class, null);
        JScrollPane scrollPane = new JScrollPane(tabAsignaciones);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
    }

    public JFrame getFrame() {
        return frame;
    }

    public JComboBox<Object> getCbProfesores() {
        return cbProfesores;
    }

    public JButton getBtnCargarAsignaciones() {
        return btnCargarAsignaciones;
    }

    public JTable getTabAsignaciones() {
        return tabAsignaciones;
    }
}
