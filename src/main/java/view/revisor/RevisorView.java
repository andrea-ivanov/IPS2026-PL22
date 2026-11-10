package view.revisor;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import model.grado.GradoAnioRevisorDisplayDTO;
import model.grado.GradoRevisionDisplayDTO;
import util.Util;
import view.comun.Estilo;

/**
 * Panel del revisor externo (HU 36411 y 36455), con dos tarjetas:
 * - METRICAS: métricas de rendimiento de los grados por año académico. Con un año concreto hay una fila por grado;
 *   con "Todos" la tabla se divide en un bloque por grado, con una fila de cabecera "Nombre - Facultad - Rama" y una
 *   fila por año académico. Los grados PENDIENTE_RENOVACION y EN_EXTINCION se marcan con el color de su estado.
 * - GRADO: un grado concreto, con sus métricas por año, la decisión y los botones "Tomar decisión" y "Atrás".
 */
public class RevisorView {
    private static final String[] COLUMNAS_ANIO = { "Nombre Grado", "Rama", "Tasa Éxito", "Tasa abandono", "Aprobados",
            "Matriculados", "Acciones" };
    private static final String[] COLUMNAS_TODOS = { "Año Académico", "Éxito", "Abandono", "Aprobados",
            "Matriculados", "Acciones" };
    private static final String[] COLUMNAS_GRADO = { "Año Académico", "Éxito", "Abandono", "Aprobados",
            "Matriculados" };
    private static final String SIN_VALOR = "—";
    private static final String VER = "Ver";
    /** Estados de grado cuyas filas se marcan con su color (requieren acción del revisor) */
    private static final List<String> ESTADOS_MARCADOS = List.of("PENDIENTE_RENOVACION", "EN_EXTINCION");
    private static final String CARD_METRICAS = "METRICAS";
    private static final String CARD_GRADO = "GRADO";

    private JFrame frame;
    private JComboBox<String> cbAnioAcademico;
    private JButton btnVolverInicio;
    private CardLayout cardLayout;
    private JPanel cardsPanel;

    // -- METRICAS --
    private JTextField txtFiltroRama;
    private JTextField txtFiltroGrado;
    private JTable tabMetricas;
    // grado y estado de cada fila de tabMetricas (null en el estado: la fila no se marca)
    private final List<String> idGradoPorFila = new ArrayList<>();
    private final List<String> estadoPorFila = new ArrayList<>();
    private Consumer<String> onVerGrado;

    // -- GRADO --
    private JLabel lblGrado;
    private JLabel lblEstado;
    private JTable tabMetricasGrado;
    private JLabel lblDecision;
    private JLabel lblFechaUltima;
    private JButton btnTomarDecision;
    private JButton btnAtras;

    public RevisorView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Panel de Revisor Externo - Grados");
        frame.setBounds(100, 100, 1200, 720);
        frame.setMinimumSize(new Dimension(1000, 620));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel contenido = Estilo.contenido(new BorderLayout(0, 12));
        frame.setContentPane(contenido);
        Estilo.maximizarCentrado(frame);

        cbAnioAcademico = new JComboBox<>();
        Estilo.estilizarCampo(cbAnioAcademico);
        cbAnioAcademico.setPreferredSize(new Dimension(140, 30));
        btnVolverInicio = Estilo.botonSecundario("Volver al inicio");
        btnVolverInicio.setToolTipText("Volver a la pantalla de selección de rol");
        contenido.add(Estilo.cabecera("PANEL DE REVISOR EXTERNO", new JLabel("Grados"),
                Estilo.etiqueta("Curso académico:"), cbAnioAcademico, btnVolverInicio), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setOpaque(false);
        cardsPanel.add(createMetricasPanel(), CARD_METRICAS);
        cardsPanel.add(createGradoPanel(), CARD_GRADO);
        contenido.add(cardsPanel, BorderLayout.CENTER);
        mostrarMetricasAnio(List.of());
    }

    private JPanel createMetricasPanel() {
        JPanel filterPanel = Estilo.panelFiltros("Filtros");
        txtFiltroRama = new JTextField();
        Estilo.addFiltro(filterPanel, "Rama conocimiento", txtFiltroRama);
        txtFiltroGrado = new JTextField();
        Estilo.addFiltro(filterPanel, "Grado", txtFiltroGrado);

        tabMetricas = new JTable(new DefaultTableModel(COLUMNAS_ANIO, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // solo el botón "Ver" de la columna Acciones
                return column == getColumnCount() - 1 && VER.equals(getValueAt(row, column));
            }
        });
        Estilo.estilizarTabla(tabMetricas);
        JPanel tablaCard = Estilo.tarjeta(new BorderLayout());
        tablaCard.add(Estilo.scrollTabla(tabMetricas), BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        panel.add(Estilo.envolverFiltros(filterPanel, 250), BorderLayout.WEST);
        panel.add(tablaCard, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Tarjeta del grado: arriba "Grado" y "Estado"; en el centro "Métricas por año"; abajo a la izquierda
     * "Decisión" y "Fecha última" y a la derecha "Tomar decisión" y "Atrás".
     */
    private JPanel createGradoPanel() {
        lblGrado = new JLabel();
        lblGrado.setFont(Estilo.NORMAL);
        lblGrado.setForeground(Estilo.TEXTO);
        lblEstado = new JLabel();
        JPanel datosGrado = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        datosGrado.setOpaque(false);
        datosGrado.add(Estilo.etiqueta("Grado:"));
        datosGrado.add(lblGrado);
        datosGrado.add(Box.createHorizontalStrut(20));
        datosGrado.add(Estilo.etiqueta("Estado:"));
        datosGrado.add(lblEstado);

        tabMetricasGrado = new JTable(new DefaultTableModel(COLUMNAS_GRADO, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        Estilo.estilizarTabla(tabMetricasGrado);
        JPanel metricas = new JPanel(new BorderLayout(0, 8));
        metricas.setOpaque(false);
        metricas.add(Estilo.tituloTarjeta("Métricas por año"), BorderLayout.NORTH);
        metricas.add(Estilo.scrollTabla(tabMetricasGrado), BorderLayout.CENTER);

        lblDecision = new JLabel();
        lblDecision.setFont(Estilo.NORMAL);
        lblDecision.setForeground(Estilo.TEXTO);
        lblFechaUltima = new JLabel();
        lblFechaUltima.setFont(Estilo.NORMAL);
        lblFechaUltima.setForeground(Estilo.TEXTO);
        JPanel decision = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        decision.setOpaque(false);
        decision.add(Estilo.etiqueta("Decisión:"));
        decision.add(lblDecision);
        decision.add(Box.createHorizontalStrut(20));
        decision.add(Estilo.etiqueta("Fecha última:"));
        decision.add(lblFechaUltima);

        btnTomarDecision = Estilo.botonPrimario("Tomar decisión");
        btnAtras = Estilo.botonSecundario("Atrás");
        JPanel abajo = new JPanel(new BorderLayout());
        abajo.setOpaque(false);
        abajo.add(decision, BorderLayout.WEST);
        abajo.add(Estilo.barraAcciones(btnTomarDecision, btnAtras), BorderLayout.EAST);

        JPanel tarjeta = Estilo.tarjeta(new BorderLayout(0, 12));
        tarjeta.add(datosGrado, BorderLayout.NORTH);
        tarjeta.add(metricas, BorderLayout.CENTER);
        tarjeta.add(abajo, BorderLayout.SOUTH);
        return tarjeta;
    }

    // --- Navegación ---

    public void showMetricasView() {
        cardLayout.show(cardsPanel, CARD_METRICAS);
    }

    public void showGradoView() {
        cardLayout.show(cardsPanel, CARD_GRADO);
    }

    // --- Tarjeta METRICAS ---

    /** Tabla de un año concreto: una fila por grado, con su botón "Ver". */
    public void mostrarMetricasAnio(List<GradoAnioRevisorDisplayDTO> metricas) {
        DefaultTableModel model = vaciarTabla();
        model.setColumnIdentifiers(COLUMNAS_ANIO);
        for (GradoAnioRevisorDisplayDTO m : metricas) {
            addFila(model, m.getIdGrado(), m.getEstado(), new Object[] {
                    m.getNombreGrado(),
                    m.getRama(),
                    porcentaje(m.getTasaExito()),
                    porcentaje(m.getTasaAbandono()),
                    m.getAprobados(),
                    m.getMatriculados(),
                    VER
            });
        }
        anchos(tabMetricas, 300, 260, 120, 120, 110, 110, 100);
        renderizar(2, 3, 4, 5);
    }

    /**
     * Tabla de "Todos": un bloque por grado (las métricas llegan ordenadas por grado). La cabecera del bloque lleva el
     * color del estado y el botón "Ver"; un grado sin métricas (sin año) es solo la cabecera.
     */
    public void mostrarMetricasTodos(List<GradoAnioRevisorDisplayDTO> metricas) {
        DefaultTableModel model = vaciarTabla();
        model.setColumnIdentifiers(COLUMNAS_TODOS);
        String gradoActual = null;
        for (GradoAnioRevisorDisplayDTO m : metricas) {
            if (!m.getIdGrado().equals(gradoActual)) {
                gradoActual = m.getIdGrado();
                addFila(model, m.getIdGrado(), m.getEstado(), new Object[] {
                        m.getNombreGrado() + " - " + m.getFacultad() + " - " + m.getRama(), null, null, null, null, VER });
            }
            if (m.getIdAnioAcademico() != null) {
                addFila(model, m.getIdGrado(), null, new Object[] {
                        m.getIdAnioAcademico(),
                        porcentaje(m.getTasaExito()),
                        porcentaje(m.getTasaAbandono()),
                        m.getAprobados(),
                        m.getMatriculados(),
                        null
                });
            }
        }
        // la primera columna es ancha porque también lleva la cabecera de cada bloque
        anchos(tabMetricas, 500, 120, 120, 110, 110, 100);
        renderizar(1, 2, 3, 4);
    }

    /** Identificador del grado de la fila indicada de la tabla de métricas; null si la fila no existe */
    public String getIdGradoFila(int fila) {
        return fila >= 0 && fila < idGradoPorFila.size() ? idGradoPorFila.get(fila) : null;
    }

    public void setOnVerGrado(Consumer<String> onVerGrado) {
        this.onVerGrado = onVerGrado;
    }

    private DefaultTableModel vaciarTabla() {
        if (tabMetricas.isEditing()) {
            tabMetricas.getCellEditor().cancelCellEditing();
        }
        DefaultTableModel model = (DefaultTableModel) tabMetricas.getModel();
        model.setRowCount(0);
        idGradoPorFila.clear();
        estadoPorFila.clear();
        return model;
    }

    private void addFila(DefaultTableModel model, String idGrado, String estado, Object[] fila) {
        idGradoPorFila.add(idGrado);
        estadoPorFila.add(estado != null && ESTADOS_MARCADOS.contains(estado) ? estado : null);
        model.addRow(fila);
    }

    /** Color de fila según el estado en todas las columnas, las indicadas centradas y el botón "Ver" en la última */
    private void renderizar(int... centradas) {
        int acciones = tabMetricas.getColumnCount() - 1;
        for (int i = 0; i < acciones; i++) {
            tabMetricas.getColumnModel().getColumn(i).setCellRenderer(new RendererFilaEstado(false));
        }
        for (int columna : centradas) {
            tabMetricas.getColumnModel().getColumn(columna).setCellRenderer(new RendererFilaEstado(true));
        }
        tabMetricas.getColumnModel().getColumn(acciones).setMinWidth(90);
        tabMetricas.getColumnModel().getColumn(acciones).setCellRenderer(new RendererVer());
        tabMetricas.getColumnModel().getColumn(acciones).setCellEditor(new EditorVer());
    }

    /** Fondo de la fila: el color del estado si se marca; si no, el alterno o el de selección */
    private Color fondoFila(int row, boolean isSelected) {
        String estado = row < estadoPorFila.size() ? estadoPorFila.get(row) : null;
        return estado != null && !isSelected ? Estilo.coloresEstado(estado)[0] : Estilo.fondoFila(row, isSelected);
    }

    /** Celdas de la tabla de métricas: filas alternas y, en los grados marcados, el color de su estado */
    private class RendererFilaEstado extends Estilo.RendererFilas {
        private final boolean centrado;

        RendererFilaEstado(boolean centrado) {
            this.centrado = centrado;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(centrado ? SwingConstants.CENTER : SwingConstants.LEFT);
            String estado = row < estadoPorFila.size() ? estadoPorFila.get(row) : null;
            if (estado != null && !isSelected) {
                Color[] colores = Estilo.coloresEstado(estado);
                setBackground(colores[0]);
                setForeground(colores[1]);
            }
            return this;
        }
    }

    /**
     * Borde del botón "Ver": un margen del color de la fila alrededor del botón con borde azul,
     * para que el botón ocupe toda la celda (cualquier clic en la celda lo pulsa).
     */
    private static void bordeBotonVer(JButton boton, Color fondoFila) {
        boton.setBorder(new CompoundBorder(new MatteBorder(3, 10, 3, 10, fondoFila),
                new LineBorder(Estilo.PRIMARIO, 1, true)));
    }

    /** Botón "Ver" en las filas que lo llevan; celda vacía en las demás */
    private class RendererVer implements TableCellRenderer {
        private final JButton boton = Estilo.botonFila(VER);
        private final RendererFilaEstado vacia = new RendererFilaEstado(false);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            if (!VER.equals(value)) {
                return vacia.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
            }
            bordeBotonVer(boton, fondoFila(row, isSelected));
            return boton;
        }
    }

    /** Al pulsar "Ver" avisa al controlador con el grado de la fila */
    private class EditorVer extends DefaultCellEditor {
        private final JButton boton = Estilo.botonFila(VER);
        private int fila;

        EditorVer() {
            super(new JCheckBox());
            boton.setBackground(Estilo.SELECCION);
            bordeBotonVer(boton, Estilo.SELECCION);
            boton.addActionListener(e -> {
                fireEditingStopped();
                if (onVerGrado != null) {
                    onVerGrado.accept(getIdGradoFila(fila));
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                int column) {
            fila = row;
            return boton;
        }

        @Override
        public Object getCellEditorValue() {
            return VER;
        }
    }

    // --- Tarjeta GRADO ---

    /**
     * Rellena la tarjeta del grado.
     *
     * @param grado Cabecera, decisión y fecha del último dictamen
     * @param metricas Métricas del grado en cada año, de más reciente a más antiguo
     */
    public void mostrarGrado(GradoRevisionDisplayDTO grado, List<GradoAnioRevisorDisplayDTO> metricas) {
        lblGrado.setText(grado.getNombre() + " - " + grado.getFacultad() + " - " + grado.getRama());
        lblEstado.setText(grado.getEstado());
        Estilo.pintarEstado(lblEstado, grado.getEstado());
        lblEstado.setBorder(new CompoundBorder(new LineBorder(Estilo.coloresEstado(grado.getEstado())[0].darker(), 1,
                true), new EmptyBorder(4, 10, 4, 10)));
        lblDecision.setText(textoDecision(grado.getDecision()));
        lblFechaUltima.setText(grado.getFechaUltimaRevision() == null ? SIN_VALOR
                : new SimpleDateFormat("dd/MM/yyyy").format(Util.isoStringToDate(grado.getFechaUltimaRevision())));

        DefaultTableModel model = (DefaultTableModel) tabMetricasGrado.getModel();
        model.setRowCount(0);
        for (GradoAnioRevisorDisplayDTO m : metricas) {
            model.addRow(new Object[] {
                    m.getIdAnioAcademico(),
                    porcentaje(m.getTasaExito()),
                    porcentaje(m.getTasaAbandono()),
                    m.getAprobados(),
                    m.getMatriculados()
            });
        }
        anchos(tabMetricasGrado, 300, 120, 120, 110, 110);
        for (int columna = 1; columna <= 4; columna++) {
            tabMetricasGrado.getColumnModel().getColumn(columna).setCellRenderer(new Estilo.RendererNumero(false));
        }
    }

    private static String textoDecision(String decision) {
        if ("FAVORABLE".equals(decision)) {
            return "Favorable";
        }
        if ("DESFAVORABLE".equals(decision)) {
            return "Desfavorable";
        }
        return SIN_VALOR;
    }

    // --- Utilidades ---

    private static String porcentaje(Double tasa) {
        return tasa == null ? SIN_VALOR : String.format(Locale.ROOT, "%.1f%%", tasa);
    }

    private static void anchos(JTable tabla, int... anchos) {
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
    }

    // --- Getters para el controlador ---

    public JFrame getFrame() {
        return frame;
    }

    public JComboBox<String> getCbAnioAcademico() {
        return cbAnioAcademico;
    }

    public JButton getBtnVolverInicio() {
        return btnVolverInicio;
    }

    public JTextField getTxtFiltroRama() {
        return txtFiltroRama;
    }

    public JTextField getTxtFiltroGrado() {
        return txtFiltroGrado;
    }

    public JTable getTabMetricas() {
        return tabMetricas;
    }

    public JButton getBtnTomarDecision() {
        return btnTomarDecision;
    }

    public JButton getBtnAtras() {
        return btnAtras;
    }
}
