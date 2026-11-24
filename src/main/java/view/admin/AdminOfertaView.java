package view.admin;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import model.fip.FipDisplayDTO;
import model.grado.GradoDisplayDTO;
import model.curso.CursoDisplayDTO;
import model.matriculacion.MatriculacionDisplayDTO;
import util.Util;
import view.comun.Estilo;

public class AdminOfertaView {
    private JFrame frame;

    // Top components
    private JLabel lblAnoAcademico;
    private JLabel lblPeriodoMatriculacion;
    private JButton btnNuevoAnoAcademico;
    private JButton btnVolver;
    private JButton btnVolverInicio;

    // Card Layout
    private JPanel cardsPanel;
    private CardLayout cardLayout;

    // -- GRADOS VIEW --
    private JTextField txtFiltroFacultadGrado;
    private JTextField txtFiltroRamaGrado;

    private JComboBox<String> cbEstadoGrado;
    private JTable tabGrados;
    private JButton btnAnadirGrado;

    // -- CURSOS VIEW --
    private JTextField txtFiltroAnoCurso;
    private JTextField txtFiltroItinerarioCurso;
    private JTextField txtFiltroProfesorCurso;
    private JComboBox<String> cbFiltroEstadoCurso;
    private JTable tabCursos;
    private JLabel lblTituloCursos;
    private JButton btnAnadirCurso;
    private JButton btnModificarGrado;
    private JButton btnEliminarGrado;

    // -- MATRICULACION VIEW --
    private JTextField txtFiltroAlumno;
    private JTable tabMatriculacion;
    private JLabel lblTituloMatriculas;
    private JButton btnModificarCurso;
    private JButton btnEliminarCurso;

    // -- MENU VIEW --
    private JButton btnGrados;
    private JButton btnFips;

    // -- FIPS VIEW (HU 37165) --
    private JTable tabFips;
    private JButton btnCrearFip;
    private JButton btnAtrasFips;

    // -- FIP VIEW (HU 37165) --
    private JLabel lblTituloFip;
    private JLabel lblProfesorFip;
    private JLabel lblCorreoFip;
    private JLabel lblFormacionFip;
    private JButton btnAtrasFip;

    // State for back navigation
    private String currentView = "MENU";

    // Callbacks de navegación para el controlador
    private BiConsumer<String, String> onVerGrado;
    private BiConsumer<String, String> onVerCurso;
    private Consumer<Integer> onVerFip;

    public AdminOfertaView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Panel de Administrador - Oferta Académica");
        frame.setBounds(100, 100, 1200, 720);
        frame.setMinimumSize(new Dimension(1000, 620));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel contenido = Estilo.contenido(new BorderLayout(0, 12));
        frame.setContentPane(contenido);
        Estilo.maximizarCentrado(frame);

        // --- TOP PANEL ---
        btnNuevoAnoAcademico = Estilo.botonPrimario("Configurar Nuevo Año Académico");
        btnVolver = Estilo.botonSecundario("‹  Volver");
        btnVolver.setEnabled(false);
        btnVolverInicio = Estilo.botonSecundario("Volver al inicio");
        btnVolverInicio.setToolTipText("Volver a la pantalla de selección de rol");
        JPanel topContainer = Estilo.cabecera("PANEL DE ADMINISTRACIÓN", new JLabel("Oferta Académica"),
                btnNuevoAnoAcademico, btnVolver, btnVolverInicio);

        // Dates panel: año académico y periodo de matriculación en curso
        JPanel datesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        datesPanel.setOpaque(false);
        datesPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        lblAnoAcademico = etiquetaFecha("Año Académico: --/--/-- - --/--/--");
        lblPeriodoMatriculacion = etiquetaFecha("Matriculación: --/--/-- - --/--/--");
        datesPanel.add(lblAnoAcademico);
        datesPanel.add(Box.createHorizontalStrut(10));
        datesPanel.add(lblPeriodoMatriculacion);
        topContainer.add(datesPanel, BorderLayout.SOUTH);

        contenido.add(topContainer, BorderLayout.NORTH);

        // --- CARDS PANEL ---
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setOpaque(false);

        cardsPanel.add(createMenuPanel(), "MENU");
        cardsPanel.add(createGradosPanel(), "GRADOS");
        cardsPanel.add(createCursosPanel(), "CURSOS");
        cardsPanel.add(createMatriculacionPanel(), "MATRICULACION");
        cardsPanel.add(createFipsPanel(), "FIPS");
        cardsPanel.add(createFipPanel(), "FIP");

        contenido.add(cardsPanel, BorderLayout.CENTER);

        // Navigation actions
        btnVolver.addActionListener(e -> goBack());
        btnAtrasFips.addActionListener(e -> goBack());
        btnAtrasFip.addActionListener(e -> goBack());
    }

    /** Etiqueta de fechas con aspecto de "píldora" informativa. */
    private static JLabel etiquetaFecha(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(Estilo.ETIQUETA);
        label.setForeground(Estilo.TEXTO);
        label.setOpaque(true);
        label.setBackground(Estilo.TARJETA);
        label.setBorder(new CompoundBorder(new LineBorder(Estilo.BORDE, 1, true), new EmptyBorder(5, 10, 5, 10)));
        return label;
    }

    /** Menú inicial: una tarjeta por cada parte del panel, como el menú de la vista del estudiante. */
    private JPanel createMenuPanel() {
        JPanel opciones = new JPanel(new GridLayout(1, 2, 25, 0));
        opciones.setOpaque(false);
        btnGrados = Estilo.botonPrimario("Grados");
        opciones.add(tarjetaOpcion("Oferta Académica",
                "Consulte y gestione los grados, sus cursos y las matrículas de cada curso.", btnGrados));
        btnFips = Estilo.botonPrimario("FIPs");
        opciones.add(tarjetaOpcion("Formación Interna del Personal (FIP)",
                "Consulte las FIP y cree nuevas asignándoles un profesor.", btnFips));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(opciones, BorderLayout.NORTH);
        return panel;
    }

    private static JPanel tarjetaOpcion(String titulo, String descripcion, JButton boton) {
        JPanel card = Estilo.tarjeta(new BorderLayout(8, 15));
        JLabel lblTitulo = Estilo.tituloTarjeta(titulo);
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(lblTitulo, BorderLayout.NORTH);
        card.add(Estilo.textoSecundario("<html>" + descripcion + "</html>"), BorderLayout.CENTER);
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panelBoton.setOpaque(false);
        panelBoton.add(boton);
        card.add(panelBoton, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createFipsPanel() {
        JLabel lblTitle = new JLabel("Vista admin - FIPs");

        String[] columnNames = { "Nombre", "Nombre Profesor", "Fecha inicio", "Fecha fin", "Acciones" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4;
            }
        };
        tabFips = new JTable(model);
        Estilo.estilizarTabla(tabFips);
        anchos(tabFips, 300, 260, 110, 110, 100);
        tabFips.getColumnModel().getColumn(4).setMinWidth(90);
        actualizarTablaFips(new ArrayList<>());

        btnCrearFip = Estilo.botonPrimario("Crear FIP");
        btnAtrasFips = Estilo.botonSecundario("Atrás");

        JPanel tablaCard = Estilo.tarjeta(new BorderLayout(0, 10));
        Estilo.estilizarTituloTarjeta(lblTitle);
        tablaCard.add(lblTitle, BorderLayout.NORTH);
        tablaCard.add(Estilo.scrollTabla(tabFips), BorderLayout.CENTER);
        tablaCard.add(Estilo.barraAcciones(btnCrearFip, btnAtrasFips), BorderLayout.SOUTH);
        return tablaCard;
    }

    private JPanel createFipPanel() {
        lblTituloFip = new JLabel("FIP - NombreFIP");
        Estilo.estilizarTituloTarjeta(lblTituloFip);
        lblProfesorFip = Estilo.etiqueta("Profesor:");
        lblCorreoFip = Estilo.etiqueta("Correo:");
        lblFormacionFip = Estilo.etiqueta("Formación:");

        JPanel datos = new JPanel(new GridLayout(3, 1, 0, 10));
        datos.setOpaque(false);
        datos.add(lblProfesorFip);
        datos.add(lblCorreoFip);
        datos.add(lblFormacionFip);
        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(datos, BorderLayout.NORTH);

        btnAtrasFip = Estilo.botonSecundario("Atrás");

        JPanel card = Estilo.tarjeta(new BorderLayout(0, 10));
        card.add(lblTituloFip, BorderLayout.NORTH);
        card.add(centro, BorderLayout.CENTER);
        card.add(Estilo.barraAcciones(btnAtrasFip), BorderLayout.SOUTH);
        return card;
    }

    private JPanel createGradosPanel() {
        JLabel lblTitle = new JLabel("Vista general grados - Administrador");

        // Filters
        JPanel filterPanel = Estilo.panelFiltros("Filtros");
        txtFiltroFacultadGrado = new JTextField();
        Estilo.addFiltro(filterPanel, "Facultad", txtFiltroFacultadGrado);
        txtFiltroRamaGrado = new JTextField();
        Estilo.addFiltro(filterPanel, "Rama de conocimiento", txtFiltroRamaGrado);
        cbEstadoGrado = new JComboBox<>(new String[] { "TODOS", "VIGENTE", "PENDIENTE_RENOVACION", "EN_EXTINCION",
                "EXTINTO" });
        Estilo.addFiltro(filterPanel, "Estado", cbEstadoGrado);

        // Table
        String[] columnNames = { "Código", "Nombre", "Facultad", "Rama", "Estado", "Acciones" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Solo la columna del botón 'Ver' es interactuable
            }
        };
        tabGrados = new JTable(model);
        Estilo.estilizarTabla(tabGrados);
        anchos(tabGrados, 80, 300, 260, 200, 170, 100);
        tabGrados.getColumnModel().getColumn(4).setMinWidth(150);
        tabGrados.getColumnModel().getColumn(5).setMinWidth(90);
        actualizarTablaGrados(new ArrayList<>());

        btnAnadirGrado = Estilo.botonPrimario("Añadir grado");

        return crearPanelListado(filterPanel, lblTitle, tabGrados, btnAnadirGrado);
    }

    private JPanel createCursosPanel() {
        lblTituloCursos = new JLabel("Vista grado NombreGrado - Administrador");

        // Filters
        JPanel filterPanel = Estilo.panelFiltros("Filtros");
        txtFiltroAnoCurso = new JTextField();
        Estilo.addFiltro(filterPanel, "Año", txtFiltroAnoCurso);
        txtFiltroItinerarioCurso = new JTextField();
        Estilo.addFiltro(filterPanel, "Itinerario", txtFiltroItinerarioCurso);
        txtFiltroProfesorCurso = new JTextField();
        Estilo.addFiltro(filterPanel, "Profesor", txtFiltroProfesorCurso);
        cbFiltroEstadoCurso = new JComboBox<>(new String[] { "TODOS", "ACTIVO", "INACTIVO" });
        Estilo.addFiltro(filterPanel, "Estado", cbFiltroEstadoCurso);

        // Table
        String[] columnNames = { "Código", "Nombre", "Año", "Itinerario", "Profesor", "Estado", "Acciones" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6;
            }
        };
        tabCursos = new JTable(model);
        Estilo.estilizarTabla(tabCursos);
        anchos(tabCursos, 80, 300, 60, 180, 240, 110, 100);
        tabCursos.getColumnModel().getColumn(5).setMinWidth(100);
        tabCursos.getColumnModel().getColumn(6).setMinWidth(90);
        actualizarTablaCursos(new ArrayList<>());

        btnAnadirCurso = Estilo.botonPrimario("Añadir curso");
        btnModificarGrado = Estilo.botonSecundario("Modificar grado");
        btnEliminarGrado = Estilo.botonPeligro("Eliminar grado");

        return crearPanelListado(filterPanel, lblTituloCursos, tabCursos, btnAnadirCurso, btnModificarGrado,
                btnEliminarGrado);
    }

    private JPanel createMatriculacionPanel() {
        lblTituloMatriculas = new JLabel("Vista matriculacion curso NombreCurso - Administrador");

        // Filters
        JPanel filterPanel = Estilo.panelFiltros("Filtros");
        txtFiltroAlumno = new JTextField();
        Estilo.addFiltro(filterPanel, "Alumno (Código/Nombre)", txtFiltroAlumno);

        // Table
        String[] columnNames = { "Código", "Alumno" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabMatriculacion = new JTable(model);
        Estilo.estilizarTabla(tabMatriculacion);
        anchos(tabMatriculacion, 120, 600);

        btnModificarCurso = Estilo.botonSecundario("Modificar curso");
        btnEliminarCurso = Estilo.botonPeligro("Eliminar curso");

        return crearPanelListado(filterPanel, lblTituloMatriculas, tabMatriculacion, btnModificarCurso,
                btnEliminarCurso);
    }

    /**
     * Card de un listado: filtros a la izquierda y, a la derecha, una tarjeta con el título,
     * la tabla y los botones de acción debajo.
     */
    private static JPanel crearPanelListado(JPanel filterPanel, JLabel titulo, JTable tabla, JButton... acciones) {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        panel.add(Estilo.envolverFiltros(filterPanel, 250), BorderLayout.WEST);

        JPanel tablaCard = Estilo.tarjeta(new BorderLayout(0, 10));
        Estilo.estilizarTituloTarjeta(titulo);
        tablaCard.add(titulo, BorderLayout.NORTH);
        tablaCard.add(Estilo.scrollTabla(tabla), BorderLayout.CENTER);
        tablaCard.add(Estilo.barraAcciones(acciones), BorderLayout.SOUTH);
        panel.add(tablaCard, BorderLayout.CENTER);
        return panel;
    }

    private static void anchos(JTable tabla, int... anchos) {
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
    }

    // --- Navigation Methods ---

    public void showMenuView() {
        cardLayout.show(cardsPanel, "MENU");
        currentView = "MENU";
        btnVolver.setEnabled(false);
    }

    public void showGradosView() {
        cardLayout.show(cardsPanel, "GRADOS");
        currentView = "GRADOS";
        btnVolver.setEnabled(true);
    }

    public void showFipsView() {
        cardLayout.show(cardsPanel, "FIPS");
        currentView = "FIPS";
        btnVolver.setEnabled(true);
    }

    public void showFipView(FipDisplayDTO fip) {
        lblTituloFip.setText("FIP - " + fip.getNombre());
        lblProfesorFip.setText("Profesor: " + fip.getResponsable());
        lblCorreoFip.setText("Correo: " + fip.getEmail());
        lblFormacionFip.setText("Formación: " + formatearFecha(fip.getFechaInicio()) + " - "
                + formatearFecha(fip.getFechaFin()));
        cardLayout.show(cardsPanel, "FIP");
        currentView = "FIP";
        btnVolver.setEnabled(true);
    }

    public void showCursosView(String nombreGrado) {
        lblTituloCursos.setText("Vista grado " + nombreGrado + " - Administrador");
        cardLayout.show(cardsPanel, "CURSOS");
        currentView = "CURSOS";
        btnVolver.setEnabled(true);
    }

    public void showMatriculacionView(String nombreCurso) {
        lblTituloMatriculas.setText("Vista matriculacion curso " + nombreCurso + " - Administrador");
        cardLayout.show(cardsPanel, "MATRICULACION");
        currentView = "MATRICULACION";
        btnVolver.setEnabled(true);
    }

    private void goBack() {
        if ("MATRICULACION".equals(currentView)) {
            cardLayout.show(cardsPanel, "CURSOS");
            currentView = "CURSOS";
        } else if ("CURSOS".equals(currentView)) {
            showGradosView();
        } else if ("FIP".equals(currentView)) {
            showFipsView();
        } else if ("GRADOS".equals(currentView) || "FIPS".equals(currentView)) {
            showMenuView();
        }
    }

    // --- Button Column in JTable utility ---

    public void setupTableVerButton(JTable table, int columnIdx, String viewType) {
        table.getColumnModel().getColumn(columnIdx).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(columnIdx).setCellEditor(new ButtonEditor(new JCheckBox(), viewType));
    }

    /**
     * Borde del botón "Ver": un margen del color de la fila alrededor del botón con borde azul,
     * para que el botón ocupe toda la celda (cualquier clic en la celda lo pulsa).
     */
    private static void bordeBotonVer(JButton boton, Color fondoFila) {
        boton.setBorder(new CompoundBorder(new MatteBorder(3, 10, 3, 10, fondoFila),
                new LineBorder(Estilo.PRIMARIO, 1, true)));
    }

    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            Estilo.estilizarBotonFila(this);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            setText("Ver");
            bordeBotonVer(this, Estilo.fondoFila(row, isSelected));
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private JTable table;
        private String viewType;

        public ButtonEditor(JCheckBox checkBox, String viewType) {
            super(checkBox);
            this.viewType = viewType;
            button = new JButton();
            button.setOpaque(true);
            Estilo.estilizarBotonFila(button);
            button.setBackground(Estilo.SELECCION);
            bordeBotonVer(button, Estilo.SELECCION);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                int column) {
            this.table = table;
            label = "Ver";
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    if ("GRADOS".equals(viewType)) {
                        String idGrado = table.getValueAt(row, 0) != null ? table.getValueAt(row, 0).toString() : "";
                        String nombreGrado = table.getValueAt(row, 1) != null ? table.getValueAt(row, 1).toString()
                                : "";
                        showCursosView(nombreGrado);
                        if (onVerGrado != null) {
                            onVerGrado.accept(idGrado, nombreGrado);
                        }
                    } else if ("CURSOS".equals(viewType)) {
                        String idCurso = table.getValueAt(row, 0) != null ? table.getValueAt(row, 0).toString() : "";
                        String nombreCurso = table.getValueAt(row, 1) != null ? table.getValueAt(row, 1).toString()
                                : "";
                        showMatriculacionView(nombreCurso);
                        if (onVerCurso != null) {
                            onVerCurso.accept(idCurso, nombreCurso);
                        }
                    } else if ("FIPS".equals(viewType) && onVerFip != null) {
                        onVerFip.accept(row);
                    }
                }
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    // --- Métodos de actualización de tablas con DTOs ---

    public void actualizarTablaGrados(List<GradoDisplayDTO> grados) {
        DefaultTableModel model = (DefaultTableModel) tabGrados.getModel();
        model.setRowCount(0);
        if (grados != null) {
            for (GradoDisplayDTO g : grados) {
                model.addRow(new Object[] {
                        g.getId(),
                        g.getNombre(),
                        g.getFacultad(),
                        g.getRama(),
                        g.getEstado(),
                        "Ver"
                });
            }
        }
        setupTableVerButton(tabGrados, 5, "GRADOS");
        tabGrados.getColumnModel().getColumn(4).setCellRenderer(new Estilo.RendererEstado());
    }

    public void actualizarTablaCursos(List<CursoDisplayDTO> cursos) {
        DefaultTableModel model = (DefaultTableModel) tabCursos.getModel();
        model.setRowCount(0);
        if (cursos != null) {
            for (CursoDisplayDTO c : cursos) {
                model.addRow(new Object[] {
                        c.getId(),
                        c.getNombre(),
                        c.getAno(),
                        c.getItinerario(),
                        c.getProfesor(),
                        c.getEstado(),
                        "Ver"
                });
            }
        }
        setupTableVerButton(tabCursos, 6, "CURSOS");
        tabCursos.getColumnModel().getColumn(2).setCellRenderer(new Estilo.RendererNumero(false));
        tabCursos.getColumnModel().getColumn(5).setCellRenderer(new Estilo.RendererEstado());
    }

    public void actualizarTablaMatriculacion(List<? extends MatriculacionDisplayDTO> matriculas) {
        DefaultTableModel model = (DefaultTableModel) tabMatriculacion.getModel();
        model.setRowCount(0);
        if (matriculas != null) {
            for (MatriculacionDisplayDTO m : matriculas) {
                model.addRow(new Object[] {
                        m.getId(),
                        m.getNombreAlumno()
                });
            }
        }
    }

    public void actualizarTablaFips(List<FipDisplayDTO> fips) {
        DefaultTableModel model = (DefaultTableModel) tabFips.getModel();
        model.setRowCount(0);
        if (fips != null) {
            for (FipDisplayDTO f : fips) {
                model.addRow(new Object[] {
                        f.getNombre(),
                        f.getResponsable(),
                        formatearFecha(f.getFechaInicio()),
                        formatearFecha(f.getFechaFin()),
                        "Ver"
                });
            }
        }
        setupTableVerButton(tabFips, 4, "FIPS");
    }

    /** Fecha ISO de la BD en el formato dd/MM/yy que escribe el administrador */
    private static String formatearFecha(String fechaIso) {
        return new SimpleDateFormat("dd/MM/yy").format(Util.isoStringToDate(fechaIso));
    }

    // --- Callbacks y Getters para controlador ---

    public void setOnVerGrado(BiConsumer<String, String> onVerGrado) {
        this.onVerGrado = onVerGrado;
    }

    public void setOnVerCurso(BiConsumer<String, String> onVerCurso) {
        this.onVerCurso = onVerCurso;
    }

    /** Recibe la fila de la tabla de FIP en la que se pulsó "Ver" */
    public void setOnVerFip(Consumer<Integer> onVerFip) {
        this.onVerFip = onVerFip;
    }

    public JButton getBtnGrados() {
        return btnGrados;
    }

    public JButton getBtnFips() {
        return btnFips;
    }

    public JTable getTabFips() {
        return tabFips;
    }

    public JButton getBtnCrearFip() {
        return btnCrearFip;
    }

    public JLabel getLblTituloCursos() {
        return lblTituloCursos;
    }

    public JButton getBtnVolverInicio() {
        return btnVolverInicio;
    }

    public JLabel getLblTituloMatriculas() {
        return lblTituloMatriculas;
    }

    public String getCurrentView() {
        return currentView;
    }

    public JFrame getFrame() {
        return frame;
    }

    public JTable getTabGrados() {
        return tabGrados;
    }

    public JTextField getTxtFiltroFacultadGrado() {
        return txtFiltroFacultadGrado;
    }

    public JTextField getTxtFiltroRamaGrado() {
        return txtFiltroRamaGrado;
    }

    public JComboBox<String> getCbEstadoGrado() {
        return cbEstadoGrado;
    }

    public JTable getTabCursos() {
        return tabCursos;
    }

    public JTextField getTxtFiltroAnoCurso() {
        return txtFiltroAnoCurso;
    }

    public JTextField getTxtFiltroItinerarioCurso() {
        return txtFiltroItinerarioCurso;
    }

    public JTextField getTxtFiltroProfesorCurso() {
        return txtFiltroProfesorCurso;
    }

    public JComboBox<String> getCbFiltroEstadoCurso() {
        return cbFiltroEstadoCurso;
    }

    public JTable getTabMatriculacion() {
        return tabMatriculacion;
    }

    public JTextField getTxtFiltroAlumno() {
        return txtFiltroAlumno;
    }

    public JButton getBtnVolver() {
        return btnVolver;
    }

    public JButton getBtnAnadirGrado() {
        return btnAnadirGrado;
    }

    public JButton getBtnAnadirCurso() {
        return btnAnadirCurso;
    }

    public JButton getBtnModificarGrado() {
        return btnModificarGrado;
    }

    public JButton getBtnEliminarGrado() {
        return btnEliminarGrado;
    }

    public JButton getBtnModificarCurso() {
        return btnModificarCurso;
    }

    public JButton getBtnEliminarCurso() {
        return btnEliminarCurso;
    }

    public JButton getBtnNuevoAnoAcademico() {
        return btnNuevoAnoAcademico;
    }

    public JLabel getLblAnoAcademico() {
        return lblAnoAcademico;
    }

    public JLabel getLblPeriodoMatriculacion() {
        return lblPeriodoMatriculacion;
    }

    public void actualizarAnoAcademico(String displayFechas) {
        lblAnoAcademico.setText("Año Académico: " + displayFechas);
    }

    public void actualizarPeriodoMatriculacion(String displayFechas) {
        lblPeriodoMatriculacion.setText("Matriculación: " + displayFechas);
    }
}
