package model.matriculacion;

/**
 * DTO que resume el estado de los hitos del régimen de permanencia del estudiante (HU 35232 - Paso 3).
 * Incluye:
 * - Mínimo anual: créditos superados en el año evaluado frente a 12 créditos.
 * - Mínimo al tercer año: créditos superados acumulados frente a 90 créditos.
 */
public class HitosPermanenciaDTO {
    private String alumnoId;
    private String gradoId;
    
    private String ultimoAnoEvaluadoId;
    private int creditosAnualesSuperados;
    private int creditosAnualesExigidos = 12;
    private boolean cumpleHitoAnual;
    
    private int creditosTotalesSuperados;
    private int creditosTotalesExigidos = 90;
    private int aniosEnGrado;
    private boolean cumpleHitoTercerAnio;

    public HitosPermanenciaDTO() {
        super();
    }

    public HitosPermanenciaDTO(String alumnoId, String gradoId, int creditosAnualesSuperados,
                               int creditosTotalesSuperados, int aniosEnGrado) {
        this(alumnoId, gradoId, null, creditosAnualesSuperados, creditosTotalesSuperados, aniosEnGrado);
    }

    public HitosPermanenciaDTO(String alumnoId, String gradoId, String ultimoAnoEvaluadoId,
                               int creditosAnualesSuperados, int creditosTotalesSuperados, int aniosEnGrado) {
        super();
        this.alumnoId = alumnoId;
        this.gradoId = gradoId;
        this.ultimoAnoEvaluadoId = ultimoAnoEvaluadoId;
        this.creditosAnualesSuperados = creditosAnualesSuperados;
        this.creditosAnualesExigidos = 12;
        this.cumpleHitoAnual = creditosAnualesSuperados >= 12;
        this.creditosTotalesSuperados = creditosTotalesSuperados;
        this.creditosTotalesExigidos = 90;
        this.aniosEnGrado = aniosEnGrado;
        this.cumpleHitoTercerAnio = aniosEnGrado < 3 || creditosTotalesSuperados >= 90;
    }

    public String getEstadoHitoAnualTexto() {
        if (ultimoAnoEvaluadoId == null || ultimoAnoEvaluadoId.isEmpty()) {
            return "1º AÑO EN CURSO";
        }
        return cumpleHitoAnual ? "SUPERADO" : "NO SUPERADO";
    }

    public String getEstadoHitoTercerAnioTexto() {
        if (aniosEnGrado < 3) {
            return "EN PROGRESO (" + aniosEnGrado + "º año de 3)";
        }
        return creditosTotalesSuperados >= creditosTotalesExigidos ? "SUPERADO" : "NO SUPERADO";
    }

    public String getAlumnoId() {
        return alumnoId;
    }

    public void setAlumnoId(String alumnoId) {
        this.alumnoId = alumnoId;
    }

    public String getGradoId() {
        return gradoId;
    }

    public void setGradoId(String gradoId) {
        this.gradoId = gradoId;
    }

    public String getUltimoAnoEvaluadoId() {
        return ultimoAnoEvaluadoId;
    }

    public void setUltimoAnoEvaluadoId(String ultimoAnoEvaluadoId) {
        this.ultimoAnoEvaluadoId = ultimoAnoEvaluadoId;
    }

    public int getCreditosAnualesSuperados() {
        return creditosAnualesSuperados;
    }

    public void setCreditosAnualesSuperados(int creditosAnualesSuperados) {
        this.creditosAnualesSuperados = creditosAnualesSuperados;
        this.cumpleHitoAnual = creditosAnualesSuperados >= this.creditosAnualesExigidos;
    }

    public int getCreditosAnualesExigidos() {
        return creditosAnualesExigidos;
    }

    public void setCreditosAnualesExigidos(int creditosAnualesExigidos) {
        this.creditosAnualesExigidos = creditosAnualesExigidos;
        this.cumpleHitoAnual = this.creditosAnualesSuperados >= creditosAnualesExigidos;
    }

    public boolean isCumpleHitoAnual() {
        return cumpleHitoAnual;
    }

    public void setCumpleHitoAnual(boolean cumpleHitoAnual) {
        this.cumpleHitoAnual = cumpleHitoAnual;
    }

    public int getCreditosTotalesSuperados() {
        return creditosTotalesSuperados;
    }

    public void setCreditosTotalesSuperados(int creditosTotalesSuperados) {
        this.creditosTotalesSuperados = creditosTotalesSuperados;
        this.cumpleHitoTercerAnio = this.aniosEnGrado < 3 || creditosTotalesSuperados >= this.creditosTotalesExigidos;
    }

    public int getCreditosTotalesExigidos() {
        return creditosTotalesExigidos;
    }

    public void setCreditosTotalesExigidos(int creditosTotalesExigidos) {
        this.creditosTotalesExigidos = creditosTotalesExigidos;
        this.cumpleHitoTercerAnio = this.aniosEnGrado < 3 || this.creditosTotalesSuperados >= creditosTotalesExigidos;
    }

    public int getAniosEnGrado() {
        return aniosEnGrado;
    }

    public void setAniosEnGrado(int aniosEnGrado) {
        this.aniosEnGrado = aniosEnGrado;
        this.cumpleHitoTercerAnio = aniosEnGrado < 3 || this.creditosTotalesSuperados >= this.creditosTotalesExigidos;
    }

    public boolean isCumpleHitoTercerAnio() {
        return cumpleHitoTercerAnio;
    }

    public void setCumpleHitoTercerAnio(boolean cumpleHitoTercerAnio) {
        this.cumpleHitoTercerAnio = cumpleHitoTercerAnio;
    }

    @Override
    public String toString() {
        return "HitosPermanenciaDTO [alumnoId=" + alumnoId + ", gradoId=" + gradoId + ", ultimoAnoEvaluadoId="
                + ultimoAnoEvaluadoId + ", creditosAnualesSuperados=" + creditosAnualesSuperados
                + ", creditosAnualesExigidos=" + creditosAnualesExigidos + ", cumpleHitoAnual=" + cumpleHitoAnual
                + ", creditosTotalesSuperados=" + creditosTotalesSuperados + ", creditosTotalesExigidos="
                + creditosTotalesExigidos + ", aniosEnGrado=" + aniosEnGrado + ", cumpleHitoTercerAnio="
                + cumpleHitoTercerAnio + "]";
    }
}
