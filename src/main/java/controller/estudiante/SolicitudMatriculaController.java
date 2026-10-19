package controller.estudiante;

import model.matriculacion.MatriculacionModel;
import model.matriculacion.OfertaCursoDTO;
import model.matriculacion.OfertaGradoDTO;
import model.matriculacion.ResumenSolicitudDTO;
import util.SwingUtil;
import util.exception.ValidationException;
import view.estudiante.SolicitudMatriculaView;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador para la Solicitud de Matrícula (HU 35235 / Fase 4).
 * Coordina la lógica de negocio, validaciones en tiempo real y persistencia transaccional.
 */
public class SolicitudMatriculaController {
    private final SolicitudMatriculaView view;
    private final MatriculacionModel model;
    private final String alumnoId;
    private final Runnable onVolverMenu;

    private boolean actualizandoTabla = false;
    private ResumenSolicitudDTO resumenActual;

    public SolicitudMatriculaController(SolicitudMatriculaView view, MatriculacionModel model, String alumnoId, Runnable onVolverMenu) {
        this.view = view;
        this.model = model != null ? model : new MatriculacionModel();
        this.alumnoId = alumnoId;
        this.onVolverMenu = onVolverMenu;
    }

    public void initController() {
        // 1. Cargar oferta de grados disponibles
        cargarGradosDisponibles();

        // 2. Escuchar cambio de grado para recargar dinámicamente las asignaturas
        view.getCbGrados().addActionListener(e -> cargarAsignaturasDelGradoSeleccionado());

        // 3. Listener en la tabla para validación de convocatorias y cálculo en tiempo real
        view.getModelCursos().addTableModelListener(this::onTablaCursosModificada);

        // 4. Navegación Paso 1 -> Menú Principal
        view.getBtnVolverMenuDesdePaso1().addActionListener(e -> {
            if (onVolverMenu != null) {
                onVolverMenu.run();
            }
        });

        // 5. Navegación Paso 1 -> Paso 2 (Continuar al Resumen con validaciones de negocio)
        view.getBtnContinuarPaso2().addActionListener(e -> validarYContinuarAlResumen());

        // 6. Navegación Paso 2 -> Paso 1 (Modificar Asignaturas)
        view.getBtnModificarAsignaturas().addActionListener(e -> view.mostrarPaso1());

        // 7. Navegación Paso 2 -> Paso 3 (Proceder al Pago)
        view.getBtnContinuarPaso3().addActionListener(e -> view.mostrarPaso3());

        // 8. Navegación Paso 3 -> Paso 2 (Volver al Resumen)
        view.getBtnVolverPaso2().addActionListener(e -> view.mostrarPaso2());

        // 9. Confirmación final de la matrícula (Persistencia transaccional)
        view.getBtnConfirmarMatricula().addActionListener(e -> confirmarSolicitudMatricula());

        // Iniciar en Paso 1
        view.mostrarPaso1();
    }

    private void cargarGradosDisponibles() {
        view.getCbGrados().removeAllItems();
        List<OfertaGradoDTO> grados = model.getOfertaGradosDisponibles();
        if (grados != null && !grados.isEmpty()) {
            for (OfertaGradoDTO g : grados) {
                view.getCbGrados().addItem(g);
            }
            cargarAsignaturasDelGradoSeleccionado();
        } else {
            view.getLblInfoEstudiante().setText("No hay grados en estado VIGENTE ofertados en este momento.");
        }
    }

    public void recargarOferta() {
        cargarGradosDisponibles();
        view.mostrarPaso1();
    }

    private void cargarAsignaturasDelGradoSeleccionado() {
        OfertaGradoDTO grado = (OfertaGradoDTO) view.getCbGrados().getSelectedItem();
        if (grado == null) {
            actualizandoTabla = true;
            view.getModelCursos().setRowCount(0);
            actualizandoTabla = false;
            view.actualizarTotales(0, 0.0);
            return;
        }

        actualizandoTabla = true;
        DefaultTableModel tm = view.getModelCursos();
        tm.setRowCount(0);

        List<OfertaCursoDTO> cursos = model.getOfertaCursosPorGrado(grado.getGradoId(), alumnoId);
        if (cursos != null) {
            for (OfertaCursoDTO c : cursos) {
                double precio = c.getPrecio() > 0 ? c.getPrecio() : c.getCreditos() * MatriculacionModel.PRECIO_CREDITO;
                tm.addRow(new Object[] {
                        Boolean.FALSE,
                        c.getCursoId(),
                        c.getNombre(),
                        c.getAnio(),
                        c.getItinerario() != null ? c.getItinerario() : "General",
                        c.getCreditos(),
                        c.getConvocatoriasConsumidas(),
                        String.format("%.2f €", precio)
                });
            }
        }

        actualizandoTabla = false;
        view.actualizarTotales(0, 0.0);
        view.getLblInfoEstudiante().setText("Se han cargado " + (cursos != null ? cursos.size() : 0) +
                " asignaturas activas para " + grado.getNombre() + ".");
    }

    private void onTablaCursosModificada(TableModelEvent e) {
        if (actualizandoTabla || e.getType() != TableModelEvent.UPDATE || e.getColumn() != 0) {
            return;
        }

        int row = e.getFirstRow();
        DefaultTableModel tm = view.getModelCursos();
        Boolean seleccionado = (Boolean) tm.getValueAt(row, 0);

        if (Boolean.TRUE.equals(seleccionado)) {
            String cursoId = (String) tm.getValueAt(row, 1);
            String nombre = (String) tm.getValueAt(row, 2);
            int convs = (Integer) tm.getValueAt(row, 6);

            // Validar límite de convocatorias (Regla 6.3: MAX_CONVOCATORIAS = 6)
            if (convs >= MatriculacionModel.MAX_CONVOCATORIAS) {
                actualizandoTabla = true;
                tm.setValueAt(Boolean.FALSE, row, 0);
                actualizandoTabla = false;
                JOptionPane.showMessageDialog(view.getFrame(),
                        "No es posible seleccionar la asignatura '" + nombre + "' (" + cursoId + "):\n" +
                                "Ha consumido el límite máximo reglamentario de " + MatriculacionModel.MAX_CONVOCATORIAS + " convocatorias evaluadas.",
                        "Límite de Convocatorias Agotado",
                        JOptionPane.WARNING_MESSAGE);
                recalcularTotales();
                return;
            }
        }

        recalcularTotales();
    }

    private void recalcularTotales() {
        DefaultTableModel tm = view.getModelCursos();
        int totalCreditos = 0;
        double totalImporte = 0.0;

        for (int i = 0; i < tm.getRowCount(); i++) {
            Boolean checked = (Boolean) tm.getValueAt(i, 0);
            if (Boolean.TRUE.equals(checked)) {
                double cred = (Double) tm.getValueAt(i, 5);
                totalCreditos += (int) cred;
                totalImporte += cred * MatriculacionModel.PRECIO_CREDITO;
            }
        }
        view.actualizarTotales(totalCreditos, totalImporte);
    }

    private void validarYContinuarAlResumen() {
        OfertaGradoDTO grado = (OfertaGradoDTO) view.getCbGrados().getSelectedItem();
        if (grado == null) {
            JOptionPane.showMessageDialog(view.getFrame(),
                    "Debe seleccionar un grado oficial para matricularse.",
                    "Grado no seleccionado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<OfertaCursoDTO> asignaturas = view.getAsignaturasSeleccionadas();
        if (asignaturas.isEmpty()) {
            JOptionPane.showMessageDialog(view.getFrame(),
                    "Debe marcar al menos una asignatura para poder continuar con la solicitud.",
                    "Sin asignaturas seleccionadas", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String anioAcademicoId = model.getAnioAcademicoActual();
        List<String> cursoIds = new ArrayList<>();
        for (OfertaCursoDTO c : asignaturas) {
            cursoIds.add(c.getCursoId());
        }

        // Ejecutar validaciones integrales de negocio antes de permitir avanzar al resumen
        try {
            model.validarSolicitudMatricula(alumnoId, grado.getGradoId(), anioAcademicoId, cursoIds);
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(view.getFrame(),
                    ex.getMessage(),
                    "Requisitos de Matrícula No Cumplidos",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Construir DTO de resumen
        resumenActual = model.obtenerDatosEstudianteParaSolicitud(alumnoId, grado.getGradoId());
        resumenActual.setAsignaturas(asignaturas);
        int totalCreds = 0;
        double totalImporte = 0.0;
        for (OfertaCursoDTO c : asignaturas) {
            totalCreds += (int) c.getCreditos();
            totalImporte += c.getPrecio();
        }
        resumenActual.setCreditosTotales(totalCreds);
        resumenActual.setImporteTotal(totalImporte);

        view.cargarResumen(resumenActual);
        view.mostrarPaso2();
    }

    private void confirmarSolicitudMatricula() {
        if (resumenActual == null) {
            JOptionPane.showMessageDialog(view.getFrame(),
                    "No se dispone de una solicitud consolidada para confirmar.",
                    "Error de Solicitud", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String metodoPago = view.getMetodoPagoSeleccionado();
        resumenActual.setMetodoPago(metodoPago);

        int confirmacion = JOptionPane.showConfirmDialog(view.getFrame(),
                "¿Está seguro de que desea confirmar la solicitud de matrícula por un importe de " +
                        String.format("%.2f €", resumenActual.getImporteTotal()) +
                        " mediante " + metodoPago + "?",
                "Confirmar Solicitud de Matrícula",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            // Persistencia transaccional en base de datos
            String matriculaId = model.registrarSolicitudMatricula(resumenActual);

            String mensajeExito;
            if ("TARJETA".equalsIgnoreCase(metodoPago)) {
                mensajeExito = "¡Matrícula registrada y pagada con éxito!\n\n" +
                        "• Código de Matrícula asignado: " + matriculaId + "\n" +
                        "• Estado de matrícula: SOLICITADA\n" +
                        "• Estado de asignaturas: PAGADA (Pago validado telemáticamente)\n" +
                        "• Importe abonado: " + String.format("%.2f €", resumenActual.getImporteTotal());
            } else {
                mensajeExito = "¡Solicitud de matrícula registrada con éxito!\n\n" +
                        "• Código de Matrícula asignado: " + matriculaId + "\n" +
                        "• Estado de matrícula: SOLICITADA\n" +
                        "• Modalidad de pago: " + metodoPago + "\n" +
                        "• Importe pendiente: " + String.format("%.2f €", resumenActual.getImporteTotal()) + "\n\n" +
                        "IMPORTANTE: Recuerde presentar el justificante de pago en secretaría\n" +
                        "para que sus asignaturas sean validadas definitivamente.";
            }

            JOptionPane.showMessageDialog(view.getFrame(),
                    mensajeExito,
                    "Matrícula Confirmada con Éxito",
                    JOptionPane.INFORMATION_MESSAGE);

            // Regresar al menú principal tras el éxito
            if (onVolverMenu != null) {
                onVolverMenu.run();
            }

        } catch (ValidationException vex) {
            JOptionPane.showMessageDialog(view.getFrame(),
                    vex.getMessage(),
                    "Error en la Validación de Matrícula",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(view.getFrame(),
                    "Ocurrió un error inesperado al registrar la matrícula:\n" + ex.getMessage(),
                    "Error Transaccional",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public SolicitudMatriculaView getView() {
        return view;
    }
}

