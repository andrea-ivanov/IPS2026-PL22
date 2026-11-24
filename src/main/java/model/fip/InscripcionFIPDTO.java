package model.fip;

/**
 * Datos de una inscripción en una FIP (HU 37046).
 * Sin Lombok a propósito: getters y setters escritos a mano.
 */
public class InscripcionFIPDTO {
	private String inscripcionId;
	private String fipId;
	private String usuarioId;
	private TipoUsuarioFIP tipoUsuario;
	private String fechaInscripcion;
	private ResultadoInscripcionFIP resultado;
	private double precio;

	public InscripcionFIPDTO() {
		super();
		this.resultado = ResultadoInscripcionFIP.PENDIENTE;
		this.precio = 0.0;
	}

	public InscripcionFIPDTO(String inscripcionId, String fipId, String usuarioId, TipoUsuarioFIP tipoUsuario,
			String fechaInscripcion) {
		this.inscripcionId = inscripcionId;
		this.fipId = fipId;
		this.usuarioId = usuarioId;
		this.tipoUsuario = tipoUsuario;
		this.fechaInscripcion = fechaInscripcion;
		this.resultado = ResultadoInscripcionFIP.PENDIENTE;
		this.precio = 0.0; // La matrícula en una FIP es siempre gratuita
	}

	public String getInscripcionId() {
		return inscripcionId;
	}

	public void setInscripcionId(String inscripcionId) {
		this.inscripcionId = inscripcionId;
	}

	public String getFipId() {
		return fipId;
	}

	public void setFipId(String fipId) {
		this.fipId = fipId;
	}

	public String getUsuarioId() {
		return usuarioId;
	}

	public void setUsuarioId(String usuarioId) {
		this.usuarioId = usuarioId;
	}

	public TipoUsuarioFIP getTipoUsuario() {
		return tipoUsuario;
	}

	public void setTipoUsuario(TipoUsuarioFIP tipoUsuario) {
		this.tipoUsuario = tipoUsuario;
	}

	public String getFechaInscripcion() {
		return fechaInscripcion;
	}

	public void setFechaInscripcion(String fechaInscripcion) {
		this.fechaInscripcion = fechaInscripcion;
	}

	public ResultadoInscripcionFIP getResultado() {
		return resultado;
	}

	public void setResultado(ResultadoInscripcionFIP resultado) {
		this.resultado = resultado;
	}

	public double getPrecio() {
		return precio;
	}

	public void setPrecio(double precio) {
		this.precio = precio;
	}
}
