package controller.secretario;

import model.matriculacion.GestionMatriculaModel;
import model.matriculacion.GrupoConsultaDTO;
import model.matriculacion.GrupoPlazasDTO;
import model.matriculacion.MatriculaSecretariaDTO;
import repository.MatriculacionRepository;
import view.secretario.SecretarioView;
import util.Database;
import util.SwingUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Controlador de la vista de Secretaría.
 * HU 35226: consulta de matriculaciones filtrando por estudiante, año, grado, curso, grupo y estado.
 * HU 35229: ciclo de vida de las matrículas y grupos (aceptar solicitudes, modificar, asignar grupos,
 * cancelar, activar al inicio del año y finalizar al terminarlo).
 */
public class SecretarioController {
    private static final String TODOS = "TODOS";

    private SecretarioView view;
    private MatriculacionRepository repository;
    // Evita recargar la tabla mientras se rellenan los desplegables de filtros
    private boolean cargandoFiltros = false;
    // Accion al pulsar "Volver al inicio" (mostrar la pantalla de seleccion de rol)
    private Runnable onVolverInicio = () -> { };

    public SecretarioController(SecretarioView view) {
        this(view, new MatriculacionRepository(new Database()));
    }

    public SecretarioController(SecretarioView view, MatriculacionRepository repository) {
        this.view = view;
        this.repository = repository;
        this.initView();
    }

    public void setOnVolverInicio(Runnable onVolverInicio) {
        this.onVolverInicio = onVolverInicio;
    }

    public void initController() {
        view.getBtnVolverInicio().addActionListener(e -> volverInicio());
        view.getBtnCargarMatriculas().addActionListener(e -> SwingUtil.exceptionWrapper(this::cargarMatriculasFiltradas));
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
        TreeSet<String> anios = new TreeSet<>();
        TreeSet<String> grados = new TreeSet<>();
        TreeSet<String> cursos = new TreeSet<>();
        for (MatriculaSecretariaDTO m : repository.getMatriculasSecretaria()) {
            anios.add(m.getAnioAcademicoId());
            grados.add(m.getGrado());
            cursos.add(m.getCurso());
        }
        cargandoFiltros = true;
        try {
            rellenarCombo(view.getCbFiltroAnio(), anios.descendingSet());
            rellenarCombo(view.getCbFiltroGrado(), grados);
            rellenarCombo(view.getCbFiltroCurso(), cursos);
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

    private void cargarMatriculasFiltradas() {
        String estado = (String) view.getCbFiltroEstado().getSelectedItem();
        String anio = (String) view.getCbFiltroAnio().getSelectedItem();
        String grado = (String) view.getCbFiltroGrado().getSelectedItem();
        String curso = (String) view.getCbFiltroCurso().getSelectedItem();
        String estudiante = normalizar(view.getTxtFiltroEstudiante().getText());
        String grupo = normalizar(view.getTxtFiltroGrupo().getText());
        String situacion = (String) view.getCbFiltroSituacion().getSelectedItem();
        String hoy = hoy();

        List<MatriculaSecretariaDTO> lista = repository.getMatriculasSecretaria();
        DefaultTableModel model = (DefaultTableModel) view.getTabMatriculas().getModel();
        model.setRowCount(0);

        for (MatriculaSecretariaDTO m : lista) {
            boolean coincide = coincideExacto(estado, m.getEstado())
                    && coincideExacto(anio, m.getAnioAcademicoId())
                    && coincideExacto(grado, m.getGrado())
                    && coincideExacto(curso, m.getCurso())
                    && (estudiante.isEmpty() || contiene(m.getEstudiante(), estudiante) || contiene(m.getDni(), estudiante))
                    && (grupo.isEmpty() || tieneGrupo(m.getGrupo(), grupo))
                    && coincideSituacion(situacion, m, hoy);
            if (coincide) {
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
        }
        view.getLblResultados().setText(model.getRowCount() + " de " + lista.size() + " matrículas mostradas");
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
        TreeSet<String> grados = new TreeSet<>();
        TreeSet<String> cursos = new TreeSet<>();
        String anioActual = repository.getAnioAcademicoActual(hoy());
        for (GrupoConsultaDTO g : repository.getGruposConsulta(anioActual)) {
            cursos.add(g.getCurso());
            for (String grado : g.getGrados().split("; ")) {
                if (!"-".equals(grado)) {
                    grados.add(grado);
                }
            }
        }
        cargandoFiltros = true;
        try {
            Object anioSeleccionado = view.getCbGruposAnio().getSelectedItem();
            DefaultComboBoxModel<String> anios = new DefaultComboBoxModel<>();
            for (String anio : repository.getAniosAcademicos()) {
                anios.addElement(anio);
            }
            view.getCbGruposAnio().setModel(anios);
            view.getCbGruposAnio().setSelectedItem(anioSeleccionado != null ? anioSeleccionado : anioActual);
            rellenarCombo(view.getCbGruposGrado(), grados);
            rellenarCombo(view.getCbGruposCurso(), cursos);
        } finally {
            cargandoFiltros = false;
        }
    }

    private void cargarGrupos() {
        String anio = (String) view.getCbGruposAnio().getSelectedItem();
        String grado = (String) view.getCbGruposGrado().getSelectedItem();
        String curso = (String) view.getCbGruposCurso().getSelectedItem();

        DefaultTableModel model = (DefaultTableModel) view.getTabGrupos().getModel();
        model.setRowCount(0);
        limpiarAlumnosGrupo();
        if (anio == null) {
            view.getLblResultadosGrupos().setText("No hay años académicos definidos");
            return;
        }
        List<GrupoConsultaDTO> grupos = repository.getGruposConsulta(anio);
        for (GrupoConsultaDTO g : grupos) {
            boolean coincide = coincideExacto(curso, g.getCurso())
                    && (grado == null || TODOS.equals(grado) || Arrays.asList(g.getGrados().split("; ")).contains(grado));
            if (coincide) {
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
        }
        view.getLblResultadosGrupos().setText(model.getRowCount() + " de " + grupos.size() + " grupos mostrados en "
                + anio + "  —  Ocupadas: cursos PAGADOS o ACTIVOS del año");
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
        for (MatriculaSecretariaDTO m : repository.getMatriculasGrupo(cursoId, grupoCodigo, anio)) {
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
        MatriculaSecretariaDTO m = repository.getMatriculaSecretaria(view.getIdMatriculaSeleccionada(),
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
        MatriculaSecretariaDTO m = repository.getMatriculaSecretaria(matriculaId, cursoId);

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

        String estadoFinal = repository.aceptarSolicitud(matriculaId, cursoId, metodoPago, hoy());
        String mensaje = "ACTIVA".equals(estadoFinal)
                ? "Solicitud aceptada. Como el año académico " + m.getAnioAcademicoId() + " ya ha comenzado, la matrícula queda ACTIVA."
                : "Solicitud aceptada. La matrícula queda PAGADA y pasará a ACTIVA al iniciar el curso.";
        JOptionPane.showMessageDialog(view.getFrame(), mensaje, "Éxito", JOptionPane.INFORMATION_MESSAGE);
        refrescar();
    }

    private void modificarMatricula() {
        String matriculaId = view.getIdMatriculaSeleccionada();
        String cursoId = view.getCursoIdSeleccionado();
        MatriculaSecretariaDTO m = repository.getMatriculaSecretaria(matriculaId, cursoId);

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

        repository.modificarMatricula(matriculaId, cursoId, metodoPago, txtEmail.getText(), hoy());
        JOptionPane.showMessageDialog(view.getFrame(), "Información de la matrícula actualizada correctamente.",
                "Modificación Exitosa", JOptionPane.INFORMATION_MESSAGE);
        refrescar();
    }

    /** Asigna o cambia los grupos del curso: uno de cada tipo, mostrando las plazas libres de cada grupo. */
    private void asignarGrupos() {
        String matriculaId = view.getIdMatriculaSeleccionada();
        String cursoId = view.getCursoIdSeleccionado();
        MatriculaSecretariaDTO m = repository.getMatriculaSecretaria(matriculaId, cursoId);
        List<GrupoPlazasDTO> grupos = repository.getGruposMatricula(matriculaId, cursoId);
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
        repository.asignarGrupos(matriculaId, cursoId, grupoPorTipo);
        JOptionPane.showMessageDialog(view.getFrame(), "Grupos asignados correctamente: " + grupoPorTipo.values(),
                "Grupos asignados", JOptionPane.INFORMATION_MESSAGE);
        refrescar();
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
        MatriculaSecretariaDTO m = repository.getMatriculaSecretaria(matriculaId, cursoId);

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
        repository.cancelarMatricula(matriculaId, cursoId, motivo, hoy(), rbTodo.isSelected());
        JOptionPane.showMessageDialog(view.getFrame(),
                "Cancelación registrada correctamente.\nMotivo: " + motivo
                        + "\nLa matrícula se conserva en el sistema con estado CANCELADA.",
                "Cancelada", JOptionPane.INFORMATION_MESSAGE);
        refrescar();
    }

    /** Recarga el listado y el detalle para que se vea el estado actualizado tras una operación. */
    private void refrescar() {
        cargarOpcionesFiltros();
        cargarMatriculasFiltradas();
        cargarGrupos();
        mostrarDetalle();
    }

    // =========================================================================
    // Auxiliares
    // =========================================================================

    private String hoy() {
        return LocalDate.now().toString();
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.toLowerCase().trim();
    }

    private static boolean coincideExacto(String filtro, String valor) {
        return filtro == null || TODOS.equals(filtro) || filtro.equalsIgnoreCase(valor);
    }

    private static boolean contiene(String valor, String filtro) {
        return valor != null && valor.toLowerCase().contains(filtro);
    }

    /** El filtro de grupo compara cada grupo asignado completo ("PL1" no coincide con "PL10"). */
    private static boolean tieneGrupo(String grupos, String filtro) {
        if (grupos == null) {
            return false;
        }
        for (String g : grupos.split(",")) {
            if (g.trim().equalsIgnoreCase(filtro)) {
                return true;
            }
        }
        return false;
    }

    private static boolean coincideSituacion(String situacion, MatriculaSecretariaDTO m, String hoy) {
        if (SecretarioView.SITUACION_PAGO_PENDIENTE.equals(situacion)) {
            return m.isPagoPendienteValidar();
        }
        if (SecretarioView.SITUACION_SIN_PAGAR.equals(situacion)) {
            return m.isSinPagarAnioIniciado(hoy);
        }
        if (SecretarioView.SITUACION_FALTAN_GRUPOS.equals(situacion)) {
            return m.isFaltanGrupos();
        }
        return true;
    }

    private void initView() {
        view.showListadoView();
        view.getFrame().setVisible(true);
    }
}
