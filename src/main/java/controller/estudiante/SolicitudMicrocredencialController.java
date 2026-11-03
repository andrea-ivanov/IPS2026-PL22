package controller.estudiante;

import model.matriculacion.MatriculacionModel;
import model.microcredencial.InscripcionMicrocredencialModel;
import model.microcredencial.OfertaMicrocredencialDTO;
import model.microcredencial.ResumenSolicitudMicrocredencialDTO;
import util.SwingUtil;
import view.estudiante.SolicitudMicrocredencialView;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la Solicitud de Matrícula en Microcredenciales (HU 35987).
 * Gestiona:
 * - Detección de ramas de conocimiento habilitadas del alumno.
 * - Filtrado y carga de la oferta de microcredenciales compatibles.
 * - Cálculo económico en tiempo real (20.0 €/ECTS) y exclusión de permanencia.
 * - Confirmación de la solicitud y elección de modalidad de pago.
 */
public class SolicitudMicrocredencialController {
    private final SolicitudMicrocredencialView view;
    private final MatriculacionModel model;
    private final InscripcionMicrocredencialModel inscripcionModel;
    private final String alumnoId;
    private final Runnable onVolverMenu;

    private List<String> ramasHabilitadas;
    private List<OfertaMicrocredencialDTO> todasMicrocredenciales;
    private boolean actualizandoTabla = false;
    private ResumenSolicitudMicrocredencialDTO resumenActual;

    public SolicitudMicrocredencialController(SolicitudMicrocredencialView view, MatriculacionModel model,
                                            InscripcionMicrocredencialModel inscripcionModel,
                                            String alumnoId, Runnable onVolverMenu) {
        this.view = view;
        this.model = model;
        this.inscripcionModel = inscripcionModel;
        this.alumnoId = alumnoId;
        this.onVolverMenu = onVolverMenu;
        this.ramasHabilitadas = new ArrayList<>();
        this.todasMicrocredenciales = new ArrayList<>();
    }

    public void initController() {
        // 1. Cargar ramas habilitadas y oferta
        cargarRamasYOferta();

        // 2. Filtro de rama en desplegable
        view.getCbFiltroRama().addActionListener(e -> filtrarPorRamaSeleccionada());

        // 3. Listener en tabla para cálculo en vivo
        view.getModelMicrocredenciales().addTableModelListener(this::onTablaMicrocredencialesModificada);

        // 4. Navegación Paso 1 -> Menú Principal
        view.getBtnVolverMenuDesdePaso1().addActionListener(e -> {
            if (onVolverMenu != null) {
                onVolverMenu.run();
            } else if (view.getFrame() != null) {
                view.getFrame().dispose();
            }
        });

        // 5. Paso 1 -> Paso 2 (Resumen)
        view.getBtnContinuarPaso2().addActionListener(e -> SwingUtil.exceptionWrapper(this::validarYContinuarAlResumen));

        // 6. Paso 2 -> Paso 1 (Modificar)
        view.getBtnModificarSeleccion().addActionListener(e -> view.mostrarPaso1());

        // 7. Paso 2 -> Paso 3 (Proceder al Pago)
        view.getBtnContinuarPaso3().addActionListener(e -> view.mostrarPaso3());

        // 8. Paso 3 -> Paso 2 (Volver al Resumen)
        view.getBtnVolverPaso2().addActionListener(e -> view.mostrarPaso2());

        // 9. Confirmación final de la matrícula
        view.getBtnConfirmarMatricula().addActionListener(e -> confirmarSolicitud());

        view.mostrarPaso1();
    }

    public void recargarOferta() {
        cargarRamasYOferta();
        view.mostrarPaso1();
    }

    private void cargarRamasYOferta() {
        // Ramas de los grados con matrícula no cancelada del alumno en el año actual
        String cleanId = model.resolverAlumnoId(alumnoId);
        ramasHabilitadas = inscripcionModel.getRamasHabilitadas(cleanId);

        if (ramasHabilitadas == null || ramasHabilitadas.isEmpty()) {
            // Caso alumno sin matrícula no cancelada este año: advertencia visible
            view.getLblRamasHabilitadas().setText("<html><font color='#C0392B'><b>Sin ramas habilitadas:</b> "
                    + "Para acceder a microcredenciales debe tener una matrícula no cancelada en un grado este año.</font></html>");
            view.getCbFiltroRama().removeAllItems();
            view.getCbFiltroRama().addItem("Ninguna rama disponible");
            todasMicrocredenciales = new ArrayList<>();
            actualizarTablaConLista(todasMicrocredenciales);
            return;
        }

        String ramasStr = String.join(", ", ramasHabilitadas);
        view.getLblRamasHabilitadas().setText("<html><b>Ramas habilitadas:</b> <font color='#0D6EFD'>" + ramasStr + "</font></html>");

        // Cargar combo de filtro
        view.getCbFiltroRama().removeAllItems();
        view.getCbFiltroRama().addItem("Todas mis ramas habilitadas");
        for (String rama : ramasHabilitadas) {
            view.getCbFiltroRama().addItem(rama);
        }

        // Consultar oferta compatible
        todasMicrocredenciales = inscripcionModel.getOferta(cleanId, LocalDate.now().toString());
        actualizarTablaConLista(todasMicrocredenciales);
    }

    private void filtrarPorRamaSeleccionada() {
        if (todasMicrocredenciales == null) return;
        String seleccion = (String) view.getCbFiltroRama().getSelectedItem();
        if (seleccion == null || seleccion.startsWith("Todas")) {
            actualizarTablaConLista(todasMicrocredenciales);
        } else {
            List<OfertaMicrocredencialDTO> filtradas = new ArrayList<>();
            for (OfertaMicrocredencialDTO mc : todasMicrocredenciales) {
                if (seleccion.equals(mc.getRamaConocimiento())) {
                    filtradas.add(mc);
                }
            }
            actualizarTablaConLista(filtradas);
        }
    }

    private void actualizarTablaConLista(List<OfertaMicrocredencialDTO> lista) {
        actualizandoTabla = true;
        DefaultTableModel tm = view.getModelMicrocredenciales();
        tm.setRowCount(0);

        if (lista != null) {
            for (OfertaMicrocredencialDTO mc : lista) {
                tm.addRow(new Object[] {
                        Boolean.FALSE,
                        mc.getMicrocredencialId(),
                        mc.getNombre(),
                        mc.getRamaConocimiento(),
                        mc.getCreditos(),
                        String.valueOf(mc.getPlazasLibres()),
                        mc.getFinMatriculacion(),
                        mc.getInicioImparticion(),
                        String.format("%.2f €", mc.getPrecio())
                });
            }
        }

        actualizandoTabla = false;
        view.actualizarTotales(0, 0.0);
    }

    private void onTablaMicrocredencialesModificada(TableModelEvent e) {
        if (actualizandoTabla || e.getType() != TableModelEvent.UPDATE || e.getColumn() != 0) {
            return;
        }
        recalcularTotales();
    }

    private void recalcularTotales() {
        DefaultTableModel tm = view.getModelMicrocredenciales();
        int totalCreditos = 0;
        double totalImporte = 0.0;

        for (int i = 0; i < tm.getRowCount(); i++) {
            Boolean checked = (Boolean) tm.getValueAt(i, 0);
            if (Boolean.TRUE.equals(checked)) {
                double cred = (Double) tm.getValueAt(i, 4);
                totalCreditos += (int) cred;
                totalImporte += cred * MatriculacionModel.PRECIO_CREDITO;
            }
        }
        view.actualizarTotales(totalCreditos, totalImporte);
    }

    private void validarYContinuarAlResumen() {
        List<OfertaMicrocredencialDTO> seleccionadas = view.getMicrocredencialesSeleccionadas();
        if (seleccionadas.isEmpty()) {
            JOptionPane.showMessageDialog(view.getFrame(),
                    "Debe seleccionar al menos una microcredencial de la oferta para continuar.",
                    "Sin microcredenciales seleccionadas", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int totalCreditos = 0;
        double totalImporte = 0.0;
        for (OfertaMicrocredencialDTO mc : seleccionadas) {
            totalCreditos += mc.getCreditos().intValue();
            totalImporte += mc.getPrecio();
        }

        String nombreCompleto = model.obtenerNombreAlumno(alumnoId);
        String cleanId = model.resolverAlumnoId(alumnoId);

        resumenActual = new ResumenSolicitudMicrocredencialDTO();
        resumenActual.setAlumnoId(cleanId);
        resumenActual.setNombreCompleto(nombreCompleto != null ? nombreCompleto : cleanId);
        resumenActual.setDni("-");
        resumenActual.setFechaSolicitud(LocalDate.now().toString());
        resumenActual.setMicrocredenciales(seleccionadas);
        resumenActual.setCreditosTotales(totalCreditos);
        resumenActual.setImporteTotal(totalImporte);

        view.cargarResumen(resumenActual);
        view.mostrarPaso2();
    }

    private void confirmarSolicitud() {
        if (resumenActual == null) {
            JOptionPane.showMessageDialog(view.getFrame(), "No hay una solicitud activa.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String metodoPago = view.getMetodoPagoSeleccionado();
        resumenActual.setMetodoPago(metodoPago);

        Object[] opciones = new Object[] { "Sí", "No" };
        int confirmacion = JOptionPane.showOptionDialog(view.getFrame(),
                "¿Desea formalizar la solicitud de matrícula en las microcredenciales seleccionadas?\n\n"
                        + "Total créditos: " + resumenActual.getCreditosTotales() + " ECTS\n"
                        + "Importe total: " + String.format("%.2f €", resumenActual.getImporteTotal()) + "\n"
                        + "Modalidad de pago: " + metodoPago + "\n\n"
                        + "Recuerde que el pago debe quedar completado antes del inicio de las clases.",
                "Confirmar Solicitud de Microcredencial",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]);

        if (confirmacion != 0) {
            return;
        }

        // Guarda la inscripción; si el modelo la rechaza, exceptionWrapper muestra el motivo y no hay mensaje de éxito
        List<String> ids = new ArrayList<>();
        for (OfertaMicrocredencialDTO mc : resumenActual.getMicrocredenciales()) {
            ids.add(mc.getMicrocredencialId());
        }
        boolean[] guardada = { false };
        SwingUtil.exceptionWrapper(() -> {
            inscripcionModel.inscribir(resumenActual.getAlumnoId(), ids, metodoPago, LocalDate.now().toString());
            guardada[0] = true;
        });
        if (!guardada[0]) {
            return;
        }

        String mensajeExito = "¡Solicitud registrada con éxito!\n\n"
                + "Modalidad de pago: " + metodoPago + "\n"
                + "Importe abonado/a abonar: " + String.format("%.2f €", resumenActual.getImporteTotal()) + "\n\n"
                + (metodoPago.equals("TARJETA")
                ? "La transacción ha sido validada. Su plaza en la microcredencial queda confirmada."
                : "Su plaza queda pendiente de validar el pago. Recuerde presentar el justificante de pago antes de la fecha de inicio del curso.");

        JOptionPane.showMessageDialog(view.getFrame(), mensajeExito,
                "Matrícula Solicitada", JOptionPane.INFORMATION_MESSAGE);

        // Regresar al menú principal
        if (onVolverMenu != null) {
            onVolverMenu.run();
        } else if (view.getFrame() != null) {
            view.getFrame().dispose();
        }
    }

    public List<String> getRamasHabilitadas() {
        return ramasHabilitadas;
    }

    public List<OfertaMicrocredencialDTO> getTodasMicrocredenciales() {
        return todasMicrocredenciales;
    }
}

