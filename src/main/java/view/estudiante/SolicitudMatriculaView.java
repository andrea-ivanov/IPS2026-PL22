package view.estudiante;

import model.matriculacion.OfertaCursoDTO;
import model.matriculacion.OfertaGradoDTO;
import model.matriculacion.OfertaMicrocredencialDTO;
import model.matriculacion.ResumenSolicitudDTO;
import model.matriculacion.ResumenSolicitudMicrocredencialDTO;
import view.comun.Estilo;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Vista unificada de Solicitud de Matrícula (HU 35235 y HU 35987).
 * Integra tanto la matrícula en Grados Oficiales como en Microcredenciales Universitarias
 * en una interfaz coherente de 3 pasos (Asistente de Matrícula):
 * - Paso 1: Selector de tipo de matrícula (Grado / Microcredenciales) y tabla de oferta con checkboxes y cálculo en vivo.
 * - Paso 2: Resumen previo consolidado antes de confirmar.
 * - Paso 3: Selección de modalidad de pago (Tarjeta, Transferencia, Carta de pago) y confirmación ("Sí" / "No").
 */
public class SolicitudMatriculaView {
    public static final String PASO_SELECCION = "PASO_SELECCION";
    public static final String PASO_RESUMEN = "PASO_RESUMEN";
    public static final String PASO_PAGO = "PASO_PAGO";

    public static final String MODO_GRADO = "MODO_GRADO";
    public static final String MODO_MICROCREDENCIAL = "MODO_MICROCREDENCIAL";

    private JFrame frame;
    private JPanel panelPrincipal;
    private CardLayout cardLayoutPasos;
    private JPanel panelPasos;

    // Indicador de pasos superior
    private JLabel lblPaso1;
    private JLabel lblPaso2;
    private JLabel lblPaso3;

    // --- Componentes Paso 1: Selector de Modo ---
    private JRadioButton rbModoGrado;
    private JRadioButton rbModoMicrocredencial;
    private ButtonGroup grupoModoMatricula;
    private CardLayout cardLayoutModos;
    private JPanel panelContenidoModos;

    // --- Componentes Paso 1: Modo Grado ---
    private JComboBox<OfertaGradoDTO> cbGrados;
    private JTable tableCursos;
    private DefaultTableModel modelCursos;
    private JLabel lblInfoEstudiante;

    // --- Componentes Paso 1: Modo Microcredenciales ---
    private JLabel lblRamasHabilitadas;
    private JComboBox<String> cbFiltroRama;
    private JTable tableMicrocredenciales;
    private DefaultTableModel modelMicrocredenciales;

    // --- Componentes Paso 1: Totales y Navegación Común ---
    private JLabel lblTotalCreditos;
    private JLabel lblTotalImporte;
    private JLabel lblAvisoTarifa;
    private JButton btnContinuarPaso2;
    private JButton btnVolverMenuDesdePaso1;

    // --- Componentes Paso 2: Resumen Previo Consolidado ---
    private JLabel lblResumenEstudiante;
    private JLabel lblResumenGradoOTipo;
    private JLabel lblResumenAnioOFecha;
    private JTable tableResumen;
    private DefaultTableModel modelResumen;
    private JLabel lblResumenCreditosTotales;
    private JLabel lblResumenImporteTotal;
    private JButton btnModificarSeleccion;
    private JButton btnContinuarPaso3;

    // --- Componentes Paso 3: Modalidad de Pago y Confirmación ---
    private JRadioButton rbTarjeta;
    private JRadioButton rbTransferencia;
    private JRadioButton rbCartaPago;
    private ButtonGroup grupoPago;
    private JTextArea txtInfoMetodoPago;
    private JLabel lblPagoImporteFinal;
    private JButton btnConfirmarMatricula;
    private JButton btnVolverPaso2;

    public SolicitudMatriculaView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Solicitud de Matrícula Universitaria");
        frame.setBounds(120, 80, 960, 680);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(new EmptyBorder(12, 16, 12, 16));
        panelPrincipal.setBackground(new Color(248, 249, 250));

        // 1. Encabezado superior con barra de progreso de pasos
        JPanel panelNorte = crearEncabezadoPasos();
        panelPrincipal.add(panelNorte, BorderLayout.NORTH);

        // 2. Panel central con CardLayout para alternar entre pasos
        cardLayoutPasos = new CardLayout();
        panelPasos = new JPanel(cardLayoutPasos);
        panelPasos.setOpaque(false);

        panelPasos.add(crearPaso1Seleccion(), PASO_SELECCION);
        panelPasos.add(crearPaso2Resumen(), PASO_RESUMEN);
        panelPasos.add(crearPaso3Pago(), PASO_PAGO);

        panelPrincipal.add(panelPasos, BorderLayout.CENTER);
        frame.getContentPane().add(panelPrincipal, BorderLayout.CENTER);
    }

    private JPanel crearEncabezadoPasos() {
        JPanel header = new JPanel(new BorderLayout(5, 8));
        header.setOpaque(false);

        JLabel lblTitulo = new JLabel("Asistente de Matrícula Universitaria", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Tahoma", Font.BOLD, 18));
        lblTitulo.setForeground(new Color(33, 37, 41));
        header.add(lblTitulo, BorderLayout.NORTH);

        // Barra indicadora de pasos
        JPanel barraPasos = new JPanel(new GridLayout(1, 3, 10, 0));
        barraPasos.setOpaque(false);
        barraPasos.setBorder(new EmptyBorder(5, 20, 10, 20));

        lblPaso1 = crearEtiquetaPaso("1. Oferta y Selección", true);
        lblPaso2 = crearEtiquetaPaso("2. Resumen Previo Consolidado", false);
        lblPaso3 = crearEtiquetaPaso("3. Modalidad de Pago y Confirmación", false);

        barraPasos.add(lblPaso1);
        barraPasos.add(lblPaso2);
        barraPasos.add(lblPaso3);

        header.add(barraPasos, BorderLayout.SOUTH);
        return header;
    }

    private JLabel crearEtiquetaPaso(String texto, boolean activo) {
        JLabel lbl = new JLabel(texto, SwingConstants.CENTER);
        lbl.setFont(new Font("Tahoma", Font.BOLD, 12));
        lbl.setOpaque(true);
        configurarEstiloPaso(lbl, activo);
        return lbl;
    }

    private void actualizarBarraPasos(int pasoActivo) {
        configurarEstiloPaso(lblPaso1, pasoActivo == 1);
        configurarEstiloPaso(lblPaso2, pasoActivo == 2);
        configurarEstiloPaso(lblPaso3, pasoActivo == 3);
    }

    private void configurarEstiloPaso(JLabel lbl, boolean activo) {
        if (activo) {
            lbl.setBackground(new Color(13, 110, 253));
            lbl.setForeground(Color.WHITE);
            lbl.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(10, 88, 202), 1, true),
                    new EmptyBorder(6, 10, 6, 10)));
        } else {
            lbl.setBackground(new Color(233, 236, 239));
            lbl.setForeground(new Color(108, 117, 125));
            lbl.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(222, 226, 230), 1, true),
                    new EmptyBorder(6, 10, 6, 10)));
        }
    }

    // =========================================================================
    // PASO 1: SELECCIÓN UNIFICADA (GRADO O MICROCREDENCIALES)
    // =========================================================================
    private JPanel crearPaso1Seleccion() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        // Subpanel norte: Selector de modalidad de matrícula (HU 35987)
        JPanel panelSelectorTipo = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 6));
        panelSelectorTipo.setBackground(Color.WHITE);
        panelSelectorTipo.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(6, 12, 6, 12)));

        JLabel lblTipo = new JLabel("Tipo de Matrícula:");
        lblTipo.setFont(new Font("Tahoma", Font.BOLD, 13));
        panelSelectorTipo.add(lblTipo);

        rbModoGrado = new JRadioButton("Asignaturas de Grado Oficial");
        rbModoGrado.setFont(new Font("Tahoma", Font.BOLD, 13));
        rbModoGrado.setSelected(true);
        rbModoGrado.setOpaque(false);

        rbModoMicrocredencial = new JRadioButton("Microcredenciales de Especialización");
        rbModoMicrocredencial.setFont(new Font("Tahoma", Font.BOLD, 13));
        rbModoMicrocredencial.setOpaque(false);

        grupoModoMatricula = new ButtonGroup();
        grupoModoMatricula.add(rbModoGrado);
        grupoModoMatricula.add(rbModoMicrocredencial);

        panelSelectorTipo.add(rbModoGrado);
        panelSelectorTipo.add(rbModoMicrocredencial);
        panel.add(panelSelectorTipo, BorderLayout.NORTH);

        // Centro: CardLayout para conmutar entre vista de Grado y vista de Microcredenciales
        cardLayoutModos = new CardLayout();
        panelContenidoModos = new JPanel(cardLayoutModos);
        panelContenidoModos.setOpaque(false);

        panelContenidoModos.add(crearPanelModoGrado(), MODO_GRADO);
        panelContenidoModos.add(crearPanelModoMicrocredencial(), MODO_MICROCREDENCIAL);

        panel.add(panelContenidoModos, BorderLayout.CENTER);

        // Subpanel sur: Totales en vivo y botones de navegación
        JPanel panelSur = new JPanel(new BorderLayout(5, 5));
        panelSur.setOpaque(false);

        JPanel panelIndicadores = new JPanel(new BorderLayout(10, 5));
        panelIndicadores.setBackground(Color.WHITE);
        panelIndicadores.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(8, 12, 8, 12)));

        JPanel panelTotales = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        panelTotales.setOpaque(false);

        lblTotalCreditos = new JLabel("Total créditos: 0 ECTS");
        lblTotalCreditos.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblTotalCreditos.setForeground(new Color(13, 110, 253));

        lblTotalImporte = new JLabel("Coste estimado: 0.00 €");
        lblTotalImporte.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblTotalImporte.setForeground(new Color(25, 135, 84));

        panelTotales.add(lblTotalCreditos);
        panelTotales.add(lblTotalImporte);
        panelIndicadores.add(panelTotales, BorderLayout.WEST);

        lblAvisoTarifa = new JLabel("(Tarifa fija oficial: 20,00 € por crédito ECTS)");
        lblAvisoTarifa.setFont(new Font("Tahoma", Font.ITALIC, 11));
        lblAvisoTarifa.setForeground(new Color(108, 117, 125));
        panelIndicadores.add(lblAvisoTarifa, BorderLayout.EAST);

        panelSur.add(panelIndicadores, BorderLayout.NORTH);

        JPanel panelBotones = new JPanel(new BorderLayout());
        panelBotones.setOpaque(false);
        panelBotones.setBorder(new EmptyBorder(6, 0, 0, 0));

        btnVolverMenuDesdePaso1 = Estilo.estilizarSecundario(new JButton("Volver al Menú"));
        btnVolverMenuDesdePaso1.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnVolverMenuDesdePaso1.setPreferredSize(new Dimension(150, 34));

        btnContinuarPaso2 = Estilo.estilizarPrimario(new JButton("Continuar al Resumen  >"));
        btnContinuarPaso2.setFont(new Font("Tahoma", Font.BOLD, 13));
        btnContinuarPaso2.setPreferredSize(new Dimension(220, 36));

        panelBotones.add(btnVolverMenuDesdePaso1, BorderLayout.WEST);
        panelBotones.add(btnContinuarPaso2, BorderLayout.EAST);

        panelSur.add(panelBotones, BorderLayout.SOUTH);
        panel.add(panelSur, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearPanelModoGrado() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);

        // Filtro Grado
        JPanel panelFiltro = new JPanel(new BorderLayout(5, 5));
        panelFiltro.setBackground(Color.WHITE);
        panelFiltro.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(10, 14, 10, 14)));

        JPanel panelCombo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelCombo.setOpaque(false);
        JLabel lblGrado = new JLabel("Grado oficial a matricular:");
        lblGrado.setFont(new Font("Tahoma", Font.BOLD, 13));
        panelCombo.add(lblGrado);

        cbGrados = new JComboBox<>();
        cbGrados.setPreferredSize(new Dimension(520, 30));
        cbGrados.setFont(new Font("Tahoma", Font.PLAIN, 12));
        cbGrados.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof OfertaGradoDTO) {
                    OfertaGradoDTO g = (OfertaGradoDTO) value;
                    setText(g.getGradoId() + " - " + g.getNombre() + " (" + g.getEstado() + ")");
                }
                return this;
            }
        });
        panelCombo.add(cbGrados);
        panelFiltro.add(panelCombo, BorderLayout.NORTH);

        lblInfoEstudiante = new JLabel("Seleccione un grado para consultar las asignaturas disponibles.");
        lblInfoEstudiante.setFont(new Font("Tahoma", Font.PLAIN, 12));
        lblInfoEstudiante.setForeground(new Color(108, 117, 125));
        lblInfoEstudiante.setBorder(new EmptyBorder(4, 5, 0, 0));
        panelFiltro.add(lblInfoEstudiante, BorderLayout.SOUTH);
        panel.add(panelFiltro, BorderLayout.NORTH);

        // Tabla de asignaturas
        JPanel panelTabla = new JPanel(new BorderLayout(5, 5));
        panelTabla.setBackground(Color.WHITE);
        panelTabla.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(8, 8, 8, 8)));

        String[] columnas = { "Seleccionar", "Código", "Asignatura", "Curso", "Itinerario", "Créditos", "Convocatorias", "Precio (€)" };
        modelCursos = new DefaultTableModel(columnas, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                switch (col) {
                    case 0:
                        return Boolean.class;
                    case 3:
                        return Integer.class;
                    case 5:
                        return Double.class;
                    case 6:
                        return Integer.class;
                    default:
                        return String.class;
                }
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0;
            }
        };

        tableCursos = new JTable(modelCursos);
        tableCursos.setRowHeight(25);
        tableCursos.setFont(new Font("Tahoma", Font.PLAIN, 12));
        tableCursos.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 12));
        tableCursos.getTableHeader().setReorderingAllowed(false);

        tableCursos.getColumnModel().getColumn(0).setPreferredWidth(85);
        tableCursos.getColumnModel().getColumn(1).setPreferredWidth(65);
        tableCursos.getColumnModel().getColumn(2).setPreferredWidth(260);
        tableCursos.getColumnModel().getColumn(3).setPreferredWidth(55);
        tableCursos.getColumnModel().getColumn(4).setPreferredWidth(90);
        tableCursos.getColumnModel().getColumn(5).setPreferredWidth(65);
        tableCursos.getColumnModel().getColumn(6).setPreferredWidth(100);
        tableCursos.getColumnModel().getColumn(7).setPreferredWidth(85);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tableCursos.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tableCursos.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        tableCursos.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        tableCursos.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tableCursos.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);

        panelTabla.add(new JScrollPane(tableCursos), BorderLayout.CENTER);
        panel.add(panelTabla, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelModoMicrocredencial() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);

        // Filtro Microcredenciales
        JPanel panelFiltro = new JPanel(new BorderLayout(6, 6));
        panelFiltro.setBackground(Color.WHITE);
        panelFiltro.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(10, 14, 10, 14)));

        lblRamasHabilitadas = new JLabel("Ramas habilitadas: Consultando...");
        lblRamasHabilitadas.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblRamasHabilitadas.setForeground(new Color(33, 37, 41));
        panelFiltro.add(lblRamasHabilitadas, BorderLayout.NORTH);

        JPanel panelFiltroRama = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelFiltroRama.setOpaque(false);
        JLabel lblFiltro = new JLabel("Filtrar por rama:");
        lblFiltro.setFont(new Font("Tahoma", Font.PLAIN, 12));
        panelFiltroRama.add(lblFiltro);

        cbFiltroRama = new JComboBox<>();
        cbFiltroRama.setPreferredSize(new Dimension(320, 28));
        cbFiltroRama.setFont(new Font("Tahoma", Font.PLAIN, 12));
        panelFiltroRama.add(cbFiltroRama);

        JLabel lblAvisoNormativa = new JLabel("(Solo ramas que estés cursando o hayas superado)");
        lblAvisoNormativa.setFont(new Font("Tahoma", Font.ITALIC, 11));
        lblAvisoNormativa.setForeground(new Color(108, 117, 125));
        panelFiltroRama.add(lblAvisoNormativa);

        panelFiltro.add(panelFiltroRama, BorderLayout.SOUTH);
        panel.add(panelFiltro, BorderLayout.NORTH);

        // Tabla de Microcredenciales
        JPanel panelTabla = new JPanel(new BorderLayout(5, 5));
        panelTabla.setBackground(Color.WHITE);
        panelTabla.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(8, 8, 8, 8)));

        String[] columnas = { "Seleccionar", "Código", "Microcredencial", "Rama de Conocimiento", "Créditos",
                "Plazas", "Fin Matrícula", "Inicio Curso", "Precio (€)" };
        modelMicrocredenciales = new DefaultTableModel(columnas, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                switch (col) {
                    case 0:
                        return Boolean.class;
                    case 4:
                        return Double.class;
                    default:
                        return String.class;
                }
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0;
            }
        };

        tableMicrocredenciales = new JTable(modelMicrocredenciales);
        tableMicrocredenciales.setRowHeight(25);
        tableMicrocredenciales.setFont(new Font("Tahoma", Font.PLAIN, 12));
        tableMicrocredenciales.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 12));
        tableMicrocredenciales.getTableHeader().setReorderingAllowed(false);

        tableMicrocredenciales.getColumnModel().getColumn(0).setPreferredWidth(85);
        tableMicrocredenciales.getColumnModel().getColumn(1).setPreferredWidth(65);
        tableMicrocredenciales.getColumnModel().getColumn(2).setPreferredWidth(260);
        tableMicrocredenciales.getColumnModel().getColumn(3).setPreferredWidth(160);
        tableMicrocredenciales.getColumnModel().getColumn(4).setPreferredWidth(65);
        tableMicrocredenciales.getColumnModel().getColumn(5).setPreferredWidth(80);
        tableMicrocredenciales.getColumnModel().getColumn(6).setPreferredWidth(95);
        tableMicrocredenciales.getColumnModel().getColumn(7).setPreferredWidth(95);
        tableMicrocredenciales.getColumnModel().getColumn(8).setPreferredWidth(85);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tableMicrocredenciales.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tableMicrocredenciales.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        tableMicrocredenciales.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        tableMicrocredenciales.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
        tableMicrocredenciales.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tableMicrocredenciales.getColumnModel().getColumn(8).setCellRenderer(rightRenderer);

        panelTabla.add(new JScrollPane(tableMicrocredenciales), BorderLayout.CENTER);
        panel.add(panelTabla, BorderLayout.CENTER);

        return panel;
    }

    // =========================================================================
    // PASO 2: RESUMEN PREVIO CONSOLIDADO (UNIFICADO)
    // =========================================================================
    private JPanel crearPaso2Resumen() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        // Tarjeta informativa con los datos del alumno y grado o modalidad
        JPanel cardInfo = new JPanel(new GridLayout(3, 1, 4, 4));
        cardInfo.setBackground(Color.WHITE);
        cardInfo.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(12, 16, 12, 16)));

        lblResumenEstudiante = new JLabel("Estudiante: ");
        lblResumenEstudiante.setFont(new Font("Tahoma", Font.PLAIN, 13));

        lblResumenGradoOTipo = new JLabel("Titulación / Modalidad: ");
        lblResumenGradoOTipo.setFont(new Font("Tahoma", Font.PLAIN, 13));

        lblResumenAnioOFecha = new JLabel("Año Académico / Fecha: ");
        lblResumenAnioOFecha.setFont(new Font("Tahoma", Font.PLAIN, 13));

        cardInfo.add(lblResumenEstudiante);
        cardInfo.add(lblResumenGradoOTipo);
        cardInfo.add(lblResumenAnioOFecha);
        panel.add(cardInfo, BorderLayout.NORTH);

        // Tabla de desglose consolidado
        JPanel panelTablaResumen = new JPanel(new BorderLayout(5, 5));
        panelTablaResumen.setBackground(Color.WHITE);
        panelTablaResumen.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(10, 10, 10, 10)));

        JLabel lblTituloTabla = new JLabel("Desglose de Asignaturas / Microcredenciales Seleccionadas:");
        lblTituloTabla.setFont(new Font("Tahoma", Font.BOLD, 13));
        panelTablaResumen.add(lblTituloTabla, BorderLayout.NORTH);

        String[] colsResumen = { "Código", "Nombre / Denominación", "Curso / Rama", "Créditos ECTS", "Importe (€)" };
        modelResumen = new DefaultTableModel(colsResumen, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tableResumen = new JTable(modelResumen);
        tableResumen.setRowHeight(24);
        tableResumen.setFont(new Font("Tahoma", Font.PLAIN, 12));
        tableResumen.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 12));
        tableResumen.getTableHeader().setReorderingAllowed(false);

        panelTablaResumen.add(new JScrollPane(tableResumen), BorderLayout.CENTER);

        // Totales inferiores
        JPanel panelTotalesResumen = new JPanel(new FlowLayout(FlowLayout.RIGHT, 25, 6));
        panelTotalesResumen.setOpaque(false);

        lblResumenCreditosTotales = new JLabel("Créditos totales: 0 ECTS");
        lblResumenCreditosTotales.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblResumenCreditosTotales.setForeground(new Color(13, 110, 253));

        lblResumenImporteTotal = new JLabel("Importe Total a Pagar: 0.00 €");
        lblResumenImporteTotal.setFont(new Font("Tahoma", Font.BOLD, 15));
        lblResumenImporteTotal.setForeground(new Color(25, 135, 84));

        panelTotalesResumen.add(lblResumenCreditosTotales);
        panelTotalesResumen.add(lblResumenImporteTotal);
        panelTablaResumen.add(panelTotalesResumen, BorderLayout.SOUTH);

        panel.add(panelTablaResumen, BorderLayout.CENTER);

        // Botones de navegación
        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.setOpaque(false);
        panelSur.setBorder(new EmptyBorder(6, 0, 0, 0));

        btnModificarSeleccion = Estilo.estilizarSecundario(new JButton("<  Modificar Selección"));
        btnModificarSeleccion.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnModificarSeleccion.setPreferredSize(new Dimension(190, 34));

        btnContinuarPaso3 = Estilo.estilizarPrimario(new JButton("Proceder al Pago  >"));
        btnContinuarPaso3.setFont(new Font("Tahoma", Font.BOLD, 13));
        btnContinuarPaso3.setPreferredSize(new Dimension(200, 36));

        panelSur.add(btnModificarSeleccion, BorderLayout.WEST);
        panelSur.add(btnContinuarPaso3, BorderLayout.EAST);

        panel.add(panelSur, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // PASO 3: MODALIDAD DE PAGO Y CONFIRMACIÓN (UNIFICADO)
    // =========================================================================
    private JPanel crearPaso3Pago() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);

        JPanel panelContenido = new JPanel();
        panelContenido.setLayout(new BoxLayout(panelContenido, BoxLayout.Y_AXIS));
        panelContenido.setBackground(Color.WHITE);
        panelContenido.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(20, 25, 20, 25)));

        JLabel lblTituloPago = new JLabel("Seleccione la Modalidad de Pago");
        lblTituloPago.setFont(new Font("Tahoma", Font.BOLD, 16));
        lblTituloPago.setForeground(new Color(33, 37, 41));
        panelContenido.add(lblTituloPago);
        panelContenido.add(Box.createVerticalStrut(15));

        rbTarjeta = new JRadioButton("Tarjeta de Crédito / Débito (Pasarela de pago segura en línea)");
        rbTarjeta.setFont(new Font("Tahoma", Font.BOLD, 13));
        rbTarjeta.setSelected(true);
        rbTarjeta.setOpaque(false);

        rbTransferencia = new JRadioButton("Transferencia Bancaria a cuenta institucional de la Universidad");
        rbTransferencia.setFont(new Font("Tahoma", Font.BOLD, 13));
        rbTransferencia.setOpaque(false);

        rbCartaPago = new JRadioButton("Carta de Pago (Abono presencial en entidad bancaria colaboradora)");
        rbCartaPago.setFont(new Font("Tahoma", Font.BOLD, 13));
        rbCartaPago.setOpaque(false);

        grupoPago = new ButtonGroup();
        grupoPago.add(rbTarjeta);
        grupoPago.add(rbTransferencia);
        grupoPago.add(rbCartaPago);

        panelContenido.add(rbTarjeta);
        panelContenido.add(Box.createVerticalStrut(8));
        panelContenido.add(rbTransferencia);
        panelContenido.add(Box.createVerticalStrut(8));
        panelContenido.add(rbCartaPago);
        panelContenido.add(Box.createVerticalStrut(18));

        // Cuadro explicativo / condiciones de validación
        JPanel panelAviso = new JPanel(new BorderLayout());
        panelAviso.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(13, 110, 253), 1, true),
                "Información y Condiciones Normativas",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Tahoma", Font.BOLD, 12),
                new Color(13, 110, 253)));
        panelAviso.setBackground(new Color(240, 248, 255));

        txtInfoMetodoPago = new JTextArea(
                "• Pago con Tarjeta: La transacción se valida de forma inmediata. Su matrícula o inscripción queda registrada de inmediato en el sistema.\n\n"
                        + "• Pago por Transferencia o Carta de Pago: La solicitud queda registrada en estado 'SOLICITADA'. Deberá presentar el justificante de abono en secretaría antes de que comience el curso.\n\n"
                        + "• Microcredenciales: Conforme a la normativa universitaria, los créditos no computan en permanencia y el abono debe completarse obligatoriamente antes de la fecha de inicio del curso.");
        txtInfoMetodoPago.setFont(new Font("Tahoma", Font.PLAIN, 12));
        txtInfoMetodoPago.setWrapStyleWord(true);
        txtInfoMetodoPago.setLineWrap(true);
        txtInfoMetodoPago.setEditable(false);
        txtInfoMetodoPago.setOpaque(false);
        txtInfoMetodoPago.setBorder(new EmptyBorder(10, 12, 10, 12));
        panelAviso.add(txtInfoMetodoPago, BorderLayout.CENTER);

        panelContenido.add(panelAviso);
        panelContenido.add(Box.createVerticalStrut(15));

        lblPagoImporteFinal = new JLabel("Importe definitivo a abonar: 0.00 €", SwingConstants.CENTER);
        lblPagoImporteFinal.setFont(new Font("Tahoma", Font.BOLD, 16));
        lblPagoImporteFinal.setForeground(new Color(25, 135, 84));
        lblPagoImporteFinal.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelContenido.add(lblPagoImporteFinal);

        panel.add(panelContenido, BorderLayout.CENTER);

        // Subpanel sur
        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.setOpaque(false);
        panelSur.setBorder(new EmptyBorder(6, 0, 0, 0));

        btnVolverPaso2 = Estilo.estilizarSecundario(new JButton("<  Volver al Resumen"));
        btnVolverPaso2.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnVolverPaso2.setPreferredSize(new Dimension(170, 34));

        btnConfirmarMatricula = Estilo.estilizarExito(new JButton("Confirmar Solicitud"));
        btnConfirmarMatricula.setFont(new Font("Tahoma", Font.BOLD, 14));
        btnConfirmarMatricula.setPreferredSize(new Dimension(280, 40));

        panelSur.add(btnVolverPaso2, BorderLayout.WEST);
        panelSur.add(btnConfirmarMatricula, BorderLayout.EAST);

        panel.add(panelSur, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // MÉTODOS DE NAVEGACIÓN Y CONFIGURACIÓN DE MODOS
    // =========================================================================
    public void mostrarPaso1() {
        cardLayoutPasos.show(panelPasos, PASO_SELECCION);
        actualizarBarraPasos(1);
    }

    public void mostrarPaso2() {
        cardLayoutPasos.show(panelPasos, PASO_RESUMEN);
        actualizarBarraPasos(2);
    }

    public void mostrarPaso3() {
        cardLayoutPasos.show(panelPasos, PASO_PAGO);
        actualizarBarraPasos(3);
    }

    public void mostrarModoGrado() {
        rbModoGrado.setSelected(true);
        cardLayoutModos.show(panelContenidoModos, MODO_GRADO);
        lblAvisoTarifa.setText("(Tarifa fija oficial: 20,00 € por crédito ECTS)");
    }

    public void mostrarModoMicrocredencial() {
        rbModoMicrocredencial.setSelected(true);
        cardLayoutModos.show(panelContenidoModos, MODO_MICROCREDENCIAL);
        lblAvisoTarifa.setText("(20,00 €/crédito — Los créditos de microcredencial NO computan para permanencia)");
    }

    public boolean isModoGrado() {
        return rbModoGrado.isSelected();
    }

    public String getMetodoPagoSeleccionado() {
        if (rbTarjeta.isSelected()) {
            return "TARJETA";
        } else if (rbTransferencia.isSelected()) {
            return "TRANSFERENCIA";
        } else if (rbCartaPago.isSelected()) {
            return "CARTA_PAGO";
        }
        return "TARJETA";
    }

    // =========================================================================
    // MÉTODOS DE APOYO DE DATOS
    // =========================================================================
    public List<OfertaCursoDTO> getAsignaturasSeleccionadas() {
        List<OfertaCursoDTO> seleccionadas = new ArrayList<>();
        for (int i = 0; i < modelCursos.getRowCount(); i++) {
            Boolean checked = (Boolean) modelCursos.getValueAt(i, 0);
            if (Boolean.TRUE.equals(checked)) {
                String cId = (String) modelCursos.getValueAt(i, 1);
                String nombre = (String) modelCursos.getValueAt(i, 2);
                int anio = (Integer) modelCursos.getValueAt(i, 3);
                String itinerario = (String) modelCursos.getValueAt(i, 4);
                double creds = (Double) modelCursos.getValueAt(i, 5);
                int convs = (Integer) modelCursos.getValueAt(i, 6);
                String precStr = (String) modelCursos.getValueAt(i, 7);
                double precio = creds * 20.0;
                try {
                    precio = Double.parseDouble(precStr.replace(" €", "").replace(",", "."));
                } catch (Exception ignored) {
                }

                OfertaCursoDTO dto = new OfertaCursoDTO();
                dto.setCursoId(cId);
                dto.setNombre(nombre);
                dto.setAnio(anio);
                dto.setItinerario(itinerario);
                dto.setCreditos(creds);
                dto.setConvocatoriasConsumidas(convs);
                dto.setPrecio(precio);
                dto.setSeleccionada(true);
                seleccionadas.add(dto);
            }
        }
        return seleccionadas;
    }

    public List<OfertaMicrocredencialDTO> getMicrocredencialesSeleccionadas() {
        List<OfertaMicrocredencialDTO> seleccionadas = new ArrayList<>();
        for (int i = 0; i < modelMicrocredenciales.getRowCount(); i++) {
            Boolean checked = (Boolean) modelMicrocredenciales.getValueAt(i, 0);
            if (Boolean.TRUE.equals(checked)) {
                String mcId = (String) modelMicrocredenciales.getValueAt(i, 1);
                String nombre = (String) modelMicrocredenciales.getValueAt(i, 2);
                String rama = (String) modelMicrocredenciales.getValueAt(i, 3);
                double creds = (Double) modelMicrocredenciales.getValueAt(i, 4);
                String finMat = (String) modelMicrocredenciales.getValueAt(i, 6);
                String iniCur = (String) modelMicrocredenciales.getValueAt(i, 7);
                String precStr = (String) modelMicrocredenciales.getValueAt(i, 8);
                double precio = creds * 20.0;
                try {
                    precio = Double.parseDouble(precStr.replace(" €", "").replace(",", "."));
                } catch (Exception ignored) {
                }

                OfertaMicrocredencialDTO dto = new OfertaMicrocredencialDTO();
                dto.setMicrocredencialId(mcId);
                dto.setNombre(nombre);
                dto.setRamaConocimiento(rama);
                dto.setCreditos(creds);
                dto.setFechaFinMatriculacion(finMat);
                dto.setFechaInicio(iniCur);
                dto.setPrecio(precio);
                dto.setSeleccionada(true);
                seleccionadas.add(dto);
            }
        }
        return seleccionadas;
    }

    public void actualizarTotales(int totalCreditos, double totalImporte) {
        lblTotalCreditos.setText(String.format("Total créditos: %d ECTS", totalCreditos));
        lblTotalImporte.setText(String.format("Coste estimado: %.2f €", totalImporte));
        lblPagoImporteFinal.setText(String.format("Importe definitivo a abonar: %.2f €", totalImporte));
    }

    public void cargarResumenGrado(ResumenSolicitudDTO resumen) {
        if (resumen == null) return;
        lblResumenEstudiante.setText("<html><b>Estudiante:</b> " + (resumen.getNombreCompleto() != null ? resumen.getNombreCompleto() : "")
                + " &nbsp;|&nbsp; <b>DNI:</b> " + (resumen.getDni() != null ? resumen.getDni() : "")
                + " &nbsp;|&nbsp; <b>Identificador:</b> " + (resumen.getAlumnoId() != null ? resumen.getAlumnoId() : "") + "</html>");
        lblResumenGradoOTipo.setText("<html><b>Grado solicitado:</b> " + (resumen.getNombreGrado() != null ? resumen.getNombreGrado() : "")
                + " (" + (resumen.getGradoId() != null ? resumen.getGradoId() : "") + ")</html>");
        lblResumenAnioOFecha.setText("<html><b>Año Académico:</b> " + (resumen.getAnioAcademicoId() != null ? resumen.getAnioAcademicoId() : "")
                + " &nbsp;|&nbsp; <b>Fecha de solicitud:</b> " + (resumen.getFechaSolicitud() != null ? resumen.getFechaSolicitud() : "") + "</html>");

        modelResumen.setRowCount(0);
        if (resumen.getAsignaturas() != null) {
            for (OfertaCursoDTO c : resumen.getAsignaturas()) {
                modelResumen.addRow(new Object[] {
                        c.getCursoId(),
                        c.getNombre(),
                        c.getAnio() + "º",
                        c.getCreditos(),
                        String.format("%.2f €", c.getPrecio())
                });
            }
        }
        lblResumenCreditosTotales.setText(String.format("Créditos totales: %d ECTS", resumen.getCreditosTotales()));
        lblResumenImporteTotal.setText(String.format("Importe Total a Pagar: %.2f €", resumen.getImporteTotal()));
        lblPagoImporteFinal.setText(String.format("Importe definitivo a abonar: %.2f €", resumen.getImporteTotal()));
    }

    public void cargarResumenMicrocredencial(ResumenSolicitudMicrocredencialDTO resumen) {
        if (resumen == null) return;
        lblResumenEstudiante.setText("<html><b>Estudiante:</b> " + (resumen.getNombreCompleto() != null ? resumen.getNombreCompleto() : "")
                + " &nbsp;|&nbsp; <b>DNI:</b> " + (resumen.getDni() != null ? resumen.getDni() : "")
                + " &nbsp;|&nbsp; <b>Identificador:</b> " + (resumen.getAlumnoId() != null ? resumen.getAlumnoId() : "") + "</html>");
        lblResumenGradoOTipo.setText("<html><b>Modalidad:</b> Microcredenciales Universitarias de Especialización</html>");
        lblResumenAnioOFecha.setText("<html><b>Fecha de solicitud:</b> " + (resumen.getFechaSolicitud() != null ? resumen.getFechaSolicitud() : "") + "</html>");

        modelResumen.setRowCount(0);
        if (resumen.getMicrocredenciales() != null) {
            for (OfertaMicrocredencialDTO mc : resumen.getMicrocredenciales()) {
                modelResumen.addRow(new Object[] {
                        mc.getMicrocredencialId(),
                        mc.getNombre(),
                        mc.getRamaConocimiento(),
                        mc.getCreditos(),
                        String.format("%.2f €", mc.getPrecio())
                });
            }
        }
        lblResumenCreditosTotales.setText(String.format("Créditos totales: %d ECTS", resumen.getCreditosTotales()));
        lblResumenImporteTotal.setText(String.format("Importe Total a Pagar: %.2f €", resumen.getImporteTotal()));
        lblPagoImporteFinal.setText(String.format("Importe definitivo a abonar: %.2f €", resumen.getImporteTotal()));
    }

    // Compatibilidad para tests existentes
    public void cargarResumen(ResumenSolicitudDTO resumen) {
        cargarResumenGrado(resumen);
    }

    // Getters
    public JFrame getFrame() { return frame; }
    public JPanel getPanelPrincipal() { return panelPrincipal; }
    public JRadioButton getRbModoGrado() { return rbModoGrado; }
    public JRadioButton getRbModoMicrocredencial() { return rbModoMicrocredencial; }
    public JComboBox<OfertaGradoDTO> getCbGrados() { return cbGrados; }
    public JTable getTableCursos() { return tableCursos; }
    public DefaultTableModel getModelCursos() { return modelCursos; }
    public JLabel getLblInfoEstudiante() { return lblInfoEstudiante; }
    public JLabel getLblRamasHabilitadas() { return lblRamasHabilitadas; }
    public JComboBox<String> getCbFiltroRama() { return cbFiltroRama; }
    public JTable getTableMicrocredenciales() { return tableMicrocredenciales; }
    public DefaultTableModel getModelMicrocredenciales() { return modelMicrocredenciales; }
    public JButton getBtnContinuarPaso2() { return btnContinuarPaso2; }
    public JButton getBtnVolverMenuDesdePaso1() { return btnVolverMenuDesdePaso1; }
    public JButton getBtnModificarAsignaturas() { return btnModificarSeleccion; }
    public JButton getBtnModificarSeleccion() { return btnModificarSeleccion; }
    public JButton getBtnContinuarPaso3() { return btnContinuarPaso3; }
    public JButton getBtnVolverPaso2() { return btnVolverPaso2; }
    public JButton getBtnConfirmarMatricula() { return btnConfirmarMatricula; }
    public JRadioButton getRbTarjeta() { return rbTarjeta; }
    public JRadioButton getRbTransferencia() { return rbTransferencia; }
    public JRadioButton getRbCartaPago() { return rbCartaPago; }
    public JTable getTableResumen() { return tableResumen; }
    public DefaultTableModel getModelResumen() { return modelResumen; }
}
