package view.rolesView;

import javax.swing.*;
import java.awt.*;

public class EstudianteView {
    private JFrame frame;
    private JLabel lblTitle;

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
        frame.getContentPane().add(lblTitle, BorderLayout.CENTER);
    }

    public JFrame getFrame() {
        return frame;
    }

    public void setEstudianteInfo(String uo) {
        lblTitle.setText("Bienvenido, Estudiante (" + uo + ")");
    }
}
