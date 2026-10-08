package controller;

import view.MainView;
import view.rolesView.AdminOfertaView;
import view.rolesView.EstudianteView;
import view.rolesView.SecretarioView;

import javax.swing.JOptionPane;
import java.awt.EventQueue;

public class MainController {
    private MainView view;

    public MainController(MainView view) {
        this.view = view;
        this.initView();
    }

    public void initController() {
        view.getBtnEntrar().addActionListener(e -> hacerLogin());
    }

    private void hacerLogin() {
        // Obtener el texto del JComboBox (ya sea seleccionado o escrito)
        String usuario = (String) view.getCbUsuarios().getSelectedItem();

        if (usuario == null || usuario.trim().isEmpty()) {
            JOptionPane.showMessageDialog(view.getFrame(), "Por favor, introduce un usuario.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        usuario = usuario.trim();

        if (usuario.equalsIgnoreCase("admin")) {
            abrirVistaAdministrador();
        } else if (usuario.equalsIgnoreCase("secretario")) {
            abrirVistaSecretario();
        } else if (usuario.toUpperCase().startsWith("UO")) {
            abrirVistaEstudiante(usuario.toUpperCase());
        } else {
            JOptionPane.showMessageDialog(view.getFrame(),
                    "Usuario no reconocido. Usa 'admin', 'secretario', o 'UO...'", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirVistaAdministrador() {
        AdminOfertaView adminView = new AdminOfertaView();
        AdminOfertaController adminController = new AdminOfertaController(adminView);
        adminController.initController();
        view.getFrame().dispose(); // Cerramos el login
    }

    private void abrirVistaSecretario() {
        SecretarioView secView = new SecretarioView();
        SecretarioController secController = new SecretarioController(secView);
        secController.initController();
        view.getFrame().dispose(); // Cerramos el login
    }

    private void abrirVistaEstudiante(String uo) {
        EstudianteView estView = new EstudianteView();
        EstudianteController estController = new EstudianteController(estView, uo);
        estController.initController();
        view.getFrame().dispose(); // Cerramos el login
    }

    private void initView() {
        view.getFrame().setVisible(true);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainView vistaPrincipal = new MainView();
                    MainController controladorPrincipal = new MainController(vistaPrincipal);
                    controladorPrincipal.initController();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ;
            }
        });
    }
}