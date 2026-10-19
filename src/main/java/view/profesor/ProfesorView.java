package view.profesor;

import model.asignaciondocente.RolDocente;
import model.grado.GradoDisplayDTO;
import model.grupo.TipoGrupo;
import model.profesor.ProfesorDTO;
import view.comun.Estilo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.stream.Stream;

public class ProfesorView {
    /** Opción de los combos de filtro que no filtra */
    public static final String FILTRO_TODOS = "TODOS";

    private JFrame frame;
    private JComboBox<ProfesorDTO> cbProfesores;
    private JButton btnCargarAsignaciones;
    private JButton btnVolverInicio;
    private JComboBox<String> cbFiltroGrado;
    private JComboBox<String> cbFiltroCurso;
    private JComboBox<String> cbFiltroTipo;
    private JComboBox<String> cbFiltroRol;
    private JTable tabAsignaciones;
    private JComboBox<String> cbActas;
    private JLabel lblEstadoActa;
    private JTable tabNotas;
    private JButton btnGuardarNotas;
    private JButton btnCerrarActa;
    // Pestaña de propuestas (HU 35243)
    private JTextField txtNombrePropuesta;
    private JTextField txtAnioPropuesta;
    private JTextField txtCreditosPropuesta;
    private JTextArea txtDescripcionPropuesta;
    private JList<GradoDisplayDTO> listGradosPropuesta;
    private JLabel lblProfesorPropuesta;
    private JButton btnEnviarPropuesta;
    private JTable tabPropuestas;

    public ProfesorView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Panel de Profesor - Asignaciones Docentes y Propuestas");
        frame.setBounds(150, 100, 1200, 720);
        frame.setMinimumSize(new Dimension(1000, 620));
        // la X cierra solo esta ventana; el controlador vuelve entonces a la pantalla de selección de rol
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel contenido = Estilo.contenido(new BorderLayout(0, 12));
        frame.setContentPane(contenido);
        Estilo.maximizarCentrado(frame);

        btnVolverInicio = Estilo.botonSecundario("Volver al inicio");
        btnVolverInicio.setToolTipText("Volver a la pantalla de selección de rol");
        JPanel panelProfesor = Estilo.cabecera("PANEL DE PROFESOR", new JLabel("Asignaciones Docentes y Propuestas"),
                btnVolverInicio);

        JPanel panelSelecProfesor = Estilo.tarjeta(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelSelecProfesor.setBorder(Estilo.bordeTarjeta(10, 14));
        panelSelecProfesor.add(Estilo.etiqueta("Profesor"));
        // se puede escribir para buscar (ComboBuscable, lo configura el controlador al cargar los profesores)
        cbProfesores = new JComboBox<>();
        Estilo.estilizarCampo(cbProfesores);
        cbProfesores.setPreferredSize(new Dimension(360, 30));
        panelSelecProfesor.add(cbProfesores);

        // carga las dos pestañas (asignaciones y propuestas) del profesor elegido
        btnCargarAsignaciones = Estilo.botonPrimario("Cargar profesor");
        panelSelecProfesor.add(btnCargarAsignaciones);
        panelSelecProfesor.add(Estilo.textoSecundario("Puede escribir en el desplegable para buscar un profesor"));

        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.setOpaque(false);
        panelSur.setBorder(new EmptyBorder(10, 0, 0, 0));
        panelSur.add(panelSelecProfesor, BorderLayout.CENTER);
        panelProfesor.add(panelSur, BorderLayout.SOUTH);
        contenido.add(panelProfesor, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        Estilo.estilizarPestanas(tabs);
        tabs.addTab("  Mis asignaciones  ", createAsignacionesPanel());
        tabs.addTab("  Proponer asignatura  ", createPropuestasPanel());
        contenido.add(tabs, BorderLayout.CENTER);
    }

    /** Pestaña de la HU 35239: asignaciones docentes con sus filtros, y actas y notas de la seleccionada */
    private JPanel createAsignacionesPanel() {
        JPanel panel = panelPestana();

        // Filtros: grado y curso se rellenan con las asignaciones del profesor al cargarlas
        JPanel panelFiltros = Estilo.tarjeta(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelFiltros.setBorder(Estilo.bordeTarjeta(10, 14));
        JLabel lblFiltros = Estilo.tituloTarjeta("Filtros");
        lblFiltros.setBorder(new EmptyBorder(0, 0, 0, 10));
        panelFiltros.add(lblFiltros);
        cbFiltroGrado = new JComboBox<>(new String[] { FILTRO_TODOS });
        addFiltro(panelFiltros, "Grado", cbFiltroGrado, 260);
        cbFiltroCurso = new JComboBox<>(new String[] { FILTRO_TODOS });
        addFiltro(panelFiltros, "Curso", cbFiltroCurso, 260);
        cbFiltroTipo = new JComboBox<>(opcionesFiltro(TipoGrupo.values()));
        addFiltro(panelFiltros, "Grupo", cbFiltroTipo, 110);
        cbFiltroRol = new JComboBox<>(opcionesFiltro(RolDocente.values()));
        addFiltro(panelFiltros, "Rol", cbFiltroRol, 150);
        panel.add(panelFiltros, BorderLayout.NORTH);

        tabAsignaciones = Estilo.tablaQueLlena();
        Estilo.estilizarTabla(tabAsignaciones);
        tabAsignaciones.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabAsignaciones.setDefaultEditor(Object.class, null);
        JPanel panelAsignaciones = Estilo.tarjeta(new BorderLayout(0, 10));
        JPanel tituloAsignaciones = new JPanel(new BorderLayout());
        tituloAsignaciones.setOpaque(false);
        tituloAsignaciones.add(Estilo.tituloTarjeta("Mis asignaciones docentes"), BorderLayout.WEST);
        tituloAsignaciones.add(Estilo.textoSecundario("Seleccione una asignación para ver sus actas"),
                BorderLayout.EAST);
        panelAsignaciones.add(tituloAsignaciones, BorderLayout.NORTH);
        panelAsignaciones.add(Estilo.scrollTabla(tabAsignaciones), BorderLayout.CENTER);

        // Actas y notas de la asignatura seleccionada en la tabla de asignaciones
        JPanel panelActa = Estilo.tarjeta(new BorderLayout(0, 10));
        JPanel panelSelecActa = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelSelecActa.setOpaque(false);
        JLabel lblTituloActa = Estilo.tituloTarjeta("Acta de la asignatura seleccionada");
        lblTituloActa.setBorder(new EmptyBorder(0, 0, 0, 10));
        panelSelecActa.add(lblTituloActa);
        panelSelecActa.add(Estilo.etiqueta("Acta"));
        cbActas = new JComboBox<>();
        Estilo.estilizarCampo(cbActas);
        cbActas.setPreferredSize(new Dimension(300, 30));
        panelSelecActa.add(cbActas);
        lblEstadoActa = new JLabel("Selecciona una asignacion");
        lblEstadoActa.setFont(Estilo.ETIQUETA);
        lblEstadoActa.setForeground(Estilo.PRIMARIO_OSCURO);
        lblEstadoActa.setOpaque(true);
        lblEstadoActa.setBackground(Estilo.SELECCION);
        lblEstadoActa.setBorder(new EmptyBorder(5, 10, 5, 10));
        panelSelecActa.add(lblEstadoActa);
        panelActa.add(panelSelecActa, BorderLayout.NORTH);

        tabNotas = Estilo.tablaQueLlena();
        Estilo.estilizarTabla(tabNotas);
        tabNotas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panelActa.add(Estilo.scrollTabla(tabNotas), BorderLayout.CENTER);

        btnGuardarNotas = Estilo.botonPrimario("Guardar Notas");
        btnGuardarNotas.setEnabled(false);
        btnCerrarActa = Estilo.botonPeligro("Cerrar Acta");
        btnCerrarActa.setEnabled(false);
        panelActa.add(Estilo.barraAcciones(btnGuardarNotas, btnCerrarActa), BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelAsignaciones, panelActa);
        splitPane.setResizeWeight(0.4);
        splitPane.setBorder(null);
        splitPane.setDividerSize(10);
        splitPane.setOpaque(false);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Pestaña de la HU 35243: formulario para proponer una asignatura en uno o varios de los grados
     * en los que imparte el profesor, y listado "Mis propuestas"
     */
    private JPanel createPropuestasPanel() {
        JPanel panel = panelPestana();

        JPanel panelFormulario = Estilo.tarjeta(new BorderLayout(20, 10));
        panelFormulario.setBorder(Estilo.bordeTarjeta(14, 18));
        panelFormulario.add(Estilo.tituloTarjeta("Nueva propuesta de asignatura"), BorderLayout.NORTH);

        JPanel panelCampos = new JPanel(new GridBagLayout());
        panelCampos.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 10);
        c.anchor = GridBagConstraints.WEST;
        txtNombrePropuesta = new JTextField(28);
        txtAnioPropuesta = new JTextField(4);
        txtCreditosPropuesta = new JTextField(4);
        txtDescripcionPropuesta = new JTextArea(4, 28);
        txtDescripcionPropuesta.setLineWrap(true);
        txtDescripcionPropuesta.setWrapStyleWord(true);
        Estilo.estilizarCampo(txtNombrePropuesta);
        Estilo.estilizarCampo(txtAnioPropuesta);
        Estilo.estilizarCampo(txtCreditosPropuesta);
        Estilo.estilizarCampo(txtDescripcionPropuesta);
        JScrollPane scrollDescripcion = new JScrollPane(txtDescripcionPropuesta);
        scrollDescripcion.setBorder(new LineBorder(Estilo.BORDE, 1, true));
        addCampo(panelCampos, c, 0, "Nombre", txtNombrePropuesta);
        addCampo(panelCampos, c, 1, "Año del grado (1-6)", txtAnioPropuesta);
        addCampo(panelCampos, c, 2, "Créditos", txtCreditosPropuesta);
        addCampo(panelCampos, c, 3, "Descripción (opcional)", scrollDescripcion);
        panelFormulario.add(panelCampos, BorderLayout.WEST);

        // grados en los que imparte el profesor, con su estado; se pueden elegir varios (Ctrl/Mayús + clic)
        JPanel panelGrados = new JPanel(new BorderLayout(0, 6));
        panelGrados.setOpaque(false);
        panelGrados.add(Estilo.etiqueta("Grados (puedes elegir varios)"), BorderLayout.NORTH);
        listGradosPropuesta = new JList<>();
        listGradosPropuesta.setFont(Estilo.NORMAL);
        listGradosPropuesta.setSelectionBackground(Estilo.SELECCION);
        listGradosPropuesta.setSelectionForeground(Estilo.TEXTO);
        listGradosPropuesta.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listGradosPropuesta.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof GradoDisplayDTO) {
                    GradoDisplayDTO g = (GradoDisplayDTO) value;
                    setText(g.getNombre() + " (" + g.getEstado() + ")");
                }
                setBorder(new EmptyBorder(4, 8, 4, 8));
                return this;
            }
        });
        JScrollPane scrollGrados = new JScrollPane(listGradosPropuesta);
        scrollGrados.setBorder(new LineBorder(Estilo.BORDE, 1, true));
        scrollGrados.setPreferredSize(new Dimension(420, 140));
        panelGrados.add(scrollGrados, BorderLayout.CENTER);
        panelFormulario.add(panelGrados, BorderLayout.EAST);

        JPanel panelBotonesPropuesta = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        panelBotonesPropuesta.setOpaque(false);
        lblProfesorPropuesta = Estilo.textoSecundario("Carga un profesor para proponer asignaturas");
        lblProfesorPropuesta.setFont(Estilo.ETIQUETA);
        panelBotonesPropuesta.add(lblProfesorPropuesta);
        btnEnviarPropuesta = Estilo.botonPrimario("Enviar propuesta");
        btnEnviarPropuesta.setEnabled(false);
        panelBotonesPropuesta.add(btnEnviarPropuesta);
        panelFormulario.add(panelBotonesPropuesta, BorderLayout.SOUTH);
        panel.add(panelFormulario, BorderLayout.NORTH);

        JPanel panelMisPropuestas = Estilo.tarjeta(new BorderLayout(0, 10));
        panelMisPropuestas.add(Estilo.tituloTarjeta("Mis propuestas"), BorderLayout.NORTH);
        tabPropuestas = Estilo.tablaQueLlena();
        Estilo.estilizarTabla(tabPropuestas);
        tabPropuestas.setDefaultEditor(Object.class, null);
        panelMisPropuestas.add(Estilo.scrollTabla(tabPropuestas), BorderLayout.CENTER);
        panel.add(panelMisPropuestas, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel panelPestana() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Estilo.FONDO);
        panel.setBorder(new EmptyBorder(12, 2, 2, 2));
        return panel;
    }

    private static void addFiltro(JPanel panel, String etiqueta, JComboBox<String> combo, int ancho) {
        panel.add(Estilo.etiqueta(etiqueta));
        Estilo.estilizarCampo(combo);
        combo.setPreferredSize(new Dimension(ancho, 30));
        panel.add(combo);
    }

    private static void addCampo(JPanel panel, GridBagConstraints c, int fila, String etiqueta, JComponent campo) {
        c.gridx = 0;
        c.gridy = fila;
        c.fill = GridBagConstraints.NONE;
        panel.add(Estilo.etiqueta(etiqueta), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        if (campo instanceof JTextField) {
            campo.setPreferredSize(new Dimension(campo.getPreferredSize().width, 30));
        }
        panel.add(campo, c);
    }

    /** TODOS seguido de los valores del enum */
    private static String[] opcionesFiltro(Enum<?>[] valores) {
        return Stream.concat(Stream.of(FILTRO_TODOS), Arrays.stream(valores).map(Enum::name)).toArray(String[]::new);
    }

    public JFrame getFrame() {
        return frame;
    }

    public JComboBox<ProfesorDTO> getCbProfesores() {
        return cbProfesores;
    }

    public JButton getBtnCargarAsignaciones() {
        return btnCargarAsignaciones;
    }

    public JButton getBtnVolverInicio() {
        return btnVolverInicio;
    }

    public JComboBox<String> getCbFiltroGrado() {
        return cbFiltroGrado;
    }

    public JComboBox<String> getCbFiltroCurso() {
        return cbFiltroCurso;
    }

    public JComboBox<String> getCbFiltroTipo() {
        return cbFiltroTipo;
    }

    public JComboBox<String> getCbFiltroRol() {
        return cbFiltroRol;
    }

    public JTable getTabAsignaciones() {
        return tabAsignaciones;
    }

    public JComboBox<String> getCbActas() {
        return cbActas;
    }

    public JLabel getLblEstadoActa() {
        return lblEstadoActa;
    }

    public JTable getTabNotas() {
        return tabNotas;
    }

    public JButton getBtnGuardarNotas() {
        return btnGuardarNotas;
    }

    public JButton getBtnCerrarActa() {
        return btnCerrarActa;
    }

    public JTextField getTxtNombrePropuesta() {
        return txtNombrePropuesta;
    }

    public JTextField getTxtAnioPropuesta() {
        return txtAnioPropuesta;
    }

    public JTextField getTxtCreditosPropuesta() {
        return txtCreditosPropuesta;
    }

    public JTextArea getTxtDescripcionPropuesta() {
        return txtDescripcionPropuesta;
    }

    public JList<GradoDisplayDTO> getListGradosPropuesta() {
        return listGradosPropuesta;
    }

    public JLabel getLblProfesorPropuesta() {
        return lblProfesorPropuesta;
    }

    public JButton getBtnEnviarPropuesta() {
        return btnEnviarPropuesta;
    }

    public JTable getTabPropuestas() {
        return tabPropuestas;
    }
}
