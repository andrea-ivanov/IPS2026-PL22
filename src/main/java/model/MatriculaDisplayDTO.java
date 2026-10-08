package model;

public class MatriculaDisplayDTO {
    private String id;
    private String estudiante;
    private String grado;
    private String estado;

    public MatriculaDisplayDTO(String id, String estudiante, String grado, String estado) {
        this.id = id;
        this.estudiante = estudiante;
        this.grado = grado;
        this.estado = estado;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEstudiante() { return estudiante; }
    public void setEstudiante(String estudiante) { this.estudiante = estudiante; }

    public String getGrado() { return grado; }
    public void setGrado(String grado) { this.grado = grado; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}