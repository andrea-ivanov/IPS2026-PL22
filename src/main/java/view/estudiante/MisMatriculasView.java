package view.estudiante;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Vista para la consulta de matrículas del estudiante (HU 35232).
 * Integra:
 * - Filtro de grado consultado (soporte de dobles grados).
 * - Ordenación de asignaturas matriculadas (por curso y nº de matrícula).
 * - Indicadores visuales de Hitos de Permanencia (créditos anuales vs 12 y acumulados vs 90).
 * - Historial detallado de calificaciones obtenidas en las convocatorias presentadas.
 */
public class MisMatriculasView {
    private JFrame frame;
    private JComboBox<Object> cbGrados;
    
    // Tabla de asignaturas matriculadas
    private JTable tableMatriculas;
    private DefaultTableModel tableModel;
    
    // Hitos de permanencia
    private JLabel lblHitoAnual;
    private JLabel lblHitoTotal;
    
    // Pestañas y calificaciones
    private JTabbedPane tabbedPane;
    private JTable tableCalificaciones;
    private DefaultTableModel tableModelCalificaciones;
    private JButton btnVerNotasSeleccionada;
    private JButton btnVerTodasNotas;
    
    private JButton btnVolver;

    public MisMatriculasView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Mis Matrículas — Expediente y Calificaciones");
        frame.setBounds(150, 100, 860, 580);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(10, 10));

        // =========================================================================
        // Panel superior: Título, selector de grado e Hitos de Permanencia
        // =========================================================================
        JPanel panelNorte = new JPanel(new BorderLayout(5, 5));
        panelNorte.setBorder(new EmptyBorder(10, 10, 0, 10));

        JLabel lblTitle = new JLabel("Listado de mis matrículas y expediente", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 17));
        panelNorte.add(lblTitle, BorderLayout.NORTH);

        JPanel panelFiltro = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JLabel lblGrado = new JLabel("Grado consultado:");
        lblGrado.setFont(new Font("Tahoma", Font.BOLD, 12));
        panelFiltro.add(lblGrado);

        cbGrados = new JComboBox<>();
        cbGrados.setPreferredSize(new Dimension(450, 26));
        panelFiltro.add(cbGrados);
        panelNorte.add(panelFiltro, BorderLayout.CENTER);

        // Panel de Hitos de Permanencia (HU 35232 - Paso 3)
        JPanel panelPermanencia = new JPanel(new GridLayout(1, 2, 10, 5));
        panelPermanencia.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Hitos del Régimen de Permanencia",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Tahoma", Font.BOLD, 12)));

        lblHitoAnual = new JLabel("Mínimo anual: Calculando...", SwingConstants.CENTER);
        lblHitoAnual.setFont(new Font("Tahoma", Font.PLAIN, 12));
        panelPermanencia.add(lblHitoAnual);

        lblHitoTotal = new JLabel("Mínimo al 3er año: Calculando...", SwingConstants.CENTER);
        lblHitoTotal.setFont(new Font("Tahoma", Font.PLAIN, 12));
        panelPermanencia.add(lblHitoTotal);

        panelNorte.add(panelPermanencia, BorderLayout.SOUTH);
        frame.getContentPane().add(panelNorte, BorderLayout.NORTH);

        // =========================================================================
        // Panel Central con Pestañas: Asignaturas y Calificaciones
        // =========================================================================
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Tahoma", Font.PLAIN, 12));

        // --- Pestaña 1: Asignaturas Matriculadas ---
        JPanel panelTabMatriculas = new JPanel(new BorderLayout(5, 5));
        panelTabMatriculas.setBorder(new EmptyBorder(5, 5, 5, 5));

        String[] colMatriculas = { "Año Académico", "Curso", "Asignatura", "Nº Matrícula", "Estado" };
        tableModel = new DefaultTableModel(colMatriculas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableMatriculas = new JTable(tableModel);
        tableMatriculas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableMatriculas.getTableHeader().setReorderingAllowed(false);
        panelTabMatriculas.add(new JScrollPane(tableMatriculas), BorderLayout.CENTER);

        JPanel panelAccionesMatriculas = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        btnVerNotasSeleccionada = new JButton("Ver Calificaciones de la Asignatura");
        panelAccionesMatriculas.add(btnVerNotasSeleccionada);
        panelTabMatriculas.add(panelAccionesMatriculas, BorderLayout.SOUTH);

        tabbedPane.addTab("Asignaturas Matriculadas", panelTabMatriculas);

        // --- Pestaña 2: Convocatorias y Calificaciones (Paso 4) ---
        JPanel panelTabCalificaciones = new JPanel(new BorderLayout(5, 5));
        panelTabCalificaciones.setBorder(new EmptyBorder(5, 5, 5, 5));

        String[] colCalificaciones = { "Año Académico", "Código", "Asignatura", "Convocatoria", "Calificación", "Resultado" };
        tableModelCalificaciones = new DefaultTableModel(colCalificaciones, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableCalificaciones = new JTable(tableModelCalificaciones);
        tableCalificaciones.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableCalificaciones.getTableHeader().setReorderingAllowed(false);
        panelTabCalificaciones.add(new JScrollPane(tableCalificaciones), BorderLayout.CENTER);

        JPanel panelAccionesCalificaciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        btnVerTodasNotas = new JButton("Actualizar Todas las Calificaciones");
        panelAccionesCalificaciones.add(btnVerTodasNotas);
        panelTabCalificaciones.add(panelAccionesCalificaciones, BorderLayout.SOUTH);

        tabbedPane.addTab("Calificaciones por Convocatoria", panelTabCalificaciones);

        frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);

        // =========================================================================
        // Panel inferior para botón Cerrar
        // =========================================================================
        JPanel panelBottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnVolver = new JButton("Cerrar");
        btnVolver.setPreferredSize(new Dimension(100, 30));
        panelBottom.add(btnVolver);
        frame.getContentPane().add(panelBottom, BorderLayout.SOUTH);
    }

    public JFrame getFrame() {
        return frame;
    }

    public JComboBox<Object> getCbGrados() {
        return cbGrados;
    }

    public JTable getTableMatriculas() {
        return tableMatriculas;
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JLabel getLblHitoAnual() {
        return lblHitoAnual;
    }

    public JLabel getLblHitoTotal() {
        return lblHitoTotal;
    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public JTable getTableCalificaciones() {
        return tableCalificaciones;
    }

    public DefaultTableModel getTableModelCalificaciones() {
        return tableModelCalificaciones;
    }

    public JButton getBtnVerNotasSeleccionada() {
        return btnVerNotasSeleccionada;
    }

    public JButton getBtnVerTodasNotas() {
        return btnVerTodasNotas;
    }

    public JButton getBtnVolver() {
        return btnVolver;
    }
}
