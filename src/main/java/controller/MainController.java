package controller;

import view.MainView;
import view.AdminOfertaView;
import java.awt.EventQueue;

public class MainController {
    private MainView view;

    public MainController(MainView view) {
        this.view = view;
        this.initView();
    }

    public void initController() {
        view.getBtnAdminOferta().addActionListener(e -> abrirVistaAdministrador());
    }

    private void abrirVistaAdministrador() {
        AdminOfertaView adminView = new AdminOfertaView();
        AdminOfertaController adminController = new AdminOfertaController(adminView);
        
        adminController.initController();
        
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
            }
        });
    }
}