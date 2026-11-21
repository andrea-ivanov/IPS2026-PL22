package controller.secretario;

import util.FechaSistema;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.DefaultTableModel;

import model.microcredencial.PropuestaMicrocredencialDTO;
import model.microcredencial.RevisionMicrocredencialModel;
import util.Database;
import util.SwingUtil;
import util.exception.ValidationException;
import view.secretario.SecretarioView;

/**
 * Controlador de la pestaña "Propuestas de Microcredenciales" de Secretaría (HU 35994):
 * listado de las microcredenciales PROPUESTAS, aceptación y rechazo con motivo.
 * Va aparte de SecretarioController para no mezclarse con la gestión de matrículas.
 */
public class RevisionMicrocredencialController {
    private static final int COLUMNA_ID = 0;
    private static final int COLUMNA_NOMBRE = 1;
    private static final int COLUMNA_PROFESOR = 2;

    private final SecretarioView view;
    private final RevisionMicrocredencialModel model;

    public RevisionMicrocredencialController(SecretarioView view, Database db) {
        this.view = view;
        this.model = new RevisionMicrocredencialModel(db);
    }

    public void initController() {
        view.getBtnAceptarPropuesta().addActionListener(e -> SwingUtil.exceptionWrapper(this::aceptarPropuesta));
        view.getBtnRechazarPropuesta().addActionListener(e -> SwingUtil.exceptionWrapper(this::rechazarPropuesta));
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
        int fila = filaSeleccionada("aceptarla");
        String microcredencialId = valorColumna(fila, COLUMNA_ID);
        String nombre = valorColumna(fila, COLUMNA_NOMBRE);
        String profesor = valorColumna(fila, COLUMNA_PROFESOR);

        int confirm = JOptionPane.showConfirmDialog(view.getFrame(),
                "¿Desea aceptar la propuesta \"" + nombre + "\" (Profesor: " + profesor + ")?\n\n"
                        + "El estado cambiará a ACEPTADA y se registrará oficialmente como microcredencial disponible en el sistema.",
                "Aceptar Propuesta", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        model.aceptarPropuesta(microcredencialId, hoy());
        JOptionPane.showMessageDialog(view.getFrame(),
                "La microcredencial \"" + nombre + "\" (" + microcredencialId + ") ha sido aceptada.\n"
                        + "Queda registrada oficialmente como microcredencial disponible en el sistema.",
                "Propuesta Aceptada", JOptionPane.INFORMATION_MESSAGE);
        cargarPropuestasPendientes();
    }

    /** Pide el motivo, que valida el modelo. Rechazar no se puede deshacer: el profesor tendría que proponerla de nuevo. */
    private void rechazarPropuesta() {
        int fila = filaSeleccionada("rechazarla");
        String microcredencialId = valorColumna(fila, COLUMNA_ID);
        String nombre = valorColumna(fila, COLUMNA_NOMBRE);
        String profesor = valorColumna(fila, COLUMNA_PROFESOR);

        JTextArea txtMotivo = new JTextArea(5, 40);
        txtMotivo.setLineWrap(true);
        txtMotivo.setWrapStyleWord(true);
        JScrollPane scrollMotivo = new JScrollPane(txtMotivo);
        scrollMotivo.setPreferredSize(new Dimension(460, 110));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("<html>Propuesta \"" + nombre + "\" (Profesor: " + profesor + ").<br>"
                + "Quedará RECHAZADA y no se podrá reutilizar: el profesor tendría que crear otra desde cero.<br><br>"
                + "Motivo del rechazo (obligatorio, máximo " + RevisionMicrocredencialModel.MAX_MOTIVO_RECHAZO
                + " caracteres):</html>"), BorderLayout.NORTH);
        panel.add(scrollMotivo, BorderLayout.CENTER);

        // Sin esto el foco queda en el botón Aceptar y lo que se escribe no entra en el cuadro
        txtMotivo.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                txtMotivo.requestFocusInWindow();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                // nada que hacer
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
                // nada que hacer
            }
        });

        // El diálogo se repite hasta que el motivo sea válido o se cancele, sin perder lo escrito
        boolean motivoValido = false;
        while (!motivoValido) {
            int opcion = JOptionPane.showConfirmDialog(view.getFrame(), panel, "Rechazar Propuesta",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                model.validarMotivoRechazo(txtMotivo.getText());
                motivoValido = true;
            } catch (ValidationException e) {
                JOptionPane.showMessageDialog(view.getFrame(), e.getMessage(), "Falta el motivo del rechazo",
                        JOptionPane.WARNING_MESSAGE);
            }
        }

        model.rechazarPropuesta(microcredencialId, txtMotivo.getText(), hoy());
        JOptionPane.showMessageDialog(view.getFrame(),
                "La propuesta \"" + nombre + "\" (" + microcredencialId + ") ha sido rechazada.\n"
                        + "El profesor deberá crear una nueva desde cero si desea volver a intentarlo.",
                "Propuesta Rechazada", JOptionPane.INFORMATION_MESSAGE);
        cargarPropuestasPendientes();
    }

    /** Fila seleccionada en el modelo de la tabla; la acción se usa en el aviso si no hay ninguna. */
    private int filaSeleccionada(String accion) {
        JTable tabla = view.getTabPropuestas();
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            throw new ValidationException("Seleccione una propuesta pendiente de la tabla para " + accion);
        }
        return tabla.convertRowIndexToModel(fila);
    }

    private String valorColumna(int filaModelo, int columna) {
        return (String) view.getTabPropuestas().getModel().getValueAt(filaModelo, columna);
    }

    private String hoy() {
        return FechaSistema.hoyStr();
    }
}
