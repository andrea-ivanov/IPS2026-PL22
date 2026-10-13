package controller.estudiante;

import view.estudiante.MisMatriculasView;
import model.matriculacion.MatriculacionModel;
import model.matriculacion.MatriculacionDisplayDTO;

import javax.swing.table.DefaultTableModel;
import java.util.List;

public class MisMatriculasController {
    private MisMatriculasView view;
    private MatriculacionModel model;
    private String uo;

    public MisMatriculasController(MisMatriculasView view, MatriculacionModel model, String uo) {
        this.view = view;
        this.model = model;
        this.uo = uo;
    }

    public void initController() {
        // Asignar los listeners a los botones
        view.getBtnVolver().addActionListener(e -> view.getFrame().dispose());

        // Cargar los datos nada más abrir la ventana
        cargarMatriculas();

        // Mostrar la ventana
        view.getFrame().setVisible(true);
    }

    private void cargarMatriculas() {
        // Pedimos al modelo (base de datos) la lista de matrículas de este UO
        List<MatriculacionDisplayDTO> matriculas = model.getMatriculasPorEstudiante(uo);

        // Obtenemos el modelo de la tabla visual
        DefaultTableModel tableModel = view.getTableModel();

        // Limpiamos la tabla por si hubiera basura de antes
        tableModel.setRowCount(0);

        // Rellenamos fila a fila
        for (MatriculacionDisplayDTO m : matriculas) {
            Object[] row = new Object[] {
                    m.getIdMatricula(),
                    m.getGrado(),
                    m.getEstado(),
                    m.getFecha()
            };
            tableModel.addRow(row);
        }
    }
}
