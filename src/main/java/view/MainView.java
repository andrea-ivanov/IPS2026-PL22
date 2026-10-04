package view;

import javax.swing.*;
import java.awt.*;

public class MainView {
    private JFrame frame;
    private JButton btnAdminOferta;

    public MainView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Sistema de Matriculación - Menú Principal");
        frame.setBounds(100, 100, 450, 250);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 20, 50));

        btnAdminOferta = new JButton("Acceder como Administrador (Oferta Académica)");
        frame.getContentPane().add(btnAdminOferta);
        
        // Aqui ir añadiendo los botones para entrar a las otras ventanas, como hice arriba
    }

    public JFrame getFrame() { return frame; }
    public JButton getBtnAdminOferta() { return btnAdminOferta; }
}