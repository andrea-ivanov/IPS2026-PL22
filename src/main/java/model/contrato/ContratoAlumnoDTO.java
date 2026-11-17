package model.contrato;

/**
 * Un contrato de un alumno, para el historial que ve el secretario (HU 37045).
 * Los nombres de los atributos coinciden con los alias de la query para que DbUtils los mapee.
 * Sin Lombok a propósito: getters y setters escritos a mano.
 */
public class ContratoAlumnoDTO {
	private String contratoId;
	private String alumnoId;
	private String descripcion;
	private String fechaInicio;
	private String fechaFin;

	public ContratoAlumnoDTO() {
		super();
	}

	public String getContratoId() {
		return contratoId;
	}

	public void setContratoId(String contratoId) {
		this.contratoId = contratoId;
	}

	public String getAlumnoId() {
		return alumnoId;
	}

	public void setAlumnoId(String alumnoId) {
		this.alumnoId = alumnoId;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
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
}
