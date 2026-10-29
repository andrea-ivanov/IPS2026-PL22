package model.matriculacion;

/**
 * DTO que representa una Microcredencial ofertada para el proceso de solicitud de matrícula (HU 35987).
 * Contiene información de rama, plazas, fechas y precio calculado (créditos * 20.0 €).
 */
public class OfertaMicrocredencialDTO {
    private String microcredencialId;
    private String nombre;
    private String descripcion;
    private String ramaConocimiento;
    private double creditos;
    private int plazasOfertadas;
    private int plazasOcupadas;
    private String fechaInicioMatriculacion;
    private String fechaFinMatriculacion;
    private String fechaInicio;
    private String fechaFin;
    private double precio;
    private boolean seleccionada;

    public OfertaMicrocredencialDTO() {
        this.seleccionada = false;
    }

    public OfertaMicrocredencialDTO(String microcredencialId, String nombre, String descripcion,
                                  String ramaConocimiento, double creditos, int plazasOfertadas,
                                  int plazasOcupadas, String fechaInicioMatriculacion,
                                  String fechaFinMatriculacion, String fechaInicio, String fechaFin) {
        this.microcredencialId = microcredencialId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.ramaConocimiento = ramaConocimiento;
        this.creditos = creditos;
        this.plazasOfertadas = plazasOfertadas;
        this.plazasOcupadas = plazasOcupadas;
        this.fechaInicioMatriculacion = fechaInicioMatriculacion;
        this.fechaFinMatriculacion = fechaFinMatriculacion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.precio = creditos * 20.0;
        this.seleccionada = false;
    }

    public String getMicrocredencialId() {
        return microcredencialId;
    }

    public void setMicrocredencialId(String microcredencialId) {
        this.microcredencialId = microcredencialId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getRamaConocimiento() {
        return ramaConocimiento;
    }

    public void setRamaConocimiento(String ramaConocimiento) {
        this.ramaConocimiento = ramaConocimiento;
    }

    public double getCreditos() {
        return creditos;
    }

    public void setCreditos(double creditos) {
        this.creditos = creditos;
        this.precio = creditos * 20.0;
    }

    public int getPlazasOfertadas() {
        return plazasOfertadas;
    }

    public void setPlazasOfertadas(int plazasOfertadas) {
        this.plazasOfertadas = plazasOfertadas;
    }

    public int getPlazasOcupadas() {
        return plazasOcupadas;
    }

    public void setPlazasOcupadas(int plazasOcupadas) {
        this.plazasOcupadas = plazasOcupadas;
    }

    public String getFechaInicioMatriculacion() {
        return fechaInicioMatriculacion;
    }

    public void setFechaInicioMatriculacion(String fechaInicioMatriculacion) {
        this.fechaInicioMatriculacion = fechaInicioMatriculacion;
    }

    public String getFechaFinMatriculacion() {
        return fechaFinMatriculacion;
    }

    public void setFechaFinMatriculacion(String fechaFinMatriculacion) {
        this.fechaFinMatriculacion = fechaFinMatriculacion;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(String fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public String getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(String fechaFin) {
        this.fechaFin = fechaFin;
    }

    public double getPrecio() {
        return precio > 0 ? precio : creditos * 20.0;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public boolean isSeleccionada() {
        return seleccionada;
    }

    public void setSeleccionada(boolean seleccionada) {
        this.seleccionada = seleccionada;
    }

    public int getPlazasLibres() {
        return Math.max(0, plazasOfertadas - plazasOcupadas);
    }

    public boolean isCompleta() {
        return plazasOfertadas > 0 && plazasOcupadas >= plazasOfertadas;
    }
}

