package model.matriculacion;

/**
 * DTO que representa un Grado disponible en la oferta académica para matricularse.
 * Compatible con Apache Commons DbUtils y componentes de interfaz Swing (JComboBox).
 */
public class OfertaGradoDTO {
	private String gradoId;
	private String nombre;
	private String facultad;
	private String ramaConocimiento;
	private String estado;

	public OfertaGradoDTO() {
	}

	public OfertaGradoDTO(String gradoId, String nombre, String facultad, String ramaConocimiento, String estado) {
		this.gradoId = gradoId;
		this.nombre = nombre;
		this.facultad = facultad;
		this.ramaConocimiento = ramaConocimiento;
		this.estado = estado;
	}

	public String getGradoId() {
		return gradoId;
	}

	public void setGradoId(String gradoId) {
		this.gradoId = gradoId;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getFacultad() {
		return facultad;
	}

	public void setFacultad(String facultad) {
		this.facultad = facultad;
	}

	public String getRamaConocimiento() {
		return ramaConocimiento;
	}

	public void setRamaConocimiento(String ramaConocimiento) {
		this.ramaConocimiento = ramaConocimiento;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	@Override
	public String toString() {
		return (gradoId != null ? gradoId + " - " : "") + (nombre != null ? nombre : "");
	}
}

