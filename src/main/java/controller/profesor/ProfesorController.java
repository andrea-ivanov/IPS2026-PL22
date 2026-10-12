package controller.profesor;

import model.ActaDisplayDTO;
import model.AsignacionDocenteDisplayDTO;
import model.CalificacionDisplayDTO;
import repository.ActaRepository;
import repository.ProfesorRepository;
import util.SwingUtil;
import view.profesor.ProfesorView;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class ProfesorController {
    private static final int COLUMNA_NOTA = 4;

    private ProfesorView view;
    private ProfesorRepository repository;
    private ActaRepository actaRepository;
    private String profesorId;
    // asignaciones del profesor seleccionado, sin filtrar: los filtros se aplican sobre esta lista
    private List<AsignacionDocenteDisplayDTO> asignaciones = new ArrayList<>();
    // las que se ven en la tabla, en el mismo orden que sus filas
    private List<AsignacionDocenteDisplayDTO> asignacionesFiltradas = new ArrayList<>();
    // asignacion seleccionada y sus actas, en el mismo orden que el combo
    private AsignacionDocenteDisplayDTO asignacionSelec;
    private List<ActaDisplayDTO> actas = new ArrayList<>();

    public ProfesorController(ProfesorView view) {
        this.view = view;
        this.repository = new ProfesorRepository();
        this.actaRepository = new ActaRepository();
        this.initView();
    }

    public void initController() {
        view.getBtnCargarAsignaciones().addActionListener(e -> SwingUtil.exceptionWrapper(() -> cargarAsignaciones()));
        view.getCbFiltroGrado().addActionListener(e -> aplicarFiltros());
        view.getCbFiltroCurso().addActionListener(e -> aplicarFiltros());
        view.getCbFiltroTipo().addActionListener(e -> aplicarFiltros());
        view.getCbFiltroRol().addActionListener(e -> aplicarFiltros());
        view.getTabAsignaciones().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                SwingUtil.exceptionWrapper(() -> cargarActas());
        });
        view.getCbActas().addActionListener(e -> SwingUtil.exceptionWrapper(() -> cargarNotas()));
        view.getBtnGuardarNotas().addActionListener(e -> SwingUtil.exceptionWrapper(() -> guardarNotas()));
    }

    private void cargarAsignaciones() {
        Object seleccionado = view.getCbProfesores().getSelectedItem();
        if (seleccionado == null)
            return;
        // el item del combo es "P001 - Nombre Apellidos": el id es lo que va antes del separador
        profesorId = seleccionado.toString().split(" - ")[0];

        asignaciones = repository.getAsignacionesDocentes(profesorId);
        cargarCombosFiltro();
        aplicarFiltros();
    }

    /** Rellena los combos de grado y curso solo con los valores de las asignaciones del profesor */
    private void cargarCombosFiltro() {
        TreeSet<String> grados = new TreeSet<>();
        TreeSet<String> cursos = new TreeSet<>();
        for (AsignacionDocenteDisplayDTO a : asignaciones) {
            grados.addAll(getGrados(a));
            cursos.add(a.getCurso());
        }
        List<String> opcionesGrado = new ArrayList<>(List.of("TODOS"));
        opcionesGrado.addAll(grados);
        List<String> opcionesCurso = new ArrayList<>(List.of("TODOS"));
        opcionesCurso.addAll(cursos);
        view.getCbFiltroGrado().setModel(new DefaultComboBoxModel<>(opcionesGrado.toArray(new String[0])));
        view.getCbFiltroCurso().setModel(new DefaultComboBoxModel<>(opcionesCurso.toArray(new String[0])));
    }

    private void aplicarFiltros() {
        String gradoSelec = (String) view.getCbFiltroGrado().getSelectedItem();
        String cursoSelec = (String) view.getCbFiltroCurso().getSelectedItem();
        String tipoSelec = (String) view.getCbFiltroTipo().getSelectedItem();
        String rolSelec = (String) view.getCbFiltroRol().getSelectedItem();

        List<AsignacionDocenteDisplayDTO> filtradas = new ArrayList<>();
        for (AsignacionDocenteDisplayDTO a : asignaciones) {
            boolean coincideGrado = "TODOS".equals(gradoSelec) || getGrados(a).contains(gradoSelec);
            boolean coincideCurso = "TODOS".equals(cursoSelec) || a.getCurso().equals(cursoSelec);
            boolean coincideTipo = "TODOS".equals(tipoSelec) || a.getTipo().equals(tipoSelec);
            boolean coincideRol = "TODOS".equals(rolSelec) || a.getRol().equals(rolSelec);

            if (coincideGrado && coincideCurso && coincideTipo && coincideRol)
                filtradas.add(a);
        }
        asignacionesFiltradas = filtradas;

        TableModel tmodel = SwingUtil.getTableModelFromPojos(filtradas,
                new String[] { "cursoId", "curso", "grado", "grupoCodigo", "tipo", "rol" });
        view.getTabAsignaciones().setModel(tmodel);
        SwingUtil.autoAdjustColumns(view.getTabAsignaciones());
    }

    /** El grado de la asignacion viene con todos los grados de la asignatura separados por comas */
    private List<String> getGrados(AsignacionDocenteDisplayDTO a) {
        if (a.getGrado() == null)
            return new ArrayList<>();
        return Arrays.asList(a.getGrado().split(", "));
    }

    /** Al seleccionar una asignacion se cargan las actas de su asignatura */
    private void cargarActas() {
        int fila = view.getTabAsignaciones().getSelectedRow();
        if (fila < 0 || fila >= asignacionesFiltradas.size()) {
            asignacionSelec = null;
            actas = new ArrayList<>();
        } else {
            asignacionSelec = asignacionesFiltradas.get(fila);
            actas = actaRepository.getActasCurso(asignacionSelec.getCursoId());
        }

        List<String> opciones = new ArrayList<>();
        for (ActaDisplayDTO a : actas)
            opciones.add(a.getAnioAcademicoId() + " - " + a.getTipoConvocatoria() + " (" + a.getEstado() + ")");
        view.getCbActas().setModel(new DefaultComboBoxModel<>(opciones.toArray(new String[0])));
        cargarNotas();
    }

    /**
     * Carga las notas del acta seleccionada. Solo se pueden editar si el profesor es
     * COORDINADOR de la asignatura y el acta esta ABIERTA; el DOCENTE solo las consulta
     */
    private void cargarNotas() {
        ActaDisplayDTO acta = getActaSeleccionada();
        if (acta == null) {
            view.getTabNotas().setModel(new DefaultTableModel());
            view.getLblEstadoActa().setText(asignacionSelec == null ? "Selecciona una asignacion" : "La asignatura no tiene actas");
            view.getBtnGuardarNotas().setEnabled(false);
            return;
        }

        boolean editable = "COORDINADOR".equals(asignacionSelec.getRol()) && "ABIERTA".equals(acta.getEstado());
        List<CalificacionDisplayDTO> notas = actaRepository.getCalificaciones(acta.getCursoId(),
                acta.getAnioAcademicoId(), acta.getTipoConvocatoria());

        DefaultTableModel model = new DefaultTableModel(new String[] { "matriculaId", "alumnoId", "alumno", "dni", "nota" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return editable && column == COLUMNA_NOTA;
            }
        };
        for (CalificacionDisplayDTO c : notas)
            model.addRow(new Object[] { c.getMatriculaId(), c.getAlumnoId(), c.getAlumno(), c.getDni(), c.getNota() });
        view.getTabNotas().setModel(model);
        SwingUtil.autoAdjustColumns(view.getTabNotas());

        if (editable)
            view.getLblEstadoActa().setText("Acta ABIERTA: escribe la nota (0 a 10), NP o dejala vacia");
        else if ("ABIERTA".equals(acta.getEstado()))
            view.getLblEstadoActa().setText("Rol DOCENTE: solo consulta");
        else
            view.getLblEstadoActa().setText("Acta CERRADA: solo lectura");
        view.getBtnGuardarNotas().setEnabled(editable);
    }

    private void guardarNotas() {
        ActaDisplayDTO acta = getActaSeleccionada();
        if (acta == null)
            return;
        // confirma la celda que se este editando para no perder el ultimo valor escrito
        if (view.getTabNotas().isEditing())
            view.getTabNotas().getCellEditor().stopCellEditing();

        Map<String, String> notas = new LinkedHashMap<>();
        TableModel model = view.getTabNotas().getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object nota = model.getValueAt(i, COLUMNA_NOTA);
            notas.put((String) model.getValueAt(i, 0), nota == null ? "" : nota.toString());
        }
        actaRepository.guardarNotas(profesorId, acta.getCursoId(), acta.getAnioAcademicoId(),
                acta.getTipoConvocatoria(), notas);

        JOptionPane.showMessageDialog(view.getFrame(), "Notas guardadas correctamente.", "Exito",
                JOptionPane.INFORMATION_MESSAGE);
        cargarNotas();
    }

    private ActaDisplayDTO getActaSeleccionada() {
        int indice = view.getCbActas().getSelectedIndex();
        if (asignacionSelec == null || indice < 0 || indice >= actas.size())
            return null;
        return actas.get(indice);
    }

    private void initView() {
        view.getCbProfesores().setModel(SwingUtil.getComboModelFromList(repository.getListaProfesores()));
        view.getFrame().setVisible(true);
    }
}
