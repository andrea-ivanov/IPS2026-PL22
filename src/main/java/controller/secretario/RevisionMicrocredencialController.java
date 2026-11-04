package controller.secretario;

import java.time.LocalDate;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import model.microcredencial.PropuestaMicrocredencialDTO;
import model.microcredencial.RevisionMicrocredencialModel;
import util.Database;
import util.SwingUtil;
import util.exception.ValidationException;
import view.secretario.SecretarioView;

/**
 * Controlador de la pestaña "Propuestas de Microcredenciales" de Secretaría (HU 35994):
 * listado de las microcredenciales PROPUESTAS y aceptación.
 * Va aparte de SecretarioController para no mezclarse con la gestión de matrículas.
 */
public class RevisionMicrocredencialController {
    private final SecretarioView view;
    private final RevisionMicrocredencialModel model;

    public RevisionMicrocredencialController(SecretarioView view) {
        this(view, new Database());
    }

    public RevisionMicrocredencialController(SecretarioView view, Database db) {
        this.view = view;
        this.model = new RevisionMicrocredencialModel(db);
    }

    public void initController() {
        view.getBtnAceptarPropuesta().addActionListener(e -> SwingUtil.exceptionWrapper(this::aceptarPropuesta));
        view.getBtnRecargarPropuestas().addActionListener(e -> SwingUtil.exceptionWrapper(this::cargarPropuestasPendientes));
        SwingUtil.exceptionWrapper(this::cargarPropuestasPendientes);
    }

    /** Rellena la tabla con las microcredenciales PROPUESTAS, la más antigua primero. */
    private void cargarPropuestasPendientes() {
        List<PropuestaMicrocredencialDTO> propuestas = model.getPropuestasPendientes();
        DefaultTableModel tabla = (DefaultTableModel) view.getTabPropuestas().getModel();
        tabla.setRowCount(0);
        for (PropuestaMicrocredencialDTO p : propuestas) {
            tabla.addRow(new Object[] {
                    p.getMicrocredencialId(),
                    p.getNombre(),
                    p.getProfesor(),
                    p.getCreditos(),
                    p.getPeriodoImparticion(),
                    p.getRamaConocimiento(),
                    p.getPlazas(),
                    p.getFechaPropuesta()
            });
        }
        view.getLblResultadosPropuestas().setText(propuestas.isEmpty()
                ? "No hay propuestas pendientes de revisión"
                : propuestas.size() + " propuestas pendientes de revisión");
    }

    private void aceptarPropuesta() {
        JTable tabla = view.getTabPropuestas();
        int row = tabla.getSelectedRow();
        if (row < 0) {
            throw new ValidationException("Seleccione una propuesta pendiente de la tabla para aceptarla");
        }
        int modelRow = tabla.convertRowIndexToModel(row);
        String microcredencialId = (String) tabla.getModel().getValueAt(modelRow, 0);
        String nombre = (String) tabla.getModel().getValueAt(modelRow, 1);
        String profesor = (String) tabla.getModel().getValueAt(modelRow, 2);

        int confirm = JOptionPane.showConfirmDialog(view.getFrame(),
                "¿Desea aceptar la propuesta \"" + nombre + "\" (Profesor: " + profesor + ")?\n\n"
                        + "El estado cambiará a ACEPTADA y se registrará oficialmente como microcredencial disponible en el sistema.",
                "Aceptar Propuesta", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        model.aceptarPropuesta(microcredencialId, LocalDate.now().toString());
        JOptionPane.showMessageDialog(view.getFrame(),
                "La microcredencial \"" + nombre + "\" (" + microcredencialId + ") ha sido aceptada.\n"
                        + "Queda registrada oficialmente como microcredencial disponible en el sistema.",
                "Propuesta Aceptada", JOptionPane.INFORMATION_MESSAGE);
        cargarPropuestasPendientes();
    }
}
