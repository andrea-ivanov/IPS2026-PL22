package controller.estudiante;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import model.contrato.ContratoAlumnoDTO;
import model.fip.MatriculaFipDisplayDTO;
import model.matriculacion.CalificacionDisplayDTO;
import model.matriculacion.HitosPermanenciaDTO;
import model.matriculacion.MatriculacionDisplayDTO;
import model.matriculacion.MatriculacionModel;
import model.microcredencial.InscripcionMicrocredencialModel;
import model.microcredencial.MatriculaMicrocredencialDisplayDTO;
import model.microcredencial.MicrocredencialModel;
import util.Database;
import util.SwingUtil;
import view.estudiante.MisMatriculasView;

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
    private InscripcionMicrocredencialModel microcredencialModel;
    private String uo;
    private Runnable onVolver;

    public MisMatriculasController(MisMatriculasView view, MatriculacionModel model, String uo) {
        this(view, model, null, uo, null);
    }

    public MisMatriculasController(MisMatriculasView view, MatriculacionModel model, String uo, Runnable onVolver) {
        this(view, model, null, uo, onVolver);
    }

    public MisMatriculasController(MisMatriculasView view, MatriculacionModel model,
            InscripcionMicrocredencialModel microcredencialModel, String uo, Runnable onVolver) {
        this.view = view;
        this.model = model;
        this.microcredencialModel = microcredencialModel != null
                ? microcredencialModel
                : (model != null && model.getDb() != null ? new InscripcionMicrocredencialModel(model.getDb()) : null);
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
        view.getBtnVerNotasSeleccionada()
                .addActionListener(e -> SwingUtil.exceptionWrapper(this::verNotasDeAsignaturaSeleccionada));

        // Botón para refrescar todas las notas
        view.getBtnVerTodasNotas().addActionListener(
                e -> SwingUtil.exceptionWrapper(() -> cargarCalificaciones(getGradoIdSeleccionado())));

        // Cargar los grados del estudiante en el desplegable
        cargarGrados();

        // Al cambiar de grado consultado, refrescar la tabla, los hitos y las
        // calificaciones
        view.getCbGrados().addActionListener(e -> SwingUtil.exceptionWrapper(this::refrescarTodo));

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
        cargarMicrocredenciales();
        cargarFipYContratos();
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
        // sin grado en el desplegable el alumno no tiene matrículas: no hay permanencia
        // que calcular
        if (gradoId == null) {
            view.getLblHitoAnual().setText("No tienes ninguna matrícula registrada");
            view.getLblHitoTotal().setText("");
            return;
        }
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

    private void cargarMicrocredenciales() {
        if (view.getTableModelMicrocredenciales() == null || microcredencialModel == null) {
            return;
        }
        DefaultTableModel modelMicro = view.getTableModelMicrocredenciales();
        modelMicro.setRowCount(0);

        String alumnoId = model != null ? model.resolverAlumnoId(uo) : uo;
        List<MatriculaMicrocredencialDisplayDTO> microcredenciales = microcredencialModel.getMatriculasAlumno(alumnoId);

        for (MatriculaMicrocredencialDisplayDTO mc : microcredenciales) {
            Object[] row = new Object[] {
                    mc.getAnioAcademicoId(),
                    mc.getMicrocredencialId(),
                    mc.getNombre(),
                    mc.getRamaConocimiento(),
                    mc.getCreditos() != null ? MicrocredencialModel.formatearCreditos(mc.getCreditos()) : "-",
                    mc.getInicioImparticion(),
                    mc.getFinImparticion(),
                    mc.getEstadoLegible(),
                    mc.getDetalleTexto(),
                    mc.getNotaTexto()
            };
            modelMicro.addRow(row);
        }
        SwingUtil.autoAdjustColumns(view.getTableMicrocredenciales());
    }

    /**
     * Carga los contratos y las Formaciones Internas del Personal (FIP) del
     * estudiante (HU 36967).
     * Si el estudiante no tiene ningún contrato registrado con la universidad, la
     * pestaña "FIP"
     * permanece oculta en el expediente.
     * 
     * TODO: MUESTRA PRUEBA DE ALUMNO CON CONTRATO HASTA QUE SE IMPLEMENTE EL MODELO
     * DE DATOS
     */
    private void cargarFipYContratos() {
        if (view.getTableModelContratos() == null || view.getTableModelFip() == null) {
            return;
        }
        String alumnoId = model != null ? model.resolverAlumnoId(uo) : uo;
        if (alumnoId == null || alumnoId.trim().isEmpty() || model == null || model.getDb() == null) {
            mostrarDatosFip(new ArrayList<>(), new ArrayList<>());
            return;
        }
        Database db = model.getDb();
        String id = alumnoId.trim();
        if (!existeTabla(db, "ContratoAlumno")) {
            // Mientras las tablas de Sprint 3 no estén en schema.sql, mostramos datos de
            // vista previa para AL0001
            if ("AL0001".equalsIgnoreCase(id)) {
                mostrarDatosFip(
                        List.of(
                                new ContratoAlumnoDTO("CT0003", "AL0001", "Apoyo a la docencia", "2027-01-15",
                                        "2027-07-15"),
                                new ContratoAlumnoDTO("CT0001", "AL0001", "Becaria del departamento", "2026-09-01",
                                        "2027-06-30"),
                                new ContratoAlumnoDTO("CT0002", "AL0001", "Colaboracion en biblioteca", "2025-10-01",
                                        "2026-06-30")),
                        List.of(
                                new MatriculaFipDisplayDTO("FIP003", "Prevencion de Riesgos en Laboratorio",
                                        "2026-12-01", "2026-12-10", "PENDIENTE"),
                                new MatriculaFipDisplayDTO("FIP002", "Proteccion de Datos y Ciberseguridad",
                                        "2026-10-05", "2026-10-15", "APTO"),
                                new MatriculaFipDisplayDTO("FIP001", "Gestion Documental Universitaria", "2026-09-10",
                                        "2026-09-20", "NO APTO")));
            } else {
                mostrarDatosFip(new ArrayList<>(), new ArrayList<>());
            }
            return;
        }
        List<ContratoAlumnoDTO> contratos = db.executeQueryPojo(ContratoAlumnoDTO.class,
                "SELECT contratoId, alumnoId, descripcion, fechaInicio, fechaFin FROM ContratoAlumno "
                        + "WHERE alumnoId = ? ORDER BY fechaInicio DESC, contratoId DESC",
                id);
        List<MatriculaFipDisplayDTO> fips = new ArrayList<>();
        if (!contratos.isEmpty() && existeTabla(db, "FIP") && existeTabla(db, "MatriculaFIP")) {
            fips = db.executeQueryPojo(MatriculaFipDisplayDTO.class,
                    "SELECT f.fipId, f.nombre, f.fechaInicio, f.fechaFin, "
                            + "CASE WHEN f.fechaCierreActa IS NULL THEN 'PENDIENTE' "
                            + "     ELSE COALESCE(mf.resultado, 'PENDIENTE') END AS resultado "
                            + "FROM MatriculaFIP mf "
                            + "JOIN FIP f ON f.fipId = mf.fipId "
                            + "WHERE mf.alumnoId = ? "
                            + "ORDER BY f.fechaInicio DESC, f.fipId DESC",
                    id);
        }
        mostrarDatosFip(contratos, fips);
    }

    /**
     * Actualiza la pestaña "FIP" de la vista con los contratos y las FIP del
     * estudiante (HU 36967).
     * Si la lista de contratos está vacía, oculta la pestaña "FIP" ("En ningún
     * caso" debe aparecer
     * para alumnos sin contrato según indicación del PO).
     */
    public void mostrarDatosFip(List<ContratoAlumnoDTO> contratos, List<MatriculaFipDisplayDTO> fips) {
        DefaultTableModel modelContratos = view.getTableModelContratos();
        DefaultTableModel modelFip = view.getTableModelFip();
        if (modelContratos == null || modelFip == null) {
            return;
        }
        modelContratos.setRowCount(0);
        modelFip.setRowCount(0);

        if (contratos == null || contratos.isEmpty()) {
            view.setMostrarPestanaFip(false);
            return;
        }

        view.setMostrarPestanaFip(true);

        for (ContratoAlumnoDTO c : contratos) {
            modelContratos.addRow(new Object[] {
                    c.getContratoId(),
                    c.getDescripcion(),
                    c.getFechaInicio(),
                    c.getFechaFin()
            });
        }
        SwingUtil.autoAdjustColumns(view.getTableContratos());

        if (fips != null) {
            for (MatriculaFipDisplayDTO f : fips) {
                modelFip.addRow(new Object[] {
                        f.getFipId(),
                        f.getNombre(),
                        f.getFechaInicio(),
                        f.getFechaFin(),
                        f.getResultadoTexto()
                });
            }
        }
        SwingUtil.autoAdjustColumns(view.getTableFip());
    }

    private boolean existeTabla(Database db, String nombreTabla) {
        return !db.executeQueryArray(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND LOWER(name) = LOWER(?)",
                nombreTabla).isEmpty();
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
