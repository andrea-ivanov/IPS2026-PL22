package view.login;

import javax.swing.*;
import java.awt.*;

public class MainView {
    private JFrame frame;
    private JComboBox<String> cbRoles;
    private JButton btnAcceder;

    public MainView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Sistema de Matriculación - Login");
        frame.setBounds(100, 100, 450, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 20, 40));

        JPanel panelCentral = new JPanel();
        panelCentral.setLayout(new GridLayout(3, 1, 10, 10));

        panelCentral.add(new JLabel("Selecciona tu rol de acceso:", SwingConstants.CENTER));

        cbRoles = new JComboBox<>(new String[] { "Administrador", "Secretario", "Profesor", "Estudiante" });
        panelCentral.add(cbRoles);

        btnAcceder = new JButton("Acceder");
        panelCentral.add(btnAcceder);

        frame.getContentPane().add(panelCentral);
    }

    public JFrame getFrame() {
        return frame;
    }

    public JComboBox<String> getCbRoles() {
        return cbRoles;
    }

    public JButton getBtnAcceder() {
        return btnAcceder;
    }
}