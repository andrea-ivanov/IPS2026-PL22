package controller.estudiante;

import model.matriculacion.MatriculacionModel;
import view.estudiante.EstudianteView;
import view.estudiante.MisMatriculasView;
import view.estudiante.SolicitudMatriculaView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Controlador de la vista principal del estudiante.
 * Gestiona la navegación fluida entre el menú de bienvenida, la consulta de
 * matrículas
 * y la solicitud de matrícula, todo en una única ventana centrada.
 */
public class EstudianteController {
    private EstudianteView view;
    private String uo;
    private Runnable onVolverLogin;
    private String nombre;

    private MisMatriculasView misMatriculasView;
    private MisMatriculasController misMatriculasController;
    private MatriculacionModel matriculacionModel;

    private SolicitudMatriculaView solicitudMatriculaView;
    private SolicitudMatriculaController solicitudMatriculaController;

    public EstudianteController(EstudianteView view) {
        this(view, null, null, null);
    }

    public EstudianteController(EstudianteView view, String uo) {
        this(view, uo, null, null);
    }

    public EstudianteController(EstudianteView view, String uo, Runnable onVolverLogin) {
        this(view, uo, null, onVolverLogin);
    }

    public EstudianteController(EstudianteView view, String uo, String nombre, Runnable onVolverLogin) {
        this.view = view;
        this.uo = uo;
        this.nombre = nombre;
        this.onVolverLogin = onVolverLogin;
        this.matriculacionModel = new MatriculacionModel();
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
        view.getBtnMisMatriculas().addActionListener(e -> abrirConsultaMatriculas());

        // 3. Opción 2: Solicitar Nueva Matrícula (se abre dentro de la misma ventana)
        view.getBtnSolicitarMatricula().addActionListener(e -> abrirSolicitudMatricula());

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
                    () -> view.mostrarMenu() // Al pulsar volver en la vista de matrículas, regresa al menú de inicio
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

    private void abrirSolicitudMatricula() {
        if (solicitudMatriculaView == null) {
            solicitudMatriculaView = new SolicitudMatriculaView();
            solicitudMatriculaController = new SolicitudMatriculaController(
                    solicitudMatriculaView,
                    matriculacionModel,
                    uo,
                    () -> {
                        // Al volver al menú principal, refrescamos también la vista de matrículas si ya estaba creada
                        if (misMatriculasController != null) {
                            misMatriculasController.refrescarTodo();
                        }
                        view.mostrarMenu();
                    }
            );
            solicitudMatriculaController.initController();

            view.getPanelSolicitudContainer().removeAll();
            view.getPanelSolicitudContainer().add(solicitudMatriculaView.getPanelPrincipal(), BorderLayout.CENTER);
            view.getPanelSolicitudContainer().revalidate();
            view.getPanelSolicitudContainer().repaint();
        } else {
            solicitudMatriculaController.recargarOferta();
        }
        view.mostrarSolicitud();
    }
}
