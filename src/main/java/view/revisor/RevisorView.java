package view.revisor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Locale;

import model.grado.GradoAnioRevisorDisplayDTO;
import view.comun.Estilo;

/**
 * Panel del revisor externo (HU 36411): métricas de rendimiento de los grados por año académico.
 * Con un año concreto hay una fila por grado; con "Todos" la tabla se divide en un bloque por grado,
 * con una fila de cabecera "Nombre - Facultad - Rama" y una fila por año académico.
 */
public class RevisorView {
    private static final String[] COLUMNAS_ANIO = { "Nombre Grado", "Rama", "Tasa Éxito", "Tasa abandono", "Aprobados",
            "Matriculados" };
    private static final String[] COLUMNAS_TODOS = { "Año Académico", "Éxito", "Abandono", "Aprobados",
            "Matriculados" };
    private static final String SIN_VALOR = "—";

    private JFrame frame;
    private JComboBox<String> cbAnioAcademico;
    private JButton btnVolverInicio;
    private JTextField txtFiltroRama;
    private JTextField txtFiltroGrado;
    private JTable tabMetricas;

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

        JPanel filterPanel = Estilo.panelFiltros("Filtros");
        txtFiltroRama = new JTextField();
        Estilo.addFiltro(filterPanel, "Rama conocimiento", txtFiltroRama);
        txtFiltroGrado = new JTextField();
        Estilo.addFiltro(filterPanel, "Grado", txtFiltroGrado);

        tabMetricas = new JTable(new DefaultTableModel(COLUMNAS_ANIO, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        Estilo.estilizarTabla(tabMetricas);
        JPanel tablaCard = Estilo.tarjeta(new BorderLayout());
        tablaCard.add(Estilo.scrollTabla(tabMetricas), BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        panel.add(Estilo.envolverFiltros(filterPanel, 250), BorderLayout.WEST);
        panel.add(tablaCard, BorderLayout.CENTER);
        contenido.add(panel, BorderLayout.CENTER);
        mostrarMetricasAnio(List.of());
    }

    /** Tabla de un año concreto: una fila por grado. */
    public void mostrarMetricasAnio(List<GradoAnioRevisorDisplayDTO> metricas) {
        DefaultTableModel model = (DefaultTableModel) tabMetricas.getModel();
        model.setRowCount(0);
        model.setColumnIdentifiers(COLUMNAS_ANIO);
        for (GradoAnioRevisorDisplayDTO m : metricas) {
            model.addRow(new Object[] {
                    m.getNombreGrado(),
                    m.getRama(),
                    porcentaje(m.getTasaExito()),
                    porcentaje(m.getTasaAbandono()),
                    m.getAprobados(),
                    m.getMatriculados()
            });
        }
        anchos(300, 260, 120, 120, 110, 110);
        centrar(2, 3, 4, 5);
    }

    /** Tabla de "Todos": un bloque por grado (las métricas llegan ordenadas por grado). */
    public void mostrarMetricasTodos(List<GradoAnioRevisorDisplayDTO> metricas) {
        DefaultTableModel model = (DefaultTableModel) tabMetricas.getModel();
        model.setRowCount(0);
        model.setColumnIdentifiers(COLUMNAS_TODOS);
        String gradoActual = null;
        for (GradoAnioRevisorDisplayDTO m : metricas) {
            if (!m.getIdGrado().equals(gradoActual)) {
                gradoActual = m.getIdGrado();
                model.addRow(new Object[] { m.getNombreGrado() + " - " + m.getFacultad() + " - " + m.getRama() });
            }
            model.addRow(new Object[] {
                    m.getIdAnioAcademico(),
                    porcentaje(m.getTasaExito()),
                    porcentaje(m.getTasaAbandono()),
                    m.getAprobados(),
                    m.getMatriculados()
            });
        }
        // la primera columna es ancha porque también lleva la cabecera de cada bloque
        anchos(500, 120, 120, 110, 110);
        centrar(1, 2, 3, 4);
    }

    private static String porcentaje(Double tasa) {
        return tasa == null ? SIN_VALOR : String.format(Locale.ROOT, "%.1f%%", tasa);
    }

    private void anchos(int... anchos) {
        for (int i = 0; i < anchos.length; i++) {
            tabMetricas.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
    }

    private void centrar(int... columnas) {
        for (int columna : columnas) {
            tabMetricas.getColumnModel().getColumn(columna).setCellRenderer(new Estilo.RendererNumero(false));
        }
    }

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
}
