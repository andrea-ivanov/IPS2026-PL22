package view.secretario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;

import static view.comun.Estilo.*;

/**
 * Vista de Secretaría con el estilo de la vista de Estudiante: fondo claro, tarjetas blancas,
 * tablas con filas alternas y estados de color. Los listados de matrículas y de grupos se muestran
 * por páginas (las filas de cada página las carga el controlador).
 */
public class SecretarioView {
    public static final int COL_MATRICULA = 0;
    public static final int COL_CURSO_ID = 1;
    public static final int COL_ESTADO = 8;
    public static final int COL_ACCION = 9;
    public static final int COL_GRUPO_CURSO_ID = 0;
    public static final int COL_GRUPO_CODIGO = 2;
    public static final int COL_GRUPO_LIBRES = 8;

    // Opciones del filtro de situaciones que revisa el secretario
    public static final String SITUACION_TODAS = "TODAS";
    public static final String SITUACION_PAGO_PENDIENTE = "Pago pendiente de validar";
    public static final String SITUACION_SIN_PAGAR = "Sin pagar (año iniciado)";
    public static final String SITUACION_FALTAN_GRUPOS = "Faltan grupos por asignar";

    private static final String TITULO_LISTADO = "Consulta de Matrículas y Grupos";
    private static final String TITULO_DETALLE = "Gestión del Ciclo de Vida de la Matrícula";
    private static final String TEXTO_ACCION = "Ver / Gestionar";

    private JFrame frame;

    private JLabel lblTituloCabecera;
    private JButton btnVolver;
    private JButton btnVolverInicio;
    private JButton btnActasCerradas;
    private JButton btnCargarMatriculas;
    private JButton btnLimpiarFiltros;
    private JLabel lblResultados;
    private PanelPaginacion paginacionMatriculas;

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
    private JLabel lblTituloAlumnosGrupo;
    private JLabel lblResultadosGrupos;
    private PanelPaginacion paginacionGrupos;

    // Pestaña de propuestas de microcredenciales
    private JTable tabPropuestas;
    private JButton btnAceptarPropuesta;
    private JButton btnRechazarPropuesta;
    private JButton btnRecargarPropuestas;
    private JLabel lblResultadosPropuestas;

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
        frame.setBounds(100, 40, 1320, 780);
        frame.setMinimumSize(new Dimension(1000, 620));
        maximizarCentrado(frame);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel contenido = new JPanel(new BorderLayout(0, 12));
        contenido.setBackground(FONDO);
        contenido.setBorder(new EmptyBorder(14, 18, 14, 18));
        frame.setContentPane(contenido);

        contenido.add(crearCabecera(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setOpaque(false);
        JTabbedPane tabsListado = new JTabbedPane();
        tabsListado.setFont(TITULO_TARJETA);
        tabsListado.addTab("  Matrículas  ", createListadoPanel());
        tabsListado.addTab("  Grupos  ", createGruposPanel());
        tabsListado.addTab("  Propuestas de Microcredenciales  ", createPropuestasPanel());
        cardsPanel.add(tabsListado, "LISTADO");
        cardsPanel.add(createDetallePanel(), "DETALLE");
        contenido.add(cardsPanel, BorderLayout.CENTER);

        btnVolver.addActionListener(e -> showListadoView());
    }

    private JPanel crearCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout(10, 0));
        cabecera.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setOpaque(false);
        JLabel lblSeccion = textoSecundario("PANEL DE SECRETARÍA");
        lblSeccion.setFont(ETIQUETA);
        lblSeccion.setForeground(PRIMARIO);
        lblTituloCabecera = new JLabel(TITULO_LISTADO);
        lblTituloCabecera.setFont(TITULO);
        lblTituloCabecera.setForeground(TEXTO);
        textos.add(lblSeccion);
        textos.add(Box.createVerticalStrut(2));
        textos.add(lblTituloCabecera);
        cabecera.add(textos, BorderLayout.WEST);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        botones.setOpaque(false);
        btnActasCerradas = botonSecundario("Actas cerradas por el sistema");
        btnActasCerradas.setForeground(new Color(133, 100, 4));
        btnActasCerradas.setToolTipText("Actas que el sistema cerró al terminar la convocatoria (HU 35239)");
        btnVolver = botonSecundario("‹  Volver al listado");
        btnVolver.setVisible(false);
        btnVolverInicio = botonSecundario("Volver al inicio");
        btnVolverInicio.setToolTipText("Volver a la pantalla de selección de rol");
        botones.add(btnActasCerradas);
        botones.add(btnVolver);
        botones.add(btnVolverInicio);
        cabecera.add(botones, BorderLayout.EAST);
        return cabecera;
    }

    // =========================================================================
    // Pestaña de matrículas
    // =========================================================================

    private JPanel createListadoPanel() {
        JPanel panel = panelPestana();

        JPanel filterPanel = crearTarjetaFiltros("Filtros de búsqueda");
        txtFiltroEstudiante = new JTextField();
        addFiltro(filterPanel, "Estudiante (nombre o DNI)", txtFiltroEstudiante);
        cbFiltroAnio = new JComboBox<>(new String[] { "TODOS" });
        addFiltro(filterPanel, "Año académico", cbFiltroAnio);
        cbFiltroGrado = new JComboBox<>(new String[] { "TODOS" });
        addFiltro(filterPanel, "Grado", cbFiltroGrado);
        cbFiltroCurso = new JComboBox<>(new String[] { "TODOS" });
        addFiltro(filterPanel, "Curso", cbFiltroCurso);
        txtFiltroGrupo = new JTextField();
        addFiltro(filterPanel, "Grupo (p. ej. PL1)", txtFiltroGrupo);
        cbFiltroEstado = new JComboBox<>(
                new String[] { "TODOS", "SOLICITADA", "PAGADA", "ACTIVA", "FINALIZADA", "CANCELADA" });
        addFiltro(filterPanel, "Estado", cbFiltroEstado);
        cbFiltroSituacion = new JComboBox<>(new String[] { SITUACION_TODAS, SITUACION_PAGO_PENDIENTE,
                SITUACION_SIN_PAGAR, SITUACION_FALTAN_GRUPOS });
        addFiltro(filterPanel, "Situación a revisar", cbFiltroSituacion);

        btnCargarMatriculas = botonPrimario("Filtrar / Recargar");
        btnLimpiarFiltros = botonSecundario("Limpiar filtros");
        addBotonesFiltro(filterPanel, btnCargarMatriculas, btnLimpiarFiltros);
        panel.add(envolverFiltros(filterPanel), BorderLayout.WEST);

        String[] columnNames = { "Matrícula", "Cód. Curso", "Año", "DNI", "Estudiante", "Grado", "Curso", "Grupos",
                "Estado", "Acción" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == COL_ACCION;
            }
        };
        tabMatriculas = new JTable(model);
        estilizarTabla(tabMatriculas);
        tabMatriculas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // El código del curso solo se usa para identificar la fila
        ocultarColumna(tabMatriculas, COL_CURSO_ID);
        anchos(tabMatriculas, 80, 0, 85, 95, 180, 190, 190, 100, 110, 135);
        anchoMinimo(tabMatriculas, 75, COL_MATRICULA);
        anchoMinimo(tabMatriculas, 85, 2, 3);
        anchoMinimo(tabMatriculas, 110, COL_ESTADO);
        anchoMinimo(tabMatriculas, 135, COL_ACCION);
        tabMatriculas.getColumnModel().getColumn(COL_ESTADO).setCellRenderer(new RendererEstado());
        setupTableGestionarButton(tabMatriculas, COL_ACCION);

        JPanel tablaCard = tarjeta(new BorderLayout(0, 10));
        JPanel tituloTabla = new JPanel(new BorderLayout());
        tituloTabla.setOpaque(false);
        tituloTabla.add(tituloTarjeta("Matrículas por curso"), BorderLayout.WEST);
        tituloTabla.add(textoSecundario("Pulse \"" + TEXTO_ACCION + "\" para ver el detalle y gestionar la matrícula"),
                BorderLayout.EAST);
        tablaCard.add(tituloTabla, BorderLayout.NORTH);
        tablaCard.add(scrollTabla(tabMatriculas), BorderLayout.CENTER);

        lblResultados = textoSecundario(" ");
        paginacionMatriculas = new PanelPaginacion();
        tablaCard.add(crearPie(lblResultados, paginacionMatriculas), BorderLayout.SOUTH);

        panel.add(tablaCard, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Pestaña de grupos (HU 35226)
    // =========================================================================

    /**
     * HU 35226: consulta de los grupos (plazas, profesores) y de los alumnos de
     * cada grupo en un año académico.
     */
    private JPanel createGruposPanel() {
        JPanel panel = panelPestana();

        JPanel filterPanel = crearTarjetaFiltros("Filtros de grupos");
        cbGruposAnio = new JComboBox<>();
        addFiltro(filterPanel, "Año académico", cbGruposAnio);
        cbGruposGrado = new JComboBox<>(new String[] { "TODOS" });
        addFiltro(filterPanel, "Grado", cbGruposGrado);
        cbGruposCurso = new JComboBox<>(new String[] { "TODOS" });
        addFiltro(filterPanel, "Curso", cbGruposCurso);
        btnCargarGrupos = botonPrimario("Filtrar / Recargar");
        addBotonesFiltro(filterPanel, btnCargarGrupos);
        JLabel ayuda = textoSecundario("<html>Ocupan plaza los cursos PAGADOS o ACTIVOS del año seleccionado.</html>");
        ayuda.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterPanel.add(Box.createVerticalStrut(12));
        filterPanel.add(ayuda);
        panel.add(envolverFiltros(filterPanel), BorderLayout.WEST);

        String[] columnasGrupos = { "Cód. Curso", "Curso", "Grupo", "Tipo", "Grados", "Profesores", "Capacidad",
                "Ocupadas", "Libres" };
        tabGrupos = new JTable(new DefaultTableModel(columnasGrupos, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        estilizarTabla(tabGrupos);
        tabGrupos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ocultarColumna(tabGrupos, COL_GRUPO_CURSO_ID);
        anchos(tabGrupos, 0, 200, 60, 170, 230, 210, 80, 80, 65);
        anchoMinimo(tabGrupos, 55, 2);
        anchoMinimo(tabGrupos, 80, 6, 7);
        anchoMinimo(tabGrupos, 60, COL_GRUPO_LIBRES);
        tabGrupos.getColumnModel().getColumn(2).setCellRenderer(new RendererNumero(false));
        tabGrupos.getColumnModel().getColumn(6).setCellRenderer(new RendererNumero(false));
        tabGrupos.getColumnModel().getColumn(7).setCellRenderer(new RendererNumero(false));
        tabGrupos.getColumnModel().getColumn(COL_GRUPO_LIBRES).setCellRenderer(new RendererNumero(true));

        JPanel gruposCard = tarjeta(new BorderLayout(0, 10));
        JPanel tituloGrupos = new JPanel(new BorderLayout());
        tituloGrupos.setOpaque(false);
        tituloGrupos.add(tituloTarjeta("Grupos"), BorderLayout.WEST);
        tituloGrupos.add(textoSecundario("Seleccione un grupo para ver sus alumnos"), BorderLayout.EAST);
        gruposCard.add(tituloGrupos, BorderLayout.NORTH);
        gruposCard.add(scrollTabla(tabGrupos), BorderLayout.CENTER);
        lblResultadosGrupos = textoSecundario(" ");
        paginacionGrupos = new PanelPaginacion();
        gruposCard.add(crearPie(lblResultadosGrupos, paginacionGrupos), BorderLayout.SOUTH);

        String[] columnasAlumnos = { "Matrícula", "DNI", "Estudiante", "Grado", "Estado" };
        tabAlumnosGrupo = new JTable(new DefaultTableModel(columnasAlumnos, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        estilizarTabla(tabAlumnosGrupo);
        anchos(tabAlumnosGrupo, 90, 100, 220, 300, 110);
        anchoMinimo(tabAlumnosGrupo, 110, 4);
        tabAlumnosGrupo.getColumnModel().getColumn(4).setCellRenderer(new RendererEstado());

        JPanel alumnosCard = tarjeta(new BorderLayout(0, 8));
        lblTituloAlumnosGrupo = tituloTarjeta(" ");
        alumnosCard.add(lblTituloAlumnosGrupo, BorderLayout.NORTH);
        alumnosCard.add(scrollTabla(tabAlumnosGrupo), BorderLayout.CENTER);
        setTituloAlumnosGrupo("Alumnos del grupo (seleccione un grupo)");

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, gruposCard, alumnosCard);
        split.setResizeWeight(0.62);
        split.setBorder(null);
        split.setDividerSize(10);
        split.setOpaque(false);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    /** Cambia el título de la lista de alumnos del grupo seleccionado. */
    public void setTituloAlumnosGrupo(String titulo) {
        lblTituloAlumnosGrupo.setText(titulo);
    }

    // =========================================================================
    // Pestaña de propuestas de microcredenciales
    // =========================================================================

    private JPanel createPropuestasPanel() {
        JPanel panel = panelPestana();

        JPanel filterPanel = crearTarjetaFiltros("Gestión de propuestas");
        btnAceptarPropuesta = botonPrimario("Aceptar Propuesta");
        btnAceptarPropuesta.setToolTipText("Aprobar propuesta para registrarla como microcredencial oficial");
        btnRechazarPropuesta = botonSecundario("Rechazar Propuesta");
        btnRechazarPropuesta.setForeground(new Color(185, 28, 28));
        btnRechazarPropuesta.setToolTipText("Rechazar propuesta indicando obligatoriamente el motivo");
        btnRecargarPropuestas = botonSecundario("Actualizar listado");

        addBotonesFiltro(filterPanel, btnAceptarPropuesta, btnRechazarPropuesta, btnRecargarPropuestas);

        JLabel ayuda = textoSecundario("<html>Seleccione una propuesta pendiente para revisarla.<br><br>"
                + "• <b>Aceptar</b>: La propuesta pasará a ACEPTADA y se registrará oficialmente como microcredencial disponible en el sistema.<br><br>"
                + "• <b>Rechazar</b>: Se exigirá introducir obligatoriamente el motivo del rechazo. La propuesta quedará invalidada.</html>");
        ayuda.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterPanel.add(Box.createVerticalStrut(12));
        filterPanel.add(ayuda);
        panel.add(envolverFiltros(filterPanel), BorderLayout.WEST);

        String[] columnasPropuestas = { "ID Propuesta", "Nombre del Curso", "Profesor Proponente", "Créditos",
                "Periodo / Año", "Grado", "Fecha Envío" };
        tabPropuestas = new JTable(new DefaultTableModel(columnasPropuestas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        estilizarTabla(tabPropuestas);
        tabPropuestas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        anchos(tabPropuestas, 100, 240, 200, 80, 110, 180, 100);
        anchoMinimo(tabPropuestas, 90, 0);
        anchoMinimo(tabPropuestas, 70, 3);
        anchoMinimo(tabPropuestas, 90, 4);

        JPanel propuestasCard = tarjeta(new BorderLayout(0, 10));
        JPanel tituloPropuestas = new JPanel(new BorderLayout());
        tituloPropuestas.setOpaque(false);
        tituloPropuestas.add(tituloTarjeta("Propuestas de Microcredenciales Pendientes"), BorderLayout.WEST);
        tituloPropuestas.add(textoSecundario("Listado de propuestas pendientes de validación"), BorderLayout.EAST);
        propuestasCard.add(tituloPropuestas, BorderLayout.NORTH);
        propuestasCard.add(scrollTabla(tabPropuestas), BorderLayout.CENTER);

        lblResultadosPropuestas = textoSecundario("0 propuestas pendientes");
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        lblResultadosPropuestas.setFont(ETIQUETA);
        pie.add(lblResultadosPropuestas, BorderLayout.WEST);
        propuestasCard.add(pie, BorderLayout.SOUTH);

        panel.add(propuestasCard, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Detalle de una matrícula (HU 35229)
    // =========================================================================

    private JPanel createDetallePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);

        lblDetalleId = valorDetalle();
        lblDetalleAnio = valorDetalle();
        lblDetalleEstudiante = valorDetalle();
        lblDetalleEmail = valorDetalle();
        lblDetalleGrado = valorDetalle();
        lblDetalleCurso = valorDetalle();
        lblDetalleGrupos = valorDetalle();
        lblDetalleEstado = valorDetalle();
        lblDetalleEstado.setBorder(new EmptyBorder(3, 10, 3, 10));
        lblDetalleEstadoMatricula = valorDetalle();
        lblDetalleEstadoMatricula.setBorder(new EmptyBorder(3, 10, 3, 10));
        lblDetallePago = valorDetalle();
        lblDetalleCancelacion = valorDetalle();

        JPanel infoCard = tarjeta(new BorderLayout(0, 12));
        infoCard.setBorder(bordeTarjeta(16, 20));
        infoCard.add(tituloTarjeta("Información de la matrícula"), BorderLayout.NORTH);

        JPanel datos = new JPanel(new GridBagLayout());
        datos.setOpaque(false);
        int fila = 0;
        fila = addDato(datos, fila, "Matrícula", lblDetalleId, "Año académico", lblDetalleAnio);
        fila = addDato(datos, fila, "Estudiante", lblDetalleEstudiante, "Correo electrónico", lblDetalleEmail);
        fila = addDato(datos, fila, "Grado", lblDetalleGrado, "Curso", lblDetalleCurso);
        fila = addDato(datos, fila, "Estado del curso", lblDetalleEstado, "Estado de la matrícula",
                lblDetalleEstadoMatricula);
        fila = addDato(datos, fila, "Grupos asignados", lblDetalleGrupos, "Pago", lblDetallePago);
        addDato(datos, fila, "Cancelación", lblDetalleCancelacion, null, null);
        infoCard.add(datos, BorderLayout.CENTER);
        panel.add(infoCard, BorderLayout.NORTH);

        JPanel accionesCard = tarjeta(new BorderLayout(0, 10));
        accionesCard.setBorder(bordeTarjeta(16, 20));
        JPanel tituloAcciones = new JPanel(new BorderLayout());
        tituloAcciones.setOpaque(false);
        tituloAcciones.add(tituloTarjeta("Gestión del ciclo de vida"), BorderLayout.WEST);
        tituloAcciones.add(textoSecundario("Solo se habilitan las acciones válidas para el estado actual"),
                BorderLayout.EAST);
        accionesCard.add(tituloAcciones, BorderLayout.NORTH);

        btnAceptarSolicitud = botonExito("Aceptar solicitud (registrar pago)");
        btnModificarMatricula = botonSecundario("Modificar matrícula");
        btnAsignarGrupos = botonPrimario("Asignar grupos");
        btnCancelarMatricula = botonPeligro("Cancelar matrícula");

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        botones.setOpaque(false);
        botones.add(btnAceptarSolicitud);
        botones.add(btnModificarMatricula);
        botones.add(btnAsignarGrupos);
        botones.add(btnCancelarMatricula);
        accionesCard.add(botones, BorderLayout.CENTER);

        JPanel norteAcciones = new JPanel(new BorderLayout());
        norteAcciones.setOpaque(false);
        norteAcciones.add(accionesCard, BorderLayout.NORTH);
        panel.add(norteAcciones, BorderLayout.CENTER);
        return panel;
    }

    private static JLabel valorDetalle() {
        JLabel label = new JLabel("-");
        label.setFont(NORMAL);
        label.setForeground(TEXTO);
        return label;
    }

    /** Añade una fila de la ficha con dos pares etiqueta / valor y devuelve la siguiente fila. */
    private static int addDato(JPanel panel, int fila, String etiqueta1, JLabel valor1, String etiqueta2,
            JLabel valor2) {
        addPar(panel, fila, 0, etiqueta1, valor1);
        if (etiqueta2 != null) {
            addPar(panel, fila, 2, etiqueta2, valor2);
        }
        return fila + 1;
    }

    private static void addPar(JPanel panel, int fila, int columna, String etiqueta, JLabel valor) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = fila;
        c.insets = new Insets(5, columna == 0 ? 0 : 30, 5, 18);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = columna;
        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(ETIQUETA);
        lbl.setForeground(TEXTO_SECUNDARIO);
        panel.add(lbl, c);

        c.gridx = columna + 1;
        c.insets = new Insets(5, 0, 5, 0);
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        panel.add(valor, c);
    }

    // =========================================================================
    // Auxiliares de construcción
    // =========================================================================

    private static JPanel panelPestana() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(FONDO);
        panel.setBorder(new EmptyBorder(12, 2, 2, 2));
        return panel;
    }

    private static JPanel crearTarjetaFiltros(String titulo) {
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

    /** Tarjeta de filtros con ancho fijo y desplazamiento vertical si la ventana es baja. */
    private static JComponent envolverFiltros(JPanel filterPanel) {
        JPanel tarjeta = new TarjetaAnchoFijo();
        tarjeta.add(filterPanel, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(tarjeta, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(260, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    /**
     * Tarjeta que dentro de un JScrollPane ocupa siempre el ancho visible (los textos largos se ajustan
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
            // Si la ventana es alta la tarjeta llega hasta abajo; si es baja aparece la barra
            return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
        }
    }

    private static void addFiltro(JPanel panel, String etiqueta, JComponent field) {
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

    private static void addBotonesFiltro(JPanel panel, JButton... botones) {
        panel.add(Box.createVerticalStrut(4));
        for (JButton boton : botones) {
            boton.setAlignmentX(Component.LEFT_ALIGNMENT);
            boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            panel.add(boton);
            panel.add(Box.createVerticalStrut(8));
        }
    }

    /** Pie de una tabla: texto con las filas mostradas a la izquierda y la paginación a la derecha. */
    private static JPanel crearPie(JLabel resultados, PanelPaginacion paginacion) {
        JPanel pie = new JPanel(new BorderLayout(10, 0));
        pie.setOpaque(false);
        resultados.setFont(ETIQUETA);
        pie.add(resultados, BorderLayout.WEST);
        if (paginacion != null) {
            pie.add(paginacion, BorderLayout.EAST);
        }
        return pie;
    }

    private static void ocultarColumna(JTable tabla, int columna) {
        TableColumnModel columnas = tabla.getColumnModel();
        columnas.getColumn(columna).setMinWidth(0);
        columnas.getColumn(columna).setMaxWidth(0);
        columnas.getColumn(columna).setWidth(0);
        columnas.getColumn(columna).setPreferredWidth(0);
    }

    private static void anchos(JTable tabla, int... anchos) {
        for (int i = 0; i < anchos.length; i++) {
            if (anchos[i] > 0) {
                tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
            }
        }
    }

    /** Ancho mínimo de columnas cortas (año, DNI, estado...) para que no se recorten al estrechar la ventana. */
    private static void anchoMinimo(JTable tabla, int ancho, int... columnas) {
        for (int columna : columnas) {
            tabla.getColumnModel().getColumn(columna).setMinWidth(ancho);
        }
    }

    public void showListadoView() {
        cardLayout.show(cardsPanel, "LISTADO");
        btnVolver.setVisible(false);
        lblTituloCabecera.setText(TITULO_LISTADO);
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
        pintarEstado(lblDetalleEstado, estado);
        lblDetalleEstadoMatricula.setText(estadoMatricula);
        pintarEstado(lblDetalleEstadoMatricula, estadoMatricula);
        lblDetallePago.setText(pago);
        lblDetalleCancelacion.setText(cancelacion);

        boolean cerrada = "CANCELADA".equals(estado) || "FINALIZADA".equals(estado);
        btnAceptarSolicitud.setEnabled("SOLICITADA".equals(estado));
        btnModificarMatricula.setEnabled(!cerrada);
        btnAsignarGrupos.setEnabled("PAGADA".equals(estado) || "ACTIVA".equals(estado));
        btnCancelarMatricula.setEnabled(!cerrada);

        cardLayout.show(cardsPanel, "DETALLE");
        btnVolver.setVisible(true);
        lblTituloCabecera.setText(TITULO_DETALLE);
    }

    private void setupTableGestionarButton(JTable table, int columnIdx) {
        table.getColumnModel().getColumn(columnIdx).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(columnIdx).setCellEditor(new ButtonEditor(new JCheckBox()));
    }

    /** Botón "Ver / Gestionar" de cada fila: blanco con borde y texto azul, centrado en la celda. */
    private static JButton crearBotonFila() {
        return botonFila(TEXTO_ACCION);
    }

    /** Celda con el botón dentro y un pequeño margen, con el fondo de la fila. */
    private static JPanel envolverBotonFila(JButton button) {
        JPanel celda = new JPanel(new BorderLayout());
        celda.setBorder(new EmptyBorder(3, 8, 3, 8));
        celda.add(button, BorderLayout.CENTER);
        return celda;
    }

    class ButtonRenderer implements javax.swing.table.TableCellRenderer {
        private final JButton button = crearBotonFila();
        private final JPanel celda = envolverBotonFila(button);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            celda.setBackground(isSelected ? SELECCION : (row % 2 == 0 ? TARJETA : FILA_ALTERNA));
            return celda;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private final JPanel celda;
        private boolean isPushed;
        private JTable table;
        private int row;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            setClickCountToStart(1);
            button = crearBotonFila();
            button.setBackground(SELECCION);
            button.addActionListener(e -> fireEditingStopped());
            celda = envolverBotonFila(button);
            celda.setBackground(SELECCION);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                int column) {
            this.table = table;
            this.row = row;
            isPushed = true;
            return celda;
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
            return TEXTO_ACCION;
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

    public JButton getBtnActasCerradas() {
        return btnActasCerradas;
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

    public PanelPaginacion getPaginacionMatriculas() {
        return paginacionMatriculas;
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

    public PanelPaginacion getPaginacionGrupos() {
        return paginacionGrupos;
    }

    public JTable getTabPropuestas() {
        return tabPropuestas;
    }

    public JButton getBtnAceptarPropuesta() {
        return btnAceptarPropuesta;
    }

    public JButton getBtnRechazarPropuesta() {
        return btnRechazarPropuesta;
    }

    public JButton getBtnRecargarPropuestas() {
        return btnRecargarPropuestas;
    }

    public JLabel getLblResultadosPropuestas() {
        return lblResultadosPropuestas;
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
