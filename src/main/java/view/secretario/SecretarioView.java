package view.secretario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class SecretarioView {
    private JFrame frame;
    
    // Top components
    private JLabel lblTituloCabecera;
    private JButton btnVolver;
    private JButton btnCargarMatriculas; 
    
    // Card Layout
    private JPanel cardsPanel;
    private CardLayout cardLayout;
    
    // -- LISTADO MATRÍCULAS VIEW --
    private JTextField txtFiltroGrado;
    private JTextField txtFiltroCurso;
    private JTextField txtFiltroGrupo;
    private JTextField txtFiltroEstudiante;
    private JComboBox<String> cbFiltroEstado;
    private JTable tabMatriculas;
    
    // -- DETALLE MATRÍCULA VIEW --
    private JLabel lblDetalleId, lblDetalleEstudiante, lblDetalleGrado, lblDetalleCurso, lblDetalleEstado;
    private JButton btnAceptarSolicitud;
    private JButton btnModificarMatricula;
    private JButton btnCancelarMatricula;
    
    private String idMatriculaSeleccionada = "";
    private String estadoMatriculaSeleccionada = ""; 

    public SecretarioView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Panel de Secretaría - Gestión de Matrículas");
        frame.setBounds(100, 100, 1050, 650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        // --- TOP PANEL ---
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setBorder(new EmptyBorder(10, 15, 10, 15));
        
        lblTituloCabecera = new JLabel("Secretaría: Consulta de Matrículas");
        lblTituloCabecera.setFont(new Font("Tahoma", Font.BOLD, 16));
        
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnVolver = new JButton("Volver al Listado");
        btnVolver.setEnabled(false);
        buttonsPanel.add(btnVolver);
        
        topContainer.add(lblTituloCabecera, BorderLayout.WEST);
        topContainer.add(buttonsPanel, BorderLayout.EAST);
        frame.getContentPane().add(topContainer, BorderLayout.NORTH);

        // --- CARDS PANEL ---
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.add(createListadoPanel(), "LISTADO");
        cardsPanel.add(createDetallePanel(), "DETALLE");
        frame.getContentPane().add(cardsPanel, BorderLayout.CENTER);
        
        btnVolver.addActionListener(e -> showListadoView());
    }

    private JPanel createListadoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // --- PANEL DE FILTROS ---
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBorder(new TitledBorder("Filtros de Búsqueda"));
        filterPanel.setPreferredSize(new Dimension(220, 0));
        
        addFilterComponent(filterPanel, "Estudiante (Nombre o DNI):");
        txtFiltroEstudiante = new JTextField();
        txtFiltroEstudiante.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        filterPanel.add(txtFiltroEstudiante);
        filterPanel.add(Box.createVerticalStrut(10));
        
        addFilterComponent(filterPanel, "Grado:");
        txtFiltroGrado = new JTextField();
        txtFiltroGrado.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        filterPanel.add(txtFiltroGrado);
        filterPanel.add(Box.createVerticalStrut(10));
        
        addFilterComponent(filterPanel, "Curso:");
        txtFiltroCurso = new JTextField();
        txtFiltroCurso.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        filterPanel.add(txtFiltroCurso);
        filterPanel.add(Box.createVerticalStrut(10));
        
        addFilterComponent(filterPanel, "Grupo:");
        txtFiltroGrupo = new JTextField();
        txtFiltroGrupo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        filterPanel.add(txtFiltroGrupo);
        filterPanel.add(Box.createVerticalStrut(10));
        
        addFilterComponent(filterPanel, "Estado:");
        cbFiltroEstado = new JComboBox<>(new String[]{"TODOS", "SOLICITADA", "PAGADA", "ACTIVA", "FINALIZADA", "CANCELADA"});
        cbFiltroEstado.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        filterPanel.add(cbFiltroEstado);
        filterPanel.add(Box.createVerticalStrut(20));
        
        btnCargarMatriculas = new JButton("Filtrar / Recargar");
        btnCargarMatriculas.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterPanel.add(btnCargarMatriculas);
        panel.add(filterPanel, BorderLayout.WEST);
        
        // --- TABLA DE RESULTADOS ---
        // AÑADIDO: Columna DNI independiente
        String[] columnNames = {"ID", "DNI", "Estudiante", "Grado", "Curso", "Grupo", "Estado", "Acción"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7; // El botón ahora está en la columna 7
            }
        };
        tabMatriculas = new JTable(model);
        tabMatriculas.setRowHeight(25);
        setupTableGestionarButton(tabMatriculas, 7);
        panel.add(new JScrollPane(tabMatriculas), BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createDetallePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));
        
        JPanel infoPanel = new JPanel(new GridLayout(5, 2, 10, 15));
        infoPanel.setBorder(new TitledBorder("Información Básica de la Matrícula"));
        
        lblDetalleId = new JLabel("-");
        lblDetalleEstudiante = new JLabel("-");
        lblDetalleGrado = new JLabel("-");
        lblDetalleCurso = new JLabel("-");
        lblDetalleEstado = new JLabel("-");
        lblDetalleEstado.setFont(new Font("Tahoma", Font.BOLD, 12));

        infoPanel.add(new JLabel("ID Matrícula:")); infoPanel.add(lblDetalleId);
        infoPanel.add(new JLabel("Estudiante:")); infoPanel.add(lblDetalleEstudiante);
        infoPanel.add(new JLabel("Grado:")); infoPanel.add(lblDetalleGrado);
        infoPanel.add(new JLabel("Curso / Grupo:")); infoPanel.add(lblDetalleCurso);
        infoPanel.add(new JLabel("Estado Actual:")); infoPanel.add(lblDetalleEstado);
        
        panel.add(infoPanel, BorderLayout.NORTH);
        
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        btnAceptarSolicitud = new JButton("Aceptar Solicitud (Pendiente de Pago)");
        btnAceptarSolicitud.setBackground(new Color(144, 238, 144)); 
        
        btnModificarMatricula = new JButton("Modificar Matrícula");
        
        btnCancelarMatricula = new JButton("Cancelar Matrícula");
        btnCancelarMatricula.setBackground(new Color(255, 182, 193)); 
        
        actionsPanel.add(btnAceptarSolicitud);
        actionsPanel.add(btnModificarMatricula);
        actionsPanel.add(btnCancelarMatricula);
        
        panel.add(actionsPanel, BorderLayout.CENTER);
        return panel;
    }
    
    private void addFilterComponent(JPanel panel, String labelText) {
        JLabel label = new JLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
    }
    
    // --- NAVEGACIÓN ---
    public void showListadoView() {
        cardLayout.show(cardsPanel, "LISTADO");
        btnVolver.setEnabled(false);
        lblTituloCabecera.setText("Secretaría: Consulta de Matrículas");
    }
    
    // AÑADIDO: Recibe el parámetro DNI extraído de la nueva columna
    public void showDetalleView(String id, String dni, String estudiante, String grado, String curso, String grupo, String estado) {
        this.idMatriculaSeleccionada = id;
        this.estadoMatriculaSeleccionada = estado.toUpperCase();
        
        lblDetalleId.setText(id);
        lblDetalleEstudiante.setText(dni + " - " + estudiante); // En la ficha de detalle sí hay espacio de sobra para concatenarlos
        lblDetalleGrado.setText(grado);
        lblDetalleCurso.setText(curso + " - Grupo " + grupo);
        lblDetalleEstado.setText(estado);
        
        btnAceptarSolicitud.setEnabled("SOLICITADA".equals(estadoMatriculaSeleccionada));
        btnCancelarMatricula.setEnabled(!"CANCELADA".equals(estadoMatriculaSeleccionada));
        
        cardLayout.show(cardsPanel, "DETALLE");
        btnVolver.setEnabled(true);
        lblTituloCabecera.setText("Secretaría: Gestión Ciclo de Vida");
    }
    
    // --- BOTÓN EN TABLA ---
    private void setupTableGestionarButton(JTable table, int columnIdx) {
        table.getColumnModel().getColumn(columnIdx).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(columnIdx).setCellEditor(new ButtonEditor(new JCheckBox()));
    }

    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() { setOpaque(true); }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText("Gestionar"); return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private boolean isPushed;
        private JTable table;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.table = table;
            button.setText("Gestionar");
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    // AÑADIDO: Capturamos la columna nueva (índice 1) y desplazamos el resto
                    String id = table.getValueAt(row, 0).toString();
                    String dni = table.getValueAt(row, 1).toString();
                    String est = table.getValueAt(row, 2).toString();
                    String gr = table.getValueAt(row, 3).toString();
                    String cur = table.getValueAt(row, 4).toString();
                    String gru = table.getValueAt(row, 5).toString();
                    String estado = table.getValueAt(row, 6).toString();
                    
                    showDetalleView(id, dni, est, gr, cur, gru, estado);
                }
            }
            isPushed = false;
            return "Gestionar";
        }
        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    // --- GETTERS ---
    public JFrame getFrame() { return frame; }
    public JButton getBtnCargarMatriculas() { return btnCargarMatriculas; }
    public JTable getTabMatriculas() { return tabMatriculas; }
    public JComboBox<String> getCbFiltroEstado() { return cbFiltroEstado; }
    
    public JTextField getTxtFiltroGrado() { return txtFiltroGrado; }
    public JTextField getTxtFiltroCurso() { return txtFiltroCurso; }
    public JTextField getTxtFiltroGrupo() { return txtFiltroGrupo; }
    public JTextField getTxtFiltroEstudiante() { return txtFiltroEstudiante; }
    
    public JButton getBtnAceptarSolicitud() { return btnAceptarSolicitud; }
    public JButton getBtnModificarMatricula() { return btnModificarMatricula; }
    public JButton getBtnCancelarMatricula() { return btnCancelarMatricula; }
    
    public String getIdMatriculaSeleccionada() { return idMatriculaSeleccionada; }
}