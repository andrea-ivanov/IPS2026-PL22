package view.login;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import view.comun.Estilo;

/**
 * Pantalla de acceso: selección del rol (y del alumno si el rol es Estudiante).
 * Tiene la barra de menús estándar de la aplicación (Archivo y Ayuda) y no se puede maximizar.
 */
public class MainView {
    private static final String NOMBRE_APLICACION = "Sistema de Matriculación";

    private JFrame frame;
    private JComboBox<String> cbRoles;
    private JPanel panelAlumno;
    private JComboBox<Object> cbAlumnos;
    private JButton btnAcceder;
    private JMenuItem btnRecargarBD;

    public MainView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame(NOMBRE_APLICACION + " - Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setJMenuBar(crearMenu());

        JPanel contenido = Estilo.contenido(new BorderLayout(0, 14));
        contenido.setBorder(new EmptyBorder(18, 24, 20, 24));
        frame.setContentPane(contenido);

        JLabel lblTitulo = new JLabel("Acceso a la aplicación");
        contenido.add(Estilo.cabecera(NOMBRE_APLICACION.toUpperCase(), lblTitulo), BorderLayout.NORTH);
        contenido.add(crearTarjetaAcceso(), BorderLayout.CENTER);

        // Tamaño fijo con hueco para la fila del alumno, que aparece y desaparece según el rol
        frame.setSize(480, 400);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
    }

    private JPanel crearTarjetaAcceso() {
        JPanel tarjeta = Estilo.tarjeta(new BorderLayout(0, 12));
        tarjeta.setBorder(Estilo.bordeTarjeta(16, 20));

        JPanel campos = new JPanel();
        campos.setLayout(new BoxLayout(campos, BoxLayout.Y_AXIS));
        campos.setOpaque(false);

        JLabel lblIndicacion = Estilo.textoSecundario("Seleccione su rol para entrar en la aplicación");
        lblIndicacion.setAlignmentX(Component.LEFT_ALIGNMENT);
        campos.add(lblIndicacion);
        campos.add(Box.createVerticalStrut(12));

        // Panel selección de rol
        JPanel panelRol = filaCampo("Rol de acceso");
        cbRoles = new JComboBox<>(new String[] { "Administrador", "Secretario", "Profesor", "Estudiante", "Revisor externo" });
        anadirCampo(panelRol, cbRoles);
        campos.add(panelRol);
        campos.add(Box.createVerticalStrut(12));

        // Panel selección de alumno (visible cuando se selecciona Estudiante)
        panelAlumno = filaCampo("Alumno");
        cbAlumnos = new JComboBox<>();
        anadirCampo(panelAlumno, cbAlumnos);
        panelAlumno.setVisible(false);
        campos.add(panelAlumno);
        tarjeta.add(campos, BorderLayout.NORTH);

        // Panel botón acceder
        btnAcceder = Estilo.botonPrimario("Acceder");
        JPanel panelBoton = new JPanel(new BorderLayout());
        panelBoton.setOpaque(false);
        panelBoton.add(btnAcceder, BorderLayout.CENTER);
        tarjeta.add(panelBoton, BorderLayout.SOUTH);
        return tarjeta;
    }

    /** Fila con la etiqueta encima del campo. */
    private static JPanel filaCampo(String etiqueta) {
        JPanel fila = new JPanel(new BorderLayout(0, 4));
        fila.setOpaque(false);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila.add(Estilo.etiqueta(etiqueta), BorderLayout.NORTH);
        return fila;
    }

    private static void anadirCampo(JPanel fila, JComponent campo) {
        Estilo.estilizarCampo(campo);
        campo.setPreferredSize(new Dimension(380, 30));
        fila.add(campo, BorderLayout.CENTER);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, fila.getPreferredSize().height));
    }

    // =========================================================================
    // Barra de menús
    // =========================================================================

    private JMenuBar crearMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu menuArchivo = new JMenu("Archivo");
        menuArchivo.setMnemonic(KeyEvent.VK_A);
        btnRecargarBD = new JMenuItem("Recargar base de datos...");
        btnRecargarBD.setToolTipText("Borra la base de datos y la vuelve a cargarla desde el archivo SQL");
        btnRecargarBD.setMnemonic(KeyEvent.VK_R);
        btnRecargarBD.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
        menuArchivo.add(btnRecargarBD);
        menuArchivo.addSeparator();
        JMenuItem itemSalir = new JMenuItem("Salir");
        itemSalir.setMnemonic(KeyEvent.VK_S);
        itemSalir.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        // Igual que cerrar la ventana con la X
        itemSalir.addActionListener(e -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
        menuArchivo.add(itemSalir);
        barra.add(menuArchivo);

        JMenu menuAyuda = new JMenu("Ayuda");
        menuAyuda.setMnemonic(KeyEvent.VK_Y);
        JMenuItem itemManual = new JMenuItem("Manual de uso");
        itemManual.setMnemonic(KeyEvent.VK_M);
        itemManual.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        itemManual.addActionListener(e -> mostrarManual());
        menuAyuda.add(itemManual);
        menuAyuda.addSeparator();
        JMenuItem itemAcercaDe = new JMenuItem("Acerca de");
        itemAcercaDe.setMnemonic(KeyEvent.VK_C);
        itemAcercaDe.addActionListener(e -> mostrarAcercaDe());
        menuAyuda.add(itemAcercaDe);
        barra.add(menuAyuda);

        for (int i = 0; i < barra.getMenuCount(); i++) {
            estilizarMenu(barra.getMenu(i));
        }
        return barra;
    }

    private static void estilizarMenu(JMenu menu) {
        menu.setFont(Estilo.NORMAL);
        for (Component item : menu.getMenuComponents()) {
            item.setFont(Estilo.NORMAL);
        }
    }

    private void mostrarManual() {
        String texto = "<html><body style='width: 380px; font-family: Tahoma; font-size: 11px; font-weight: normal'>"
                + "<p>Seleccione su <b>rol de acceso</b> y pulse <b>Acceder</b>:</p><ul>"
                + "<li><b>Administrador</b>: oferta académica (grados, cursos y años académicos).</li>"
                + "<li><b>Secretario</b>: consulta de matrículas y grupos y gestión de su ciclo de vida "
                + "(aceptar solicitudes, modificar, asignar grupos y cancelar).</li>"
                + "<li><b>Profesor</b>: asignaciones docentes, actas de las asignaturas y propuestas de asignaturas.</li>"
                + "<li><b>Estudiante</b>: elija también el alumno para consultar su expediente o solicitar una matrícula.</li>"
                + "<li><b>Revisor externo</b>: métricas de rendimiento de los grados por año académico "
                + "(tasas de éxito y abandono, aprobados y matriculados).</li>"
                + "</ul><p>Desde cada panel, <b>Volver al inicio</b> (o Cerrar Sesión) regresa a esta pantalla.</p>"
                + "<p><b>Archivo &gt; Recargar base de datos</b> vuelve a cargar los datos iniciales "
                + "(se pierden los cambios hechos en la aplicación).</p></body></html>";
        JOptionPane.showMessageDialog(frame, texto, "Manual de uso", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarAcercaDe() {
        String texto = "<html><body style='font-family: Tahoma; font-size: 11px; font-weight: normal'>"
                + "<p style='font-size: 13px'><b>" + NOMBRE_APLICACION + "</b></p>"
                + "<p>Gestión de la oferta académica, matrículas, grupos y actas.</p><br>"
                + "<p>Ingeniería de Procesos de Software (IPS) - 2026</p>"
                + "<p>Grupo PL22</p></body></html>";
        JOptionPane.showMessageDialog(frame, texto, "Acerca de", JOptionPane.INFORMATION_MESSAGE);
    }

    public JFrame getFrame() {
        return frame;
    }

    public JComboBox<String> getCbRoles() {
        return cbRoles;
    }

    public JPanel getPanelAlumno() {
        return panelAlumno;
    }

    public JComboBox<Object> getCbAlumnos() {
        return cbAlumnos;
    }

    public JButton getBtnAcceder() {
        return btnAcceder;
    }

    /** Opción "Recargar base de datos" del menú Archivo. */
    public JMenuItem getBtnRecargarBD() {
        return btnRecargarBD;
    }
}
