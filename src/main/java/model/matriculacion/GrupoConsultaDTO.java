package model.matriculacion;

/**
 * Grupo de un curso para la consulta de grupos de la Secretaría (HU 35226): grados en los que
 * se imparte el curso, profesores del grupo y plazas ocupadas en un año académico.
 *
 * Sin Lombok a propósito: el compilador de VS Code/Eclipse generaba clases corruptas.
 */
public class GrupoConsultaDTO {
	private String cursoId;
	private String curso;
	private String grados; // Nombres de los grados del curso separados por "; "
	private String grupoCodigo;
	private String tipo;
	private int capacidad;
	private int ocupadas; // Matrículas PAGADAS o ACTIVAS del año consultado en este grupo
	private String profesores;

	public GrupoConsultaDTO() {
		super();
	}

	public int getPlazasLibres() {
		return capacidad - ocupadas;
	}

	public String getCursoId() { return cursoId; }
	public void setCursoId(String cursoId) { this.cursoId = cursoId; }

	public String getCurso() { return curso; }
	public void setCurso(String curso) { this.curso = curso; }

	public String getGrados() { return grados; }
	public void setGrados(String grados) { this.grados = grados; }

	public String getGrupoCodigo() { return grupoCodigo; }
	public void setGrupoCodigo(String grupoCodigo) { this.grupoCodigo = grupoCodigo; }

	public String getTipo() { return tipo; }
	public void setTipo(String tipo) { this.tipo = tipo; }

	public int getCapacidad() { return capacidad; }
	public void setCapacidad(int capacidad) { this.capacidad = capacidad; }

	public int getOcupadas() { return ocupadas; }
	public void setOcupadas(int ocupadas) { this.ocupadas = ocupadas; }

	public String getProfesores() { return profesores; }
	public void setProfesores(String profesores) { this.profesores = profesores; }

	@Override
	public String toString() {
		return cursoId + " " + grupoCodigo + " (" + tipo + ", " + ocupadas + "/" + capacidad + ")";
	}
}
