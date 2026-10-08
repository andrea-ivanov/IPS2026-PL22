package controller;

import view.rolesView.ProfesorView;

public class ProfesorController {
    private ProfesorView view;

    public ProfesorController(ProfesorView view) {
        this.view = view;
    }

    public void initController() {
        view.getFrame().setVisible(true);
    }
}

