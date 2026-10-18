package controller.estudiante;

import model.matriculacion.MatriculacionModel;
import view.estudiante.EstudianteView;
import view.estudiante.MisMatriculasView;

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
        MisMatriculasView matriculasView = new MisMatriculasView();
        MatriculacionModel matriculacionModel = new MatriculacionModel();
        MisMatriculasController matriculasController = new MisMatriculasController(matriculasView, matriculacionModel, uo);
        matriculasController.initController();
    }
}
