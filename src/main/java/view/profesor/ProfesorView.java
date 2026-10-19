package view.profesor;

import model.asignaciondocente.RolDocente;
import model.grado.GradoDisplayDTO;
import model.grupo.TipoGrupo;
import model.profesor.ProfesorDTO;

import javax.swing.*;
import javax.swing.border.TitledBorder;
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
        frame.setBounds(150, 100, 980, 680);
        // la X cierra solo esta ventana; el controlador vuelve entonces a la pantalla de selección de rol
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        JPanel panelProfesor = new JPanel(new BorderLayout());
        JPanel panelSelecProfesor = new JPanel();
        panelSelecProfesor.add(new JLabel("Profesor:"));
        // se puede escribir para buscar (ComboBuscable, lo configura el controlador al cargar los profesores)
        cbProfesores = new JComboBox<>();
        cbProfesores.setPreferredSize(new Dimension(280, 26));
        panelSelecProfesor.add(cbProfesores);

        // carga las dos pestañas (asignaciones y propuestas) del profesor elegido
        btnCargarAsignaciones = new JButton("Cargar profesor");
        panelSelecProfesor.add(btnCargarAsignaciones);
        panelProfesor.add(panelSelecProfesor, BorderLayout.CENTER);

        JPanel panelVolver = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnVolverInicio = new JButton("Volver al inicio");
        btnVolverInicio.setToolTipText("Volver a la pantalla de selección de rol");
        panelVolver.add(btnVolverInicio);
        panelProfesor.add(panelVolver, BorderLayout.EAST);
        frame.getContentPane().add(panelProfesor, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Mis asignaciones", createAsignacionesPanel());
        tabs.addTab("Proponer asignatura", createPropuestasPanel());
        frame.getContentPane().add(tabs, BorderLayout.CENTER);
    }

    /** Pestaña de la HU 35239: asignaciones docentes con sus filtros, y actas y notas de la seleccionada */
    private JPanel createAsignacionesPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Filtros: grado y curso se rellenan con las asignaciones del profesor al cargarlas
        JPanel panelFiltros = new JPanel();
        panelFiltros.add(new JLabel("Grado:"));
        cbFiltroGrado = new JComboBox<>(new String[] { FILTRO_TODOS });
        panelFiltros.add(cbFiltroGrado);

        panelFiltros.add(new JLabel("Curso:"));
        cbFiltroCurso = new JComboBox<>(new String[] { FILTRO_TODOS });
        panelFiltros.add(cbFiltroCurso);

        panelFiltros.add(new JLabel("Grupo:"));
        cbFiltroTipo = new JComboBox<>(opcionesFiltro(TipoGrupo.values()));
        panelFiltros.add(cbFiltroTipo);

        panelFiltros.add(new JLabel("Rol:"));
        cbFiltroRol = new JComboBox<>(opcionesFiltro(RolDocente.values()));
        panelFiltros.add(cbFiltroRol);
        panel.add(panelFiltros, BorderLayout.NORTH);

        tabAsignaciones = new JTable();
        tabAsignaciones.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabAsignaciones.setDefaultEditor(Object.class, null);
        JScrollPane scrollPane = new JScrollPane(tabAsignaciones);

        // Actas y notas de la asignatura seleccionada en la tabla de asignaciones
        JPanel panelActa = new JPanel(new BorderLayout());
        panelActa.setBorder(new TitledBorder("Acta de la asignatura seleccionada"));

        JPanel panelSelecActa = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelSelecActa.add(new JLabel("Acta:"));
        cbActas = new JComboBox<>();
        panelSelecActa.add(cbActas);
        lblEstadoActa = new JLabel("Selecciona una asignacion");
        lblEstadoActa.setFont(new Font("Tahoma", Font.BOLD, 12));
        panelSelecActa.add(lblEstadoActa);
        panelActa.add(panelSelecActa, BorderLayout.NORTH);

        tabNotas = new JTable();
        tabNotas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panelActa.add(new JScrollPane(tabNotas), BorderLayout.CENTER);

        JPanel panelBotonesActa = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnGuardarNotas = new JButton("Guardar Notas");
        btnGuardarNotas.setEnabled(false);
        panelBotonesActa.add(btnGuardarNotas);
        btnCerrarActa = new JButton("Cerrar Acta");
        btnCerrarActa.setEnabled(false);
        panelBotonesActa.add(btnCerrarActa);
        panelActa.add(panelBotonesActa, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, panelActa);
        splitPane.setResizeWeight(0.4);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Pestaña de la HU 35243: formulario para proponer una asignatura en uno o varios de los grados
     * en los que imparte el profesor, y listado "Mis propuestas"
     */
    private JPanel createPropuestasPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel panelFormulario = new JPanel(new BorderLayout(10, 0));
        panelFormulario.setBorder(new TitledBorder("Nueva propuesta"));

        JPanel panelCampos = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        txtNombrePropuesta = new JTextField(28);
        txtAnioPropuesta = new JTextField(4);
        txtCreditosPropuesta = new JTextField(4);
        txtDescripcionPropuesta = new JTextArea(4, 28);
        txtDescripcionPropuesta.setLineWrap(true);
        txtDescripcionPropuesta.setWrapStyleWord(true);
        addCampo(panelCampos, c, 0, "Nombre:", txtNombrePropuesta);
        addCampo(panelCampos, c, 1, "Año del grado (1-6):", txtAnioPropuesta);
        addCampo(panelCampos, c, 2, "Créditos:", txtCreditosPropuesta);
        addCampo(panelCampos, c, 3, "Descripción (opcional):", new JScrollPane(txtDescripcionPropuesta));
        panelFormulario.add(panelCampos, BorderLayout.CENTER);

        // grados en los que imparte el profesor, con su estado; se pueden elegir varios (Ctrl/Mayús + clic)
        JPanel panelGrados = new JPanel(new BorderLayout());
        panelGrados.add(new JLabel("Grados (puedes elegir varios):"), BorderLayout.NORTH);
        listGradosPropuesta = new JList<>();
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
                return this;
            }
        });
        JScrollPane scrollGrados = new JScrollPane(listGradosPropuesta);
        scrollGrados.setPreferredSize(new Dimension(380, 120));
        panelGrados.add(scrollGrados, BorderLayout.CENTER);
        panelFormulario.add(panelGrados, BorderLayout.EAST);

        JPanel panelBotonesPropuesta = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        lblProfesorPropuesta = new JLabel("Carga un profesor para proponer asignaturas");
        panelBotonesPropuesta.add(lblProfesorPropuesta);
        btnEnviarPropuesta = new JButton("Enviar propuesta");
        btnEnviarPropuesta.setEnabled(false);
        panelBotonesPropuesta.add(btnEnviarPropuesta);
        panelFormulario.add(panelBotonesPropuesta, BorderLayout.SOUTH);
        panel.add(panelFormulario, BorderLayout.NORTH);

        JPanel panelMisPropuestas = new JPanel(new BorderLayout());
        panelMisPropuestas.setBorder(new TitledBorder("Mis propuestas"));
        tabPropuestas = new JTable();
        tabPropuestas.setDefaultEditor(Object.class, null);
        panelMisPropuestas.add(new JScrollPane(tabPropuestas), BorderLayout.CENTER);
        panel.add(panelMisPropuestas, BorderLayout.CENTER);
        return panel;
    }

    private static void addCampo(JPanel panel, GridBagConstraints c, int fila, String etiqueta, JComponent campo) {
        c.gridx = 0;
        c.gridy = fila;
        c.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(etiqueta), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
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
