package model.gestionmatricula;

/**
 * Filtros del listado de matrículas de Secretaría (HU 35226). Se aplican en la consulta SQL
 * para que con muchas matrículas solo se lea de la base de datos la página que se muestra.
 * Un filtro nulo o vacío no filtra.
 *
 * Sin Lombok a propósito (ver MatriculaSecretariaDTO).
 */
public class FiltroMatriculasSecretaria {
	private String estudiante; // Parte del nombre completo o del DNI, sin distinguir mayúsculas
	private String anioAcademicoId;
	private String grado; // Nombre del grado
	private String curso; // Nombre del curso
	private String grupo; // Código de grupo completo ("PL1" no coincide con "PL10")
	private String estado; // Estado del curso matriculado
	private SituacionSecretaria situacion = SituacionSecretaria.TODAS;
	private String fechaHoy; // Necesaria para la situación SIN_PAGAR (yyyy-MM-dd)

	public String getEstudiante() { return estudiante; }
	public FiltroMatriculasSecretaria setEstudiante(String estudiante) { this.estudiante = estudiante; return this; }
	public String getAnioAcademicoId() { return anioAcademicoId; }
	public FiltroMatriculasSecretaria setAnioAcademicoId(String anioAcademicoId) { this.anioAcademicoId = anioAcademicoId; return this; }
	public String getGrado() { return grado; }
	public FiltroMatriculasSecretaria setGrado(String grado) { this.grado = grado; return this; }
	public String getCurso() { return curso; }
	public FiltroMatriculasSecretaria setCurso(String curso) { this.curso = curso; return this; }
	public String getGrupo() { return grupo; }
	public FiltroMatriculasSecretaria setGrupo(String grupo) { this.grupo = grupo; return this; }
	public String getEstado() { return estado; }
	public FiltroMatriculasSecretaria setEstado(String estado) { this.estado = estado; return this; }
	public SituacionSecretaria getSituacion() { return situacion; }
	public FiltroMatriculasSecretaria setSituacion(SituacionSecretaria situacion) {
		this.situacion = situacion != null ? situacion : SituacionSecretaria.TODAS;
		return this;
	}
	public String getFechaHoy() { return fechaHoy; }
	public FiltroMatriculasSecretaria setFechaHoy(String fechaHoy) { this.fechaHoy = fechaHoy; return this; }
}
