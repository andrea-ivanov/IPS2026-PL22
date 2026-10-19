package view.estudiante;

import model.matriculacion.OfertaCursoDTO;
import model.matriculacion.OfertaGradoDTO;
import model.matriculacion.ResumenSolicitudDTO;

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
 * Vista de Solicitud de Matrícula (HU 35235 / Fase 3).
 * Interfaz por pasos (Asistente de Matrícula):
 * - Paso 1: Selección de Grado ofertado y tabla de asignaturas con checkboxes y cálculo en vivo.
 * - Paso 2: Resumen previo consolidado antes de confirmar.
 * - Paso 3: Selección del método de pago (Tarjeta, Transferencia, Carta de pago) y confirmación.
 */
public class SolicitudMatriculaView {
    public static final String PASO_SELECCION = "PASO_SELECCION";
    public static final String PASO_RESUMEN = "PASO_RESUMEN";
    public static final String PASO_PAGO = "PASO_PAGO";

    private JFrame frame;
    private JPanel panelPrincipal;
    private CardLayout cardLayoutPasos;
    private JPanel panelPasos;

    // Indicador de pasos
    private JLabel lblPaso1;
    private JLabel lblPaso2;
    private JLabel lblPaso3;

    // --- Componentes Paso 1: Selección ---
    private JComboBox<OfertaGradoDTO> cbGrados;
    private JTable tableCursos;
    private DefaultTableModel modelCursos;
    private JLabel lblTotalCreditos;
    private JLabel lblTotalImporte;
    private JLabel lblInfoEstudiante;
    private JButton btnContinuarPaso2;
    private JButton btnVolverMenuDesdePaso1;

    // --- Componentes Paso 2: Resumen ---
    private JLabel lblResumenEstudiante;
    private JLabel lblResumenGrado;
    private JLabel lblResumenAnio;
    private JTable tableResumenAsignaturas;
    private DefaultTableModel modelResumenAsignaturas;
    private JLabel lblResumenCreditosTotales;
    private JLabel lblResumenImporteTotal;
    private JButton btnModificarAsignaturas;
    private JButton btnContinuarPaso3;

    // --- Componentes Paso 3: Método de Pago y Confirmación ---
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
        frame = new JFrame("Solicitud de Nueva Matrícula");
        frame.setBounds(120, 80, 940, 680);
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

        JLabel lblTitulo = new JLabel("Asistente de Solicitud de Matrícula", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Tahoma", Font.BOLD, 18));
        lblTitulo.setForeground(new Color(33, 37, 41));
        header.add(lblTitulo, BorderLayout.NORTH);

        // Barra indicadora de pasos
        JPanel barraPasos = new JPanel(new GridLayout(1, 3, 10, 0));
        barraPasos.setOpaque(false);
        barraPasos.setBorder(new EmptyBorder(5, 20, 10, 20));

        lblPaso1 = crearEtiquetaPaso("1. Selección de Asignaturas", true);
        lblPaso2 = crearEtiquetaPaso("2. Resumen Previo", false);
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
    // PASO 1: SELECCIÓN DE GRADO Y ASIGNATURAS
    // =========================================================================
    private JPanel crearPaso1Seleccion() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        // Subpanel norte: Selector de Grado ofertado e Info
        JPanel panelFiltro = new JPanel(new BorderLayout(5, 5));
        panelFiltro.setBackground(Color.WHITE);
        panelFiltro.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(12, 16, 12, 16)));

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
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
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
        lblInfoEstudiante.setBorder(new EmptyBorder(6, 5, 0, 0));
        panelFiltro.add(lblInfoEstudiante, BorderLayout.SOUTH);

        panel.add(panelFiltro, BorderLayout.NORTH);

        // Subpanel centro: Tabla de asignaturas con checkboxes
        JPanel panelTabla = new JPanel(new BorderLayout(5, 5));
        panelTabla.setBackground(Color.WHITE);
        panelTabla.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(10, 10, 10, 10)));

        String[] columnas = { "Seleccionar", "Código", "Asignatura", "Curso", "Itinerario", "Créditos", "Convocatorias", "Precio (€)" };
        modelCursos = new DefaultTableModel(columnas, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                switch (col) {
                    case 0: return Boolean.class;
                    case 3: return Integer.class;
                    case 5: return Double.class;
                    case 6: return Integer.class;
                    default: return String.class;
                }
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0; // Solo la casilla de selección es editable
            }
        };

        tableCursos = new JTable(modelCursos);
        tableCursos.setRowHeight(26);
        tableCursos.setFont(new Font("Tahoma", Font.PLAIN, 12));
        tableCursos.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 12));
        tableCursos.getTableHeader().setReorderingAllowed(false);

        // Anchos de columna preferidos
        tableCursos.getColumnModel().getColumn(0).setPreferredWidth(85);
        tableCursos.getColumnModel().getColumn(1).setPreferredWidth(65);
        tableCursos.getColumnModel().getColumn(2).setPreferredWidth(260);
        tableCursos.getColumnModel().getColumn(3).setPreferredWidth(55);
        tableCursos.getColumnModel().getColumn(4).setPreferredWidth(90);
        tableCursos.getColumnModel().getColumn(5).setPreferredWidth(65);
        tableCursos.getColumnModel().getColumn(6).setPreferredWidth(100);
        tableCursos.getColumnModel().getColumn(7).setPreferredWidth(85);

        // Alineación centrada para columnas numéricas y de código
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tableCursos.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tableCursos.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        tableCursos.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        tableCursos.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tableCursos.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);

        JScrollPane scrollPane = new JScrollPane(tableCursos);
        panelTabla.add(scrollPane, BorderLayout.CENTER);

        // Indicadores en vivo en la parte inferior de la tabla
        JPanel panelIndicadores = new JPanel(new BorderLayout(10, 5));
        panelIndicadores.setOpaque(false);
        panelIndicadores.setBorder(new EmptyBorder(8, 5, 2, 5));

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

        JLabel lblTarifa = new JLabel("(Tarifa fija oficial: 20,00 € por crédito ECTS)");
        lblTarifa.setFont(new Font("Tahoma", Font.ITALIC, 11));
        lblTarifa.setForeground(new Color(108, 117, 125));
        panelIndicadores.add(lblTarifa, BorderLayout.EAST);

        panelTabla.add(panelIndicadores, BorderLayout.SOUTH);
        panel.add(panelTabla, BorderLayout.CENTER);

        // Subpanel sur: Botones de navegación
        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.setOpaque(false);
        panelSur.setBorder(new EmptyBorder(6, 0, 0, 0));

        btnVolverMenuDesdePaso1 = new JButton("Volver al Menú");
        btnVolverMenuDesdePaso1.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnVolverMenuDesdePaso1.setPreferredSize(new Dimension(150, 34));

        btnContinuarPaso2 = new JButton("Continuar al Resumen  >");
        btnContinuarPaso2.setFont(new Font("Tahoma", Font.BOLD, 13));
        btnContinuarPaso2.setPreferredSize(new Dimension(220, 36));
        btnContinuarPaso2.setBackground(new Color(13, 110, 253));

        panelSur.add(btnVolverMenuDesdePaso1, BorderLayout.WEST);
        panelSur.add(btnContinuarPaso2, BorderLayout.EAST);

        panel.add(panelSur, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // PASO 2: RESUMEN PREVIO CONSOLIDADO
    // =========================================================================
    private JPanel crearPaso2Resumen() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        // Subpanel norte: Tarjeta informativa con los datos del alumno y grado
        JPanel cardInfo = new JPanel(new GridLayout(3, 1, 4, 4));
        cardInfo.setBackground(Color.WHITE);
        cardInfo.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(12, 16, 12, 16)));

        lblResumenEstudiante = new JLabel("Estudiante: ");
        lblResumenEstudiante.setFont(new Font("Tahoma", Font.PLAIN, 13));

        lblResumenGrado = new JLabel("Grado solicitado: ");
        lblResumenGrado.setFont(new Font("Tahoma", Font.PLAIN, 13));

        lblResumenAnio = new JLabel("Año Académico: ");
        lblResumenAnio.setFont(new Font("Tahoma", Font.PLAIN, 13));

        cardInfo.add(lblResumenEstudiante);
        cardInfo.add(lblResumenGrado);
        cardInfo.add(lblResumenAnio);

        panel.add(cardInfo, BorderLayout.NORTH);

        // Subpanel centro: Desglose de asignaturas seleccionadas
        JPanel panelTablaResumen = new JPanel(new BorderLayout(5, 5));
        panelTablaResumen.setBackground(Color.WHITE);
        panelTablaResumen.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(10, 10, 10, 10)));

        JLabel lblTituloTabla = new JLabel("Desglose de Asignaturas Seleccionadas:");
        lblTituloTabla.setFont(new Font("Tahoma", Font.BOLD, 13));
        panelTablaResumen.add(lblTituloTabla, BorderLayout.NORTH);

        String[] colsResumen = { "Código", "Nombre de Asignatura", "Curso", "Créditos ECTS", "Importe (€)" };
        modelResumenAsignaturas = new DefaultTableModel(colsResumen, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tableResumenAsignaturas = new JTable(modelResumenAsignaturas);
        tableResumenAsignaturas.setRowHeight(24);
        tableResumenAsignaturas.setFont(new Font("Tahoma", Font.PLAIN, 12));
        tableResumenAsignaturas.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 12));
        tableResumenAsignaturas.getTableHeader().setReorderingAllowed(false);

        panelTablaResumen.add(new JScrollPane(tableResumenAsignaturas), BorderLayout.CENTER);

        // Resumen numérico inferior
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

        // Subpanel sur: Botones
        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.setOpaque(false);
        panelSur.setBorder(new EmptyBorder(6, 0, 0, 0));

        btnModificarAsignaturas = new JButton("<  Modificar Asignaturas");
        btnModificarAsignaturas.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnModificarAsignaturas.setPreferredSize(new Dimension(190, 34));

        btnContinuarPaso3 = new JButton("Proceder al Pago  >");
        btnContinuarPaso3.setFont(new Font("Tahoma", Font.BOLD, 13));
        btnContinuarPaso3.setPreferredSize(new Dimension(200, 36));

        panelSur.add(btnModificarAsignaturas, BorderLayout.WEST);
        panelSur.add(btnContinuarPaso3, BorderLayout.EAST);

        panel.add(panelSur, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // PASO 3: MODALIDAD DE PAGO Y CONFIRMACIÓN
    // =========================================================================
    private JPanel crearPaso3Pago() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);

        // Subpanel central con opciones de pago y advertencia
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
                "Información de Validación Administrativa",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Tahoma", Font.BOLD, 12),
                new Color(13, 110, 253)));
        panelAviso.setBackground(new Color(240, 248, 255));

        txtInfoMetodoPago = new JTextArea(
                "• Pago con Tarjeta: La transacción se valida de forma inmediata. Su matrícula quedará registrada y las asignaturas pasarán directamente al estado 'PAGADA'.\n\n" +
                "• Pago por Transferencia o Carta de Pago: La matrícula quedará registrada en estado 'SOLICITADA' con asignaturas pendientes de pago. Deberá presentar el justificante de abono en secretaría para su validación manual definitiva.");
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

        // Subpanel sur: Botón de confirmación y volver
        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.setOpaque(false);
        panelSur.setBorder(new EmptyBorder(6, 0, 0, 0));

        btnVolverPaso2 = new JButton("<  Volver al Resumen");
        btnVolverPaso2.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnVolverPaso2.setPreferredSize(new Dimension(170, 34));

        btnConfirmarMatricula = new JButton("Confirmar Solicitud de Matrícula");
        btnConfirmarMatricula.setFont(new Font("Tahoma", Font.BOLD, 14));
        btnConfirmarMatricula.setPreferredSize(new Dimension(280, 40));
        btnConfirmarMatricula.setBackground(new Color(25, 135, 84));

        panelSur.add(btnVolverPaso2, BorderLayout.WEST);
        panelSur.add(btnConfirmarMatricula, BorderLayout.EAST);

        panel.add(panelSur, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // MÉTODOS DE NAVEGACIÓN ENTRE PASOS
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

    // =========================================================================
    // MÉTODOS DE APOYO PARA LLENAR Y CONSULTAR DATOS DE LA VISTA
    // =========================================================================
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

    public void actualizarTotales(int totalCreditos, double totalImporte) {
        lblTotalCreditos.setText(String.format("Total créditos: %d ECTS", totalCreditos));
        lblTotalImporte.setText(String.format("Coste estimado: %.2f €", totalImporte));
        lblPagoImporteFinal.setText(String.format("Importe definitivo a abonar: %.2f €", totalImporte));
    }

    public void cargarResumen(ResumenSolicitudDTO resumen) {
        if (resumen == null) return;
        lblResumenEstudiante.setText("<html><b>Estudiante:</b> " + (resumen.getNombreCompleto() != null ? resumen.getNombreCompleto() : "") +
                " &nbsp;|&nbsp; <b>DNI:</b> " + (resumen.getDni() != null ? resumen.getDni() : "") +
                " &nbsp;|&nbsp; <b>Identificador:</b> " + (resumen.getAlumnoId() != null ? resumen.getAlumnoId() : "") + "</html>");
        lblResumenGrado.setText("<html><b>Grado solicitado:</b> " + (resumen.getNombreGrado() != null ? resumen.getNombreGrado() : "") +
                " (" + (resumen.getGradoId() != null ? resumen.getGradoId() : "") + ")</html>");
        lblResumenAnio.setText("<html><b>Año Académico:</b> " + (resumen.getAnioAcademicoId() != null ? resumen.getAnioAcademicoId() : "") +
                " &nbsp;|&nbsp; <b>Fecha de solicitud:</b> " + (resumen.getFechaSolicitud() != null ? resumen.getFechaSolicitud() : "") + "</html>");

        modelResumenAsignaturas.setRowCount(0);
        if (resumen.getAsignaturas() != null) {
            for (OfertaCursoDTO c : resumen.getAsignaturas()) {
                modelResumenAsignaturas.addRow(new Object[] {
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

    // Getters
    public JFrame getFrame() { return frame; }
    public JPanel getPanelPrincipal() { return panelPrincipal; }
    public JComboBox<OfertaGradoDTO> getCbGrados() { return cbGrados; }
    public JTable tableCursos() { return tableCursos; }
    public JTable getTableCursos() { return tableCursos; }
    public DefaultTableModel getModelCursos() { return modelCursos; }
    public JLabel getLblInfoEstudiante() { return lblInfoEstudiante; }
    public JButton getBtnContinuarPaso2() { return btnContinuarPaso2; }
    public JButton getBtnVolverMenuDesdePaso1() { return btnVolverMenuDesdePaso1; }
    public JButton getBtnModificarAsignaturas() { return btnModificarAsignaturas; }
    public JButton getBtnContinuarPaso3() { return btnContinuarPaso3; }
    public JButton getBtnVolverPaso2() { return btnVolverPaso2; }
    public JButton getBtnConfirmarMatricula() { return btnConfirmarMatricula; }
    public JRadioButton getRbTarjeta() { return rbTarjeta; }
    public JRadioButton getRbTransferencia() { return rbTransferencia; }
    public JRadioButton getRbCartaPago() { return rbCartaPago; }
}

