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
    public static final String TAB_FIP_TITULO = "  FIP  ";

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

    // Pestaña de microcredenciales (HU 35987, HU 36410 y HU 36459)
    private JTable tableMicrocredenciales;
    private DefaultTableModel tableModelMicrocredenciales;

    // Pestaña de FIP y contratos (HU 36967 - solo visible si el alumno tiene contrato)
    private JPanel panelPestanaFip;
    private JTable tableContratos;
    private DefaultTableModel tableModelContratos;
    private JTable tableFip;
    private DefaultTableModel tableModelFip;

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

        // --- Pestaña 3: Microcredenciales (HU 35987, HU 36410 y HU 36459) ---
        String[] colMicrocredenciales = { "Año Académico", "Código", "Microcredencial", "Rama", "Créditos",
                "Inicio Impartición", "Fin Impartición", "Estado", "Detalle / Motivo", "Nota" };
        tableModelMicrocredenciales = new DefaultTableModel(colMicrocredenciales, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableMicrocredenciales = Estilo.tablaQueLlena(tableModelMicrocredenciales);
        Estilo.estilizarTabla(tableMicrocredenciales);
        tableMicrocredenciales.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableMicrocredenciales.getTableHeader().setReorderingAllowed(false);
        centrar(tableMicrocredenciales, 0, 1, 4, 5, 6, 9);
        tableMicrocredenciales.getColumnModel().getColumn(7).setCellRenderer(new Estilo.RendererEstado());

        tabbedPane.addTab("  Microcredenciales  ", crearPestanaTabla("Mis microcredenciales",
                "Inscripciones en microcredenciales, su estado (incluyendo cancelaciones por baja demanda) y calificaciones",
                tableMicrocredenciales, null));

        // --- Pestaña 4: FIP y Contratos (HU 36967 - se añade dinámicamente solo si tiene contrato) ---
        panelPestanaFip = crearPestanaFip();

        panelPrincipal.add(tabbedPane, BorderLayout.CENTER);

        frame.getContentPane().add(panelPrincipal, BorderLayout.CENTER);
    }

    /**
     * Construye el contenido de la pestaña "FIP" (HU 36967):
     * - Arriba: tarjeta con el listado de contratos del estudiante con la universidad.
     * - Abajo: tarjeta con las Formaciones Internas del Personal (FIP), sus fechas y resultados ('PENDIENTE', 'APTO', 'NO APTO').
     */
    private JPanel crearPestanaFip() {
        JPanel pestana = new JPanel(new BorderLayout(0, 12));
        pestana.setBackground(Estilo.FONDO);
        pestana.setBorder(new EmptyBorder(12, 2, 2, 2));

        // 1. Tarjeta superior: Mis contratos con la universidad
        String[] colContratos = { "Código", "Descripción", "Fecha Inicio", "Fecha Fin" };
        tableModelContratos = new DefaultTableModel(colContratos, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableContratos = Estilo.tablaQueLlena(tableModelContratos);
        Estilo.estilizarTabla(tableContratos);
        tableContratos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableContratos.getTableHeader().setReorderingAllowed(false);
        centrar(tableContratos, 0, 2, 3);

        JPanel tarjetaContratos = Estilo.tarjeta(new BorderLayout(0, 10));
        JPanel cabeceraContratos = new JPanel(new BorderLayout());
        cabeceraContratos.setOpaque(false);
        cabeceraContratos.add(Estilo.tituloTarjeta("Mis contratos con la universidad"), BorderLayout.WEST);
        cabeceraContratos.add(Estilo.textoSecundario("Historial de contratos registrados en secretaría"),
                BorderLayout.EAST);
        tarjetaContratos.add(cabeceraContratos, BorderLayout.NORTH);
        JScrollPane scrollContratos = Estilo.scrollTabla(tableContratos);
        scrollContratos.setPreferredSize(new Dimension(0, 130));
        tarjetaContratos.add(scrollContratos, BorderLayout.CENTER);

        // 2. Tarjeta inferior: Mis formaciones internas (FIP)
        String[] colFip = { "Código", "Formación (FIP)", "Fecha Inicio", "Fecha Fin", "Resultado" };
        tableModelFip = new DefaultTableModel(colFip, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableFip = Estilo.tablaQueLlena(tableModelFip);
        Estilo.estilizarTabla(tableFip);
        tableFip.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableFip.getTableHeader().setReorderingAllowed(false);
        centrar(tableFip, 0, 2, 3);
        tableFip.getColumnModel().getColumn(4).setCellRenderer(new Estilo.RendererEstado());

        JPanel tarjetaFip = Estilo.tarjeta(new BorderLayout(0, 10));
        JPanel cabeceraFip = new JPanel(new BorderLayout());
        cabeceraFip.setOpaque(false);
        cabeceraFip.add(Estilo.tituloTarjeta("Mis formaciones internas (FIP)"), BorderLayout.WEST);
        cabeceraFip.add(
                Estilo.textoSecundario("Formaciones Internas del Personal en las que estás inscrito y su resultado"),
                BorderLayout.EAST);
        tarjetaFip.add(cabeceraFip, BorderLayout.NORTH);
        tarjetaFip.add(Estilo.scrollTabla(tableFip), BorderLayout.CENTER);

        pestana.add(tarjetaContratos, BorderLayout.NORTH);
        pestana.add(tarjetaFip, BorderLayout.CENTER);
        return pestana;
    }

    /**
     * Muestra u oculta la pestaña "FIP" a la derecha de "Microcredenciales" (HU 36967).
     * Según indicación del PO, si el alumno no tiene ningún contrato con la universidad,
     * en ningún caso debe aparecer la pestaña de FIP.
     */
    public void setMostrarPestanaFip(boolean mostrar) {
        if (tabbedPane == null || panelPestanaFip == null) {
            return;
        }
        int idx = tabbedPane.indexOfComponent(panelPestanaFip);
        if (mostrar && idx < 0) {
            tabbedPane.addTab(TAB_FIP_TITULO, panelPestanaFip);
        } else if (!mostrar && idx >= 0) {
            if (tabbedPane.getSelectedIndex() == idx && tabbedPane.getTabCount() > 1) {
                tabbedPane.setSelectedIndex(0);
            }
            tabbedPane.removeTabAt(idx);
        }
    }

    public boolean isPestanaFipVisible() {
        return tabbedPane != null && panelPestanaFip != null && tabbedPane.indexOfComponent(panelPestanaFip) >= 0;
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
        if (accion != null) {
            tarjeta.add(Estilo.barraAcciones(accion), BorderLayout.SOUTH);
        }
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

    public JTable getTableMicrocredenciales() {
        return tableMicrocredenciales;
    }

    public DefaultTableModel getTableModelMicrocredenciales() {
        return tableModelMicrocredenciales;
    }

    public JPanel getPanelPestanaFip() {
        return panelPestanaFip;
    }

    public JTable getTableContratos() {
        return tableContratos;
    }

    public DefaultTableModel getTableModelContratos() {
        return tableModelContratos;
    }

    public JTable getTableFip() {
        return tableFip;
    }

    public DefaultTableModel getTableModelFip() {
        return tableModelFip;
    }
}
