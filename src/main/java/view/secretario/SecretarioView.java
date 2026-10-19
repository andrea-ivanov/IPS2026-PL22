package view.secretario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class SecretarioView {
    public static final int COL_MATRICULA = 0;
    public static final int COL_CURSO_ID = 1;
    public static final int COL_ACCION = 9;
    public static final int COL_GRUPO_CURSO_ID = 0;
    public static final int COL_GRUPO_CODIGO = 2;

    // Opciones del filtro de situaciones que revisa el secretario
    public static final String SITUACION_TODAS = "TODAS";
    public static final String SITUACION_PAGO_PENDIENTE = "Pago pendiente de validar";
    public static final String SITUACION_SIN_PAGAR = "Sin pagar (año iniciado)";
    public static final String SITUACION_FALTAN_GRUPOS = "Faltan grupos por asignar";

    private JFrame frame;

    private JLabel lblTituloCabecera;
    private JButton btnVolver;
    private JButton btnVolverInicio;
    private JButton btnCargarMatriculas;
    private JButton btnLimpiarFiltros;
    private JLabel lblResultados;

    private JPanel cardsPanel;
    private CardLayout cardLayout;

    private JComboBox<String> cbFiltroAnio;
    private JComboBox<String> cbFiltroGrado;
    private JComboBox<String> cbFiltroCurso;
    private JTextField txtFiltroGrupo;
    private JTextField txtFiltroEstudiante;
    private JComboBox<String> cbFiltroEstado;
    private JComboBox<String> cbFiltroSituacion;
    private JTable tabMatriculas;

    // Pestaña de consulta de grupos (HU 35226)
    private JComboBox<String> cbGruposAnio;
    private JComboBox<String> cbGruposGrado;
    private JComboBox<String> cbGruposCurso;
    private JButton btnCargarGrupos;
    private JTable tabGrupos;
    private JTable tabAlumnosGrupo;
    private JScrollPane scrollAlumnosGrupo;
    private JLabel lblResultadosGrupos;

    private JLabel lblDetalleId, lblDetalleAnio, lblDetalleEstudiante, lblDetalleEmail, lblDetalleGrado,
            lblDetalleCurso,
            lblDetalleGrupos, lblDetalleEstado, lblDetalleEstadoMatricula, lblDetallePago, lblDetalleCancelacion;
    private JButton btnAceptarSolicitud;
    private JButton btnModificarMatricula;
    private JButton btnAsignarGrupos;
    private JButton btnCancelarMatricula;

    private String idMatriculaSeleccionada = "";
    private String cursoIdSeleccionado = "";
    private Runnable onGestionar = () -> {
    };

    public SecretarioView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Panel de Secretaría - Gestión de Matrículas");
        frame.setBounds(100, 100, 1050, 650);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setBorder(new EmptyBorder(10, 15, 10, 15));

        lblTituloCabecera = new JLabel("Secretaría: Consulta de Matrículas y Grupos");
        lblTituloCabecera.setFont(new Font("Tahoma", Font.BOLD, 16));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnVolver = new JButton("Volver al Listado");
        btnVolver.setEnabled(false);
        buttonsPanel.add(btnVolver);
        btnVolverInicio = new JButton("Volver al inicio");
        btnVolverInicio.setToolTipText("Volver a la pantalla de selección de rol");
        buttonsPanel.add(btnVolverInicio);

        topContainer.add(lblTituloCabecera, BorderLayout.WEST);
        topContainer.add(buttonsPanel, BorderLayout.EAST);
        frame.getContentPane().add(topContainer, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        JTabbedPane tabsListado = new JTabbedPane();
        tabsListado.addTab("Matrículas", createListadoPanel());
        tabsListado.addTab("Grupos", createGruposPanel());
        cardsPanel.add(tabsListado, "LISTADO");
        cardsPanel.add(createDetallePanel(), "DETALLE");
        frame.getContentPane().add(cardsPanel, BorderLayout.CENTER);

        btnVolver.addActionListener(e -> showListadoView());
    }

    private JPanel createListadoPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBorder(new TitledBorder("Filtros de Búsqueda"));
        filterPanel.setPreferredSize(new Dimension(240, 0));

        addFilterComponent(filterPanel, "Estudiante (Nombre o DNI):");
        txtFiltroEstudiante = new JTextField();
        addFilterField(filterPanel, txtFiltroEstudiante);

        addFilterComponent(filterPanel, "Año académico:");
        cbFiltroAnio = new JComboBox<>(new String[] { "TODOS" });
        addFilterField(filterPanel, cbFiltroAnio);

        addFilterComponent(filterPanel, "Grado:");
        cbFiltroGrado = new JComboBox<>(new String[] { "TODOS" });
        addFilterField(filterPanel, cbFiltroGrado);

        addFilterComponent(filterPanel, "Curso:");
        cbFiltroCurso = new JComboBox<>(new String[] { "TODOS" });
        addFilterField(filterPanel, cbFiltroCurso);

        addFilterComponent(filterPanel, "Grupo (p. ej. PL1):");
        txtFiltroGrupo = new JTextField();
        addFilterField(filterPanel, txtFiltroGrupo);

        addFilterComponent(filterPanel, "Estado:");
        cbFiltroEstado = new JComboBox<>(
                new String[] { "TODOS", "SOLICITADA", "PAGADA", "ACTIVA", "FINALIZADA", "CANCELADA" });
        addFilterField(filterPanel, cbFiltroEstado);

        addFilterComponent(filterPanel, "Situación a revisar:");
        cbFiltroSituacion = new JComboBox<>(new String[] { SITUACION_TODAS, SITUACION_PAGO_PENDIENTE,
                SITUACION_SIN_PAGAR, SITUACION_FALTAN_GRUPOS });
        addFilterField(filterPanel, cbFiltroSituacion);

        btnCargarMatriculas = new JButton("Filtrar / Recargar");
        btnCargarMatriculas.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterPanel.add(btnCargarMatriculas);
        filterPanel.add(Box.createVerticalStrut(5));
        btnLimpiarFiltros = new JButton("Limpiar filtros");
        btnLimpiarFiltros.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterPanel.add(btnLimpiarFiltros);
        panel.add(filterPanel, BorderLayout.WEST);

        String[] columnNames = { "Matrícula", "Cód. Curso", "Año", "DNI", "Estudiante", "Grado", "Curso", "Grupos",
                "Estado", "Acción" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == COL_ACCION;
            }
        };
        tabMatriculas = new JTable(model);
        tabMatriculas.setRowHeight(25);
        tabMatriculas.setAutoCreateRowSorter(true);
        tabMatriculas.setFillsViewportHeight(true);

        // El código del curso solo se usa para identificar la fila
        tabMatriculas.getColumnModel().getColumn(COL_CURSO_ID).setMinWidth(0);
        tabMatriculas.getColumnModel().getColumn(COL_CURSO_ID).setMaxWidth(0);
        tabMatriculas.getColumnModel().getColumn(COL_CURSO_ID).setWidth(0);

        setupTableGestionarButton(tabMatriculas, COL_ACCION);
        panel.add(new JScrollPane(tabMatriculas), BorderLayout.CENTER);

        lblResultados = new JLabel(" ");
        lblResultados.setBorder(new EmptyBorder(5, 10, 5, 10));
        panel.add(lblResultados, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * HU 35226: consulta de los grupos (plazas, profesores) y de los alumnos de
     * cada grupo en un año académico.
     */
    private JPanel createGruposPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBorder(new TitledBorder("Filtros de Grupos"));
        filterPanel.setPreferredSize(new Dimension(240, 0));

        addFilterComponent(filterPanel, "Año académico:");
        cbGruposAnio = new JComboBox<>();
        addFilterField(filterPanel, cbGruposAnio);

        addFilterComponent(filterPanel, "Grado:");
        cbGruposGrado = new JComboBox<>(new String[] { "TODOS" });
        addFilterField(filterPanel, cbGruposGrado);

        addFilterComponent(filterPanel, "Curso:");
        cbGruposCurso = new JComboBox<>(new String[] { "TODOS" });
        addFilterField(filterPanel, cbGruposCurso);

        btnCargarGrupos = new JButton("Filtrar / Recargar");
        btnCargarGrupos.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterPanel.add(btnCargarGrupos);
        panel.add(filterPanel, BorderLayout.WEST);

        String[] columnasGrupos = { "Cód. Curso", "Curso", "Grupo", "Tipo", "Grados", "Profesores", "Capacidad",
                "Ocupadas", "Libres" };
        tabGrupos = new JTable(new DefaultTableModel(columnasGrupos, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        tabGrupos.setRowHeight(22);
        tabGrupos.setAutoCreateRowSorter(true);
        tabGrupos.setFillsViewportHeight(true);
        tabGrupos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabGrupos.getColumnModel().getColumn(COL_GRUPO_CURSO_ID).setMinWidth(0);
        tabGrupos.getColumnModel().getColumn(COL_GRUPO_CURSO_ID).setMaxWidth(0);
        tabGrupos.getColumnModel().getColumn(COL_GRUPO_CURSO_ID).setWidth(0);

        String[] columnasAlumnos = { "Matrícula", "DNI", "Estudiante", "Grado", "Estado" };
        tabAlumnosGrupo = new JTable(new DefaultTableModel(columnasAlumnos, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        tabAlumnosGrupo.setRowHeight(22);
        tabAlumnosGrupo.setAutoCreateRowSorter(true);
        tabAlumnosGrupo.setFillsViewportHeight(true);

        scrollAlumnosGrupo = new JScrollPane(tabAlumnosGrupo);
        scrollAlumnosGrupo.setBorder(new TitledBorder("Alumnos del grupo (seleccione un grupo)"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tabGrupos), scrollAlumnosGrupo);
        split.setResizeWeight(0.6);
        panel.add(split, BorderLayout.CENTER);

        lblResultadosGrupos = new JLabel(" ");
        lblResultadosGrupos.setBorder(new EmptyBorder(5, 10, 5, 10));
        panel.add(lblResultadosGrupos, BorderLayout.SOUTH);

        return panel;
    }

    /** Cambia el título de la lista de alumnos del grupo seleccionado. */
    public void setTituloAlumnosGrupo(String titulo) {
        scrollAlumnosGrupo.setBorder(new TitledBorder(titulo));
    }

    private JPanel createDetallePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 40, 20, 40));

        JPanel infoPanel = new JPanel(new GridLayout(11, 2, 10, 8));
        infoPanel.setBorder(new TitledBorder("Información de la Matrícula"));

        lblDetalleId = new JLabel("-");
        lblDetalleAnio = new JLabel("-");
        lblDetalleEstudiante = new JLabel("-");
        lblDetalleEmail = new JLabel("-");
        lblDetalleGrado = new JLabel("-");
        lblDetalleCurso = new JLabel("-");
        lblDetalleGrupos = new JLabel("-");
        lblDetalleEstado = new JLabel("-");
        lblDetalleEstado.setFont(new Font("Tahoma", Font.BOLD, 12));
        lblDetalleEstadoMatricula = new JLabel("-");
        lblDetallePago = new JLabel("-");
        lblDetalleCancelacion = new JLabel("-");

        infoPanel.add(new JLabel("Matrícula:"));
        infoPanel.add(lblDetalleId);
        infoPanel.add(new JLabel("Año académico:"));
        infoPanel.add(lblDetalleAnio);
        infoPanel.add(new JLabel("Estudiante:"));
        infoPanel.add(lblDetalleEstudiante);
        infoPanel.add(new JLabel("Correo electrónico:"));
        infoPanel.add(lblDetalleEmail);
        infoPanel.add(new JLabel("Grado:"));
        infoPanel.add(lblDetalleGrado);
        infoPanel.add(new JLabel("Curso:"));
        infoPanel.add(lblDetalleCurso);
        infoPanel.add(new JLabel("Grupos asignados:"));
        infoPanel.add(lblDetalleGrupos);
        infoPanel.add(new JLabel("Estado del curso:"));
        infoPanel.add(lblDetalleEstado);
        infoPanel.add(new JLabel("Estado de la matrícula:"));
        infoPanel.add(lblDetalleEstadoMatricula);
        infoPanel.add(new JLabel("Pago:"));
        infoPanel.add(lblDetallePago);
        infoPanel.add(new JLabel("Cancelación:"));
        infoPanel.add(lblDetalleCancelacion);

        panel.add(infoPanel, BorderLayout.NORTH);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        actionsPanel.setBorder(new TitledBorder("Gestión del ciclo de vida"));
        btnAceptarSolicitud = new JButton("Aceptar Solicitud (registrar pago)");
        btnAceptarSolicitud.setBackground(new Color(144, 238, 144));

        btnModificarMatricula = new JButton("Modificar Matrícula");

        btnAsignarGrupos = new JButton("Asignar Grupos");

        btnCancelarMatricula = new JButton("Cancelar Matrícula");
        btnCancelarMatricula.setBackground(new Color(255, 182, 193));

        actionsPanel.add(btnAceptarSolicitud);
        actionsPanel.add(btnModificarMatricula);
        actionsPanel.add(btnAsignarGrupos);
        actionsPanel.add(btnCancelarMatricula);

        panel.add(actionsPanel, BorderLayout.CENTER);
        return panel;
    }

    private void addFilterComponent(JPanel panel, String labelText) {
        JLabel label = new JLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
    }

    private void addFilterField(JPanel panel, JComponent field) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(field);
        panel.add(Box.createVerticalStrut(10));
    }

    public void showListadoView() {
        cardLayout.show(cardsPanel, "LISTADO");
        btnVolver.setEnabled(false);
        lblTituloCabecera.setText("Secretaría: Consulta de Matrículas y Grupos");
    }

    /**
     * Muestra el detalle de una matrícula. Los botones solo se habilitan en los
     * estados en los que la acción es válida.
     */
    public void showDetalleView(String id, String anio, String estudiante, String email, String grado, String curso,
            String grupos, String estado, String estadoMatricula, String pago, String cancelacion) {
        lblDetalleId.setText(id);
        lblDetalleAnio.setText(anio);
        lblDetalleEstudiante.setText(estudiante);
        lblDetalleEmail.setText(email);
        lblDetalleGrado.setText(grado);
        lblDetalleCurso.setText(curso);
        lblDetalleGrupos.setText(grupos);
        lblDetalleEstado.setText(estado);
        lblDetalleEstadoMatricula.setText(estadoMatricula);
        lblDetallePago.setText(pago);
        lblDetalleCancelacion.setText(cancelacion);

        boolean cerrada = "CANCELADA".equals(estado) || "FINALIZADA".equals(estado);
        btnAceptarSolicitud.setEnabled("SOLICITADA".equals(estado));
        btnModificarMatricula.setEnabled(!cerrada);
        btnAsignarGrupos.setEnabled("PAGADA".equals(estado) || "ACTIVA".equals(estado));
        btnCancelarMatricula.setEnabled(!cerrada);

        cardLayout.show(cardsPanel, "DETALLE");
        btnVolver.setEnabled(true);
        lblTituloCabecera.setText("Secretaría: Gestión Ciclo de Vida");
    }

    private void setupTableGestionarButton(JTable table, int columnIdx) {
        table.getColumnModel().getColumn(columnIdx).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(columnIdx).setCellEditor(new ButtonEditor(new JCheckBox()));
    }

    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            setText("Ver / Gestionar");
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private boolean isPushed;
        private JTable table;
        private int row;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                int column) {
            this.table = table;
            this.row = row;
            button.setText("Ver / Gestionar");
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed && row >= 0 && row < table.getRowCount()) {
                // Se lee del modelo para que no influya ordenar filas ni mover columnas
                int fila = table.convertRowIndexToModel(row);
                idMatriculaSeleccionada = table.getModel().getValueAt(fila, COL_MATRICULA).toString();
                cursoIdSeleccionado = table.getModel().getValueAt(fila, COL_CURSO_ID).toString();
                // Se ejecuta tras terminar la edicion para no modificar la tabla mientras se
                // edita
                SwingUtilities.invokeLater(onGestionar);
            }
            isPushed = false;
            return "Ver / Gestionar";
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    public JFrame getFrame() {
        return frame;
    }

    public JButton getBtnVolverInicio() {
        return btnVolverInicio;
    }

    public JButton getBtnCargarMatriculas() {
        return btnCargarMatriculas;
    }

    public JButton getBtnLimpiarFiltros() {
        return btnLimpiarFiltros;
    }

    public JLabel getLblResultados() {
        return lblResultados;
    }

    public JTable getTabMatriculas() {
        return tabMatriculas;
    }

    public JComboBox<String> getCbFiltroEstado() {
        return cbFiltroEstado;
    }

    public JComboBox<String> getCbFiltroSituacion() {
        return cbFiltroSituacion;
    }

    public JButton getBtnAsignarGrupos() {
        return btnAsignarGrupos;
    }

    public JComboBox<String> getCbFiltroAnio() {
        return cbFiltroAnio;
    }

    public JComboBox<String> getCbFiltroGrado() {
        return cbFiltroGrado;
    }

    public JComboBox<String> getCbFiltroCurso() {
        return cbFiltroCurso;
    }

    public JTextField getTxtFiltroGrupo() {
        return txtFiltroGrupo;
    }

    public JTextField getTxtFiltroEstudiante() {
        return txtFiltroEstudiante;
    }

    public JButton getBtnAceptarSolicitud() {
        return btnAceptarSolicitud;
    }

    public JButton getBtnModificarMatricula() {
        return btnModificarMatricula;
    }

    public JButton getBtnCancelarMatricula() {
        return btnCancelarMatricula;
    }

    public JComboBox<String> getCbGruposAnio() {
        return cbGruposAnio;
    }

    public JComboBox<String> getCbGruposGrado() {
        return cbGruposGrado;
    }

    public JComboBox<String> getCbGruposCurso() {
        return cbGruposCurso;
    }

    public JButton getBtnCargarGrupos() {
        return btnCargarGrupos;
    }

    public JTable getTabGrupos() {
        return tabGrupos;
    }

    public JTable getTabAlumnosGrupo() {
        return tabAlumnosGrupo;
    }

    public JLabel getLblResultadosGrupos() {
        return lblResultadosGrupos;
    }

    public String getIdMatriculaSeleccionada() {
        return idMatriculaSeleccionada;
    }

    public String getCursoIdSeleccionado() {
        return cursoIdSeleccionado;
    }

    public void setOnGestionar(Runnable onGestionar) {
        this.onGestionar = onGestionar;
    }
}
