package controller.login;

import view.login.MainView;
import view.admin.AdminOfertaView;
import view.secretario.SecretarioView; // Tu vista de secretario
import view.profesor.ProfesorView;
import java.awt.EventQueue;

import controller.admin.AdminOfertaController;
import controller.secretario.SecretarioController;
import controller.profesor.ProfesorController;

public class MainController {
    private MainView view;

    public MainController(MainView view) {
        this.view = view;
        this.initView();
    }

    public void initController() {
        view.getBtnAcceder().addActionListener(e -> gestionarAcceso());
    }

    private void gestionarAcceso() {
        String rolSeleccionado = (String) view.getCbRoles().getSelectedItem();
        if (rolSeleccionado == null)
            return;

        switch (rolSeleccionado) {
            case "Administrador":
                abrirVistaAdministrador();
                break;
            case "Secretario":
                abrirVistaSecretario();
                break;
            case "Profesor":
                abrirVistaProfesor();
                break;
            case "Estudiante":
                abrirVistaEstudiante();
                break;
        }
    }

    private void abrirVistaAdministrador() {
        AdminOfertaView adminView = new AdminOfertaView();
        AdminOfertaController adminController = new AdminOfertaController(adminView);
        adminController.initController();
        view.getFrame().setVisible(false);
    }

    private void abrirVistaSecretario() {
        SecretarioView secView = new SecretarioView();
        SecretarioController secController = new SecretarioController(secView);
        secController.initController();
        view.getFrame().setVisible(false);
    }

    private void abrirVistaProfesor() {
        ProfesorView profView = new ProfesorView();
        ProfesorController profController = new ProfesorController(profView);
        profController.initController();
        view.getFrame().setVisible(false);
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