package model.matriculacion;

/**
 * DTO para la visualización de calificaciones obtenidas por un alumno
 * en las distintas convocatorias presentadas (HU 35232 - Paso 4).
 */
public class CalificacionDisplayDTO {
    private String anioAcademicoId;
    private String cursoId;
    private String curso;
    private String tipoConvocatoria;
    private Double nota;

    public CalificacionDisplayDTO() {
        super();
    }

    public CalificacionDisplayDTO(String anioAcademicoId, String cursoId, String curso, String tipoConvocatoria, Double nota) {
        super();
        this.anioAcademicoId = anioAcademicoId;
        this.cursoId = cursoId;
        this.curso = curso;
        this.tipoConvocatoria = tipoConvocatoria;
        this.nota = nota;
    }

    /**
     * Devuelve la nota con formato decimal o "No presentado" si es nula.
     */
    public String getNotaTexto() {
        if (nota == null) {
            return "No presentado";
        }
        return String.format(java.util.Locale.US, "%.2f", nota);
    }

    /**
     * Determina el resultado académico de la convocatoria.
     */
    public String getResultado() {
        if (nota == null) {
            return "NO PRESENTADO";
        }
        return nota >= 5.0 ? "APROBADO" : "SUSPENSO";
    }

    /**
     * Convierte el enum técnico a formato legible para la interfaz gráfica.
     * Ej: ORDINARIA_ENERO -> Ordinaria Enero
     */
    public String getConvocatoriaLegible() {
        if (tipoConvocatoria == null || tipoConvocatoria.isEmpty()) {
            return "-";
        }
        String[] partes = tipoConvocatoria.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (p.length() > 0) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public String getAnioAcademicoId() {
        return anioAcademicoId;
    }

    public void setAnioAcademicoId(String anioAcademicoId) {
        this.anioAcademicoId = anioAcademicoId;
    }

    public String getCursoId() {
        return cursoId;
    }

    public void setCursoId(String cursoId) {
        this.cursoId = cursoId;
    }

    public String getCurso() {
        return curso;
    }

    public void setCurso(String curso) {
        this.curso = curso;
    }

    public String getTipoConvocatoria() {
        return tipoConvocatoria;
    }

    public void setTipoConvocatoria(String tipoConvocatoria) {
        this.tipoConvocatoria = tipoConvocatoria;
    }

    public Double getNota() {
        return nota;
    }

    public void setNota(Double nota) {
        this.nota = nota;
    }

    @Override
    public String toString() {
        return "CalificacionDisplayDTO [anioAcademicoId=" + anioAcademicoId + ", cursoId=" + cursoId + ", curso="
                + curso + ", tipoConvocatoria=" + tipoConvocatoria + ", nota=" + nota + "]";
    }
}
