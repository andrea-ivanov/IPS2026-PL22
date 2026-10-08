package view;

import javax.swing.*;
import java.awt.*;

public class MainView {
    private JFrame frame;
    private JComboBox<String> cbUsuarios;
    private JButton btnEntrar;

    public MainView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Sistema de Matriculación - Login");
        frame.setBounds(100, 100, 450, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 20, 50));

        JLabel lblUsuario = new JLabel("Usuario:");
        frame.getContentPane().add(lblUsuario);

        // Desplegable (editable para que puedan escribir un UO si no está en la lista)
        String[] usuariosDemo = {"admin", "secretario", "UO123456", "UO987654"};
        cbUsuarios = new JComboBox<>(usuariosDemo);
        cbUsuarios.setEditable(true); // Permite escribir texto libre (como un cuadro de texto)
        cbUsuarios.setPreferredSize(new Dimension(200, 30));
        frame.getContentPane().add(cbUsuarios);

        btnEntrar = new JButton("Entrar");
        frame.getContentPane().add(btnEntrar);
    }

    public JFrame getFrame() { return frame; }
    public JComboBox<String> getCbUsuarios() { return cbUsuarios; }
    public JButton getBtnEntrar() { return btnEntrar; }
}