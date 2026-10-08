package view.rolesView;

import javax.swing.*;
import java.awt.*;

public class AdminOfertaView {
    private JFrame frame;
    private JComboBox<String> cbEstadoGrado;
    private JButton btnCargarMocks;
    private JTable tabGrados;

    public AdminOfertaView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Panel de Administrador - Oferta Académica");
        frame.setBounds(100, 100, 600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        JPanel panelNorte = new JPanel();
        panelNorte.add(new JLabel("Estado:"));
        cbEstadoGrado = new JComboBox<>(new String[] { "TODOS", "VIGENTE", "EN_EXTINCION", "EXTINTO" });
        panelNorte.add(cbEstadoGrado);

        btnCargarMocks = new JButton("Cargar Datos (Prueba)");
        panelNorte.add(btnCargarMocks);
        frame.getContentPane().add(panelNorte, BorderLayout.NORTH);

        tabGrados = new JTable();
        tabGrados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(tabGrados);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
    }

    public JFrame getFrame() {
        return frame;
    }

    public JComboBox<String> getCbEstadoGrado() {
        return cbEstadoGrado;
    }

    public JButton getBtnCargarMocks() {
        return btnCargarMocks;
    }

    public JTable getTabGrados() {
        return tabGrados;
    }
}