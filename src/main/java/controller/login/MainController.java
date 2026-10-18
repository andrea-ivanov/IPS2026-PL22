package controller.login;

import view.login.MainView;
import view.admin.AdminOfertaView;
import view.estudiante.EstudianteView;
import view.secretario.SecretarioView; // Tu vista de secretario
import view.profesor.ProfesorView;
import java.awt.EventQueue;
import java.time.LocalDate;

import controller.admin.AdminOfertaController;
import controller.estudiante.EstudianteController;
import controller.secretario.SecretarioController;
import controller.profesor.ProfesorController;
import repository.AlumnoRepository;
import repository.MatriculacionRepository;
import util.Database;
import util.SwingUtil;

public class MainController {
    private MainView view;

    public MainController(MainView view) {
        this.view = view;
        this.initView();
    }

    public void initController() {
        // HU 35229: el sistema mantiene al día el estado de las matrículas en cada arranque,
        // antes de abrir cualquier vista (PAGADA -> ACTIVA al iniciar el año, ACTIVA -> FINALIZADA al terminarlo)
        SwingUtil.exceptionWrapper(() -> new MatriculacionRepository(new Database())
                .actualizarCicloVida(LocalDate.now().toString()));
        cargarAlumnos();

        view.getCbRoles().addActionListener(e -> actualizarVisibilidadAlumno());
        actualizarVisibilidadAlumno();

        view.getBtnAcceder().addActionListener(e -> gestionarAcceso());
    }

    private void cargarAlumnos() {
        AlumnoRepository repo = new AlumnoRepository();
        view.getCbAlumnos().setModel(SwingUtil.getComboModelFromList(repo.getListaAlumnos()));
    }

    private void actualizarVisibilidadAlumno() {
        boolean esEstudiante = "Estudiante".equals(view.getCbRoles().getSelectedItem());
        view.getPanelAlumno().setVisible(esEstudiante);
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
        }
    }

    private void abrirVistaEstudiante() {
        Object itemSeleccionado = view.getCbAlumnos().getSelectedItem();
        String uo = null;
        if (itemSeleccionado != null) {
            uo = itemSeleccionado.toString().split(" - ")[0].trim();
        }
        EstudianteView estudianteView = new EstudianteView();
        EstudianteController estudianteController = new EstudianteController(estudianteView, uo);
        estudianteController.initController();
        view.getFrame().setVisible(false);
    }

    private void abrirVistaAdministrador() {
        AdminOfertaView adminView = new AdminOfertaView();
        AdminOfertaController adminController = new AdminOfertaController(adminView);
        adminController.setOnVolverInicio(() -> view.getFrame().setVisible(true));
        adminController.initController();
        view.getFrame().setVisible(false);
    }

    private void abrirVistaSecretario() {
        SecretarioView secView = new SecretarioView();
        SecretarioController secController = new SecretarioController(secView);
        secController.setOnVolverInicio(() -> view.getFrame().setVisible(true));
        secController.initController();
        view.getFrame().setVisible(false);
    }

    private void abrirVistaProfesor() {
        ProfesorView profView = new ProfesorView();
        ProfesorController profController = new ProfesorController(profView);
        profController.initController();
        view.getFrame().setVisible(false);
    }

    private void initView() {
        view.getFrame().setVisible(true);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainView vistaPrincipal = new MainView();
                    MainController controladorPrincipal = new MainController(vistaPrincipal);
                    controladorPrincipal.initController();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ;
            }
        });
    }
}