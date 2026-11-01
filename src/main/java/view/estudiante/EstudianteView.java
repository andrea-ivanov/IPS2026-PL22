package view.estudiante;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

import view.comun.Estilo;

/**
 * Vista principal del estudiante.
 * Gestiona de forma unificada en una sola ventana (mediante CardLayout):
 * - Menú principal de opciones del estudiante (Consulta y Nueva Matriculación).
 * - Vista embebida de consulta de matrículas, calificaciones e hitos de
 * permanencia.
 * - Vista embebida de solicitud de matrícula (HU 35235).
 * - Navegación hacia atrás (volver al login o volver al menú de inicio).
 */
public class EstudianteView {
    public static final String CARD_MENU = "CARD_MENU";
    public static final String CARD_CONSULTA = "CARD_CONSULTA";
    public static final String CARD_SOLICITUD = "CARD_SOLICITUD";
    public static final String CARD_MICROCREDENCIAL = "CARD_MICROCREDENCIAL";

    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel cardsPanel;

    // --- Componentes del Menú Principal ---
    private JLabel lblTitle;
    private JButton btnMisMatriculas;
    private JButton btnSolicitarMatricula;
    private JButton btnSolicitarMicrocredencial;
    private JButton btnVolverLogin;

    // --- Contenedores embebidos de submódulos ---
    private JPanel panelConsultaContainer;
    private JPanel panelSolicitudContainer;
    private JPanel panelMicrocredencialContainer;

    public EstudianteView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Vista Estudiante");
        frame.setBounds(100, 80, 920, 650);
        Estilo.maximizarCentrado(frame);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);

        // 1. Tarjeta: Menú Principal
        JPanel panelMenu = crearPanelMenu();
        cardsPanel.add(panelMenu, CARD_MENU);

        // 2. Tarjeta: Contenedor para Consulta de Matrículas
        panelConsultaContainer = new JPanel(new BorderLayout());
        cardsPanel.add(panelConsultaContainer, CARD_CONSULTA);

        // 3. Tarjeta: Contenedor para Solicitud de Matrícula
        panelSolicitudContainer = new JPanel(new BorderLayout());
        cardsPanel.add(panelSolicitudContainer, CARD_SOLICITUD);

        // 4. Tarjeta: Contenedor para Solicitud de Microcredenciales
        panelMicrocredencialContainer = new JPanel(new BorderLayout());
        cardsPanel.add(panelMicrocredencialContainer, CARD_MICROCREDENCIAL);

        frame.getContentPane().add(cardsPanel, BorderLayout.CENTER);
    }

    private JPanel crearPanelMenu() {
        JPanel panel = new JPanel(new BorderLayout(15, 20));
        panel.setBorder(new EmptyBorder(25, 40, 25, 40));
        panel.setBackground(new Color(248, 249, 250));

        // --- Encabezado ---
        JPanel panelNorte = new JPanel();
        panelNorte.setLayout(new BoxLayout(panelNorte, BoxLayout.Y_AXIS));
        panelNorte.setOpaque(false);

        lblTitle = new JLabel("¡Bienvenido!");
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 22));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitle.setForeground(new Color(33, 37, 41));

        panelNorte.add(lblTitle);
        panelNorte.add(Box.createVerticalStrut(6));
        panel.add(panelNorte, BorderLayout.NORTH);

        // --- Panel Central con las Opciones Principales ---
        JPanel panelOpciones = new JPanel(new GridLayout(1, 2, 25, 0));
        panelOpciones.setOpaque(false);
        panelOpciones.setBorder(new EmptyBorder(15, 10, 15, 10));

        // Opción 1: Consulta de Matrículas (Columna Izquierda)
        JPanel cardConsulta = crearTarjetaOpcion(
                "Consultar Mis Matrículas",
                "<html>Acceda a su historial académico, consulte las asignaturas matriculadas, sus calificaciones en las convocatorias e indicadores del régimen de permanencia.</html>",
                "Mi Expediente",
                false);
        btnMisMatriculas = (JButton) cardConsulta.getClientProperty("botonAccion");
        panelOpciones.add(cardConsulta);

        // Columna Derecha: Matrícula Oficial arriba y Microcredenciales justo debajo
        JPanel panelDerecho = new JPanel(new GridLayout(2, 1, 0, 15));
        panelDerecho.setOpaque(false);

        // Opción 2: Nueva Solicitud de Matrícula (Grados Oficiales)
        JPanel cardSolicitud = crearTarjetaOpcion(
                "Solicitar Nueva Matrícula",
                "<html>Explore la oferta académica en grados y cursos activos, seleccione asignaturas con cálculo automático e importe y confirme el método de pago.</html>",
                "Nueva Solicitud",
                true);
        btnSolicitarMatricula = (JButton) cardSolicitud.getClientProperty("botonAccion");
        panelDerecho.add(cardSolicitud);

        // Opción 3: Solicitar Nueva Microcredencial (justo debajo del recuadro
        // anterior)
        JPanel cardMicrocredencial = crearTarjetaOpcion(
                "Solicitar Nueva Microcredencial",
                "<html>Inscríbase en microcredenciales de especialización vinculadas a sus ramas habilitadas, con tarifa de 20 €/crédito y sin impacto en permanencia.</html>",
                "Nueva Microcredencial",
                true);
        btnSolicitarMicrocredencial = (JButton) cardMicrocredencial.getClientProperty("botonAccion");
        panelDerecho.add(cardMicrocredencial);

        panelOpciones.add(panelDerecho);
        panel.add(panelOpciones, BorderLayout.CENTER);

        // --- Panel Sur con Botón para Volver al Login ---
        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelSur.setOpaque(false);
        btnVolverLogin = Estilo.estilizarSecundario(new JButton("Cerrar Sesión"));
        btnVolverLogin.setPreferredSize(new Dimension(270, 36));
        btnVolverLogin.setFont(new Font("Tahoma", Font.BOLD, 13));
        panelSur.add(btnVolverLogin);
        panel.add(panelSur, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearTarjetaOpcion(String titulo, String descripcion, String textoBoton) {
        return crearTarjetaOpcion(titulo, descripcion, textoBoton, false);
    }

    private JPanel crearTarjetaOpcion(String titulo, String descripcion, String textoBoton, boolean compacta) {
        JPanel card = new JPanel(new BorderLayout(8, compacta ? 8 : 15));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(compacta ? 12 : 20, compacta ? 16 : 20, compacta ? 10 : 20, compacta ? 16 : 20)));

        // Título de la opción
        JLabel lblOpcionTitulo = new JLabel(titulo, SwingConstants.CENTER);
        lblOpcionTitulo.setFont(new Font("Tahoma", Font.BOLD, compacta ? 14 : 16));
        lblOpcionTitulo.setForeground(new Color(13, 110, 253));
        card.add(lblOpcionTitulo, BorderLayout.NORTH);

        // Descripción
        JLabel lblDesc = new JLabel(descripcion);
        lblDesc.setFont(new Font("Tahoma", Font.PLAIN, compacta ? 12 : 13));
        lblDesc.setForeground(new Color(73, 80, 87));
        lblDesc.setVerticalAlignment(SwingConstants.TOP);
        card.add(lblDesc, BorderLayout.CENTER);

        // Botón de acción
        JButton btn = Estilo.estilizarPrimario(new JButton(textoBoton));
        btn.setFont(new Font("Tahoma", Font.BOLD, compacta ? 12 : 13));
        btn.setPreferredSize(new Dimension(compacta ? 190 : 200, compacta ? 32 : 38));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel panelBtn = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panelBtn.setOpaque(false);
        panelBtn.add(btn);
        card.add(panelBtn, BorderLayout.SOUTH);

        card.putClientProperty("botonAccion", btn);
        return card;
    }

    public void mostrarMenu() {
        cardLayout.show(cardsPanel, CARD_MENU);
        frame.setTitle("Vista Estudiante — Menú Principal");
    }

    public void mostrarConsulta() {
        cardLayout.show(cardsPanel, CARD_CONSULTA);
        frame.setTitle("Vista Estudiante — Consulta de Matrículas y Calificaciones");
    }

    public void mostrarSolicitud() {
        cardLayout.show(cardsPanel, CARD_SOLICITUD);
        frame.setTitle("Vista Estudiante — Solicitud de Nueva Matrícula");
    }

    public void mostrarMicrocredencial() {
        cardLayout.show(cardsPanel, CARD_MICROCREDENCIAL);
        frame.setTitle("Vista Estudiante — Solicitud de Microcredenciales");
    }

    public JFrame getFrame() {
        return frame;
    }

    public JButton getBtnMisMatriculas() {
        return btnMisMatriculas;
    }

    public JButton getBtnSolicitarMatricula() {
        return btnSolicitarMatricula;
    }

    public JButton getBtnSolicitarMicrocredencial() {
        return btnSolicitarMicrocredencial;
    }

    public JButton getBtnVolverLogin() {
        return btnVolverLogin;
    }

    public JPanel getPanelConsultaContainer() {
        return panelConsultaContainer;
    }

    public JPanel getPanelSolicitudContainer() {
        return panelSolicitudContainer;
    }

    public JPanel getPanelMicrocredencialContainer() {
        return panelMicrocredencialContainer;
    }

    public JLabel getLblTitle() {
        return lblTitle;
    }

    public void setEstudianteInfo(String uo, String nombre) {
        if (nombre != null && !nombre.trim().isEmpty()) {
            lblTitle.setText("¡Bienvenido " + nombre.trim() + "!");
        } else if (uo != null && !uo.trim().isEmpty()) {
            lblTitle.setText("¡Bienvenido " + uo.trim() + "!");
        } else {
            lblTitle.setText("¡Bienvenido!");
        }
    }
}
