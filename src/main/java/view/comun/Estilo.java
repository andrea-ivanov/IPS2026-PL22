
package view.comun;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Estética común de todas las vistas (la de las vistas de Estudiante y
 * Secretaría): fondo gris claro,
 * tarjetas blancas con borde redondeado, Tahoma, azul como color principal,
 * tablas con filas alternas
 * y estados de color. Solo afecta al aspecto de los componentes, nunca a su
 * comportamiento.
 */
public final class Estilo {
    public static final Color FONDO = new Color(248, 249, 250);
    public static final Color TARJETA = Color.WHITE;
    public static final Color BORDE = new Color(222, 226, 230);
    public static final Color TEXTO = new Color(33, 37, 41);
    public static final Color TEXTO_SECUNDARIO = new Color(108, 117, 125);
    public static final Color PRIMARIO = new Color(13, 110, 253);
    public static final Color PRIMARIO_OSCURO = new Color(10, 88, 202);
    public static final Color EXITO = new Color(25, 135, 84);
    public static final Color PELIGRO = new Color(220, 53, 69);
    public static final Color AVISO = new Color(133, 100, 4);
    public static final Color CABECERA_TABLA = new Color(233, 236, 239);
    public static final Color FILA_ALTERNA = new Color(248, 249, 250);
    public static final Color SELECCION = new Color(207, 226, 255);
    public static final Color DESHABILITADO = new Color(233, 236, 239);

    public static final Font TITULO = new Font("Tahoma", Font.BOLD, 20);
    public static final Font SUBTITULO = new Font("Tahoma", Font.PLAIN, 12);
    public static final Font TITULO_TARJETA = new Font("Tahoma", Font.BOLD, 14);
    public static final Font ETIQUETA = new Font("Tahoma", Font.BOLD, 12);
    public static final Font NORMAL = new Font("Tahoma", Font.PLAIN, 12);
    public static final Font BOTON = new Font("Tahoma", Font.BOLD, 12);

    private static final boolean ES_MAC = System.getProperty("os.name", "").toLowerCase().contains("mac");

    private Estilo() {
    }

    private static Image iconoAppCache;
    private static Icon iconoInfoCache;
    private static Icon iconoAvisoCache;
    private static Icon iconoErrorCache;
    private static Icon iconoPreguntaCache;

    // =========================================================================
    // Ventanas e Iconos
    // =========================================================================

    /**
     * Centra la ventana con su tamaño normal (el que recupera al restaurarla),
     * aplica el icono de la aplicación (eliminando el logo de Java por defecto) y
     * la abre maximizada.
     */
    public static void maximizarCentrado(JFrame frame) {
        aplicarIconoVentana(frame);
        frame.setLocationRelativeTo(null);
        frame.setExtendedState(frame.getExtendedState() | Frame.MAXIMIZED_BOTH);
    }

    /**
     * Aplica el icono de la aplicación a una ventana (JFrame o JDialog),
     * reemplazando el icono por defecto de Java (cafetera).
     */
    public static void aplicarIconoVentana(Window window) {
        if (window == null)
            return;
        try {
            Image icono = getIconoApp();
            if (icono != null && window instanceof Frame) {
                ((Frame) window).setIconImage(icono);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Obtiene el icono de la aplicación (escudo académico).
     * Si existe un recurso en /img/logo.png o /img/escudo.png lo carga; si no,
     * genera un emblema vectorial universitario con birrete nítido y antialiasing.
     */
    public static Image getIconoApp() {
        if (iconoAppCache != null)
            return iconoAppCache;
        try {
            java.net.URL res = Estilo.class.getResource("/img/logo.png");
            if (res == null)
                res = Estilo.class.getResource("/img/escudo.png");
            if (res != null) {
                iconoAppCache = new ImageIcon(res).getImage();
                return iconoAppCache;
            }
        } catch (Exception ignored) {
        }

        // Generar icono universitario nítido de 32x32
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(PRIMARIO);
        g2.fillRoundRect(2, 2, 28, 28, 8, 8);
        g2.setColor(Color.WHITE);
        Polygon birrete = new Polygon();
        birrete.addPoint(16, 8);
        birrete.addPoint(27, 13);
        birrete.addPoint(16, 18);
        birrete.addPoint(5, 13);
        g2.fillPolygon(birrete);
        g2.fillRect(11, 18, 10, 5);
        g2.dispose();
        iconoAppCache = img;
        return iconoAppCache;
    }

    public static Icon getIconoInfo() {
        if (iconoInfoCache != null)
            return iconoInfoCache;
        iconoInfoCache = crearIconoCircular(EXITO, "✓");
        return iconoInfoCache;
    }

    public static Icon getIconoAviso() {
        if (iconoAvisoCache != null)
            return iconoAvisoCache;
        iconoAvisoCache = crearIconoCircular(new Color(230, 150, 0), "!");
        return iconoAvisoCache;
    }

    public static Icon getIconoError() {
        if (iconoErrorCache != null)
            return iconoErrorCache;
        iconoErrorCache = crearIconoCircular(PELIGRO, "✕");
        return iconoErrorCache;
    }

    public static Icon getIconoPregunta() {
        if (iconoPreguntaCache != null)
            return iconoPreguntaCache;
        iconoPreguntaCache = crearIconoCircular(PRIMARIO, "?");
        return iconoPreguntaCache;
    }

    private static Icon crearIconoCircular(Color colorFondo, String simbolo) {
        int size = 38;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(colorFondo);
        g2.fillOval(2, 2, size - 4, size - 4);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Tahoma", Font.BOLD, 22));
        FontMetrics fm = g2.getFontMetrics();
        int x = (size - fm.stringWidth(simbolo)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent() - 1;
        g2.drawString(simbolo, x, y);
        g2.dispose();
        return new ImageIcon(img);
    }

    // =========================================================================
    // Cuadros de aviso personalizados (sin el logo de Java)
    // =========================================================================

    public static void mostrarMensaje(Component parent, String mensaje, String titulo) {
        JOptionPane.showMessageDialog(parent, mensaje, titulo, JOptionPane.PLAIN_MESSAGE, getIconoInfo());
    }

    public static void mostrarAviso(Component parent, String mensaje, String titulo) {
        JOptionPane.showMessageDialog(parent, mensaje, titulo, JOptionPane.PLAIN_MESSAGE, getIconoAviso());
    }

    public static void mostrarError(Component parent, String mensaje, String titulo) {
        JOptionPane.showMessageDialog(parent, mensaje, titulo, JOptionPane.PLAIN_MESSAGE, getIconoError());
    }

    public static int confirmar(Component parent, String mensaje, String titulo, Object[] opciones) {
        return JOptionPane.showOptionDialog(parent, mensaje, titulo,
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, getIconoPregunta(),
                opciones, opciones != null && opciones.length > 0 ? opciones[0] : null);
    }

    /** Panel raíz de una ventana: fondo gris claro con margen. */
    public static JPanel contenido(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(FONDO);
        panel.setBorder(new EmptyBorder(14, 18, 14, 18));
        return panel;
    }

    /**
     * Cabecera de una vista: sección en azul ("PANEL DE …") con el título debajo a
     * la izquierda
     * y los botones a la derecha.
     */
    public static JPanel cabecera(String seccion, JLabel titulo, JComponent... botones) {
        JPanel cabecera = new JPanel(new BorderLayout(10, 0));
        cabecera.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setOpaque(false);
        JLabel lblSeccion = new JLabel(seccion);
        lblSeccion.setFont(ETIQUETA);
        lblSeccion.setForeground(PRIMARIO);
        titulo.setFont(TITULO);
        titulo.setForeground(TEXTO);
        textos.add(lblSeccion);
        textos.add(Box.createVerticalStrut(2));
        textos.add(titulo);
        cabecera.add(textos, BorderLayout.WEST);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        panelBotones.setOpaque(false);
        for (JComponent boton : botones) {
            panelBotones.add(boton);
        }
        cabecera.add(panelBotones, BorderLayout.EAST);
        return cabecera;
    }

    // =========================================================================
    // Tarjetas y textos
    // =========================================================================

    /**
     * Panel blanco con borde redondeado, como las tarjetas de la vista de
     * Estudiante.
     */
    public static JPanel tarjeta(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(TARJETA);
        panel.setBorder(bordeTarjeta(12, 14));
        return panel;
    }

    public static Border bordeTarjeta(int vertical, int horizontal) {
        return new CompoundBorder(new LineBorder(BORDE, 1, true),
                new EmptyBorder(vertical, horizontal, vertical, horizontal));
    }

    public static JLabel tituloTarjeta(String texto) {
        JLabel label = new JLabel(texto);
        estilizarTituloTarjeta(label);
        return label;
    }

    public static void estilizarTituloTarjeta(JLabel label) {
        label.setFont(TITULO_TARJETA);
        label.setForeground(PRIMARIO);
    }

    public static JLabel etiqueta(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(ETIQUETA);
        label.setForeground(TEXTO);
        return label;
    }

    public static JLabel textoSecundario(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(SUBTITULO);
        label.setForeground(TEXTO_SECUNDARIO);
        return label;
    }

    // =========================================================================
    // Tarjeta de filtros (columna izquierda de los listados)
    // =========================================================================

    /**
     * Panel de filtros con su título; se rellena con addFiltro y se coloca con
     * envolverFiltros.
     */
    public static JPanel panelFiltros(String titulo) {
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBackground(TARJETA);
        JLabel lblTitulo = tituloTarjeta(titulo);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterPanel.add(lblTitulo);
        filterPanel.add(Box.createVerticalStrut(6));
        JSeparator separador = new JSeparator();
        separador.setForeground(BORDE);
        separador.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separador.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterPanel.add(separador);
        filterPanel.add(Box.createVerticalStrut(10));
        return filterPanel;
    }

    /** Añade al panel de filtros una etiqueta con su campo debajo. */
    public static void addFiltro(JPanel panel, String etiqueta, JComponent field) {
        JLabel label = etiqueta(etiqueta);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(4));
        estilizarCampo(field);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        field.setPreferredSize(new Dimension(200, 30));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(field);
        panel.add(Box.createVerticalStrut(12));
    }

    /**
     * Tarjeta de filtros con ancho fijo y desplazamiento vertical si la ventana es
     * baja.
     */
    public static JComponent envolverFiltros(JPanel filterPanel, int ancho) {
        JPanel tarjeta = new TarjetaAnchoFijo();
        tarjeta.add(filterPanel, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(tarjeta, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(ancho, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    /**
     * Tarjeta que dentro de un JScrollPane ocupa siempre el ancho visible (los
     * textos largos se ajustan
     * en vez de ensanchar los filtros) y solo se desplaza en vertical.
     */
    private static class TarjetaAnchoFijo extends JPanel implements Scrollable {
        TarjetaAnchoFijo() {
            super(new BorderLayout());
            setBackground(TARJETA);
            setBorder(bordeTarjeta(12, 14));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            // Si la ventana es alta la tarjeta llega hasta abajo; si es baja aparece la
            // barra
            return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
        }
    }

    /**
     * Da el estilo común a un diálogo de formulario ya construido: fondo gris
     * claro, el formulario en una
     * tarjeta blanca (etiquetas en negrita y campos con borde) y el panel de
     * botones transparente.
     */
    public static void estilizarFormulario(JDialog dialog, JPanel formPanel, JPanel... panelesTransparentes) {
        JPanel contenido = (JPanel) dialog.getContentPane();
        contenido.setBackground(FONDO);
        contenido.setBorder(new EmptyBorder(14, 14, 10, 14));
        formPanel.setBackground(TARJETA);
        formPanel.setBorder(bordeTarjeta(16, 18));
        for (Component c : formPanel.getComponents()) {
            if (c instanceof JLabel) {
                c.setFont(ETIQUETA);
                c.setForeground(TEXTO);
            } else if (c instanceof JComponent) {
                estilizarCampo((JComponent) c);
                if (c instanceof JTextField) {
                    // Altura cómoda para escribir
                    Dimension d = c.getPreferredSize();
                    c.setPreferredSize(new Dimension(Math.max(d.width, 220), 30));
                }
            }
        }
        for (JPanel panel : panelesTransparentes) {
            panel.setOpaque(false);
        }
    }

    /** Panel transparente con los botones de acción alineados a la derecha. */
    public static JPanel barraAcciones(JComponent... botones) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);
        for (JComponent boton : botones) {
            panel.add(boton);
        }
        return panel;
    }

    // =========================================================================
    // Botones
    // =========================================================================

    /**
     * Asegura que el botón pinte correctamente su color de fondo en todos los
     * sistemas operativos.
     * En macOS, el Look and Feel nativo (Aqua) ignora setBackground a menos que se
     * configure
     * como opaco y use BasicButtonUI para renderizar los colores personalizados.
     */
    public static void prepararBoton(JButton boton) {
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        if (ES_MAC) {
            boton.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        }
    }

    /** Botón principal (azul). */
    public static JButton botonPrimario(String texto) {
        return estilizarPrimario(new JButton(texto));
    }

    public static JButton botonExito(String texto) {
        return estilizarExito(new JButton(texto));
    }

    public static JButton botonPeligro(String texto) {
        return estilizarPeligro(new JButton(texto));
    }

    /** Botón secundario: blanco con borde gris. */
    public static JButton botonSecundario(String texto) {
        return estilizarSecundario(new JButton(texto));
    }

    public static JButton estilizarPrimario(JButton boton) {
        return estilizarColor(boton, PRIMARIO, PRIMARIO_OSCURO);
    }

    public static JButton estilizarExito(JButton boton) {
        return estilizarColor(boton, EXITO, EXITO.darker());
    }

    public static JButton estilizarPeligro(JButton boton) {
        return estilizarColor(boton, PELIGRO, PELIGRO.darker());
    }

    public static JButton estilizarSecundario(JButton boton) {
        prepararBoton(boton);
        boton.setFont(BOTON);
        boton.setBackground(TARJETA);
        boton.setForeground(TEXTO);
        boton.setFocusPainted(false);
        boton.setBorder(new CompoundBorder(new LineBorder(BORDE, 1, true), new EmptyBorder(6, 14, 6, 14)));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }

    /**
     * Botón de color con texto blanco; al deshabilitarse se pinta en gris para que
     * se note.
     */
    private static JButton estilizarColor(JButton boton, Color fondo, Color borde) {
        prepararBoton(boton);
        boton.setFont(BOTON);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Runnable pintar = () -> {
            boolean activo = boton.isEnabled();
            boton.setBackground(activo ? fondo : DESHABILITADO);
            boton.setBorder(new CompoundBorder(new LineBorder(activo ? borde : BORDE, 1, true),
                    new EmptyBorder(7, 16, 7, 16)));
        };
        pintar.run();
        boton.addPropertyChangeListener("enabled", e -> pintar.run());
        return boton;
    }

    /**
     * Botón de una fila de tabla ("Ver", "Ver / Gestionar"): blanco con borde y
     * texto azul.
     */
    public static JButton botonFila(String texto) {
        return estilizarBotonFila(new JButton(texto));
    }

    public static JButton estilizarBotonFila(JButton button) {
        prepararBoton(button);
        button.setFont(new Font("Tahoma", Font.BOLD, 11));
        button.setForeground(PRIMARIO);
        button.setBackground(TARJETA);
        button.setFocusPainted(false);
        button.setBorder(new LineBorder(PRIMARIO, 1, true));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /** Celda que contiene un botón de fila con un pequeño margen. */
    public static JPanel celdaBotonFila(JButton button) {
        JPanel celda = new JPanel(new BorderLayout());
        celda.setBorder(new EmptyBorder(3, 8, 3, 8));
        celda.add(button, BorderLayout.CENTER);
        return celda;
    }

    /** Fondo de la fila (alterno o seleccionado) para pintar celdas propias. */
    public static Color fondoFila(int row, boolean seleccionada) {
        return seleccionada ? SELECCION : (row % 2 == 0 ? TARJETA : FILA_ALTERNA);
    }

    // =========================================================================
    // Campos y tablas
    // =========================================================================

    public static void estilizarCampo(JComponent campo) {
        campo.setFont(NORMAL);
        if (campo instanceof JTextField) {
            campo.setBorder(new CompoundBorder(new LineBorder(BORDE, 1, true), new EmptyBorder(4, 6, 4, 6)));
        } else if (campo instanceof JTextArea) {
            campo.setBorder(new EmptyBorder(4, 6, 4, 6));
        } else {
            campo.setBackground(TARJETA);
        }
    }

    /**
     * Tabla con filas alternas, cabecera gris, solo líneas horizontales y selección
     * azul clara.
     * Se aplica a nivel de tabla (renderers por defecto y cabecera), así que se
     * mantiene aunque
     * después se cambie el modelo de la tabla.
     */
    public static void estilizarTabla(JTable tabla) {
        tabla.setFont(NORMAL);
        tabla.setRowHeight(28);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(BORDE);
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setSelectionBackground(SELECCION);
        tabla.setSelectionForeground(TEXTO);
        tabla.setFillsViewportHeight(true);
        tabla.setDefaultRenderer(Object.class, new RendererFilas());
        tabla.setDefaultRenderer(Number.class, new RendererNumero(false));

        JTableHeader cabecera = tabla.getTableHeader();
        cabecera.setFont(ETIQUETA);
        cabecera.setBackground(CABECERA_TABLA);
        cabecera.setForeground(TEXTO);
        cabecera.setReorderingAllowed(false);
        cabecera.setPreferredSize(new Dimension(cabecera.getPreferredSize().width, 30));
    }

    /**
     * Tabla que, si sus columnas caben, se estira hasta llenar todo el ancho
     * visible aunque se haya
     * desactivado el redimensionado automático (SwingUtil.autoAdjustColumns ajusta
     * las columnas a su
     * contenido con AUTO_RESIZE_OFF). Si no caben, se desplaza en horizontal como
     * antes.
     */
    public static JTable tablaQueLlena() {
        return new TablaQueLlena(null);
    }

    public static JTable tablaQueLlena(javax.swing.table.TableModel model) {
        return new TablaQueLlena(model);
    }

    private static class TablaQueLlena extends JTable {
        TablaQueLlena(javax.swing.table.TableModel model) {
            super();
            if (model != null) {
                setModel(model);
            }
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            if (getAutoResizeMode() != AUTO_RESIZE_OFF) {
                return super.getScrollableTracksViewportWidth();
            }
            Container padre = getParent();
            return padre instanceof JViewport && anchoPreferidoColumnas() < padre.getWidth();
        }

        @Override
        public void doLayout() {
            if (getAutoResizeMode() == AUTO_RESIZE_OFF && getScrollableTracksViewportWidth()) {
                // Reparte el ancho sobrante entre las columnas sin cambiar el modo elegido para
                // la tabla
                autoResizeMode = AUTO_RESIZE_ALL_COLUMNS;
                try {
                    super.doLayout();
                } finally {
                    autoResizeMode = AUTO_RESIZE_OFF;
                }
            } else {
                super.doLayout();
            }
        }

        private int anchoPreferidoColumnas() {
            int ancho = 0;
            for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
                ancho += getColumnModel().getColumn(i).getPreferredWidth();
            }
            return ancho;
        }
    }

    public static JScrollPane scrollTabla(JTable tabla) {
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new LineBorder(BORDE, 1));
        scroll.getViewport().setBackground(TARJETA);
        return scroll;
    }

    public static void estilizarPestanas(JTabbedPane pestanas) {
        pestanas.setFont(TITULO_TARJETA);
    }

    // =========================================================================
    // Estados de color
    // =========================================================================

    private static final Color[] VERDE = { new Color(209, 231, 221), new Color(15, 81, 50) };
    private static final Color[] AZUL = { new Color(207, 226, 255), new Color(8, 66, 152) };
    private static final Color[] AMBAR = { new Color(255, 243, 205), new Color(133, 100, 4) };
    private static final Color[] GRIS = { new Color(226, 227, 229), new Color(65, 70, 75) };
    private static final Color[] ROJO = { new Color(248, 215, 218), new Color(132, 32, 41) };

    /** Colores (fondo, texto) de cada estado de matrícula, grado, curso o acta. */
    public static Color[] coloresEstado(String estado) {
        if (estado == null) {
            return new Color[] { TARJETA, TEXTO };
        }
        switch (estado) {
            case "ACTIVA":
            case "ACTIVO":
            case "VIGENTE":
            case "ABIERTA":
            case "INSCRITA":
                return VERDE;
            case "PAGADA":
                return AZUL;
            case "SOLICITADA":
            case "PENDIENTE_RENOVACION":
            case "PENDIENTE":
            case "PROPUESTA":
                return AMBAR;
            case "FINALIZADA":
            case "INACTIVO":
            case "EXTINTO":
            case "CERRADA":
            case "ABANDONADA":
                return GRIS;
            case "CANCELADA":
            case "CANCELADA_BAJA_DEMANDA":
            case "Cancelada por baja demanda":
            case "CANCELADA POR BAJA DEMANDA":
            case "Cancelada por impago":
            case "CANCELADA POR IMPAGO":
            case "EN_EXTINCION":
            case "RECHAZADA":
                return ROJO;
            case "ACEPTADA":
            case "APROBADO":
                return VERDE;
            case "SUSPENSO":
                return ROJO;
            case "NO PRESENTADO":
                return GRIS;
            default:
                return new Color[] { TARJETA, TEXTO };
        }
    }

    /** Pinta una etiqueta como la "píldora" de color del estado. */
    public static void pintarEstado(JLabel label, String estado) {
        Color[] colores = coloresEstado(estado);
        label.setOpaque(true);
        label.setBackground(colores[0]);
        label.setForeground(colores[1]);
        label.setFont(ETIQUETA);
    }

    // =========================================================================
    // Renderers
    // =========================================================================

    /** Filas alternas blanco / gris muy claro con algo de margen a la izquierda. */
    public static class RendererFilas extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            setBorder(new EmptyBorder(0, 8, 0, 8));
            setFont(NORMAL);
            if (!isSelected) {
                setBackground(row % 2 == 0 ? TARJETA : FILA_ALTERNA);
                setForeground(TEXTO);
            }
            return this;
        }
    }

    /**
     * Columna de estado: el texto se muestra centrado y con el color del estado.
     */
    public static class RendererEstado extends RendererFilas {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            Color[] colores = coloresEstado(value == null ? null : value.toString());
            setBackground(colores[0]);
            setForeground(colores[1]);
            setFont(ETIQUETA);
            setBorder(new CompoundBorder(new EmptyBorder(3, 6, 3, 6), new LineBorder(colores[0].darker(), 1, true)));
            return this;
        }
    }

    /**
     * Columna numérica centrada; si se indica, en rojo cuando vale 0 (p. ej. plazas
     * libres).
     */
    public static class RendererNumero extends RendererFilas {
        private final boolean resaltarCero;

        public RendererNumero(boolean resaltarCero) {
            this.resaltarCero = resaltarCero;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            if (resaltarCero && value instanceof Number) {
                boolean completo = ((Number) value).intValue() <= 0;
                setForeground(completo ? PELIGRO : EXITO);
                setFont(ETIQUETA);
            }
            return this;
        }
    }
}
