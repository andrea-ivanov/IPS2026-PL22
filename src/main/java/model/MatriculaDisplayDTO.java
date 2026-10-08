package model;

public class MatriculaDisplayDTO {
    private String idMatricula;
    private String nombreGrado;
    private String estado;
    private String fecha;
    
    // Aquí iremos añadiendo más campos según pida la HU (notas, convocatorias, hitos...)

    public String getIdMatricula() { return idMatricula; }
    public void setIdMatricula(String idMatricula) { this.idMatricula = idMatricula; }
    
    public String getNombreGrado() { return nombreGrado; }
    public void setNombreGrado(String nombreGrado) { this.nombreGrado = nombreGrado; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
}

