package view.login;

import javax.swing.*;
import java.awt.*;

public class MainView {
    private JFrame frame;
    private JComboBox<String> cbRoles;
    private JPanel panelAlumno;
    private JComboBox<Object> cbAlumnos;
    private JButton btnAcceder;
    private JButton btnRecargarBD;

    public MainView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Sistema de Matriculación - Login");
        frame.setBounds(100, 100, 480, 260);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(10, 10));

        JPanel panelCentral = new JPanel();
        panelCentral.setLayout(new BoxLayout(panelCentral, BoxLayout.Y_AXIS));
        panelCentral.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Panel selección de rol
        JPanel panelRol = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panelRol.add(new JLabel("Rol de acceso:"));
        cbRoles = new JComboBox<>(new String[] { "Administrador", "Secretario", "Profesor", "Estudiante" });
        panelRol.add(cbRoles);
        panelCentral.add(panelRol);

        // Panel selección de alumno (visible cuando se selecciona Estudiante)
        panelAlumno = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panelAlumno.add(new JLabel("Alumno:"));
        cbAlumnos = new JComboBox<>();
        cbAlumnos.setPreferredSize(new Dimension(280, 26));
        panelAlumno.add(cbAlumnos);
        panelAlumno.setVisible(false);
        panelCentral.add(panelAlumno);

        // Panel botón acceder
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnAcceder = new JButton("Acceder");
        panelBoton.add(btnAcceder);
        btnRecargarBD = new JButton("Recargar base de datos");
        btnRecargarBD.setToolTipText("Borra la base de datos y la vuelve a cargarla desde el archivo SQL");
        panelBoton.add(btnRecargarBD);
        panelCentral.add(panelBoton);

        frame.getContentPane().add(panelCentral, BorderLayout.CENTER);
    }

    public JFrame getFrame() {
        return frame;
    }

    public JComboBox<String> getCbRoles() {
        return cbRoles;
    }

    public JPanel getPanelAlumno() {
        return panelAlumno;
    }

    public JComboBox<Object> getCbAlumnos() {
        return cbAlumnos;
    }

    public JButton getBtnAcceder() {
        return btnAcceder;
    }

    public JButton getBtnRecargarBD() {
        return btnRecargarBD;
    }
}