package controller.estudiante;

import view.estudiante.MisMatriculasView;
import model.matriculacion.MatriculacionModel;
import model.matriculacion.MatriculacionDisplayDTO;

import javax.swing.table.DefaultTableModel;
import java.util.List;

import util.SwingUtil;

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

        // Cargar los grados del estudiante en el desplegable
        cargarGrados();

        // Al cambiar de grado consultado, refrescar la tabla
        view.getCbGrados().addActionListener(e -> cargarMatriculas());

        // Cargar las matrículas del grado seleccionado inicialmente
        cargarMatriculas();

        // Mostrar la ventana
        view.getFrame().setVisible(true);
    }

    private void cargarGrados() {
        List<Object[]> grados = model.getGradosPorEstudiante(uo);
        view.getCbGrados().setModel(SwingUtil.getComboModelFromList(grados));
    }

    private void cargarMatriculas() {
        Object seleccionado = view.getCbGrados().getSelectedItem();
        String gradoId = null;
        if (seleccionado != null) {
            gradoId = seleccionado.toString().split(" - ")[0].trim();
        }

        // Pedimos al modelo las matrículas del estudiante filtradas por el grado consultado
        List<MatriculaDisplayDTO> matriculas = model.getMatriculasPorEstudianteYGrado(uo, gradoId);

        // Obtenemos el modelo de la tabla visual
        DefaultTableModel tableModel = view.getTableModel();

        // Limpiamos la tabla por si hubiera basura de antes
        tableModel.setRowCount(0);

        // Rellenamos fila a fila con el detalle de las asignaturas matriculadas (HU 35232)
        for (MatriculaDisplayDTO m : matriculas) {
            Object[] row = new Object[] {
                    m.getAnioAcademicoId(),
                    m.getAnio() > 0 ? m.getAnio() + "º" : "-",
                    m.getCurso(),
                    m.getNumMatricula(),
                    m.getEstado()
            };
            tableModel.addRow(row);
        }
        SwingUtil.autoAdjustColumns(view.getTableMatriculas());
    }
}
