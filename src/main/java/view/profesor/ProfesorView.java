package view.profesor;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class ProfesorView {
    private JFrame frame;
    private JComboBox<Object> cbProfesores;
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

    public ProfesorView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Panel de Profesor - Asignaciones Docentes");
        frame.setBounds(150, 100, 900, 650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        JPanel panelNorte = new JPanel(new GridLayout(2, 1));

        JPanel panelProfesor = new JPanel(new BorderLayout());
        JPanel panelSelecProfesor = new JPanel();
        panelSelecProfesor.add(new JLabel("Profesor:"));
        // se puede escribir para buscar (ComboBuscable, lo configura el controlador al cargar los profesores)
        cbProfesores = new JComboBox<>();
        cbProfesores.setPreferredSize(new Dimension(280, 26));
        panelSelecProfesor.add(cbProfesores);

        btnCargarAsignaciones = new JButton("Ver Asignaciones");
        panelSelecProfesor.add(btnCargarAsignaciones);
        panelProfesor.add(panelSelecProfesor, BorderLayout.CENTER);

        JPanel panelVolver = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnVolverInicio = new JButton("Volver al inicio");
        btnVolverInicio.setToolTipText("Volver a la pantalla de selección de rol");
        panelVolver.add(btnVolverInicio);
        panelProfesor.add(panelVolver, BorderLayout.EAST);
        panelNorte.add(panelProfesor);

        // Filtros: grado y curso se rellenan con las asignaciones del profesor al cargarlas
        JPanel panelFiltros = new JPanel();
        panelFiltros.add(new JLabel("Grado:"));
        cbFiltroGrado = new JComboBox<>(new String[] { "TODOS" });
        panelFiltros.add(cbFiltroGrado);

        panelFiltros.add(new JLabel("Curso:"));
        cbFiltroCurso = new JComboBox<>(new String[] { "TODOS" });
        panelFiltros.add(cbFiltroCurso);

        panelFiltros.add(new JLabel("Grupo:"));
        cbFiltroTipo = new JComboBox<>(new String[] { "TODOS", "TE", "PL", "PA" });
        panelFiltros.add(cbFiltroTipo);

        panelFiltros.add(new JLabel("Rol:"));
        cbFiltroRol = new JComboBox<>(new String[] { "TODOS", "COORDINADOR", "DOCENTE" });
        panelFiltros.add(cbFiltroRol);
        panelNorte.add(panelFiltros);

        frame.getContentPane().add(panelNorte, BorderLayout.NORTH);

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
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);
    }

    public JFrame getFrame() {
        return frame;
    }

    public JComboBox<Object> getCbProfesores() {
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
}
