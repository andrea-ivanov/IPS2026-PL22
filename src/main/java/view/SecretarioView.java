package view;

import javax.swing.*;
import java.awt.*;

public class SecretarioView {
    private JFrame frame;
    private JComboBox<String> cbFiltroEstado;
    private JButton btnCargarMatriculas;
    private JTable tabMatriculas;

    public SecretarioView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Panel de Secretaría - Gestión de Matrículas");
        frame.setBounds(100, 100, 750, 450);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        JPanel panelNorte = new JPanel();
        panelNorte.add(new JLabel("Estado Matrícula:"));
        cbFiltroEstado = new JComboBox<>(new String[]{"TODOS", "SOLICITUD", "PAGADA", "ACTIVA", "FINALIZADA", "CANCELADA"});
        panelNorte.add(cbFiltroEstado);
        
        btnCargarMatriculas = new JButton("Cargar / Filtrar Matrículas");
        panelNorte.add(btnCargarMatriculas);
        
        frame.getContentPane().add(panelNorte, BorderLayout.NORTH);

        tabMatriculas = new JTable();
        tabMatriculas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(tabMatriculas);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
    }

    public JFrame getFrame() { return frame; }
    public JComboBox<String> getCbFiltroEstado() { return cbFiltroEstado; }
    public JButton getBtnCargarMatriculas() { return btnCargarMatriculas; }
    public JTable getTabMatriculas() { return tabMatriculas; }
}