package model.fip;

/**
 * Un asistente inscrito en una FIP, para la tabla de inscritos de Secretaría (HU 37046).
 * Los nombres de los atributos coinciden con los alias de la query para que DbUtils los mapee.
 * Sin Lombok a propósito: getters y setters escritos a mano.
 */
public class InscritoFipDTO {
	private String inscripcionId;
	private String usuarioId;
	private String usuario;
	private String tipo;
	private String fechaInscripcion;
	private String resultado;

	public InscritoFipDTO() {
		super();
	}

	public String getInscripcionId() {
		return inscripcionId;
	}

	public void setInscripcionId(String inscripcionId) {
		this.inscripcionId = inscripcionId;
	}

	public String getUsuarioId() {
		return usuarioId;
	}

	public void setUsuarioId(String usuarioId) {
		this.usuarioId = usuarioId;
	}

	public String getUsuario() {
		return usuario;
	}

	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getFechaInscripcion() {
		return fechaInscripcion;
	}

	public void setFechaInscripcion(String fechaInscripcion) {
		this.fechaInscripcion = fechaInscripcion;
	}

	public String getResultado() {
		return resultado;
	}

	public void setResultado(String resultado) {
		this.resultado = resultado;
	}
}
