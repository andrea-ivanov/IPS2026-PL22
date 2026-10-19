package controller.estudiante;

import view.estudiante.MisMatriculasView;
import model.matriculacion.MatriculacionModel;
import model.matriculacion.MatriculacionDisplayDTO;
import model.matriculacion.CalificacionDisplayDTO;
import model.matriculacion.HitosPermanenciaDTO;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.util.List;

import util.SwingUtil;

/**
 * Controlador de la vista de matrículas del estudiante (HU 35232).
 * Gestiona:
 * - Selección de grado (soporte dobles grados).
 * - Ordenación de asignaturas por curso y número de matrícula (Paso 2).
 * - Cálculo y visualización de Hitos de Permanencia anuales y acumulados (Paso
 * 3).
 * - Historial y consulta interactiva de calificaciones en convocatorias
 * presentadas (Paso 4).
 */
public class MisMatriculasController {
    private MisMatriculasView view;
    private MatriculacionModel model;
    private String uo;
    private Runnable onVolver;

    public MisMatriculasController(MisMatriculasView view, MatriculacionModel model, String uo) {
        this(view, model, uo, null);
    }

    public MisMatriculasController(MisMatriculasView view, MatriculacionModel model, String uo, Runnable onVolver) {
        this.view = view;
        this.model = model;
        this.uo = uo;
        this.onVolver = onVolver;
    }

    public void initController() {
        // Asignar los listeners a los botones
        view.getBtnVolver().addActionListener(e -> {
            if (onVolver != null) {
                onVolver.run();
            } else if (view.getFrame() != null) {
                view.getFrame().dispose();
            }
        });

        // Botón para ver notas de la asignatura seleccionada
        view.getBtnVerNotasSeleccionada().addActionListener(e -> verNotasDeAsignaturaSeleccionada());

        // Botón para refrescar todas las notas
        view.getBtnVerTodasNotas().addActionListener(e -> {
            String gradoId = getGradoIdSeleccionado();
            cargarCalificaciones(gradoId);
        });

        // Cargar los grados del estudiante en el desplegable
        cargarGrados();

        // Al cambiar de grado consultado, refrescar la tabla, los hitos y las
        // calificaciones
        view.getCbGrados().addActionListener(e -> refrescarTodo());

        // Carga inicial
        refrescarTodo();

        // Mostrar la ventana solo si funciona en modo independiente
        if (onVolver == null && view.getFrame() != null) {
            view.getFrame().setLocationRelativeTo(null);
            view.getFrame().setVisible(true);
        }
    }

    public void refrescarTodo() {
        String gradoId = getGradoIdSeleccionado();
        cargarMatriculas(gradoId);
        cargarHitosPermanencia(gradoId);
        cargarCalificaciones(gradoId);
    }

    private String getGradoIdSeleccionado() {
        Object seleccionado = view.getCbGrados().getSelectedItem();
        if (seleccionado != null) {
            String str = seleccionado.toString();
            if (str.contains(" - ")) {
                return str.split(" - ")[0].trim();
            }
            return str.trim();
        }
        return null;
    }

    private void cargarGrados() {
        List<Object[]> grados = model.getGradosPorEstudiante(uo);
        view.getCbGrados().setModel(SwingUtil.getComboModelFromList(grados));
    }

    private void cargarMatriculas(String gradoId) {
        // Pedimos al modelo las matrículas del estudiante ordenadas por curso (gc.anio)
        // y numMatricula DESC
        List<MatriculacionDisplayDTO> matriculas = model.getMatriculasPorEstudianteYGrado(uo, gradoId);

        DefaultTableModel tableModel = view.getTableModel();
        tableModel.setRowCount(0);

        for (MatriculacionDisplayDTO m : matriculas) {
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

    private void cargarHitosPermanencia(String gradoId) {
        HitosPermanenciaDTO hitos = model.getHitosPermanencia(uo, gradoId);

        // 1. Hito anual: créditos superados en el último año evaluado vs 12
        String anoInfo = hitos.getUltimoAnoEvaluadoId() != null ? " (" + hitos.getUltimoAnoEvaluadoId() + ")" : "";
        String estadoAnual = hitos.getEstadoHitoAnualTexto();
        String colorAnual = hitos.isCumpleHitoAnual() ? "green" : "#C0392B";
        if ("1º AÑO EN CURSO".equals(estadoAnual)) {
            colorAnual = "#2980B9";
        }
        view.getLblHitoAnual()
                .setText("<html><b>Mínimo anual" + anoInfo + ":</b> " + hitos.getCreditosAnualesSuperados() + " / "
                        + hitos.getCreditosAnualesExigidos() + " créditos superados &nbsp; "
                        + "<font color='" + colorAnual + "'>[" + estadoAnual + "]</font></html>");

        // 2. Hito acumulado: créditos totales en el grado vs 90 al tercer año
        String estadoTotal = hitos.getEstadoHitoTercerAnioTexto();
        String colorTotal = hitos.isCumpleHitoTercerAnio() ? "green" : "#C0392B";
        if (hitos.getAniosEnGrado() < 3) {
            colorTotal = "#2980B9"; // Azul para en progreso
        }
        view.getLblHitoTotal().setText("<html><b>Mínimo 3er año:</b> " + hitos.getCreditosTotalesSuperados() + " / "
                + hitos.getCreditosTotalesExigidos() + " créditos superados (" + hitos.getAniosEnGrado()
                + " años) &nbsp; "
                + "<font color='" + colorTotal + "'>[" + estadoTotal + "]</font></html>");
    }

    private void cargarCalificaciones(String gradoId) {
        List<CalificacionDisplayDTO> calificaciones = model.getCalificacionesPorEstudianteYGrado(uo, gradoId);

        DefaultTableModel modelCalificaciones = view.getTableModelCalificaciones();
        modelCalificaciones.setRowCount(0);

        for (CalificacionDisplayDTO c : calificaciones) {
            Object[] row = new Object[] {
                    c.getAnioAcademicoId(),
                    c.getCursoId(),
                    c.getCurso(),
                    c.getConvocatoriaLegible(),
                    c.getNotaTexto(),
                    c.getResultado()
            };
            modelCalificaciones.addRow(row);
        }
        SwingUtil.autoAdjustColumns(view.getTableCalificaciones());
    }

    private void verNotasDeAsignaturaSeleccionada() {
        int selectedRow = view.getTableMatriculas().getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(view.getFrame(),
                    "Por favor, selecciona una asignatura de la tabla para ver sus calificaciones.",
                    "Asignatura no seleccionada", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String asignatura = (String) view.getTableModel().getValueAt(selectedRow, 2);

        // Cambiar a la pestaña de calificaciones
        view.getTabbedPane().setSelectedIndex(1);

        // Filtrar o resaltar las notas de esa asignatura
        DefaultTableModel modelCalificaciones = view.getTableModelCalificaciones();
        boolean encontrada = false;
        for (int i = 0; i < modelCalificaciones.getRowCount(); i++) {
            String cursoFila = (String) modelCalificaciones.getValueAt(i, 2);
            if (cursoFila != null && cursoFila.trim().equalsIgnoreCase(asignatura.trim())) {
                view.getTableCalificaciones().setRowSelectionInterval(i, i);
                view.getTableCalificaciones()
                        .scrollRectToVisible(view.getTableCalificaciones().getCellRect(i, 0, true));
                encontrada = true;
                break;
            }
        }
        if (!encontrada) {
            JOptionPane.showMessageDialog(view.getFrame(),
                    "No constan calificaciones ni convocatorias registradas para '" + asignatura + "'.",
                    "Sin calificaciones", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
