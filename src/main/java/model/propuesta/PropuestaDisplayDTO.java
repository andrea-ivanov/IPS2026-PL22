package model.propuesta;

import lombok.*;

/**
 * Una propuesta de asignatura de un profesor, para el listado "Mis propuestas" (HU 35243)
 * y la revisión de propuestas por parte de Secretaría.
 * Los nombres de los atributos coinciden con los alias de la query para que DbUtils los mapee.
 */
@Getter @Setter
public class PropuestaDisplayDTO {
	private String propuestaId;
	private String gradoId;
	private String grado;
	private String profesorId;
	private String profesorNombre;
	private String nombre;
	private Integer anio;
	private Double creditos;
	private String descripcion;
	private String fechaPropuesta;
	private String estado;

	public PropuestaDisplayDTO() {
		super();
	}

	public PropuestaDisplayDTO(String propuestaId, String gradoId, String grado, String nombre, Integer anio,
			Double creditos, String descripcion, String fechaPropuesta, String estado) {
		super();
		this.propuestaId = propuestaId;
		this.gradoId = gradoId;
		this.grado = grado;
		this.nombre = nombre;
		this.anio = anio;
		this.creditos = creditos;
		this.descripcion = descripcion;
		this.fechaPropuesta = fechaPropuesta;
		this.estado = estado;
	}

	public PropuestaDisplayDTO(String propuestaId, String gradoId, String grado, String profesorId,
			String profesorNombre, String nombre, Integer anio, Double creditos, String descripcion,
			String fechaPropuesta, String estado) {
		super();
		this.propuestaId = propuestaId;
		this.gradoId = gradoId;
		this.grado = grado;
		this.profesorId = profesorId;
		this.profesorNombre = profesorNombre;
		this.nombre = nombre;
		this.anio = anio;
		this.creditos = creditos;
		this.descripcion = descripcion;
		this.fechaPropuesta = fechaPropuesta;
		this.estado = estado;
	}
}
