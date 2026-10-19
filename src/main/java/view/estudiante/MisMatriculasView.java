package view.estudiante;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import view.comun.Estilo;

/**
 * Vista para la consulta de matrículas del estudiante (HU 35232).
 * Integra:
 * - Filtro de grado consultado (soporte de dobles grados).
 * - Ordenación de asignaturas matriculadas (por curso y nº de matrícula).
 * - Indicadores visuales de Hitos de Permanencia (créditos anuales vs 12 y
 * acumulados vs 90).
 * - Historial detallado de calificaciones obtenidas en las convocatorias
 * presentadas.
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
    private JPanel panelPrincipal;

    public MisMatriculasView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Mis Matrículas — Expediente y Calificaciones");
        frame.setBounds(150, 100, 1000, 680);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        panelPrincipal = Estilo.contenido(new BorderLayout(0, 12));

        // =========================================================================
        // Panel superior: Título, selector de grado e Hitos de Permanencia
        // =========================================================================
        btnVolver = Estilo.botonSecundario("‹  Atrás");
        btnVolver.setToolTipText("Volver al menú del estudiante");
        JPanel panelNorte = Estilo.cabecera("MI EXPEDIENTE", new JLabel("Listado de mis matrículas y expediente"),
                btnVolver);

        JPanel panelDatos = new JPanel(new BorderLayout(0, 12));
        panelDatos.setOpaque(false);
        panelDatos.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel panelFiltro = Estilo.tarjeta(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelFiltro.setBorder(Estilo.bordeTarjeta(10, 14));
        panelFiltro.add(Estilo.etiqueta("Grado consultado"));
        cbGrados = new JComboBox<>();
        Estilo.estilizarCampo(cbGrados);
        cbGrados.setPreferredSize(new Dimension(520, 30));
        panelFiltro.add(cbGrados);
        panelDatos.add(panelFiltro, BorderLayout.NORTH);

        // Panel de Hitos de Permanencia (HU 35232 - Paso 3)
        JPanel panelPermanencia = Estilo.tarjeta(new BorderLayout(0, 10));
        panelPermanencia.add(Estilo.tituloTarjeta("Hitos del Régimen de Permanencia"), BorderLayout.NORTH);
        JPanel panelHitos = new JPanel(new GridLayout(1, 2, 12, 0));
        panelHitos.setOpaque(false);
        lblHitoAnual = etiquetaHito("Mínimo anual: Calculando...");
        panelHitos.add(lblHitoAnual);
        lblHitoTotal = etiquetaHito("Mínimo al 3er año: Calculando...");
        panelHitos.add(lblHitoTotal);
        panelPermanencia.add(panelHitos, BorderLayout.CENTER);
        panelDatos.add(panelPermanencia, BorderLayout.CENTER);

        panelNorte.add(panelDatos, BorderLayout.SOUTH);
        panelPrincipal.add(panelNorte, BorderLayout.NORTH);

        // =========================================================================
        // Panel Central con Pestañas: Asignaturas y Calificaciones
        // =========================================================================
        tabbedPane = new JTabbedPane();
        Estilo.estilizarPestanas(tabbedPane);

        // --- Pestaña 1: Asignaturas Matriculadas ---
        String[] colMatriculas = { "Año Académico", "Curso", "Asignatura", "Nº Matrícula", "Estado" };
        tableModel = new DefaultTableModel(colMatriculas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableMatriculas = Estilo.tablaQueLlena(tableModel);
        Estilo.estilizarTabla(tableMatriculas);
        tableMatriculas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableMatriculas.getTableHeader().setReorderingAllowed(false);
        centrar(tableMatriculas, 0, 1, 3);
        tableMatriculas.getColumnModel().getColumn(4).setCellRenderer(new Estilo.RendererEstado());

        btnVerNotasSeleccionada = Estilo.botonPrimario("Ver Calificaciones de la Asignatura");
        tabbedPane.addTab("  Asignaturas Matriculadas  ", crearPestanaTabla("Asignaturas matriculadas",
                "Seleccione una asignatura para ver sus calificaciones", tableMatriculas, btnVerNotasSeleccionada));

        // --- Pestaña 2: Convocatorias y Calificaciones (Paso 4) ---
        String[] colCalificaciones = { "Año Académico", "Código", "Asignatura", "Convocatoria", "Calificación",
                "Resultado" };
        tableModelCalificaciones = new DefaultTableModel(colCalificaciones, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableCalificaciones = Estilo.tablaQueLlena(tableModelCalificaciones);
        Estilo.estilizarTabla(tableCalificaciones);
        tableCalificaciones.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableCalificaciones.getTableHeader().setReorderingAllowed(false);
        centrar(tableCalificaciones, 0, 1, 4);
        tableCalificaciones.getColumnModel().getColumn(5).setCellRenderer(new Estilo.RendererEstado());

        btnVerTodasNotas = Estilo.botonSecundario("Actualizar Todas las Calificaciones");
        tabbedPane.addTab("  Calificaciones por Convocatoria  ", crearPestanaTabla("Calificaciones por convocatoria",
                "Convocatorias presentadas en el grado consultado", tableCalificaciones, btnVerTodasNotas));

        panelPrincipal.add(tabbedPane, BorderLayout.CENTER);

        frame.getContentPane().add(panelPrincipal, BorderLayout.CENTER);
    }

    /** Recuadro gris claro con borde para cada hito (el controlador escribe su texto con colores). */
    private static JLabel etiquetaHito(String texto) {
        JLabel label = new JLabel(texto, SwingConstants.CENTER);
        label.setFont(new Font("Tahoma", Font.PLAIN, 13));
        label.setForeground(Estilo.TEXTO);
        label.setOpaque(true);
        label.setBackground(Estilo.FILA_ALTERNA);
        label.setBorder(new CompoundBorder(new LineBorder(Estilo.BORDE, 1, true), new EmptyBorder(10, 12, 10, 12)));
        return label;
    }

    /** Contenido de una pestaña: tarjeta con título, la tabla y el botón de acción abajo a la derecha. */
    private static JPanel crearPestanaTabla(String titulo, String ayuda, JTable tabla, JButton accion) {
        JPanel pestana = new JPanel(new BorderLayout());
        pestana.setBackground(Estilo.FONDO);
        pestana.setBorder(new EmptyBorder(12, 2, 2, 2));

        JPanel tarjeta = Estilo.tarjeta(new BorderLayout(0, 10));
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        cabecera.add(Estilo.tituloTarjeta(titulo), BorderLayout.WEST);
        cabecera.add(Estilo.textoSecundario(ayuda), BorderLayout.EAST);
        tarjeta.add(cabecera, BorderLayout.NORTH);
        tarjeta.add(Estilo.scrollTabla(tabla), BorderLayout.CENTER);
        tarjeta.add(Estilo.barraAcciones(accion), BorderLayout.SOUTH);
        pestana.add(tarjeta, BorderLayout.CENTER);
        return pestana;
    }

    private static void centrar(JTable tabla, int... columnas) {
        for (int columna : columnas) {
            tabla.getColumnModel().getColumn(columna).setCellRenderer(new Estilo.RendererNumero(false));
        }
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
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
