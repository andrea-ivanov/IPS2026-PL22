package controller.profesor;

import util.FechaSistema;
import model.acta.ActaDisplayDTO;
import model.acta.ActaModel;
import model.propuesta.PropuestaModel;
import model.acta.CalificacionActaDisplayDTO;
import model.acta.EstadoActa;
import model.asignaciondocente.AsignacionDocenteDisplayDTO;
import model.grado.GradoDisplayDTO;
import model.microcredencial.ActaMicrocredencialModel;
import model.microcredencial.CalificacionMicrocredencialDTO;
import model.microcredencial.MicrocredencialDisplayDTO;
import model.microcredencial.MicrocredencialModel;
import model.anoacademico.AnoAcademicoModel;
import model.profesor.ProfesorDTO;
import model.profesor.ProfesorModel;
import model.propuesta.PropuestaDisplayDTO;
import util.Database;
import util.SwingUtil;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;
import view.comun.Estilo;
import view.profesor.MicrocredencialFormDialog;
import view.profesor.ProfesorView;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class ProfesorController {
    private static final int COLUMNA_NOTA = 4;

    private ProfesorView view;
    private ProfesorModel profesorModel;
    private ActaModel actaModel;
    private PropuestaModel propuestaModel;
    private MicrocredencialModel microcredencialModel;
    private ActaMicrocredencialModel actaMicrocredencialModel;
    private AnoAcademicoModel anoAcademicoModel;
    // profesor que ha entrado desde la pantalla de inicio
    private ProfesorDTO profesor;
    private String profesorId;
    // asignaciones del profesor seleccionado, sin filtrar: los filtros se aplican sobre esta lista
    private List<AsignacionDocenteDisplayDTO> asignaciones = new ArrayList<>();
    // las que se ven en la tabla, en el mismo orden que sus filas
    private List<AsignacionDocenteDisplayDTO> asignacionesFiltradas = new ArrayList<>();
    // asignacion seleccionada y sus actas, en el mismo orden que el combo
    private AsignacionDocenteDisplayDTO asignacionSelec;
    private List<ActaDisplayDTO> actas = new ArrayList<>();
    // microcredenciales del profesor, en el mismo orden que las filas de su tabla (HU 36459)
    private List<MicrocredencialDisplayDTO> microcredenciales = new ArrayList<>();
    // Accion al pulsar "Volver al inicio" (mostrar la pantalla de seleccion de rol)
    private Runnable onVolverInicio = () -> { };

    public ProfesorController(ProfesorView view, Database db, ProfesorDTO profesor) {
        this.view = view;
        this.profesor = profesor;
        this.profesorId = profesor.getProfesorId();
        this.profesorModel = new ProfesorModel(db);
        this.actaModel = new ActaModel(db);
        this.propuestaModel = new PropuestaModel(db);
        this.microcredencialModel = new MicrocredencialModel(db);
        this.actaMicrocredencialModel = new ActaMicrocredencialModel(db);
        this.anoAcademicoModel = new AnoAcademicoModel(db);
        this.initView();
    }

    public void setOnVolverInicio(Runnable onVolverInicio) {
        this.onVolverInicio = onVolverInicio;
    }

    public void initController() {
        view.getBtnVolverInicio().addActionListener(e -> view.getFrame().dispose());
        // tanto el boton como la X de la ventana la cierran: al cerrarse se vuelve a la seleccion de rol
        view.getFrame().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                onVolverInicio.run();
            }
        });
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
        view.getBtnCerrarActa().addActionListener(e -> SwingUtil.exceptionWrapper(() -> cerrarActa()));
        view.getBtnEnviarPropuesta().addActionListener(e -> SwingUtil.exceptionWrapper(() -> enviarPropuesta()));
        view.getBtnProponerMicrocredencial()
                .addActionListener(e -> SwingUtil.exceptionWrapper(() -> proponerMicrocredencial()));
        view.getTabMicrocredenciales().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                SwingUtil.exceptionWrapper(() -> cargarActaMicrocredencial());
        });
        view.getBtnGuardarNotasMicrocredencial()
                .addActionListener(e -> SwingUtil.exceptionWrapper(() -> guardarNotasMicrocredencial()));
        view.getBtnCerrarActaMicrocredencial()
                .addActionListener(e -> SwingUtil.exceptionWrapper(() -> cerrarActaMicrocredencial()));
        // el profesor ya viene elegido de la pantalla de inicio: se cargan sus pestañas al abrir
        SwingUtil.exceptionWrapper(() -> cargarAsignaciones());
    }

    /** Carga las tres pestañas (asignaciones, propuestas y microcredenciales) del profesor */
    private void cargarAsignaciones() {
        asignaciones = profesorModel.getAsignacionesDocentes(profesorId);
        cargarCombosFiltro();
        aplicarFiltros();
        cargarPropuestas();
        cargarMicrocredenciales();
    }

    /** Rellena los combos de grado y curso solo con los valores de las asignaciones del profesor */
    private void cargarCombosFiltro() {
        TreeSet<String> grados = new TreeSet<>();
        TreeSet<String> cursos = new TreeSet<>();
        for (AsignacionDocenteDisplayDTO a : asignaciones) {
            grados.addAll(getGrados(a));
            cursos.add(a.getCurso());
        }
        List<String> opcionesGrado = new ArrayList<>(List.of(ProfesorView.FILTRO_TODOS));
        opcionesGrado.addAll(grados);
        List<String> opcionesCurso = new ArrayList<>(List.of(ProfesorView.FILTRO_TODOS));
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
            boolean coincideGrado = ProfesorView.FILTRO_TODOS.equals(gradoSelec) || getGrados(a).contains(gradoSelec);
            boolean coincideCurso = ProfesorView.FILTRO_TODOS.equals(cursoSelec) || a.getCurso().equals(cursoSelec);
            boolean coincideTipo = ProfesorView.FILTRO_TODOS.equals(tipoSelec) || a.getTipo().equals(tipoSelec);
            boolean coincideRol = ProfesorView.FILTRO_TODOS.equals(rolSelec) || a.getRol().equals(rolSelec);

            if (coincideGrado && coincideCurso && coincideTipo && coincideRol)
                filtradas.add(a);
        }
        asignacionesFiltradas = filtradas;

        TableModel tmodel = SwingUtil.getTableModelFromPojos(filtradas,
                new String[] { "cursoId", "curso", "grado", "grupoCodigo", "tipo", "rol" });
        ((DefaultTableModel) tmodel).setColumnIdentifiers(
                new String[] { "Cód. Curso", "Curso", "Grados", "Grupo", "Tipo", "Rol" });
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
            actas = actaModel.getActasCurso(asignacionSelec.getCursoId());
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
            view.getBtnCerrarActa().setEnabled(false);
            return;
        }

        boolean editable = ActaModel.puedeEditarNotas(asignacionSelec.getRolDocente(), acta.getEstadoActa());
        List<CalificacionActaDisplayDTO> notas = actaModel.getCalificaciones(acta.getCursoId(),
                acta.getAnioAcademicoId(), acta.getTipoConvocatoria());

        DefaultTableModel model = new DefaultTableModel(new String[] { "Matrícula", "Cód. Alumno", "Alumno", "DNI", "Nota" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return editable && column == COLUMNA_NOTA;
            }
        };
        for (CalificacionActaDisplayDTO c : notas)
            model.addRow(new Object[] { c.getMatriculaId(), c.getAlumnoId(), c.getAlumno(), c.getDni(), c.getNota() });
        view.getTabNotas().setModel(model);
        SwingUtil.autoAdjustColumns(view.getTabNotas());

        if (editable)
            view.getLblEstadoActa().setText("Acta ABIERTA: escribe la nota (0 a 10), NP o dejala vacia");
        else if (acta.getEstadoActa() == EstadoActa.ABIERTA)
            view.getLblEstadoActa().setText("Rol DOCENTE: solo consulta");
        else
            view.getLblEstadoActa().setText("Acta CERRADA: solo lectura");
        if (notas.isEmpty())
            view.getLblEstadoActa().setText(view.getLblEstadoActa().getText()
                    + " · Ningún alumno en esta convocatoria: todos aprobaron en una anterior o no hay matriculados");
        view.getBtnGuardarNotas().setEnabled(editable);
        view.getBtnCerrarActa().setEnabled(editable);
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
        actaModel.guardarNotas(profesorId, acta.getCursoId(), acta.getAnioAcademicoId(),
                acta.getTipoConvocatoria(), notas);

        JOptionPane.showMessageDialog(view.getFrame(), "Notas guardadas correctamente.", "Exito",
                JOptionPane.INFORMATION_MESSAGE);
        cargarNotas();
    }

    private void cerrarActa() {
        ActaDisplayDTO acta = getActaSeleccionada();
        if (acta == null)
            return;
        int confirm = JOptionPane.showConfirmDialog(view.getFrame(),
                "¿Estás seguro de que deseas cerrar el acta de " + asignacionSelec.getCurso() + " ("
                        + acta.getAnioAcademicoId() + " - " + acta.getTipoConvocatoria() + ")?\n"
                        + "Los alumnos sin nota quedarán como NP (no presentado).\n"
                        + "Una vez cerrada, las notas no se podrán modificar.",
                "Cerrar Acta", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            actaModel.cerrarActa(profesorId, acta.getCursoId(), acta.getAnioAcademicoId(),
                    acta.getTipoConvocatoria(), FechaSistema.hoyStr());

            JOptionPane.showMessageDialog(view.getFrame(), "Acta cerrada correctamente.", "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
            int indice = view.getCbActas().getSelectedIndex();
            cargarActas(); // recarga las actas para que el combo muestre el nuevo estado
            view.getCbActas().setSelectedIndex(indice);
        }
    }

    // =========================================================================
    // HU 35243: propuestas de asignaturas
    // =========================================================================

    /** Grados en los que imparte el profesor (con su estado) y su listado "Mis propuestas" */
    private void cargarPropuestas() {
        List<GradoDisplayDTO> grados = propuestaModel.getGradosProfesor(profesorId);
        view.getListGradosPropuesta().setListData(grados.toArray(new GradoDisplayDTO[0]));

        List<PropuestaDisplayDTO> propuestas = propuestaModel.getPropuestas(profesorId);
        view.getLblProfesorPropuesta().setText("Propuestas de " + profesor
                + (propuestas.isEmpty() ? ": todavía no ha enviado ninguna" : ""));
        TableModel tmodel = SwingUtil.getTableModelFromPojos(propuestas,
                new String[] { "propuestaId", "grado", "nombre", "anio", "creditos", "fechaPropuesta", "estado" });
        ((DefaultTableModel) tmodel).setColumnIdentifiers(
                new String[] { "Propuesta", "Grado", "Nombre", "Año", "Créditos", "Fecha", "Estado" });
        view.getTabPropuestas().setModel(tmodel);
        SwingUtil.autoAdjustColumns(view.getTabPropuestas());
        view.getBtnEnviarPropuesta().setEnabled(true);
    }

    /**
     * Se validan los datos antes de pedir confirmación, para avisar de los errores sin preguntar;
     * al confirmar se crea una propuesta PENDIENTE por grado elegido (o ninguna si alguna falla)
     */
    private void enviarPropuesta() {
        List<GradoDisplayDTO> gradosSelec = view.getListGradosPropuesta().getSelectedValuesList();
        List<String> gradoIds = new ArrayList<>();
        for (GradoDisplayDTO g : gradosSelec)
            gradoIds.add(g.getId());
        String nombre = view.getTxtNombrePropuesta().getText();
        String anio = view.getTxtAnioPropuesta().getText();
        String creditos = view.getTxtCreditosPropuesta().getText();
        String descripcion = view.getTxtDescripcionPropuesta().getText();

        propuestaModel.validarPropuestas(profesorId, gradoIds, nombre, anio, creditos, descripcion);

        StringBuilder grados = new StringBuilder();
        for (GradoDisplayDTO g : gradosSelec)
            grados.append("\n- ").append(g.getNombre());
        int confirm = JOptionPane.showConfirmDialog(view.getFrame(),
                "¿Quieres enviar la propuesta \"" + nombre.trim() + "\" (año " + anio.trim() + ", " + creditos.trim()
                        + " créditos)?\nSe creará una propuesta PENDIENTE por cada grado:" + grados,
                "Enviar propuesta", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        List<String> creadas = propuestaModel.crearPropuestas(profesorId, gradoIds, nombre, anio, creditos,
                descripcion, FechaSistema.hoyStr());
        JOptionPane.showMessageDialog(view.getFrame(),
                "Propuesta enviada correctamente: " + String.join(", ", creadas) + " (PENDIENTE).", "Éxito",
                JOptionPane.INFORMATION_MESSAGE);
        limpiarFormularioPropuesta();
        cargarPropuestas();
    }

    private void limpiarFormularioPropuesta() {
        view.getTxtNombrePropuesta().setText("");
        view.getTxtAnioPropuesta().setText("");
        view.getTxtCreditosPropuesta().setText("");
        view.getTxtDescripcionPropuesta().setText("");
        view.getListGradosPropuesta().clearSelection();
    }

    // =========================================================================
    // HU 35988: propuestas de microcredenciales
    // =========================================================================

    /** Listado "Mis microcredenciales" del profesor cargado, con su estado (HU 35988 y HU 36410) */
    private void cargarMicrocredenciales() {
        microcredenciales = microcredencialModel.getMicrocredenciales(profesorId);
        view.getLblProfesorMicrocredencial().setText("Microcredenciales de " + profesor
                + (microcredenciales.isEmpty() ? ": todavía no ha propuesto ninguna" : ""));
        TableModel tmodel = SwingUtil.getTableModelFromPojos(microcredenciales,
                new String[] { "microcredencialId", "anioAcademicoId", "nombre", "ramaConocimiento", "creditos", "plazas",
                        "inicioMatriculacion", "finMatriculacion", "inicioImparticion", "finImparticion",
                        "fechaPropuesta", "estadoLegible", "detalleEstado" });
        ((DefaultTableModel) tmodel).setColumnIdentifiers(
                new String[] { "Código", "Año", "Nombre", "Rama", "Créditos", "Plazas", "Inicio matrícula",
                        "Fin matrícula", "Inicio impartición", "Fin impartición", "Propuesta", "Estado",
                        "Detalle / Resolución" });
        view.getTabMicrocredenciales().setModel(tmodel);
        view.getTabMicrocredenciales().getColumnModel().getColumn(11).setCellRenderer(new Estilo.RendererEstado());
        SwingUtil.autoAdjustColumns(view.getTabMicrocredenciales());
        view.getBtnProponerMicrocredencial().setEnabled(true);
        cargarActaMicrocredencial();
    }

    /**
     * Abre el formulario; al guardar se valida todo y se crea como PROPUESTA en el año académico actual.
     * Si algo no es válido se muestra el error y el formulario sigue abierto
     */
    private void proponerMicrocredencial() {
        String anioAcademicoId;
        try {
            anioAcademicoId = anoAcademicoModel.buscarActual().getAnioAcademicoId();
        } catch (ResourceNotFoundException e) {
            throw new ValidationException("No hay ningún año académico configurado");
        }
        MicrocredencialFormDialog form = new MicrocredencialFormDialog(view.getFrame());
        // el fin de la matriculacion se recalcula al escribir el inicio
        form.getTxtInicioMatriculacion().getDocument().addDocumentListener(new DocumentListener() {
            private void actualizar() {
                form.getTxtFinMatriculacion().setText(
                        MicrocredencialModel.calcularFinMatriculacion(form.getTxtInicioMatriculacion().getText()));
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                actualizar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                actualizar();
            }
        });
        form.getBtnGuardar().addActionListener(e -> SwingUtil.exceptionWrapper(() -> {
            String nombre = form.getTxtNombre().getText();
            String id = microcredencialModel.crearMicrocredencial(profesorId, anioAcademicoId, nombre,
                    (String) form.getCbRama().getSelectedItem(), form.getTxtPlazas().getText(),
                    form.getTxtCreditos().getText(), form.getTxtInicioMatriculacion().getText(),
                    form.getTxtInicioImparticion().getText(), form.getTxtFinImparticion().getText(),
                    FechaSistema.hoyStr());
            form.cerrar();
            JOptionPane.showMessageDialog(view.getFrame(), "Microcredencial " + nombre.trim() + " propuesta correctamente ("
                    + id + ", PROPUESTA).", "Proponer microcredencial", JOptionPane.INFORMATION_MESSAGE);
            cargarMicrocredenciales();
        }));
        form.mostrar();
    }

    // =========================================================================
    // HU 36459: acta de la microcredencial seleccionada
    // =========================================================================

    /** La microcredencial seleccionada en "Mis microcredenciales", o null */
    private MicrocredencialDisplayDTO getMicrocredencialSeleccionada() {
        int fila = view.getTabMicrocredenciales().getSelectedRow();
        return fila < 0 || fila >= microcredenciales.size() ? null : microcredenciales.get(fila);
    }

    /**
     * Carga el acta de la microcredencial seleccionada: solo los alumnos con la inscripción pagada. Las notas se
     * pueden editar desde el inicio de la impartición hasta cerrar el acta, y el acta se puede cerrar desde el fin
     */
    private void cargarActaMicrocredencial() {
        MicrocredencialDisplayDTO mc = getMicrocredencialSeleccionada();
        view.getBtnGuardarNotasMicrocredencial().setEnabled(false);
        view.getBtnCerrarActaMicrocredencial().setEnabled(false);
        if (mc == null || !tieneActa(mc)) {
            view.getTabNotasMicrocredencial().setModel(new DefaultTableModel());
            view.getLblEstadoActaMicrocredencial().setText(mc == null ? "Selecciona una microcredencial"
                    : "Esta microcredencial no tiene acta (" + mc.getEstadoLegible() + ")");
            return;
        }

        String hoy = FechaSistema.hoyStr();
        boolean editable = ActaMicrocredencialModel.puedeEditarNotas(mc, hoy);
        boolean cerrable = ActaMicrocredencialModel.puedeCerrarActa(mc, hoy);
        List<CalificacionMicrocredencialDTO> notas = actaMicrocredencialModel.getCalificaciones(profesorId,
                mc.getMicrocredencialId());

        DefaultTableModel model = new DefaultTableModel(
                new String[] { "Matrícula", "Cód. Alumno", "Alumno", "DNI", "Nota" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return editable && column == COLUMNA_NOTA;
            }
        };
        for (CalificacionMicrocredencialDTO c : notas)
            model.addRow(new Object[] { c.getMatriculaId(), c.getAlumnoId(), c.getAlumno(), c.getDni(),
                    c.getNotaTexto() });
        view.getTabNotasMicrocredencial().setModel(model);
        SwingUtil.autoAdjustColumns(view.getTabNotasMicrocredencial());

        String estado;
        if (!ActaMicrocredencialModel.actaAbierta(mc))
            estado = "Acta CERRADA el " + mc.getFechaCierreActa() + ": solo lectura"
                    + (Integer.valueOf(1).equals(mc.getCierreAutomatico()) ? " (cerrada por el sistema)" : "");
        else if (hoy.compareTo(ActaMicrocredencialModel.finPlazoCierre(mc)) > 0)
            estado = "Acta ABIERTA: el plazo para cerrarla terminó el "
                    + ActaMicrocredencialModel.finPlazoCierre(mc) + " y la cerrará el sistema";
        else if (!editable)
            estado = "La microcredencial empieza el " + mc.getInicioImparticion()
                    + ": todavía no se pueden poner notas";
        else if (!cerrable)
            estado = "Acta ABIERTA: escribe la nota (0 a 10), NP o déjala vacía · Se podrá cerrar a partir del "
                    + mc.getFinImparticion();
        else
            estado = "Acta ABIERTA: escribe la nota (0 a 10), NP o déjala vacía · Puedes cerrarla hasta el "
                    + ActaMicrocredencialModel.finPlazoCierre(mc);
        if (notas.isEmpty())
            estado += " · Ningún alumno con la inscripción pagada";
        view.getLblEstadoActaMicrocredencial().setText(estado);
        view.getBtnGuardarNotasMicrocredencial().setEnabled(editable);
        view.getBtnCerrarActaMicrocredencial().setEnabled(cerrable);
    }

    /** Solo las microcredenciales aceptadas (acta abierta) o finalizadas (acta cerrada) tienen acta */
    private static boolean tieneActa(MicrocredencialDisplayDTO mc) {
        return ActaMicrocredencialModel.actaAbierta(mc) || "FINALIZADA".equals(mc.getEstado());
    }

    /** Notas escritas en la tabla del acta (clave: matrícula); confirma antes la celda que se esté editando */
    private Map<String, String> getNotasMicrocredencial() {
        if (view.getTabNotasMicrocredencial().isEditing())
            view.getTabNotasMicrocredencial().getCellEditor().stopCellEditing();
        Map<String, String> notas = new LinkedHashMap<>();
        TableModel model = view.getTabNotasMicrocredencial().getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object nota = model.getValueAt(i, COLUMNA_NOTA);
            notas.put((String) model.getValueAt(i, 0), nota == null ? "" : nota.toString());
        }
        return notas;
    }

    private void guardarNotasMicrocredencial() {
        MicrocredencialDisplayDTO mc = getMicrocredencialSeleccionada();
        if (mc == null)
            return;
        actaMicrocredencialModel.guardarNotas(profesorId, mc.getMicrocredencialId(), getNotasMicrocredencial(),
                FechaSistema.hoyStr());
        JOptionPane.showMessageDialog(view.getFrame(), "Notas guardadas correctamente.", "Guardar notas",
                JOptionPane.INFORMATION_MESSAGE);
        cargarActaMicrocredencial();
    }

    /**
     * Pide confirmación y cierra el acta; antes guarda las notas escritas en la tabla para no perderlas.
     * Después recarga "Mis microcredenciales" (pasa a FINALIZADA) y el acta, que queda de solo lectura
     */
    private void cerrarActaMicrocredencial() {
        MicrocredencialDisplayDTO mc = getMicrocredencialSeleccionada();
        if (mc == null)
            return;
        Map<String, String> notas = getNotasMicrocredencial();
        int confirm = JOptionPane.showConfirmDialog(view.getFrame(),
                "¿Seguro que quieres cerrar el acta de " + mc.getNombre() + " (" + mc.getMicrocredencialId() + ")?\n"
                        + "Los alumnos sin nota quedarán como no presentados (NP).\n"
                        + "Una vez cerrada, las notas no se podrán modificar.",
                "Cerrar acta", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        String hoy = FechaSistema.hoyStr();
        actaMicrocredencialModel.guardarNotas(profesorId, mc.getMicrocredencialId(), notas, hoy);
        actaMicrocredencialModel.cerrarActa(profesorId, mc.getMicrocredencialId(), hoy);
        JOptionPane.showMessageDialog(view.getFrame(), "Acta cerrada correctamente.", "Acta cerrada",
                JOptionPane.INFORMATION_MESSAGE);
        cargarMicrocredenciales();
        seleccionarMicrocredencial(mc.getMicrocredencialId());
    }

    private void seleccionarMicrocredencial(String microcredencialId) {
        for (int i = 0; i < microcredenciales.size(); i++) {
            if (microcredenciales.get(i).getMicrocredencialId().equals(microcredencialId)) {
                view.getTabMicrocredenciales().setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    private ActaDisplayDTO getActaSeleccionada() {
        int indice = view.getCbActas().getSelectedIndex();
        if (asignacionSelec == null || indice < 0 || indice >= actas.size())
            return null;
        return actas.get(indice);
    }

    private void initView() {
        view.getLblProfesor().setText(profesor.getNombre() + " " + profesor.getApellidos() + " ("
                + profesor.getProfesorId() + ")");
        view.getFrame().setVisible(true);
    }
}
