package controller.secretario;

import model.gestionmatricula.FiltroMatriculasSecretaria;
import model.gestionmatricula.GestionMatriculaModel;
import model.gestionmatricula.GrupoConsultaDTO;
import model.gestionmatricula.GrupoPlazasDTO;
import model.gestionmatricula.MatriculaSecretariaDTO;
import model.gestionmatricula.SituacionSecretaria;
import model.acta.ActaModel;
import view.secretario.SecretarioView;
import util.Database;
import util.Paginacion;
import util.SwingUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controlador de la vista de Secretaría.
 * HU 35226: consulta de matriculaciones filtrando por estudiante, año, grado, curso, grupo y estado.
 * HU 35229: ciclo de vida de las matrículas y grupos (aceptar solicitudes, modificar, asignar grupos,
 * cancelar, activar al inicio del año y finalizar al terminarlo).
 * HU 35239: consulta de las actas cerradas por el sistema (notificaciones a secretaría).
 */
public class SecretarioController {
    private static final String TODOS = "TODOS";
    // Con una base de datos grande solo se lee de la BD la página que se muestra
    private static final int FILAS_POR_PAGINA = 25;

    private SecretarioView view;
    private final Paginacion paginacionMatriculas = new Paginacion(FILAS_POR_PAGINA);
    private final Paginacion paginacionGrupos = new Paginacion(FILAS_POR_PAGINA);
    private final GestionMatriculaModel gestionModel;
    private final ActaModel actaModel;
    private final RevisionMicrocredencialController revisionController;
    // Evita recargar la tabla mientras se rellenan los desplegables de filtros
    private boolean cargandoFiltros = false;
    // Accion al pulsar "Volver al inicio" (mostrar la pantalla de seleccion de rol)
    private Runnable onVolverInicio = () -> { };

    public SecretarioController(SecretarioView view, Database db) {
        this.view = view;
        this.gestionModel = new GestionMatriculaModel(db);
        this.actaModel = new ActaModel(db);
        this.revisionController = new RevisionMicrocredencialController(view, db);
        this.initView();
    }

    public void setOnVolverInicio(Runnable onVolverInicio) {
        this.onVolverInicio = onVolverInicio;
    }

    public void initController() {
        view.getBtnVolverInicio().addActionListener(e -> volverInicio());
        view.getBtnActasCerradas().addActionListener(e -> SwingUtil.exceptionWrapper(this::mostrarActasCerradas));
        view.getBtnCargarMatriculas().addActionListener(e -> SwingUtil.exceptionWrapper(this::cargarMatriculasFiltradas));
        view.getPaginacionMatriculas().conectar(paginacionMatriculas,
                () -> SwingUtil.exceptionWrapper(this::cargarPaginaMatriculas));
        view.getPaginacionGrupos().conectar(paginacionGrupos,
                () -> SwingUtil.exceptionWrapper(this::cargarPaginaGrupos));
        view.getBtnLimpiarFiltros().addActionListener(e -> SwingUtil.exceptionWrapper(this::limpiarFiltros));
        view.getCbFiltroEstado().addActionListener(e -> filtrarSiProcede());
        view.getCbFiltroAnio().addActionListener(e -> filtrarSiProcede());
        view.getCbFiltroGrado().addActionListener(e -> filtrarSiProcede());
        view.getCbFiltroCurso().addActionListener(e -> filtrarSiProcede());
        view.getTxtFiltroEstudiante().addActionListener(e -> filtrarSiProcede());
        view.getTxtFiltroGrupo().addActionListener(e -> filtrarSiProcede());
        view.getCbFiltroSituacion().addActionListener(e -> filtrarSiProcede());

        view.setOnGestionar(() -> SwingUtil.exceptionWrapper(this::mostrarDetalle));
        view.getBtnAceptarSolicitud().addActionListener(e -> SwingUtil.exceptionWrapper(this::aceptarSolicitud));
        view.getBtnModificarMatricula().addActionListener(e -> SwingUtil.exceptionWrapper(this::modificarMatricula));
        view.getBtnAsignarGrupos().addActionListener(e -> SwingUtil.exceptionWrapper(this::asignarGrupos));
        view.getBtnCancelarMatricula().addActionListener(e -> SwingUtil.exceptionWrapper(this::cancelarMatricula));

        // Consulta de grupos (HU 35226)
        view.getCbGruposAnio().addActionListener(e -> filtrarGruposSiProcede());
        view.getCbGruposGrado().addActionListener(e -> filtrarGruposSiProcede());
        view.getCbGruposCurso().addActionListener(e -> filtrarGruposSiProcede());
        view.getBtnCargarGrupos().addActionListener(e -> SwingUtil.exceptionWrapper(this::cargarGrupos));
        view.getTabGrupos().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                SwingUtil.exceptionWrapper(this::cargarAlumnosGrupo);
            }
        });

        // Revisión de microcredenciales propuestas (HU 35994), en su propio controlador
        revisionController.initController();

        // El ciclo de vida (activar/finalizar por fechas) ya se ha actualizado al arrancar la aplicación
        // (MainController), así que abrir la consulta no modifica ningún dato
        SwingUtil.exceptionWrapper(() -> {
            cargarOpcionesFiltros();
            cargarMatriculasFiltradas();
            cargarOpcionesGrupos();
            cargarGrupos();
        });
    }

    /** Cierra la ventana de Secretaría y vuelve a la pantalla de selección de rol. */
    private void volverInicio() {
        view.getFrame().dispose();
        onVolverInicio.run();
    }

    // =========================================================================
    // HU 35226: consulta
    // =========================================================================

    private void filtrarSiProcede() {
        if (!cargandoFiltros) {
            SwingUtil.exceptionWrapper(this::cargarMatriculasFiltradas);
        }
    }

    /** Rellena los desplegables de año, grado y curso con los valores existentes en las matrículas. */
    private void cargarOpcionesFiltros() {
        cargandoFiltros = true;
        try {
            rellenarCombo(view.getCbFiltroAnio(), gestionModel.getAniosConMatriculas());
            rellenarCombo(view.getCbFiltroGrado(), gestionModel.getGradosConMatriculas());
            rellenarCombo(view.getCbFiltroCurso(), gestionModel.getCursosConMatriculas());
        } finally {
            cargandoFiltros = false;
        }
    }

    private void rellenarCombo(JComboBox<String> combo, Iterable<String> valores) {
        Object seleccionado = combo.getSelectedItem();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement(TODOS);
        for (String v : valores) {
            model.addElement(v);
        }
        combo.setModel(model);
        combo.setSelectedItem(seleccionado != null ? seleccionado : TODOS);
    }

    private void limpiarFiltros() {
        cargandoFiltros = true;
        try {
            view.getTxtFiltroEstudiante().setText("");
            view.getTxtFiltroGrupo().setText("");
            view.getCbFiltroAnio().setSelectedItem(TODOS);
            view.getCbFiltroGrado().setSelectedItem(TODOS);
            view.getCbFiltroCurso().setSelectedItem(TODOS);
            view.getCbFiltroEstado().setSelectedItem(TODOS);
            view.getCbFiltroSituacion().setSelectedItem(SecretarioView.SITUACION_TODAS);
        } finally {
            cargandoFiltros = false;
        }
        cargarMatriculasFiltradas();
    }

    /** Aplica los filtros: el listado vuelve a la primera página. */
    private void cargarMatriculasFiltradas() {
        paginacionMatriculas.primera();
        cargarPaginaMatriculas();
    }

    /** Carga la página actual del listado con los filtros (filtrado y paginación se hacen en la consulta SQL). */
    private void cargarPaginaMatriculas() {
        FiltroMatriculasSecretaria filtro = new FiltroMatriculasSecretaria()
                .setEstado(valorFiltro(view.getCbFiltroEstado()))
                .setAnioAcademicoId(valorFiltro(view.getCbFiltroAnio()))
                .setGrado(valorFiltro(view.getCbFiltroGrado()))
                .setCurso(valorFiltro(view.getCbFiltroCurso()))
                .setEstudiante(view.getTxtFiltroEstudiante().getText())
                .setGrupo(view.getTxtFiltroGrupo().getText())
                .setSituacion(situacion((String) view.getCbFiltroSituacion().getSelectedItem()))
                .setFechaHoy(hoy());

        // Primero el total, que puede recolocar la página si tras un cambio hay menos filas
        paginacionMatriculas.setTotalFilas(gestionModel.contarMatriculas(filtro));
        List<MatriculaSecretariaDTO> pagina = gestionModel.getMatriculas(filtro,
                paginacionMatriculas.getOffset(), paginacionMatriculas.getFilasPorPagina());

        DefaultTableModel model = (DefaultTableModel) view.getTabMatriculas().getModel();
        model.setRowCount(0);
        for (MatriculaSecretariaDTO m : pagina) {
            model.addRow(new Object[] {
                m.getMatriculaId(),
                m.getCursoId(),
                m.getAnioAcademicoId(),
                m.getDni(),
                m.getEstudiante(),
                m.getGrado(),
                m.getCurso(),
                m.getGrupo(),
                m.getEstado(),
                "Ver / Gestionar"
            });
        }
        view.getLblResultados().setText(textoResultados(paginacionMatriculas, "matrícula", "matrículas"));
        view.getPaginacionMatriculas().actualizar(paginacionMatriculas);
    }

    // =========================================================================
    // HU 35226: consulta de grupos
    // =========================================================================

    private void filtrarGruposSiProcede() {
        if (!cargandoFiltros) {
            SwingUtil.exceptionWrapper(this::cargarGrupos);
        }
    }

    /** Rellena los años académicos (seleccionando el año en curso) y los grados y cursos que tienen grupos. */
    private void cargarOpcionesGrupos() {
        String anioActual = gestionModel.getAnioAcademicoActual(hoy());
        cargandoFiltros = true;
        try {
            Object anioSeleccionado = view.getCbGruposAnio().getSelectedItem();
            DefaultComboBoxModel<String> anios = new DefaultComboBoxModel<>();
            for (String anio : gestionModel.getAniosAcademicos()) {
                anios.addElement(anio);
            }
            view.getCbGruposAnio().setModel(anios);
            view.getCbGruposAnio().setSelectedItem(anioSeleccionado != null ? anioSeleccionado : anioActual);
            rellenarCombo(view.getCbGruposGrado(), gestionModel.getGradosConGrupos());
            rellenarCombo(view.getCbGruposCurso(), gestionModel.getCursosConGrupos());
        } finally {
            cargandoFiltros = false;
        }
    }

    /** Aplica los filtros de grupos: la consulta vuelve a la primera página. */
    private void cargarGrupos() {
        paginacionGrupos.primera();
        cargarPaginaGrupos();
    }

    /** Carga la página actual de grupos con su ocupación en el año seleccionado. */
    private void cargarPaginaGrupos() {
        String anio = (String) view.getCbGruposAnio().getSelectedItem();
        String grado = valorFiltro(view.getCbGruposGrado());
        String curso = valorFiltro(view.getCbGruposCurso());

        DefaultTableModel model = (DefaultTableModel) view.getTabGrupos().getModel();
        model.setRowCount(0);
        limpiarAlumnosGrupo();
        if (anio == null) {
            paginacionGrupos.setTotalFilas(0);
            view.getPaginacionGrupos().actualizar(paginacionGrupos);
            view.getLblResultadosGrupos().setText("No hay años académicos definidos");
            return;
        }
        paginacionGrupos.setTotalFilas(gestionModel.contarGruposConsulta(grado, curso));
        for (GrupoConsultaDTO g : gestionModel.getGruposConsulta(anio, grado, curso, paginacionGrupos.getOffset(),
                paginacionGrupos.getFilasPorPagina())) {
            model.addRow(new Object[] {
                g.getCursoId(),
                g.getCurso(),
                g.getGrupoCodigo(),
                nombreTipoGrupo(g.getTipo()),
                g.getGrados(),
                g.getProfesores(),
                g.getCapacidad(),
                g.getOcupadas(),
                g.getPlazasLibres()
            });
        }
        view.getLblResultadosGrupos().setText(textoResultados(paginacionGrupos, "grupo", "grupos") + " en " + anio);
        view.getPaginacionGrupos().actualizar(paginacionGrupos);
    }

    /** Muestra los alumnos del grupo seleccionado en el año académico elegido. */
    private void cargarAlumnosGrupo() {
        JTable tabla = view.getTabGrupos();
        String anio = (String) view.getCbGruposAnio().getSelectedItem();
        if (tabla.getSelectedRow() < 0 || anio == null) {
            limpiarAlumnosGrupo();
            return;
        }
        int fila = tabla.convertRowIndexToModel(tabla.getSelectedRow());
        String cursoId = tabla.getModel().getValueAt(fila, SecretarioView.COL_GRUPO_CURSO_ID).toString();
        String grupoCodigo = tabla.getModel().getValueAt(fila, SecretarioView.COL_GRUPO_CODIGO).toString();
        String curso = tabla.getModel().getValueAt(fila, 1).toString();

        DefaultTableModel model = (DefaultTableModel) view.getTabAlumnosGrupo().getModel();
        model.setRowCount(0);
        for (MatriculaSecretariaDTO m : gestionModel.getMatriculasGrupo(cursoId, grupoCodigo, anio)) {
            model.addRow(new Object[] { m.getMatriculaId(), m.getDni(), m.getEstudiante(), m.getGrado(), m.getEstado() });
        }
        view.setTituloAlumnosGrupo("Alumnos de " + curso + " - " + grupoCodigo + " en " + anio
                + " (" + model.getRowCount() + ")");
    }

    private void limpiarAlumnosGrupo() {
        ((DefaultTableModel) view.getTabAlumnosGrupo().getModel()).setRowCount(0);
        view.setTituloAlumnosGrupo("Alumnos del grupo (seleccione un grupo)");
    }

    private void mostrarDetalle() {
        MatriculaSecretariaDTO m = gestionModel.getMatricula(view.getIdMatriculaSeleccionada(),
                view.getCursoIdSeleccionado());
        String pago = String.format(Locale.US, "%.2f €", m.getImporte() != null ? m.getImporte() : 0.0)
                + (m.getMetodoPago() != null ? " - " + m.getMetodoPago() : " - sin método de pago")
                + (m.getFechaPago() != null ? " (" + m.getFechaPago() + ")" : "")
                + (m.getPagoValidado() == 1 ? " - validado" : " - pendiente de validar");
        String cancelacion = m.getMotivoCancelacion() != null
                ? m.getMotivoCancelacion() + " (" + m.getFechaCancelacion() + ")"
                : "-";
        String grupos = m.getGrupo() + (m.isFaltanGrupos() ? "  (faltan " + m.getGruposPendientes() + " por asignar)" : "");
        view.showDetalleView(m.getMatriculaId(), m.getAnioAcademicoId(), m.getDni() + " - " + m.getEstudiante(),
                m.getEmail(), m.getGrado(), m.getCursoId() + " - " + m.getCurso(), grupos, m.getEstado(),
                m.getEstadoMatricula(), pago, cancelacion);
    }

    // =========================================================================
    // HU 35229: ciclo de vida
    // =========================================================================

    private void aceptarSolicitud() {
        String matriculaId = view.getIdMatriculaSeleccionada();
        String cursoId = view.getCursoIdSeleccionado();
        MatriculaSecretariaDTO m = gestionModel.getMatricula(matriculaId, cursoId);

        // Si la solicitud no tiene metodo de pago hay que registrarlo al aceptarla
        String metodoPago = m.getMetodoPago();
        if (metodoPago == null) {
            metodoPago = (String) JOptionPane.showInputDialog(view.getFrame(),
                    "La solicitud no tiene método de pago. Seleccione el método con el que se ha pagado:",
                    "Registrar pago", JOptionPane.QUESTION_MESSAGE, null,
                    GestionMatriculaModel.METODOS_PAGO.toArray(), GestionMatriculaModel.METODOS_PAGO.get(0));
            if (metodoPago == null) {
                return;
            }
        }
        int confirm = JOptionPane.showConfirmDialog(view.getFrame(),
                "¿Aceptar la solicitud " + matriculaId + " (" + m.getCurso() + ") y validar el pago por " + metodoPago + "?",
                "Aceptar Solicitud y Pago", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String estadoFinal = gestionModel.aceptarSolicitud(matriculaId, cursoId, metodoPago, hoy());
        // Se refresca antes del aviso para que detrás ya se vea el nuevo estado guardado en la BD
        refrescar();
        String mensaje;
        if ("ACTIVA".equals(estadoFinal)) {
            mensaje = "Solicitud aceptada. Como el año académico " + m.getAnioAcademicoId() + " ya ha comenzado, la matrícula queda ACTIVA.";
        } else if ("FINALIZADA".equals(estadoFinal)) {
            mensaje = "Solicitud aceptada. Como el año académico " + m.getAnioAcademicoId() + " ya ha terminado, la matrícula queda FINALIZADA.";
        } else {
            mensaje = "Solicitud aceptada. La matrícula queda PAGADA y pasará a ACTIVA al iniciar el curso.";
        }
        JOptionPane.showMessageDialog(view.getFrame(), mensaje, "Éxito", JOptionPane.INFORMATION_MESSAGE);
    }

    private void modificarMatricula() {
        String matriculaId = view.getIdMatriculaSeleccionada();
        String cursoId = view.getCursoIdSeleccionado();
        MatriculaSecretariaDTO m = gestionModel.getMatricula(matriculaId, cursoId);

        JComboBox<Object> cbMetodo = new JComboBox<>(GestionMatriculaModel.METODOS_PAGO.toArray());
        if (m.getMetodoPago() == null) {
            cbMetodo.insertItemAt("(sin método de pago)", 0);
            cbMetodo.setSelectedIndex(0);
        } else {
            cbMetodo.setSelectedItem(m.getMetodoPago());
        }
        // Un pago ya validado no se puede rehacer con otro método
        if (m.getPagoValidado() == 1) {
            cbMetodo.setEnabled(false);
            cbMetodo.setToolTipText("El pago ya está validado");
        }
        JTextField txtEmail = new JTextField(m.getEmail(), 25);

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("Método de pago:"));
        panel.add(cbMetodo);
        panel.add(new JLabel("Email de contacto:"));
        panel.add(txtEmail);

        int opcion = JOptionPane.showConfirmDialog(view.getFrame(), panel,
                "Modificar matrícula " + matriculaId + " (" + m.getCurso() + ")",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }
        Object metodo = cbMetodo.getSelectedItem();
        String metodoPago = GestionMatriculaModel.METODOS_PAGO.contains(metodo) ? (String) metodo : null;

        gestionModel.modificarMatricula(matriculaId, cursoId, metodoPago, txtEmail.getText(), hoy());
        refrescar();
        JOptionPane.showMessageDialog(view.getFrame(), "Información de la matrícula actualizada correctamente.",
                "Modificación Exitosa", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Asigna o cambia los grupos del curso: uno de cada tipo, mostrando las plazas libres de cada grupo. */
    private void asignarGrupos() {
        String matriculaId = view.getIdMatriculaSeleccionada();
        String cursoId = view.getCursoIdSeleccionado();
        MatriculaSecretariaDTO m = gestionModel.getMatricula(matriculaId, cursoId);
        List<GrupoPlazasDTO> grupos = gestionModel.getGrupos(matriculaId, cursoId);
        if (grupos.isEmpty()) {
            JOptionPane.showMessageDialog(view.getFrame(), "El curso " + m.getCurso() + " no tiene grupos definidos.",
                    "Asignar grupos", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Un desplegable por cada tipo de grupo del curso (TE, PA, PL)
        Map<String, JComboBox<GrupoPlazasDTO>> combos = new LinkedHashMap<>();
        for (GrupoPlazasDTO g : grupos) {
            JComboBox<GrupoPlazasDTO> combo = combos.computeIfAbsent(g.getTipo(), t -> new JComboBox<>());
            combo.addItem(g);
            if (g.estaAsignado()) {
                combo.setSelectedItem(g);
            }
        }
        JPanel panel = new JPanel(new GridLayout(combos.size() + 1, 2, 5, 5));
        panel.add(new JLabel("Tipo de grupo"));
        panel.add(new JLabel("Grupo (plazas libres)"));
        for (Map.Entry<String, JComboBox<GrupoPlazasDTO>> e : combos.entrySet()) {
            panel.add(new JLabel(nombreTipoGrupo(e.getKey()) + ":"));
            panel.add(e.getValue());
        }

        int opcion = JOptionPane.showConfirmDialog(view.getFrame(), panel,
                "Asignar grupos - " + matriculaId + " (" + m.getCurso() + ")",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }
        Map<String, String> grupoPorTipo = new LinkedHashMap<>();
        for (Map.Entry<String, JComboBox<GrupoPlazasDTO>> e : combos.entrySet()) {
            grupoPorTipo.put(e.getKey(), ((GrupoPlazasDTO) e.getValue().getSelectedItem()).getGrupoCodigo());
        }
        gestionModel.asignarGrupos(matriculaId, cursoId, grupoPorTipo);
        refrescar();
        JOptionPane.showMessageDialog(view.getFrame(), "Grupos asignados correctamente: " + grupoPorTipo.values(),
                "Grupos asignados", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String nombreTipoGrupo(String tipo) {
        switch (tipo) {
            case "TE": return "Teoría (TE)";
            case "PA": return "Prácticas de aula (PA)";
            case "PL": return "Prácticas de laboratorio (PL)";
            default: return tipo;
        }
    }

    private void cancelarMatricula() {
        String matriculaId = view.getIdMatriculaSeleccionada();
        String cursoId = view.getCursoIdSeleccionado();
        MatriculaSecretariaDTO m = gestionModel.getMatricula(matriculaId, cursoId);

        JComboBox<Object> cbMotivo = new JComboBox<>(GestionMatriculaModel.MOTIVOS_CANCELACION.toArray());
        JRadioButton rbCurso = new JRadioButton("Solo el curso " + m.getCurso(), true);
        JRadioButton rbTodo = new JRadioButton("Toda la matrícula " + matriculaId);
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(rbCurso);
        grupo.add(rbTodo);

        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        panel.add(new JLabel("Motivo de cancelación:"));
        panel.add(cbMotivo);
        panel.add(rbCurso);
        panel.add(rbTodo);

        int opcion = JOptionPane.showConfirmDialog(view.getFrame(), panel, "Confirmar Cancelación",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }
        String motivo = (String) cbMotivo.getSelectedItem();
        gestionModel.cancelar(matriculaId, cursoId, motivo, hoy(), rbTodo.isSelected());
        refrescar();
        JOptionPane.showMessageDialog(view.getFrame(),
                "Cancelación registrada correctamente.\nMotivo: " + motivo
                        + "\nLa matrícula se conserva en el sistema con estado CANCELADA.",
                "Cancelada", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Recarga el listado y el detalle para que se vea el estado actualizado tras una operación,
     * manteniendo la página en la que estaba el secretario.
     */
    private void refrescar() {
        cargarOpcionesFiltros();
        cargarPaginaMatriculas();
        cargarPaginaGrupos();
        mostrarDetalle();
    }

    // =========================================================================
    // HU 35239: avisos de actas cerradas por el sistema
    // =========================================================================

    /**
     * Cada acta que el sistema cerro al terminar la convocatoria sin que la cerrase el coordinador
     * es una notificacion para secretaria (Acta.cierreAutomatico = 1). El cierre lo hace MainController al arrancar
     */
    private void mostrarActasCerradas() {
        List<Object[]> actas = actaModel.getActasCerradasAutomaticamente();
        if (actas.isEmpty()) {
            JOptionPane.showMessageDialog(view.getFrame(), "No hay actas cerradas por el sistema.",
                    "Actas cerradas por el sistema", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DefaultTableModel model = new DefaultTableModel(new String[] { "Curso", "Asignatura", "Año", "Convocatoria",
                "Fecha cierre", "Coordinador" }, 0);
        for (Object[] a : actas)
            model.addRow(a);
        JTable tabla = new JTable(model);
        tabla.setDefaultEditor(Object.class, null);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new java.awt.Dimension(750, 200));
        JOptionPane.showMessageDialog(view.getFrame(), scroll,
                "Actas cerradas por el sistema (" + actas.size() + ")", JOptionPane.WARNING_MESSAGE);
    }

    // =========================================================================
    // Auxiliares
    // =========================================================================

    private String hoy() {
        return LocalDate.now().toString();
    }

    /** Valor seleccionado en un desplegable de filtro, o nulo si es "TODOS" (no filtra). */
    private static String valorFiltro(JComboBox<String> combo) {
        String valor = (String) combo.getSelectedItem();
        return valor == null || TODOS.equals(valor) ? null : valor;
    }

    /** Situación del modelo que corresponde a la opción elegida en la vista. */
    private static SituacionSecretaria situacion(String opcion) {
        if (SecretarioView.SITUACION_PAGO_PENDIENTE.equals(opcion)) {
            return SituacionSecretaria.PAGO_PENDIENTE;
        }
        if (SecretarioView.SITUACION_SIN_PAGAR.equals(opcion)) {
            return SituacionSecretaria.SIN_PAGAR;
        }
        if (SecretarioView.SITUACION_FALTAN_GRUPOS.equals(opcion)) {
            return SituacionSecretaria.FALTAN_GRUPOS;
        }
        return SituacionSecretaria.TODAS;
    }

    /** Texto del pie de un listado paginado, p. ej. "Mostrando 26 - 50 de 21.660 matrículas". */
    private static String textoResultados(Paginacion p, String singular, String plural) {
        if (p.getTotalFilas() == 0) {
            return "No hay " + plural + " con estos filtros";
        }
        Locale es = Locale.forLanguageTag("es-ES");
        return String.format(es, "Mostrando %,d - %,d de %,d %s", p.getDesde(), p.getHasta(), p.getTotalFilas(),
                p.getTotalFilas() == 1 ? singular : plural);
    }

    private void initView() {
        view.showListadoView();
        view.getFrame().setVisible(true);
    }
}
