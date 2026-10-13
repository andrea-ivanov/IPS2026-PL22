package controller.estudiante;

import view.estudiante.EstudianteView;

public class EstudianteController {
    private EstudianteView view;
    private String uo;

    public EstudianteController(EstudianteView view) {
        this(view, null);
    }

    public EstudianteController(EstudianteView view, String uo) {
        this.view = view;
        this.uo = uo;
    }

    public void initController() {
        view.setEstudianteInfo(uo);

        // Listener para abrir Mis Matrículas
        view.getBtnMisMatriculas().addActionListener(e -> abrirMisMatriculas());

        view.getFrame().setVisible(true);
    }

    private void abrirMisMatriculas() {
        view.estudiante.MisMatriculasView matriculasView = new view.estudiante.MisMatriculasView();
        model.MatriculaModel matriculaModel = new model.MatriculaModel();
        MisMatriculasController matriculasController = new MisMatriculasController(matriculasView, matriculaModel, uo);
        matriculasController.initController();
    }
}
