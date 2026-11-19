package model.fip;

/**
 * DTO para mostrar las Formaciones Internas del Personal (FIP) de un estudiante
 * en la pestaña "FIP" de su expediente ("Mis Matrículas", HU 36967).
 */
public class MatriculaFipDisplayDTO {
	private String fipId;
	private String nombre;
	private String fechaInicio;
	private String fechaFin;
	private String resultado;

	public MatriculaFipDisplayDTO() {
		super();
	}

	public MatriculaFipDisplayDTO(String fipId, String nombre, String fechaInicio, String fechaFin, String resultado) {
		this.fipId = fipId;
		this.nombre = nombre;
		this.fechaInicio = fechaInicio;
		this.fechaFin = fechaFin;
		this.resultado = resultado;
	}

	public String getFipId() {
		return fipId;
	}

	public void setFipId(String fipId) {
		this.fipId = fipId;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
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

	public String getResultado() {
		return resultado;
	}

	public void setResultado(String resultado) {
		this.resultado = resultado;
	}

	/**
	 * Resultado legible para la columna "Resultado": 'PENDIENTE', 'APTO' o 'NO APTO'.
	 */
	public String getResultadoTexto() {
		if (resultado == null || resultado.trim().isEmpty()) {
			return "PENDIENTE";
		}
		return resultado.trim().toUpperCase();
	}
}
