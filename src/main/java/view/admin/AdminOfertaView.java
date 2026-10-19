package view.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import model.grado.GradoDisplayDTO;
import model.curso.CursoDisplayDTO;
import model.matriculacion.MatriculacionDisplayDTO;

public class AdminOfertaView {
    private JFrame frame;

    // Top components
    private JLabel lblAnoAcademico;
    private JLabel lblPeriodoMatriculacion;
    private JButton btnNuevoAnoAcademico;
    private JButton btnVolver;
    private JButton btnVolverInicio;

    // Card Layout
    private JPanel cardsPanel;
    private CardLayout cardLayout;

    // -- GRADOS VIEW --
    private JTextField txtFiltroFacultadGrado;
    private JTextField txtFiltroRamaGrado;

    private JComboBox<String> cbEstadoGrado;
    private JTable tabGrados;
    private JButton btnAnadirGrado;

    // -- CURSOS VIEW --
    private JTextField txtFiltroAnoCurso;
    private JTextField txtFiltroItinerarioCurso;
    private JTextField txtFiltroProfesorCurso;
    private JComboBox<String> cbFiltroEstadoCurso;
    private JTable tabCursos;
    private JLabel lblTituloCursos;
    private JButton btnAnadirCurso;
    private JButton btnModificarGrado;
    private JButton btnEliminarGrado;

    // -- MATRICULACION VIEW --
    private JTextField txtFiltroAlumno;
    private JTable tabMatriculacion;
    private JLabel lblTituloMatriculas;
    private JButton btnModificarCurso;
    private JButton btnEliminarCurso;

    // State for back navigation
    private String currentView = "GRADOS";

    // Callbacks de navegación para el controlador
    private BiConsumer<String, String> onVerGrado;
    private BiConsumer<String, String> onVerCurso;

    public AdminOfertaView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Panel de Administrador - Oferta Académica");
        frame.setBounds(100, 100, 1000, 650);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        // --- TOP PANEL ---
        JPanel topContainer = new JPanel(new BorderLayout());

        // Dates panel
        JPanel datesPanel = new JPanel(new GridLayout(2, 1));
        datesPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        lblAnoAcademico = new JLabel("Año Académico: --/--/-- - --/--/--");
        lblAnoAcademico.setFont(new Font("Tahoma", Font.BOLD, 12));
        lblPeriodoMatriculacion = new JLabel("Matriculación: --/--/-- - --/--/--");
        lblPeriodoMatriculacion.setFont(new Font("Tahoma", Font.BOLD, 12));
        datesPanel.add(lblAnoAcademico);
        datesPanel.add(lblPeriodoMatriculacion);

        // Buttons panel (Left to right: Configurar Nuevo Año Académico y volver)
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnNuevoAnoAcademico = new JButton("Configurar Nuevo Año Académico");
        btnVolver = new JButton("Volver");
        btnVolver.setEnabled(false);

        buttonsPanel.add(btnNuevoAnoAcademico);
        buttonsPanel.add(btnVolver);

        btnVolverInicio = new JButton("Volver al inicio");
        btnVolverInicio.setToolTipText("Volver a la pantalla de selección de rol");
        buttonsPanel.add(btnVolverInicio);

        topContainer.add(datesPanel, BorderLayout.WEST);
        topContainer.add(buttonsPanel, BorderLayout.EAST);

        frame.getContentPane().add(topContainer, BorderLayout.NORTH);

        // --- CARDS PANEL ---
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);

        cardsPanel.add(createGradosPanel(), "GRADOS");
        cardsPanel.add(createCursosPanel(), "CURSOS");
        cardsPanel.add(createMatriculacionPanel(), "MATRICULACION");

        frame.getContentPane().add(cardsPanel, BorderLayout.CENTER);

        // Navigation actions
        btnVolver.addActionListener(e -> goBack());
    }

    private JPanel createGradosPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel lblTitle = new JLabel("Vista general grados - Administrador");
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblTitle.setBorder(new EmptyBorder(5, 10, 5, 10));
        panel.add(lblTitle, BorderLayout.NORTH);

        // Filters
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBorder(new TitledBorder("Filtros"));
        filterPanel.setPreferredSize(new Dimension(200, 0));

        addFilterComponent(filterPanel, "Facultad");
        txtFiltroFacultadGrado = new JTextField();
        txtFiltroFacultadGrado.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        filterPanel.add(txtFiltroFacultadGrado);

        filterPanel.add(Box.createVerticalStrut(10));
        addFilterComponent(filterPanel, "Rama de conocimiento");
        txtFiltroRamaGrado = new JTextField();
        txtFiltroRamaGrado.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        filterPanel.add(txtFiltroRamaGrado);

        filterPanel.add(Box.createVerticalStrut(10));
        addFilterComponent(filterPanel, "Estado");
        cbEstadoGrado = new JComboBox<>(new String[] { "TODOS", "VIGENTE", "PENDIENTE_RENOVACION", "EN_EXTINCION" });
        cbEstadoGrado.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        filterPanel.add(cbEstadoGrado);

        panel.add(filterPanel, BorderLayout.WEST);

        // Table
        String[] columnNames = { "Código", "Nombre", "Facultad", "Rama", "Estado", "Acciones" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Solo la columna del botón 'Ver' es interactuable
            }
        };
        tabGrados = new JTable(model);
        actualizarTablaGrados(new ArrayList<>());

        panel.add(new JScrollPane(tabGrados), BorderLayout.CENTER);

        JPanel accionesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnAnadirGrado = new JButton("Añadir grado");
        accionesPanel.add(btnAnadirGrado);
        panel.add(accionesPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createCursosPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        lblTituloCursos = new JLabel("Vista grado NombreGrado - Administrador");
        lblTituloCursos.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblTituloCursos.setBorder(new EmptyBorder(5, 10, 5, 10));
        panel.add(lblTituloCursos, BorderLayout.NORTH);

        // Filters
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBorder(new TitledBorder("Filtros"));
        filterPanel.setPreferredSize(new Dimension(200, 0));

        addFilterComponent(filterPanel, "Año");
        txtFiltroAnoCurso = new JTextField();
        txtFiltroAnoCurso.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        filterPanel.add(txtFiltroAnoCurso);

        filterPanel.add(Box.createVerticalStrut(10));
        addFilterComponent(filterPanel, "Itinerario");
        txtFiltroItinerarioCurso = new JTextField();
        txtFiltroItinerarioCurso.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        filterPanel.add(txtFiltroItinerarioCurso);

        filterPanel.add(Box.createVerticalStrut(10));
        addFilterComponent(filterPanel, "Profesor");
        txtFiltroProfesorCurso = new JTextField();
        txtFiltroProfesorCurso.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        filterPanel.add(txtFiltroProfesorCurso);

        filterPanel.add(Box.createVerticalStrut(10));
        addFilterComponent(filterPanel, "Estado");
        cbFiltroEstadoCurso = new JComboBox<>(new String[] { "TODOS", "ACTIVO", "INACTIVO" });
        cbFiltroEstadoCurso.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        filterPanel.add(cbFiltroEstadoCurso);

        panel.add(filterPanel, BorderLayout.WEST);

        // Table
        String[] columnNames = { "Código", "NombreCurso", "Año", "Itinerario", "Profesor", "Estado", "Acciones" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6;
            }
        };
        tabCursos = new JTable(model);
        actualizarTablaCursos(new ArrayList<>());

        panel.add(new JScrollPane(tabCursos), BorderLayout.CENTER);

        JPanel accionesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnAnadirCurso = new JButton("Añadir curso");
        btnModificarGrado = new JButton("Modificar grado");
        btnEliminarGrado = new JButton("Eliminar grado");
        accionesPanel.add(btnAnadirCurso);
        accionesPanel.add(btnModificarGrado);
        accionesPanel.add(btnEliminarGrado);
        panel.add(accionesPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createMatriculacionPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        lblTituloMatriculas = new JLabel("Vista matriculacion curso NombreCurso - Administrador");
        lblTituloMatriculas.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblTituloMatriculas.setBorder(new EmptyBorder(5, 10, 5, 10));
        panel.add(lblTituloMatriculas, BorderLayout.NORTH);

        // Filters
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBorder(new TitledBorder("Filtros"));
        filterPanel.setPreferredSize(new Dimension(200, 0));

        addFilterComponent(filterPanel, "Alumno (Código/Nombre)");
        txtFiltroAlumno = new JTextField();
        txtFiltroAlumno.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        filterPanel.add(txtFiltroAlumno);

        panel.add(filterPanel, BorderLayout.WEST);

        // Table
        String[] columnNames = { "Código", "NombreAlumno" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabMatriculacion = new JTable(model);

        panel.add(new JScrollPane(tabMatriculacion), BorderLayout.CENTER);

        JPanel accionesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnModificarCurso = new JButton("Modificar curso");
        btnEliminarCurso = new JButton("Eliminar curso");
        accionesPanel.add(btnModificarCurso);
        accionesPanel.add(btnEliminarCurso);
        panel.add(accionesPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void addFilterComponent(JPanel panel, String labelText) {
        JLabel label = new JLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
    }

    // --- Navigation Methods ---

    public void showGradosView() {
        cardLayout.show(cardsPanel, "GRADOS");
        currentView = "GRADOS";
        btnVolver.setEnabled(false);
    }

    public void showCursosView(String nombreGrado) {
        lblTituloCursos.setText("Vista grado " + nombreGrado + " - Administrador");
        cardLayout.show(cardsPanel, "CURSOS");
        currentView = "CURSOS";
        btnVolver.setEnabled(true);
    }

    public void showMatriculacionView(String nombreCurso) {
        lblTituloMatriculas.setText("Vista matriculacion curso " + nombreCurso + " - Administrador");
        cardLayout.show(cardsPanel, "MATRICULACION");
        currentView = "MATRICULACION";
        btnVolver.setEnabled(true);
    }

    private void goBack() {
        if ("MATRICULACION".equals(currentView)) {
            cardLayout.show(cardsPanel, "CURSOS");
            currentView = "CURSOS";
        } else if ("CURSOS".equals(currentView)) {
            showGradosView();
        }
    }

    // --- Button Column in JTable utility ---

    public void setupTableVerButton(JTable table, int columnIdx, String viewType) {
        table.getColumnModel().getColumn(columnIdx).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(columnIdx).setCellEditor(new ButtonEditor(new JCheckBox(), viewType));
    }

    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            setText("Ver");
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private JTable table;
        private String viewType;

        public ButtonEditor(JCheckBox checkBox, String viewType) {
            super(checkBox);
            this.viewType = viewType;
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                int column) {
            this.table = table;
            label = "Ver";
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    if ("GRADOS".equals(viewType)) {
                        String idGrado = table.getValueAt(row, 0) != null ? table.getValueAt(row, 0).toString() : "";
                        String nombreGrado = table.getValueAt(row, 1) != null ? table.getValueAt(row, 1).toString()
                                : "";
                        showCursosView(nombreGrado);
                        if (onVerGrado != null) {
                            onVerGrado.accept(idGrado, nombreGrado);
                        }
                    } else if ("CURSOS".equals(viewType)) {
                        String idCurso = table.getValueAt(row, 0) != null ? table.getValueAt(row, 0).toString() : "";
                        String nombreCurso = table.getValueAt(row, 1) != null ? table.getValueAt(row, 1).toString()
                                : "";
                        showMatriculacionView(nombreCurso);
                        if (onVerCurso != null) {
                            onVerCurso.accept(idCurso, nombreCurso);
                        }
                    }
                }
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    // --- Métodos de actualización de tablas con DTOs ---

    public void actualizarTablaGrados(List<GradoDisplayDTO> grados) {
        DefaultTableModel model = (DefaultTableModel) tabGrados.getModel();
        model.setRowCount(0);
        if (grados != null) {
            for (GradoDisplayDTO g : grados) {
                model.addRow(new Object[] {
                        g.getId(),
                        g.getNombre(),
                        g.getFacultad(),
                        g.getRama(),
                        g.getEstado(),
                        "Ver"
                });
            }
        }
        setupTableVerButton(tabGrados, 5, "GRADOS");
    }

    public void actualizarTablaCursos(List<CursoDisplayDTO> cursos) {
        DefaultTableModel model = (DefaultTableModel) tabCursos.getModel();
        model.setRowCount(0);
        if (cursos != null) {
            for (CursoDisplayDTO c : cursos) {
                model.addRow(new Object[] {
                        c.getId(),
                        c.getNombre(),
                        c.getAno(),
                        c.getItinerario(),
                        c.getProfesor(),
                        c.getEstado(),
                        "Ver"
                });
            }
        }
        setupTableVerButton(tabCursos, 6, "CURSOS");
    }

    public void actualizarTablaMatriculacion(List<? extends MatriculacionDisplayDTO> matriculas) {
        DefaultTableModel model = (DefaultTableModel) tabMatriculacion.getModel();
        model.setRowCount(0);
        if (matriculas != null) {
            for (MatriculacionDisplayDTO m : matriculas) {
                model.addRow(new Object[] {
                        m.getId(),
                        m.getNombreAlumno()
                });
            }
        }
    }

    // --- Callbacks y Getters para controlador ---

    public void setOnVerGrado(BiConsumer<String, String> onVerGrado) {
        this.onVerGrado = onVerGrado;
    }

    public void setOnVerCurso(BiConsumer<String, String> onVerCurso) {
        this.onVerCurso = onVerCurso;
    }

    public JLabel getLblTituloCursos() {
        return lblTituloCursos;
    }

    public JButton getBtnVolverInicio() { 
        return btnVolverInicio;
    }

    public JLabel getLblTituloMatriculas() {
        return lblTituloMatriculas;
    }

    public String getCurrentView() {
        return currentView;
    }

    public JFrame getFrame() {
        return frame;
    }

    public JTable getTabGrados() {
        return tabGrados;
    }

    public JTextField getTxtFiltroFacultadGrado() {
        return txtFiltroFacultadGrado;
    }

    public JTextField getTxtFiltroRamaGrado() {
        return txtFiltroRamaGrado;
    }

    public JComboBox<String> getCbEstadoGrado() {
        return cbEstadoGrado;
    }

    public JTable getTabCursos() {
        return tabCursos;
    }

    public JTextField getTxtFiltroAnoCurso() {
        return txtFiltroAnoCurso;
    }

    public JTextField getTxtFiltroItinerarioCurso() {
        return txtFiltroItinerarioCurso;
    }

    public JTextField getTxtFiltroProfesorCurso() {
        return txtFiltroProfesorCurso;
    }

    public JComboBox<String> getCbFiltroEstadoCurso() {
        return cbFiltroEstadoCurso;
    }

    public JTable getTabMatriculacion() {
        return tabMatriculacion;
    }

    public JTextField getTxtFiltroAlumno() {
        return txtFiltroAlumno;
    }

    public JButton getBtnVolver() {
        return btnVolver;
    }

    public JButton getBtnAnadirGrado() {
        return btnAnadirGrado;
    }

    public JButton getBtnAnadirCurso() {
        return btnAnadirCurso;
    }

    public JButton getBtnModificarGrado() {
        return btnModificarGrado;
    }

    public JButton getBtnEliminarGrado() {
        return btnEliminarGrado;
    }

    public JButton getBtnModificarCurso() {
        return btnModificarCurso;
    }

    public JButton getBtnEliminarCurso() {
        return btnEliminarCurso;
    }

    public JButton getBtnNuevoAnoAcademico() {
        return btnNuevoAnoAcademico;
    }

    public JLabel getLblAnoAcademico() {
        return lblAnoAcademico;
    }

    public JLabel getLblPeriodoMatriculacion() {
        return lblPeriodoMatriculacion;
    }

    public void actualizarAnoAcademico(String displayFechas) {
        lblAnoAcademico.setText("Año Académico: " + displayFechas);
    }

    public void actualizarPeriodoMatriculacion(String displayFechas) {
        lblPeriodoMatriculacion.setText("Matriculación: " + displayFechas);
    }
}