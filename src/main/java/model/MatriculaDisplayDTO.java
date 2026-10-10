package model;

public class MatriculaDisplayDTO {
    private String id;
    private String estudiante;
    private String dni;
    private String grado;
    private String curso;
    private String grupo;
    private String estado;

    public MatriculaDisplayDTO(String id, String estudiante, String dni, String grado, String curso, String grupo,
            String estado) {
        this.id = id;
        this.estudiante = estudiante;
        this.dni = dni;
        this.grado = grado;
        this.curso = curso;
        this.grupo = grupo;
        this.estado = estado;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(String estudiante) {
        this.estudiante = estudiante;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getGrado() {
        return grado;
    }

    public void setGrado(String grado) {
        this.grado = grado;
    }

    public String getCurso() {
        return curso;
    }

    public void setCurso(String curso) {
        this.curso = curso;
    }

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Object getIdMatricula() {
        return id;
    }
}