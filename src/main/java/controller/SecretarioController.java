package controller;

import view.rolesView.SecretarioView;

public class SecretarioController {
    private SecretarioView view;

    public SecretarioController(SecretarioView view) {
        this.view = view;
    }

    public void initController() {
        view.getFrame().setVisible(true);
    }
}
