package model.microcredencial;

/**
 * Una microcredencial PROPUESTA pendiente de revisión por el secretario (HU 35994).
 * Los nombres de los atributos coinciden con los alias de la query para que DbUtils los mapee.
 * Sin Lombok a propósito: getters y setters escritos a mano.
 */
public class PropuestaMicrocredencialDTO {
	private String microcredencialId;
	private String anioAcademicoId;
	private String nombre;
	private String profesorId;
	private String profesor;
	private String ramaConocimiento;
	private Double creditos;
	private Integer plazas;
	private String inicioImparticion;
	private String finImparticion;
	private String fechaPropuesta;

	public PropuestaMicrocredencialDTO() {
		super();
	}

	/** Periodo de impartición para mostrar: "inicio - fin" */
	public String getPeriodoImparticion() {
		return inicioImparticion + " - " + finImparticion;
	}

	public String getMicrocredencialId() {
		return microcredencialId;
	}

	public void setMicrocredencialId(String microcredencialId) {
		this.microcredencialId = microcredencialId;
	}

	public String getAnioAcademicoId() {
		return anioAcademicoId;
	}

	public void setAnioAcademicoId(String anioAcademicoId) {
		this.anioAcademicoId = anioAcademicoId;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getProfesorId() {
		return profesorId;
	}

	public void setProfesorId(String profesorId) {
		this.profesorId = profesorId;
	}

	public String getProfesor() {
		return profesor;
	}

	public void setProfesor(String profesor) {
		this.profesor = profesor;
	}

	public String getRamaConocimiento() {
		return ramaConocimiento;
	}

	public void setRamaConocimiento(String ramaConocimiento) {
		this.ramaConocimiento = ramaConocimiento;
	}

	public Double getCreditos() {
		return creditos;
	}

	public void setCreditos(Double creditos) {
		this.creditos = creditos;
	}

	public Integer getPlazas() {
		return plazas;
	}

	public void setPlazas(Integer plazas) {
		this.plazas = plazas;
	}

	public String getInicioImparticion() {
		return inicioImparticion;
	}

	public void setInicioImparticion(String inicioImparticion) {
		this.inicioImparticion = inicioImparticion;
	}

	public String getFinImparticion() {
		return finImparticion;
	}

	public void setFinImparticion(String finImparticion) {
		this.finImparticion = finImparticion;
	}

	public String getFechaPropuesta() {
		return fechaPropuesta;
	}

	public void setFechaPropuesta(String fechaPropuesta) {
		this.fechaPropuesta = fechaPropuesta;
	}
}
