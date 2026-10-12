package view.estudiante;

import javax.swing.*;
import java.awt.*;

public class EstudianteView {
    private JFrame frame;
    private JLabel lblTitle;
    private JButton btnMisMatriculas;

    public EstudianteView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Panel de Estudiante");
        frame.setBounds(150, 150, 600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        lblTitle = new JLabel("Bienvenido, Estudiante", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 18));
        frame.getContentPane().add(lblTitle, BorderLayout.NORTH);

        JPanel panelBotones = new JPanel();
        panelBotones.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 50));

        btnMisMatriculas = new JButton("Consultar Mis Matrículas");
        panelBotones.add(btnMisMatriculas);

        frame.getContentPane().add(panelBotones, BorderLayout.CENTER);
    }

    public JButton getBtnMisMatriculas() {
        return btnMisMatriculas;
    }

    public JFrame getFrame() {
        return frame;
    }

    public void setEstudianteInfo(String uo) {
        if (uo != null && !uo.trim().isEmpty()) {
            lblTitle.setText("Bienvenido, Estudiante (" + uo + ")");
        } else {
            lblTitle.setText("Bienvenido, Estudiante");
        }
    }
}
