package controller.estudiante;

import model.matriculacion.MatriculacionModel;
import model.microcredencial.InscripcionMicrocredencialModel;
import util.Database;
import util.SwingUtil;
import view.estudiante.EstudianteView;
import view.estudiante.MisMatriculasView;
import view.estudiante.SolicitudMatriculaView;

import java.awt.*;

/**
 * Controlador de la vista principal del estudiante.
 * Gestiona la navegación fluida entre el menú de bienvenida, la consulta de
 * matrículas y la solicitud unificada de matrícula (grados y microcredenciales),
 * todo en una única ventana centrada.
 */
public class EstudianteController {
    private EstudianteView view;
    private String uo;
    private Runnable onVolverLogin;
    private String nombre;

    private MisMatriculasView misMatriculasView;
    private MisMatriculasController misMatriculasController;
    private MatriculacionModel matriculacionModel;
    private InscripcionMicrocredencialModel inscripcionMicrocredencialModel;

    private SolicitudMatriculaView solicitudMatriculaView;
    private SolicitudMatriculaController solicitudMatriculaController;

    private SolicitudMicrocredencialView solicitudMicrocredencialView;
    private SolicitudMicrocredencialController solicitudMicrocredencialController;

    public EstudianteController(EstudianteView view, Database db) {
        this(view, db, null, null, null);
    }

    public EstudianteController(EstudianteView view, Database db, String uo) {
        this(view, db, uo, null, null);
    }

    public EstudianteController(EstudianteView view, Database db, String uo, Runnable onVolverLogin) {
        this(view, db, uo, null, onVolverLogin);
    }

    public EstudianteController(EstudianteView view, Database db, String uo, String nombre, Runnable onVolverLogin) {
        this.view = view;
        this.uo = uo;
        this.nombre = nombre;
        this.onVolverLogin = onVolverLogin;
        this.matriculacionModel = new MatriculacionModel(db);
        this.inscripcionMicrocredencialModel = new InscripcionMicrocredencialModel(db);
        if ((this.nombre == null || this.nombre.trim().isEmpty()) && this.uo != null) {
            this.nombre = this.matriculacionModel.obtenerNombreAlumno(this.uo);
        }
    }

    public void initController() {
        // Establecer cartel de bienvenida con el nombre del estudiante
        view.setEstudianteInfo(uo, nombre);

        // 1. Navegabilidad: Volver al Login
        view.getBtnVolverLogin().addActionListener(e -> volverAlLogin());

        // 2. Opción 1: Consultar Mis Matrículas (se abre dentro de la misma ventana)
        view.getBtnMisMatriculas().addActionListener(e -> SwingUtil.exceptionWrapper(this::abrirConsultaMatriculas));

        // 3. Opción 2: Solicitar Nueva Matrícula (Grado Oficial)
        view.getBtnSolicitarMatricula().addActionListener(e -> SwingUtil.exceptionWrapper(this::abrirSolicitudMatricula));

        // 4. Opción 3: Solicitar Nueva Microcredencial (Microcredenciales)
        view.getBtnSolicitarMicrocredencial().addActionListener(e -> SwingUtil.exceptionWrapper(this::abrirSolicitudMicrocredencial));

        // Mostrar menú inicial y centrar
        view.mostrarMenu();
        view.getFrame().setLocationRelativeTo(null);
        view.getFrame().setVisible(true);
    }

    private void volverAlLogin() {
        view.getFrame().setVisible(false);
        view.getFrame().dispose();
        if (onVolverLogin != null) {
            onVolverLogin.run();
        }
    }

    private void abrirConsultaMatriculas() {
        if (misMatriculasView == null) {
            misMatriculasView = new MisMatriculasView();
            misMatriculasController = new MisMatriculasController(
                    misMatriculasView,
                    matriculacionModel,
                    uo,
                    () -> view.mostrarMenu()
            );
            misMatriculasController.initController();

            view.getPanelConsultaContainer().removeAll();
            view.getPanelConsultaContainer().add(misMatriculasView.getPanelPrincipal(), BorderLayout.CENTER);
            view.getPanelConsultaContainer().revalidate();
            view.getPanelConsultaContainer().repaint();
        } else {
            misMatriculasController.refrescarTodo();
        }
        view.mostrarConsulta();
    }

    private void asegurarSolicitudMatriculaInicializada() {
        if (solicitudMatriculaView == null) {
            solicitudMatriculaView = new SolicitudMatriculaView();
            solicitudMatriculaController = new SolicitudMatriculaController(
                    solicitudMatriculaView,
                    matriculacionModel,
                    uo,
                    () -> {
                        if (misMatriculasController != null) {
                            misMatriculasController.refrescarTodo();
                        }
                        view.mostrarMenu();
                    });
            solicitudMatriculaController.initController();

            view.getPanelSolicitudContainer().removeAll();
            view.getPanelSolicitudContainer().add(solicitudMatriculaView.getPanelPrincipal(), BorderLayout.CENTER);
            view.getPanelSolicitudContainer().revalidate();
            view.getPanelSolicitudContainer().repaint();
        } else {
            solicitudMatriculaController.recargarOferta();
        }
    }

    private void abrirSolicitudMatricula() {
        asegurarSolicitudMatriculaInicializada();
        solicitudMatriculaController.seleccionarModoGrado();
        view.mostrarSolicitud();
    }

    private void abrirSolicitudMicrocredencial() {
        if (solicitudMicrocredencialView == null) {
            solicitudMicrocredencialView = new SolicitudMicrocredencialView();
            solicitudMicrocredencialController = new SolicitudMicrocredencialController(
                    solicitudMicrocredencialView,
                    matriculacionModel,
                    inscripcionMicrocredencialModel,
                    uo,
                    () -> {
                        if (misMatriculasController != null) {
                            misMatriculasController.refrescarTodo();
                        }
                        view.mostrarMenu();
                    });
            solicitudMicrocredencialController.initController();

            view.getPanelMicrocredencialContainer().removeAll();
            view.getPanelMicrocredencialContainer().add(solicitudMicrocredencialView.getPanelPrincipal(), BorderLayout.CENTER);
            view.getPanelMicrocredencialContainer().revalidate();
            view.getPanelMicrocredencialContainer().repaint();
        } else {
            solicitudMicrocredencialController.recargarOferta();
        }
        view.mostrarMicrocredencial();
    }
}
