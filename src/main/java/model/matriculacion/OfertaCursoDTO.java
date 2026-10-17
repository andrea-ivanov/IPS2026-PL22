package model.matriculacion;

/**
 * DTO que representa una Asignatura (Curso) ofertada dentro de un Grado
 * para el proceso de solicitud de matrícula.
 */
public class OfertaCursoDTO {
	private String cursoId;
	private String nombre;
	private int anio;
	private double creditos;
	private String itinerario;
	private double precio;
	private int convocatoriasConsumidas;
	private boolean seleccionado;

	public OfertaCursoDTO() {
		this.seleccionado = false;
	}

	public OfertaCursoDTO(String cursoId, String nombre, int anio, double creditos, String itinerario) {
		this.cursoId = cursoId;
		this.nombre = nombre;
		this.anio = anio;
		this.creditos = creditos;
		this.itinerario = itinerario;
		this.precio = creditos * 20.0; // Precio oficial: 20 euros por crédito
		this.convocatoriasConsumidas = 0;
		this.seleccionado = false;
	}

	public String getCursoId() {
		return cursoId;
	}

	public void setCursoId(String cursoId) {
		this.cursoId = cursoId;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public int getAnio() {
		return anio;
	}

	public void setAnio(int anio) {
		this.anio = anio;
	}

	public double getCreditos() {
		return creditos;
	}

	public void setCreditos(double creditos) {
		this.creditos = creditos;
		this.precio = creditos * 20.0;
	}

	public String getItinerario() {
		return itinerario;
	}

	public void setItinerario(String itinerario) {
		this.itinerario = itinerario;
	}

	public double getPrecio() {
		return precio;
	}

	public void setPrecio(double precio) {
		this.precio = precio;
	}

	public int getConvocatoriasConsumidas() {
		return convocatoriasConsumidas;
	}

	public void setConvocatoriasConsumidas(int convocatoriasConsumidas) {
		this.convocatoriasConsumidas = convocatoriasConsumidas;
	}

	public boolean isSeleccionado() {
		return seleccionado;
	}

	public void setSeleccionado(boolean seleccionado) {
		this.seleccionado = seleccionado;
	}

	/**
	 * Verifica si la asignatura puede ser seleccionada según el límite de convocatorias (máximo 6 con nota).
	 */
	public boolean isConvocatoriasDisponibles() {
		return convocatoriasConsumidas < 6;
	}

	@Override
	public String toString() {
		return (cursoId != null ? cursoId + " - " : "") + (nombre != null ? nombre : "") + " (" + creditos + " ECTS)";
	}
}

