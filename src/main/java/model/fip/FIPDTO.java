package model.fip;

/**
 * Datos de una Formación para la Integración Profesional (FIP) (HU 37046).
 * Sin Lombok a propósito: getters y setters escritos a mano.
 */
public class FIPDTO {
	private String fipId;
	private String nombre;
	private String fechaInicio;
	private String fechaFin;
	private String profesorResponsableId;
	private int plazasMaximas;
	private int plazasOcupadas;

	public FIPDTO() {
		super();
	}

	public FIPDTO(String fipId, String nombre, String fechaInicio, String fechaFin, String profesorResponsableId,
			int plazasMaximas, int plazasOcupadas) {
		this.fipId = fipId;
		this.nombre = nombre;
		this.fechaInicio = fechaInicio;
		this.fechaFin = fechaFin;
		this.profesorResponsableId = profesorResponsableId;
		this.plazasMaximas = plazasMaximas;
		this.plazasOcupadas = plazasOcupadas;
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

	public String getProfesorResponsableId() {
		return profesorResponsableId;
	}

	public void setProfesorResponsableId(String profesorResponsableId) {
		this.profesorResponsableId = profesorResponsableId;
	}

	public int getPlazasMaximas() {
		return plazasMaximas;
	}

	public void setPlazasMaximas(int plazasMaximas) {
		this.plazasMaximas = plazasMaximas;
	}

	public int getPlazasOcupadas() {
		return plazasOcupadas;
	}

	public void setPlazasOcupadas(int plazasOcupadas) {
		this.plazasOcupadas = plazasOcupadas;
	}

	public int getPlazasDisponibles() {
		return Math.max(0, plazasMaximas - plazasOcupadas);
	}

	public boolean hayPlazasDisponibles() {
		return plazasMaximas <= 0 || plazasOcupadas < plazasMaximas;
	}

	/** Texto del desplegable de FIP de Secretaría */
	@Override
	public String toString() {
		return fipId + " - " + nombre + " (empieza el " + fechaInicio + ")";
	}
}
