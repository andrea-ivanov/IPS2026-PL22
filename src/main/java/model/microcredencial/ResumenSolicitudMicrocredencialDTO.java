package model.microcredencial;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO que consolida los datos de la solicitud de matrícula en microcredenciales
 * antes de su confirmación y pago (HU 35987).
 */
public class ResumenSolicitudMicrocredencialDTO {
    private String alumnoId;
    private String nombreCompleto;
    private String dni;
    private String fechaSolicitud;
    private List<OfertaMicrocredencialDTO> microcredenciales;
    private int creditosTotales;
    private double importeTotal;
    private String metodoPago;

    public ResumenSolicitudMicrocredencialDTO() {
        this.microcredenciales = new ArrayList<>();
    }

    public ResumenSolicitudMicrocredencialDTO(String alumnoId, String nombreCompleto, String dni,
                                            String fechaSolicitud, List<OfertaMicrocredencialDTO> microcredenciales,
                                            int creditosTotales, double importeTotal, String metodoPago) {
        this.alumnoId = alumnoId;
        this.nombreCompleto = nombreCompleto;
        this.dni = dni;
        this.fechaSolicitud = fechaSolicitud;
        this.microcredenciales = microcredenciales != null ? microcredenciales : new ArrayList<>();
        this.creditosTotales = creditosTotales;
        this.importeTotal = importeTotal;
        this.metodoPago = metodoPago;
    }

    public String getAlumnoId() {
        return alumnoId;
    }

    public void setAlumnoId(String alumnoId) {
        this.alumnoId = alumnoId;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(String fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public List<OfertaMicrocredencialDTO> getMicrocredenciales() {
        return microcredenciales;
    }

    public void setMicrocredenciales(List<OfertaMicrocredencialDTO> microcredenciales) {
        this.microcredenciales = microcredenciales != null ? microcredenciales : new ArrayList<>();
    }

    public int getCreditosTotales() {
        return creditosTotales;
    }

    public void setCreditosTotales(int creditosTotales) {
        this.creditosTotales = creditosTotales;
    }

    public double getImporteTotal() {
        return importeTotal;
    }

    public void setImporteTotal(double importeTotal) {
        this.importeTotal = importeTotal;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }
}

