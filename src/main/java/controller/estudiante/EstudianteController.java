package controller.estudiante;

import view.estudiante.EstudianteView;

public class EstudianteController {
    private EstudianteView view;
    private String uo;

    public EstudianteController(EstudianteView view, String uo) {
        this.view = view;
        this.uo = uo;
    }

    public void initController() {
        view.setEstudianteInfo(uo);
        view.getFrame().setVisible(true);
    }
}
