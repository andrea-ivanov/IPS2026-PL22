package controller.secretario;

import model.MatriculaDisplayDTO;
import view.secretario.SecretarioView;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

public class SecretarioController {
    private SecretarioView view;

    public SecretarioController(SecretarioView view) {
        this.view = view;
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
        
        List<MatriculaDisplayDTO> listaCompleta = obtenerMocksBD();
        
        DefaultTableModel model = (DefaultTableModel) view.getTabMatriculas().getModel();
        model.setRowCount(0);

        for (MatriculaDisplayDTO m : listaCompleta) {
            boolean coincideEstado = "TODOS".equals(estadoSelec) || m.getEstado().equalsIgnoreCase(estadoSelec);
            
            boolean coincideEstudiante = estudianteSelec.isEmpty() || 
                                         m.getEstudiante().toLowerCase().contains(estudianteSelec) || 
                                         m.getDni().toLowerCase().contains(estudianteSelec);
                                         
            boolean coincideGrado = gradoSelec.isEmpty() || m.getGrado().toLowerCase().contains(gradoSelec);
            boolean coincideCurso = cursoSelec.isEmpty() || m.getCurso().toLowerCase().contains(cursoSelec);
            boolean coincideGrupo = grupoSelec.isEmpty() || m.getGrupo().toLowerCase().contains(grupoSelec);
            
            if (coincideEstado && coincideEstudiante && coincideGrado && coincideCurso && coincideGrupo) {
                // AÑADIDO: El DNI ahora va en su propia columna aislada
                model.addRow(new Object[]{
                    m.getId(), 
                    m.getDni(),
                    m.getEstudiante(), 
                    m.getGrado(), 
                    m.getCurso(),
                    m.getGrupo(),
                    m.getEstado(), 
                    "Gestionar"
                });
            }
        }
        
        view.getTabMatriculas().setAutoCreateRowSorter(true);
        view.getTabMatriculas().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        view.getTabMatriculas().setFillsViewportHeight(true);
    }

    // --- GESTIÓN DEL CICLO DE VIDA ---
    
    private void aceptarSolicitud() {
        String idMatricula = view.getIdMatriculaSeleccionada();
        int confirm = JOptionPane.showConfirmDialog(view.getFrame(), 
                "¿Estás seguro de que deseas aceptar la solicitud " + idMatricula + " y registrar su pago?", 
                "Aceptar Solicitud y Pago", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                
        if (confirm == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(view.getFrame(), "Solicitud " + idMatricula + " ACEPTADA correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            view.showListadoView();
            cargarMatriculasFiltradas();
        }
    }

    private void modificarMatricula() {
        String idMatricula = view.getIdMatriculaSeleccionada();
        JOptionPane.showMessageDialog(view.getFrame(), 
                "Se abrirá el formulario para editar datos de la matrícula: " + idMatricula, 
                "Modificar Matrícula", JOptionPane.INFORMATION_MESSAGE);
    }

    private void cancelarMatricula() {
        String idMatricula = view.getIdMatriculaSeleccionada();
        String[] motivos = {"IMPAGO", "FUERZA_MAYOR", "INCOMPATIBILIDAD"};
        
        String motivoSeleccionado = (String) JOptionPane.showInputDialog(
                view.getFrame(),
                "Seleccione el motivo de cancelación obligatorio para la matrícula " + idMatricula + ":",
                "Confirmar Cancelación",
                JOptionPane.WARNING_MESSAGE,
                null,
                motivos,
                motivos[0]);

        if (motivoSeleccionado != null) {
            JOptionPane.showMessageDialog(view.getFrame(), "Matrícula " + idMatricula + " cancelada correctamente.\nMotivo registrado: " + motivoSeleccionado, "Cancelación Completada", JOptionPane.INFORMATION_MESSAGE);
            view.showListadoView();
            cargarMatriculasFiltradas();
        }
    }

    // --- DATOS DE PRUEBA (MOCKS) ---
    private List<MatriculaDisplayDTO> obtenerMocksBD() {
        List<MatriculaDisplayDTO> datos = new ArrayList<>();
        datos.add(new MatriculaDisplayDTO("M001", "Alejandro El Harchi Argüelles", "71649234A", "Ing. Informática", "3º Curso", "A", "SOLICITADA"));
        datos.add(new MatriculaDisplayDTO("M002", "Lucía Gómez", "53819200B", "Ing. Informática", "1º Curso", "B", "PAGADA"));
        datos.add(new MatriculaDisplayDTO("M003", "Carlos Pérez", "12345678C", "Matemáticas", "2º Curso", "A", "ACTIVA"));
        datos.add(new MatriculaDisplayDTO("M004", "Sofía Martín", "87654321D", "Física", "4º Curso", "C", "CANCELADA"));
        return datos;
    }

    private void initView() {
        view.showListadoView();
        view.getFrame().setVisible(true);
    }
}