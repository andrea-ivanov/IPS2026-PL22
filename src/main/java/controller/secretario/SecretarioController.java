package controller.secretario;

import model.matriculacion.MatriculacionDisplayDTO;
import repository.MatriculacionRepository;
import view.secretario.SecretarioView;
import giis.demo.util.Database; 

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class SecretarioController {
    private SecretarioView view;
    private MatriculacionRepository repository;

    public SecretarioController(SecretarioView view) {
        this.view = view;
        
        // Conectamos con la base de datos existente sin borrarla al iniciar
        Database db = new Database();
        this.repository = new MatriculaRepository(db); 
        
        this.initView();
    }

    public void initController() {
        view.getBtnCargarMatriculas().addActionListener(e -> cargarMatriculasFiltradas());
        view.getBtnAceptarSolicitud().addActionListener(e -> aceptarSolicitud());
        view.getBtnModificarMatricula().addActionListener(e -> modificarMatricula());
        view.getBtnCancelarMatricula().addActionListener(e -> cancelarMatricula());
        
        cargarMatriculasFiltradas();
    }

    private void cargarMatriculasFiltradas() {
        String estadoSelec = (String) view.getCbFiltroEstado().getSelectedItem();
        String estudianteSelec = view.getTxtFiltroEstudiante().getText().toLowerCase().trim();
        String gradoSelec = view.getTxtFiltroGrado().getText().toLowerCase().trim();
        String cursoSelec = view.getTxtFiltroCurso().getText().toLowerCase().trim();
        String grupoSelec = view.getTxtFiltroGrupo().getText().toLowerCase().trim();
        
        List<MatriculaDisplayDTO> listaCompleta = repository.getTodasLasMatriculas();
        
        DefaultTableModel model = (DefaultTableModel) view.getTabMatriculas().getModel();
        model.setRowCount(0);

        for (MatriculaDisplayDTO m : listaCompleta) {
            boolean coincideEstado = "TODOS".equals(estadoSelec) || (m.getEstado() != null && m.getEstado().equalsIgnoreCase(estadoSelec));
            
            boolean coincideEstudiante = estudianteSelec.isEmpty() || 
                                         (m.getEstudiante() != null && m.getEstudiante().toLowerCase().contains(estudianteSelec)) || 
                                         (m.getDni() != null && m.getDni().toLowerCase().contains(estudianteSelec));
                                         
            boolean coincideGrado = gradoSelec.isEmpty() || (m.getGrado() != null && m.getGrado().toLowerCase().contains(gradoSelec));
            boolean coincideCurso = cursoSelec.isEmpty() || (m.getCurso() != null && m.getCurso().toLowerCase().contains(cursoSelec));
            boolean coincideGrupo = grupoSelec.isEmpty() || (m.getGrupo() != null && m.getGrupo().toLowerCase().contains(grupoSelec));
            
            if (coincideEstado && coincideEstudiante && coincideGrado && coincideCurso && coincideGrupo) {
                model.addRow(new Object[]{
                    m.getId(), 
                    m.getEmail(),
                    m.getDni(),
                    m.getEstudiante(), 
                    m.getGrado(), 
                    m.getCurso(),
                    m.getGrupo(),
                    m.getEstado(), 
                    "Ver / Gestionar"
                });
            }
        }
        
        view.getTabMatriculas().setAutoCreateRowSorter(true);
        view.getTabMatriculas().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        view.getTabMatriculas().setFillsViewportHeight(true);
    }
    
    private void aceptarSolicitud() {
        String idMatricula = view.getIdMatriculaSeleccionada();
        int confirm = JOptionPane.showConfirmDialog(view.getFrame(), 
                "¿Estás seguro de que deseas aceptar la solicitud (Ref: " + idMatricula + ") y registrar su pago?", 
                "Aceptar Solicitud y Pago", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                
        if (confirm == JOptionPane.YES_OPTION) {
            repository.actualizarEstadoMatricula(idMatricula, "PAGADA");
            JOptionPane.showMessageDialog(view.getFrame(), "Solicitud ACEPTADA correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            view.showListadoView();
            cargarMatriculasFiltradas(); 
        }
    }

    private void modificarMatricula() {
        String idMatricula = view.getIdMatriculaSeleccionada();
        MatriculaDisplayDTO matricula = repository.getMatriculaById(idMatricula);
        
        if (matricula != null) {
            String nuevoEmail = (String) JOptionPane.showInputDialog(
                    view.getFrame(),
                    "Modificar el correo electrónico de contacto para la matrícula:",
                    "Modificar Información (Ref: " + idMatricula + ")",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    matricula.getEmail());

            if (nuevoEmail != null && !nuevoEmail.trim().isEmpty()) {
                repository.modificarEmailMatricula(idMatricula, nuevoEmail);
                JOptionPane.showMessageDialog(view.getFrame(), "Información de la matrícula actualizada correctamente en la Base de Datos.", "Modificación Exitosa", JOptionPane.INFORMATION_MESSAGE);
                view.showListadoView();
                cargarMatriculasFiltradas(); 
            }
        }
    }

    private void cancelarMatricula() {
        String idMatricula = view.getIdMatriculaSeleccionada();
        String[] motivos = {"IMPAGO", "FUERZA_MAYOR", "INCOMPATIBILIDAD"};
        
        String motivoSeleccionado = (String) JOptionPane.showInputDialog(
                view.getFrame(),
                "Seleccione el motivo de cancelación obligatorio:",
                "Confirmar Cancelación",
                JOptionPane.WARNING_MESSAGE, null, motivos, motivos[0]);

        if (motivoSeleccionado != null) {
            repository.cancelarMatricula(idMatricula, motivoSeleccionado);
            JOptionPane.showMessageDialog(view.getFrame(), "Matrícula cancelada correctamente.\nMotivo: " + motivoSeleccionado, "Cancelada", JOptionPane.INFORMATION_MESSAGE);
            view.showListadoView();
            cargarMatriculasFiltradas(); 
        }
    }

    private void initView() {
        view.showListadoView();
        view.getFrame().setVisible(true);
    }
}