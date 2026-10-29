package view.estudiante;

import model.matriculacion.OfertaMicrocredencialDTO;
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
 * Vista de Solicitud de Matrícula en Microcredenciales (HU 35987).
 * Flujo por pasos (Asistente de Microcredenciales):
 * - Paso 1: Oferta de microcredenciales filtrada por rama de conocimiento del estudiante,
 *           con cálculo en vivo y exclusión de permanencia.
 * - Paso 2: Resumen previo consolidado antes de confirmar.
 * - Paso 3: Selección de modalidad de pago (Tarjeta, Transferencia, Carta de pago)
 *           con aviso normativo de abono previo al inicio del curso.
 */
public class SolicitudMicrocredencialView {
    public static final String PASO_SELECCION = "PASO_SELECCION";
    public static final String PASO_RESUMEN = "PASO_RESUMEN";
    public static final String PASO_PAGO = "PASO_PAGO";

    private JFrame frame;
    private JPanel panelPrincipal;
    private CardLayout cardLayoutPasos;
    private JPanel panelPasos;

    // Indicador de pasos superior
    private JLabel lblPaso1;
    private JLabel lblPaso2;
    private JLabel lblPaso3;

    // --- Componentes Paso 1: Selección ---
    private JLabel lblRamasHabilitadas;
    private JComboBox<String> cbFiltroRama;
    private JTable tableMicrocredenciales;
    private DefaultTableModel modelMicrocredenciales;
    private JLabel lblTotalCreditos;
    private JLabel lblTotalImporte;
    private JButton btnContinuarPaso2;
    private JButton btnVolverMenuDesdePaso1;

    // --- Componentes Paso 2: Resumen ---
    private JLabel lblResumenEstudiante;
    private JLabel lblResumenFecha;
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

    public SolicitudMicrocredencialView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Solicitud de Matrícula en Microcredenciales");
        frame.setBounds(120, 80, 960, 680);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(new EmptyBorder(12, 16, 12, 16));
        panelPrincipal.setBackground(new Color(248, 249, 250));

        // 1. Encabezado con indicador visual de pasos
        JPanel panelNorte = crearEncabezadoPasos();
        panelPrincipal.add(panelNorte, BorderLayout.NORTH);

        // 2. Panel central con CardLayout
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

        JLabel lblTitulo = new JLabel("Matriculación en Microcredenciales Universitarias", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Tahoma", Font.BOLD, 18));
        lblTitulo.setForeground(new Color(33, 37, 41));
        header.add(lblTitulo, BorderLayout.NORTH);

        JPanel barraPasos = new JPanel(new GridLayout(1, 3, 10, 0));
        barraPasos.setOpaque(false);
        barraPasos.setBorder(new EmptyBorder(5, 20, 10, 20));

        lblPaso1 = crearEtiquetaPaso("1. Oferta y Selección de Microcredenciales", true);
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
    // PASO 1: SELECCIÓN DE MICROCREDENCIALES
    // =========================================================================
    private JPanel crearPaso1Seleccion() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        // Subpanel norte: Ramas habilitadas y filtro
        JPanel panelFiltro = new JPanel(new BorderLayout(8, 8));
        panelFiltro.setBackground(Color.WHITE);
        panelFiltro.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(12, 16, 12, 16)));

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

        JLabel lblAvisoNormativa = new JLabel("(Solo se muestran microcredenciales de ramas que estés cursando o hayas superado)");
        lblAvisoNormativa.setFont(new Font("Tahoma", Font.ITALIC, 11));
        lblAvisoNormativa.setForeground(new Color(108, 117, 125));
        panelFiltroRama.add(lblAvisoNormativa);

        panelFiltro.add(panelFiltroRama, BorderLayout.SOUTH);
        panel.add(panelFiltro, BorderLayout.NORTH);

        // Subpanel centro: Tabla con checkboxes
        JPanel panelTabla = new JPanel(new BorderLayout(5, 5));
        panelTabla.setBackground(Color.WHITE);
        panelTabla.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(10, 10, 10, 10)));

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
        tableMicrocredenciales.setRowHeight(26);
        tableMicrocredenciales.setFont(new Font("Tahoma", Font.PLAIN, 12));
        tableMicrocredenciales.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 12));
        tableMicrocredenciales.getTableHeader().setReorderingAllowed(false);

        // Anchos preferidos
        tableMicrocredenciales.getColumnModel().getColumn(0).setPreferredWidth(85);
        tableMicrocredenciales.getColumnModel().getColumn(1).setPreferredWidth(70);
        tableMicrocredenciales.getColumnModel().getColumn(2).setPreferredWidth(270);
        tableMicrocredenciales.getColumnModel().getColumn(3).setPreferredWidth(170);
        tableMicrocredenciales.getColumnModel().getColumn(4).setPreferredWidth(65);
        tableMicrocredenciales.getColumnModel().getColumn(5).setPreferredWidth(80);
        tableMicrocredenciales.getColumnModel().getColumn(6).setPreferredWidth(95);
        tableMicrocredenciales.getColumnModel().getColumn(7).setPreferredWidth(95);
        tableMicrocredenciales.getColumnModel().getColumn(8).setPreferredWidth(85);

        // Renderers centrados y numéricos
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

        // Panel de totales y aviso de permanencia
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

        JLabel lblAvisoPermanencia = new JLabel("(20,00 €/crédito — Los créditos de microcredencial NO computan para la permanencia)");
        lblAvisoPermanencia.setFont(new Font("Tahoma", Font.BOLD, 11));
        lblAvisoPermanencia.setForeground(new Color(108, 117, 125));
        panelIndicadores.add(lblAvisoPermanencia, BorderLayout.EAST);

        panelTabla.add(panelIndicadores, BorderLayout.SOUTH);
        panel.add(panelTabla, BorderLayout.CENTER);

        // Subpanel sur: Botones
        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.setOpaque(false);
        panelSur.setBorder(new EmptyBorder(6, 0, 0, 0));

        btnVolverMenuDesdePaso1 = Estilo.estilizarSecundario(new JButton("Volver al Menú"));
        btnVolverMenuDesdePaso1.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnVolverMenuDesdePaso1.setPreferredSize(new Dimension(150, 34));

        btnContinuarPaso2 = Estilo.estilizarPrimario(new JButton("Continuar al Resumen  >"));
        btnContinuarPaso2.setFont(new Font("Tahoma", Font.BOLD, 13));
        btnContinuarPaso2.setPreferredSize(new Dimension(220, 36));

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

        // Tarjeta superior
        JPanel cardInfo = new JPanel(new GridLayout(2, 1, 4, 4));
        cardInfo.setBackground(Color.WHITE);
        cardInfo.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(12, 16, 12, 16)));

        lblResumenEstudiante = new JLabel("Estudiante: ");
        lblResumenEstudiante.setFont(new Font("Tahoma", Font.PLAIN, 13));

        lblResumenFecha = new JLabel("Tipo de matrícula: Microcredenciales de Especialización");
        lblResumenFecha.setFont(new Font("Tahoma", Font.PLAIN, 13));

        cardInfo.add(lblResumenEstudiante);
        cardInfo.add(lblResumenFecha);
        panel.add(cardInfo, BorderLayout.NORTH);

        // Desglose
        JPanel panelTablaResumen = new JPanel(new BorderLayout(5, 5));
        panelTablaResumen.setBackground(Color.WHITE);
        panelTablaResumen.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(10, 10, 10, 10)));

        JLabel lblTituloTabla = new JLabel("Microcredenciales Seleccionadas para Inscripción:");
        lblTituloTabla.setFont(new Font("Tahoma", Font.BOLD, 13));
        panelTablaResumen.add(lblTituloTabla, BorderLayout.NORTH);

        String[] colsResumen = { "Código", "Microcredencial", "Rama de Conocimiento", "Créditos ECTS", "Importe (€)" };
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

        // Botones sur
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
    // PASO 3: MODALIDAD DE PAGO Y CONFIRMACIÓN
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

        JLabel lblTituloPago = new JLabel("Modalidad de Pago de la Microcredencial");
        lblTituloPago.setFont(new Font("Tahoma", Font.BOLD, 16));
        lblTituloPago.setForeground(new Color(33, 37, 41));
        panelContenido.add(lblTituloPago);
        panelContenido.add(Box.createVerticalStrut(15));

        rbTarjeta = new JRadioButton("Tarjeta de Crédito / Débito (Validación inmediata de la plaza)");
        rbTarjeta.setFont(new Font("Tahoma", Font.BOLD, 13));
        rbTarjeta.setSelected(true);
        rbTarjeta.setOpaque(false);

        rbTransferencia = new JRadioButton("Transferencia Bancaria a cuenta institucional de la Universidad");
        rbTransferencia.setFont(new Font("Tahoma", Font.BOLD, 13));
        rbTransferencia.setOpaque(false);

        rbCartaPago = new JRadioButton("Carta de Pago (Abono en ventanilla bancaria colaboradora)");
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

        // Cuadro normativo específico de Microcredenciales
        JPanel panelAviso = new JPanel(new BorderLayout());
        panelAviso.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(13, 110, 253), 1, true),
                "Condiciones Normativas Oficiales",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Tahoma", Font.BOLD, 12),
                new Color(13, 110, 253)));
        panelAviso.setBackground(new Color(240, 248, 255));

        txtInfoMetodoPago = new JTextArea(
                "• Plazo de Matrícula y Pago: El periodo de matriculación es previo al inicio de la microcredencial. El pago debe quedar completamente realizado antes de la fecha de inicio del curso.\n\n"
                        + "• Cómputo de Permanencia: Conforme a la normativa universitaria, los créditos matriculados en microcredenciales no computan en los umbrales del régimen de permanencia ni cuentan con limitación de créditos máximos.");
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

        btnConfirmarMatricula = Estilo.estilizarExito(new JButton("Confirmar Matrícula en Microcredencial"));
        btnConfirmarMatricula.setFont(new Font("Tahoma", Font.BOLD, 14));
        btnConfirmarMatricula.setPreferredSize(new Dimension(320, 40));

        panelSur.add(btnVolverPaso2, BorderLayout.WEST);
        panelSur.add(btnConfirmarMatricula, BorderLayout.EAST);

        panel.add(panelSur, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // MÉTODOS DE NAVEGACIÓN
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

    public void cargarResumen(ResumenSolicitudMicrocredencialDTO resumen) {
        if (resumen == null)
            return;
        lblResumenEstudiante.setText(
                "<html><b>Estudiante:</b> " + (resumen.getNombreCompleto() != null ? resumen.getNombreCompleto() : "") +
                        " &nbsp;|&nbsp; <b>DNI:</b> " + (resumen.getDni() != null ? resumen.getDni() : "") +
                        " &nbsp;|&nbsp; <b>Identificador:</b> "
                        + (resumen.getAlumnoId() != null ? resumen.getAlumnoId() : "") + "</html>");
        lblResumenFecha.setText("<html><b>Tipo de matrícula:</b> Microcredenciales Universitarias &nbsp;|&nbsp; <b>Fecha:</b> "
                + (resumen.getFechaSolicitud() != null ? resumen.getFechaSolicitud() : "") + "</html>");

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

    // Getters
    public JFrame getFrame() {
        return frame;
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    public JLabel getLblRamasHabilitadas() {
        return lblRamasHabilitadas;
    }

    public JComboBox<String> getCbFiltroRama() {
        return cbFiltroRama;
    }

    public JTable getTableMicrocredenciales() {
        return tableMicrocredenciales;
    }

    public DefaultTableModel getModelMicrocredenciales() {
        return modelMicrocredenciales;
    }

    public JButton getBtnVolverMenuDesdePaso1() {
        return btnVolverMenuDesdePaso1;
    }

    public JButton getBtnContinuarPaso2() {
        return btnContinuarPaso2;
    }

    public JButton getBtnModificarSeleccion() {
        return btnModificarSeleccion;
    }

    public JButton getBtnContinuarPaso3() {
        return btnContinuarPaso3;
    }

    public JButton getBtnVolverPaso2() {
        return btnVolverPaso2;
    }

    public JButton getBtnConfirmarMatricula() {
        return btnConfirmarMatricula;
    }

    public JRadioButton getRbTarjeta() {
        return rbTarjeta;
    }

    public JRadioButton getRbTransferencia() {
        return rbTransferencia;
    }

    public JRadioButton getRbCartaPago() {
        return rbCartaPago;
    }
}

