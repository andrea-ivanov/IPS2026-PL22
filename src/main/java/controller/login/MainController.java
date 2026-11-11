package controller.login;

import view.login.MainView;
import view.admin.AdminOfertaView;
import view.estudiante.EstudianteView;
import view.secretario.SecretarioView; // Tu vista de secretario
import view.profesor.ProfesorView;
import view.revisor.RevisorView;
import java.awt.EventQueue;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.time.LocalDate;

import controller.admin.AdminOfertaController;
import controller.estudiante.EstudianteController;
import controller.secretario.SecretarioController;
import controller.profesor.ProfesorController;
import controller.revisor.RevisorController;
import model.alumno.AlumnoModel;
import model.profesor.ProfesorDTO;
import model.profesor.ProfesorModel;
import model.acta.ActaModel;
import model.gestionmatricula.GestionMatriculaModel;
import model.grado.GradoModel;
import util.ComboBuscable;
import util.Database;
import util.SwingUtil;

public class MainController {
    private MainView view;
    private final Database db;

    public MainController(MainView view, Database db) {
        this.view = view;
        this.db = db;
        this.initView();
    }

    public void initController() {
        // Asegurar que todos los diálogos Swing muestren los botones en español en
        // cualquier SO
        UIManager.put("OptionPane.yesButtonText", "Sí");
        UIManager.put("OptionPane.noButtonText", "No");
        UIManager.put("OptionPane.okButtonText", "Aceptar");
        UIManager.put("OptionPane.cancelButtonText", "Cancelar");

        actualizarEstadosPorFecha();
        cargarProfesores();
        cargarAlumnos();

        view.getCbRoles().addActionListener(e -> actualizarVisibilidadUsuario());
        actualizarVisibilidadUsuario();

        view.getBtnAcceder().addActionListener(e -> SwingUtil.exceptionWrapper(this::gestionarAcceso));
        view.getBtnRecargarBD().addActionListener(e -> SwingUtil.exceptionWrapper(this::recargarBaseDatos));
    }

    /**
     * Lo que el sistema actualiza solo segun la fecha actual: se hace al arrancar y
     * al recargar la BD
     */
    private void actualizarEstadosPorFecha() {
        // HU 36455: al empezar el año, los grados en extinción, los pendientes de renovación sin dictamen y los
        // que no tienen matrículas pagadas pasan a EXTINTO y se cancelan sus matrículas del año. Va antes que el
        // ciclo de vida, para que este no active las matrículas que se cancelan
        SwingUtil.exceptionWrapper(() -> new GradoModel(db).extinguirGradosAlIniciarAnio(LocalDate.now().toString()));
        // HU 35229: el sistema mantiene al día el estado de las matrículas en cada
        // arranque,
        // antes de abrir cualquier vista (PAGADA -> ACTIVA al iniciar el año, ACTIVA ->
        // FINALIZADA al terminarlo)
        SwingUtil.exceptionWrapper(() -> new GestionMatriculaModel(db)
                .actualizarCicloVida(LocalDate.now().toString()));
        // HU 35239: las actas abiertas cuya convocatoria ya ha terminado las cierra el
        // sistema
        // (cierreAutomatico = 1); secretaria las consulta como avisos en su vista
        SwingUtil.exceptionWrapper(() -> new ActaModel(db).cerrarActasVencidas(LocalDate.now().toString()));
    }

    /**
     * La BD es persistente: data.sql solo se carga sola si la BD no tiene tablas.
     * Este boton la borra y la vuelve a cargar desde schema.sql y data.sql (se
     * pierden los cambios hechos en la aplicacion)
     */
    private void recargarBaseDatos() {
        int confirm = JOptionPane.showConfirmDialog(view.getFrame(),
                "¿Seguro que quieres recargar la base de datos desde schema.sql y data.sql?\n"
                        + "Se perderán todos los cambios hechos en la aplicación.",
                "Recargar base de datos", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        db.createDatabase(false);
        db.loadDatabase();
        actualizarEstadosPorFecha();
        cargarProfesores();
        cargarAlumnos();
        JOptionPane.showMessageDialog(view.getFrame(), "Base de datos recargada correctamente.", "Éxito",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void cargarProfesores() {
        ProfesorModel profesorModel = new ProfesorModel(db);
        view.getCbProfesores().setModel(
                new DefaultComboBoxModel<>(profesorModel.getProfesores().toArray(new ProfesorDTO[0])));
        // se puede escribir parte del nombre, apellidos o id para buscar el profesor
        // (como el combo de alumnos)
        ComboBuscable.hacerBuscable(view.getCbProfesores());
    }

    private void cargarAlumnos() {
        AlumnoModel alumnoModel = new AlumnoModel(db);
        view.getCbAlumnos().setModel(SwingUtil.getComboModelFromList(alumnoModel.getListaAlumnos()));
        // se puede escribir parte del nombre, apellidos o UO para buscar el alumno
        // (como el combo de profesores)
        ComboBuscable.hacerBuscable(view.getCbAlumnos());
    }

    /**
     * Solo se ve la fila del usuario que corresponde al rol: el profesor o el
     * alumno
     */
    private void actualizarVisibilidadUsuario() {
        Object rol = view.getCbRoles().getSelectedItem();
        view.getPanelProfesor().setVisible("Profesor".equals(rol));
        view.getPanelAlumno().setVisible("Estudiante".equals(rol));
        view.getFrame().revalidate();
        view.getFrame().repaint();
    }

    private void gestionarAcceso() {
        String rolSeleccionado = (String) view.getCbRoles().getSelectedItem();
        if (rolSeleccionado == null)
            return;

        switch (rolSeleccionado) {
            case "Administrador":
                abrirVistaAdministrador();
                break;
            case "Secretario":
                abrirVistaSecretario();
                break;
            case "Profesor":
                abrirVistaProfesor();
                break;
            case "Estudiante":
                abrirVistaEstudiante();
                break;
            case "Revisor externo":
                abrirVistaRevisor();
                break;
        }
    }

    private void abrirVistaEstudiante() {
        // lo escrito tiene que corresponder a un único alumno de la lista
        Object itemSeleccionado = ComboBuscable.getSeleccion(view.getCbAlumnos());
        if (itemSeleccionado == null) {
            JOptionPane.showMessageDialog(view.getFrame(),
                    "Escriba o seleccione un alumno de la lista (si hay varios que coinciden, elija uno).",
                    "Seleccione un alumno", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String uo = null;
        String nombre = null;
        if (itemSeleccionado != null) {
            String itemStr = itemSeleccionado.toString().trim();
            if (itemStr.contains(" - ")) {
                String[] parts = itemStr.split(" - ", 2);
                uo = parts[0].trim();
                nombre = parts[1].trim();
            } else {
                uo = itemStr;
            }
        }
        EstudianteView estudianteView = new EstudianteView();
        EstudianteController estudianteController = new EstudianteController(estudianteView, db, uo, nombre, () -> {
            view.getFrame().setLocationRelativeTo(null);
            view.getFrame().setVisible(true);
        });
        estudianteController.initController();
        view.getFrame().setVisible(false);
    }

    private void abrirVistaAdministrador() {
        AdminOfertaView adminView = new AdminOfertaView();
        AdminOfertaController adminController = new AdminOfertaController(adminView, db);
        adminController.setOnVolverInicio(() -> view.getFrame().setVisible(true));
        adminController.initController();
        view.getFrame().setVisible(false);
    }

    private void abrirVistaSecretario() {
        SecretarioView secView = new SecretarioView();
        SecretarioController secController = new SecretarioController(secView, db);
        secController.setOnVolverInicio(() -> view.getFrame().setVisible(true));
        secController.initController();
        view.getFrame().setVisible(false);
    }

    private void abrirVistaProfesor() {
        // lo escrito tiene que corresponder a un único profesor de la lista
        ProfesorDTO profesor = ComboBuscable.getSeleccion(view.getCbProfesores());
        if (profesor == null) {
            JOptionPane.showMessageDialog(view.getFrame(),
                    "Escriba o seleccione un profesor de la lista (si hay varios que coinciden, elija uno).",
                    "Seleccione un profesor", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ProfesorView profView = new ProfesorView();
        ProfesorController profController = new ProfesorController(profView, db, profesor);
        profController.setOnVolverInicio(() -> view.getFrame().setVisible(true));
        profController.initController();
        view.getFrame().setVisible(false);
    }

    private void abrirVistaRevisor() {
        RevisorView revisorView = new RevisorView();
        RevisorController revisorController = new RevisorController(revisorView, db);
        revisorController.setOnVolverInicio(() -> view.getFrame().setVisible(true));
        revisorController.initController();
        view.getFrame().setVisible(false);
    }

    private void initView() {
        view.getFrame().setLocationRelativeTo(null);
        view.getFrame().setVisible(true);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    Database db = new Database();
                    MainView vistaPrincipal = new MainView();
                    MainController controladorPrincipal = new MainController(vistaPrincipal, db);
                    controladorPrincipal.initController();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ;
            }
        });
    }
}