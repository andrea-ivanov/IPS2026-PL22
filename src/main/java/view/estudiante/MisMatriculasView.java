package view.estudiante;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class MisMatriculasView {
    private JFrame frame;
    private JComboBox<Object> cbGrados;
    private JTable tableMatriculas;
    private DefaultTableModel tableModel;
    private JButton btnVolver;

    public MisMatriculasView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Mis Matrículas");
        frame.setBounds(200, 200, 700, 400);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // DISPOSE para no cerrar toda la app al volver
        frame.getContentPane().setLayout(new BorderLayout(10, 10));

        // Panel superior: Título y selector de grado consultado
        JPanel panelNorte = new JPanel(new BorderLayout(5, 5));
        JLabel lblTitle = new JLabel("Listado de mis matrículas", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 16));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        panelNorte.add(lblTitle, BorderLayout.NORTH);

        JPanel panelFiltro = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panelFiltro.add(new JLabel("Grado consultado:"));
        cbGrados = new JComboBox<>();
        cbGrados.setPreferredSize(new Dimension(380, 26));
        panelFiltro.add(cbGrados);
        panelNorte.add(panelFiltro, BorderLayout.CENTER);

        frame.getContentPane().add(panelNorte, BorderLayout.NORTH);

        // Tabla central con el listado de asignaturas matriculadas (HU 35232)
        String[] columnNames = { "Año Académico", "Curso", "Asignatura", "Nº Matrícula", "Estado" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Hacemos que las celdas no sean editables a mano
            }
        };

        tableMatriculas = new JTable(tableModel);
        tableMatriculas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableMatriculas.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(tableMatriculas);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        // Panel inferior para botones
        JPanel panelBottom = new JPanel();
        btnVolver = new JButton("Cerrar");
        panelBottom.add(btnVolver);
        frame.getContentPane().add(panelBottom, BorderLayout.SOUTH);
    }

    public JFrame getFrame() {
        return frame;
    }

    public JTable getTableMatriculas() {
        return tableMatriculas;
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JComboBox<Object> getCbGrados() {
        return cbGrados;
    }

    public JButton getBtnVolver() {
        return btnVolver;
    }
}
