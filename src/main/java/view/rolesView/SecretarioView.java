package view.rolesView;

import javax.swing.*;
import java.awt.*;

public class SecretarioView {
    private JFrame frame;

    public SecretarioView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Panel de Secretario");
        frame.setBounds(150, 150, 600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        JLabel lblTitle = new JLabel("Bienvenido, Secretario", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 18));
        frame.getContentPane().add(lblTitle, BorderLayout.CENTER);
    }

    public JFrame getFrame() {
        return frame;
    }
}
