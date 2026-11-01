package controller.estudiante;

import model.matriculacion.MatriculacionModel;
import model.matriculacion.OfertaCursoDTO;
import model.matriculacion.OfertaGradoDTO;
import model.matriculacion.OfertaMicrocredencialDTO;
import model.matriculacion.ResumenSolicitudDTO;
import model.matriculacion.ResumenSolicitudMicrocredencialDTO;
import util.exception.ValidationException;
import util.SwingUtil;
import view.estudiante.SolicitudMatriculaView;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador unificado para la Solicitud de Matrícula (HU 35235 y HU 35987).
 * Coordina la lógica de negocio, validaciones en tiempo real y confirmación
 * tanto para Asignaturas de Grados Oficiales como para Microcredenciales Universitarias.
 */
public class SolicitudMatriculaController {
    private final SolicitudMatriculaView view;
    private final MatriculacionModel model;
    private final String alumnoId;
    private final Runnable onVolverMenu;

    private boolean actualizandoTabla = false;
    private ResumenSolicitudDTO resumenGradoActual;
    private ResumenSolicitudMicrocredencialDTO resumenMicroActual;

    private List<String> ramasHabilitadas;
    private List<OfertaMicrocredencialDTO> todasMicrocredenciales;

    public SolicitudMatriculaController(SolicitudMatriculaView view, MatriculacionModel model, String alumnoId,
                                        Runnable onVolverMenu) {
        this.view = view;
        this.model = model != null ? model : new MatriculacionModel();
        this.alumnoId = alumnoId;
        this.onVolverMenu = onVolverMenu;
        this.ramasHabilitadas = new ArrayList<>();
        this.todasMicrocredenciales = new ArrayList<>();
    }

    public void initController() {
        // 1. Cargar oferta de grados y microcredenciales
        cargarGradosDisponibles();
        cargarRamasYOfertaMicrocredenciales();

        // 2. Conmutador de modo de matrícula (Grado / Microcredencial)
        view.getRbModoGrado().addActionListener(e -> seleccionarModoGrado());
        view.getRbModoMicrocredencial().addActionListener(e -> seleccionarModoMicrocredencial());

        // 3. Listeners específicos de Grado
        view.getCbGrados().addActionListener(e -> SwingUtil.exceptionWrapper(this::cargarAsignaturasDelGradoSeleccionado));
        view.getModelCursos().addTableModelListener(this::onTablaCursosModificada);

        // 4. Listeners específicos de Microcredenciales
        view.getCbFiltroRama().addActionListener(e -> filtrarMicrocredencialesPorRama());
        view.getModelMicrocredenciales().addTableModelListener(this::onTablaMicrocredencialesModificada);

        // 5. Navegación Paso 1 -> Menú Principal
        view.getBtnVolverMenuDesdePaso1().addActionListener(e -> {
            if (onVolverMenu != null) {
                onVolverMenu.run();
            } else if (view.getFrame() != null) {
                view.getFrame().dispose();
            }
        });

        // 6. Navegación Paso 1 -> Paso 2 (Continuar al Resumen con validaciones de negocio)
        view.getBtnContinuarPaso2().addActionListener(e -> SwingUtil.exceptionWrapper(this::validarYContinuarAlResumen));

        // 7. Navegación Paso 2 -> Paso 1 (Modificar Selección)
        view.getBtnModificarSeleccion().addActionListener(e -> view.mostrarPaso1());

        // 8. Navegación Paso 2 -> Paso 3 (Proceder al Pago)
        view.getBtnContinuarPaso3().addActionListener(e -> view.mostrarPaso3());

        // 9. Navegación Paso 3 -> Paso 2 (Volver al Resumen)
        view.getBtnVolverPaso2().addActionListener(e -> view.mostrarPaso2());

        // 10. Confirmación final de la matrícula (Grado o Microcredencial)
        view.getBtnConfirmarMatricula().addActionListener(e -> confirmarSolicitud());

        // Iniciar por defecto en Paso 1 y modo Grado
        view.mostrarModoGrado();
        view.mostrarPaso1();
    }

    public void seleccionarModoGrado() {
        view.mostrarModoGrado();
        desmarcarMicrocredenciales();
        recalcularTotales();
    }

    public void seleccionarModoMicrocredencial() {
        view.mostrarModoMicrocredencial();
        desmarcarCursos();
        recalcularTotales();
    }

    public void recargarOferta() {
        cargarGradosDisponibles();
        cargarRamasYOfertaMicrocredenciales();
        view.mostrarPaso1();
    }

    // =========================================================================
    // LÓGICA DE GRADO OFICIAL
    // =========================================================================
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

    private void cargarAsignaturasDelGradoSeleccionado() {
        OfertaGradoDTO grado = (OfertaGradoDTO) view.getCbGrados().getSelectedItem();
        if (grado == null) {
            actualizandoTabla = true;
            view.getModelCursos().setRowCount(0);
            actualizandoTabla = false;
            recalcularTotales();
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
        recalcularTotales();
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
                                "Ha consumido el límite máximo reglamentario de " + MatriculacionModel.MAX_CONVOCATORIAS
                                + " convocatorias evaluadas.",
                        "Límite de Convocatorias Agotado",
                        JOptionPane.WARNING_MESSAGE);
                recalcularTotales();
                return;
            }
        }

        recalcularTotales();
    }

    private void desmarcarCursos() {
        actualizandoTabla = true;
        DefaultTableModel tm = view.getModelCursos();
        for (int i = 0; i < tm.getRowCount(); i++) {
            tm.setValueAt(Boolean.FALSE, i, 0);
        }
        actualizandoTabla = false;
    }

    // =========================================================================
    // LÓGICA DE MICROCREDENCIALES
    // =========================================================================
    private void cargarRamasYOfertaMicrocredenciales() {
        ramasHabilitadas = model.getRamasHabilitadasPorEstudiante(alumnoId);

        if (ramasHabilitadas == null || ramasHabilitadas.isEmpty()) {
            view.getLblRamasHabilitadas().setText("<html><font color='#C0392B'><b>Sin ramas habilitadas:</b> "
                    + "Para acceder a microcredenciales debe estar matriculado o haber finalizado un grado universitario.</font></html>");
            view.getCbFiltroRama().removeAllItems();
            view.getCbFiltroRama().addItem("Ninguna rama disponible");
            todasMicrocredenciales = new ArrayList<>();
            actualizarTablaMicrocredenciales(todasMicrocredenciales);
            return;
        }

        String ramasStr = String.join(", ", ramasHabilitadas);
        view.getLblRamasHabilitadas().setText("<html><b>Ramas habilitadas:</b> <font color='#0D6EFD'>" + ramasStr + "</font></html>");

        view.getCbFiltroRama().removeAllItems();
        view.getCbFiltroRama().addItem("Todas mis ramas habilitadas");
        for (String rama : ramasHabilitadas) {
            view.getCbFiltroRama().addItem(rama);
        }

        todasMicrocredenciales = model.getOfertaMicrocredenciales(ramasHabilitadas);
        actualizarTablaMicrocredenciales(todasMicrocredenciales);
    }

    private void filtrarMicrocredencialesPorRama() {
        if (todasMicrocredenciales == null) return;
        String seleccion = (String) view.getCbFiltroRama().getSelectedItem();
        if (seleccion == null || seleccion.startsWith("Todas")) {
            actualizarTablaMicrocredenciales(todasMicrocredenciales);
        } else {
            List<OfertaMicrocredencialDTO> filtradas = new ArrayList<>();
            for (OfertaMicrocredencialDTO mc : todasMicrocredenciales) {
                if (seleccion.equalsIgnoreCase(mc.getRamaConocimiento())) {
                    filtradas.add(mc);
                }
            }
            actualizarTablaMicrocredenciales(filtradas);
        }
    }

    private void actualizarTablaMicrocredenciales(List<OfertaMicrocredencialDTO> lista) {
        actualizandoTabla = true;
        DefaultTableModel tm = view.getModelMicrocredenciales();
        tm.setRowCount(0);

        if (lista != null) {
            for (OfertaMicrocredencialDTO mc : lista) {
                String plazasTexto = mc.getPlazasOcupadas() + " / " + mc.getPlazasOfertadas();
                double precio = mc.getPrecio() > 0 ? mc.getPrecio() : mc.getCreditos() * MatriculacionModel.PRECIO_CREDITO;
                tm.addRow(new Object[] {
                        Boolean.FALSE,
                        mc.getMicrocredencialId(),
                        mc.getNombre(),
                        mc.getRamaConocimiento(),
                        mc.getCreditos(),
                        plazasTexto,
                        mc.getFechaFinMatriculacion() != null ? mc.getFechaFinMatriculacion() : "-",
                        mc.getFechaInicio() != null ? mc.getFechaInicio() : "-",
                        String.format("%.2f €", precio)
                });
            }
        }

        actualizandoTabla = false;
        recalcularTotales();
    }

    private void onTablaMicrocredencialesModificada(TableModelEvent e) {
        if (actualizandoTabla || e.getType() != TableModelEvent.UPDATE || e.getColumn() != 0) {
            return;
        }
        recalcularTotales();
    }

    private void desmarcarMicrocredenciales() {
        actualizandoTabla = true;
        DefaultTableModel tm = view.getModelMicrocredenciales();
        for (int i = 0; i < tm.getRowCount(); i++) {
            tm.setValueAt(Boolean.FALSE, i, 0);
        }
        actualizandoTabla = false;
    }

    // =========================================================================
    // CÁLCULO DE TOTALES UNIFICADO
    // =========================================================================
    private void recalcularTotales() {
        int totalCreditos = 0;
        double totalImporte = 0.0;

        if (view.isModoGrado()) {
            DefaultTableModel tm = view.getModelCursos();
            for (int i = 0; i < tm.getRowCount(); i++) {
                Boolean checked = (Boolean) tm.getValueAt(i, 0);
                if (Boolean.TRUE.equals(checked)) {
                    double cred = (Double) tm.getValueAt(i, 5);
                    totalCreditos += (int) cred;
                    totalImporte += cred * MatriculacionModel.PRECIO_CREDITO;
                }
            }
        } else {
            DefaultTableModel tm = view.getModelMicrocredenciales();
            for (int i = 0; i < tm.getRowCount(); i++) {
                Boolean checked = (Boolean) tm.getValueAt(i, 0);
                if (Boolean.TRUE.equals(checked)) {
                    double cred = (Double) tm.getValueAt(i, 4);
                    totalCreditos += (int) cred;
                    totalImporte += cred * MatriculacionModel.PRECIO_CREDITO;
                }
            }
        }

        view.actualizarTotales(totalCreditos, totalImporte);
    }

    // =========================================================================
    // VALIDACIONES Y NAVEGACIÓN AL RESUMEN
    // =========================================================================
    private void validarYContinuarAlResumen() {
        if (view.isModoGrado()) {
            validarGradoYContinuarAlResumen();
        } else {
            validarMicrocredencialYContinuarAlResumen();
        }
    }

    private void validarGradoYContinuarAlResumen() {
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

        // Ejecutar validaciones integrales de negocio
        try {
            model.validarSolicitudMatricula(alumnoId, grado.getGradoId(), anioAcademicoId, cursoIds);
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(view.getFrame(),
                    ex.getMessage(),
                    "Requisitos de Matrícula No Cumplidos",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        resumenGradoActual = model.obtenerDatosEstudianteParaSolicitud(alumnoId, grado.getGradoId());
        resumenGradoActual.setAsignaturas(asignaturas);
        int totalCreds = 0;
        double totalImporte = 0.0;
        for (OfertaCursoDTO c : asignaturas) {
            totalCreds += (int) c.getCreditos();
            totalImporte += c.getPrecio();
        }
        resumenGradoActual.setCreditosTotales(totalCreds);
        resumenGradoActual.setImporteTotal(totalImporte);

        view.cargarResumenGrado(resumenGradoActual);
        view.mostrarPaso2();
    }

    private void validarMicrocredencialYContinuarAlResumen() {
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
            totalCreditos += (int) mc.getCreditos();
            totalImporte += mc.getPrecio();
        }

        String nombreCompleto = model.obtenerNombreAlumno(alumnoId);
        String cleanId = model.resolverAlumnoId(alumnoId);

        resumenMicroActual = new ResumenSolicitudMicrocredencialDTO();
        resumenMicroActual.setAlumnoId(cleanId);
        resumenMicroActual.setNombreCompleto(nombreCompleto != null ? nombreCompleto : cleanId);
        resumenMicroActual.setDni("-");
        resumenMicroActual.setFechaSolicitud(LocalDate.now().toString());
        resumenMicroActual.setMicrocredenciales(seleccionadas);
        resumenMicroActual.setCreditosTotales(totalCreditos);
        resumenMicroActual.setImporteTotal(totalImporte);

        view.cargarResumenMicrocredencial(resumenMicroActual);
        view.mostrarPaso2();
    }

    // =========================================================================
    // CONFIRMACIÓN DE SOLICITUD Y PAGO
    // =========================================================================
    private void confirmarSolicitud() {
        if (view.isModoGrado()) {
            confirmarSolicitudGrado();
        } else {
            confirmarSolicitudMicrocredencial();
        }
    }

    private void confirmarSolicitudGrado() {
        if (resumenGradoActual == null) {
            JOptionPane.showMessageDialog(view.getFrame(),
                    "No se dispone de una solicitud consolidada para confirmar.",
                    "Error de Solicitud", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String metodoPago = view.getMetodoPagoSeleccionado();
        resumenGradoActual.setMetodoPago(metodoPago);

        Object[] opciones = new Object[] { "Sí", "No" };
        int confirmacion = JOptionPane.showOptionDialog(view.getFrame(),
                "¿Está seguro de que desea confirmar la solicitud de matrícula por un importe de " +
                        String.format("%.2f €", resumenGradoActual.getImporteTotal()) +
                        " mediante " + metodoPago + "?",
                "Confirmar Solicitud de Matrícula",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]);

        if (confirmacion != 0) {
            return;
        }

        try {
            String matriculaId = model.registrarSolicitudMatricula(resumenGradoActual);

            String mensajeExito;
            if ("TARJETA".equals(metodoPago)) {
                mensajeExito = "¡Matrícula confirmada y abonada con éxito!\n\n" +
                        "• Código de Matrícula: " + matriculaId + "\n" +
                        "• Estado de matrícula: SOLICITADA\n" +
                        "• Asignaturas: PAGADA (pago validado por pasarela telemática)\n" +
                        "• Importe total abonado: " + String.format("%.2f €", resumenGradoActual.getImporteTotal());
            } else {
                mensajeExito = "¡Solicitud de matrícula registrada con éxito!\n\n" +
                        "• Código de Matrícula asignado: " + matriculaId + "\n" +
                        "• Estado de matrícula: SOLICITADA\n" +
                        "• Modalidad de pago: " + metodoPago + "\n" +
                        "• Importe pendiente: " + String.format("%.2f €", resumenGradoActual.getImporteTotal()) + "\n\n" +
                        "IMPORTANTE: Recuerde presentar el justificante de pago en secretaría\n" +
                        "para que sus asignaturas sean validadas definitivamente.";
            }

            JOptionPane.showMessageDialog(view.getFrame(),
                    mensajeExito,
                    "Matrícula Confirmada con Éxito",
                    JOptionPane.INFORMATION_MESSAGE);

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

    private void confirmarSolicitudMicrocredencial() {
        if (resumenMicroActual == null) {
            JOptionPane.showMessageDialog(view.getFrame(), "No hay una solicitud activa de microcredencial.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String metodoPago = view.getMetodoPagoSeleccionado();
        resumenMicroActual.setMetodoPago(metodoPago);

        Object[] opciones = new Object[] { "Sí", "No" };
        int confirmacion = JOptionPane.showOptionDialog(view.getFrame(),
                "¿Desea formalizar la solicitud de matrícula en las microcredenciales seleccionadas?\n\n"
                        + "Total créditos: " + resumenMicroActual.getCreditosTotales() + " ECTS\n"
                        + "Importe total: " + String.format("%.2f €", resumenMicroActual.getImporteTotal()) + "\n"
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

        String mensajeExito = "¡Solicitud registrada con éxito!\n\n"
                + "Modalidad de pago: " + metodoPago + "\n"
                + "Importe abonado/a abonar: " + String.format("%.2f €", resumenMicroActual.getImporteTotal()) + "\n\n"
                + (metodoPago.equals("TARJETA")
                ? "La transacción ha sido validada. Su plaza en la microcredencial queda confirmada."
                : "Recuerde presentar el justificante de pago antes de la fecha de inicio del curso.");

        JOptionPane.showMessageDialog(view.getFrame(), mensajeExito,
                "Matrícula Solicitada", JOptionPane.INFORMATION_MESSAGE);

        if (onVolverMenu != null) {
            onVolverMenu.run();
        } else if (view.getFrame() != null) {
            view.getFrame().dispose();
        }
    }

    public SolicitudMatriculaView getView() {
        return view;
    }

    public List<String> getRamasHabilitadas() {
        return ramasHabilitadas;
    }

    public List<OfertaMicrocredencialDTO> getTodasMicrocredenciales() {
        return todasMicrocredenciales;
    }
}
