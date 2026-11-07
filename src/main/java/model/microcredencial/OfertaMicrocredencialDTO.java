package model.microcredencial;

import lombok.*;

/**
 * Una microcredencial que el alumno puede solicitar (HU 35987).
 * Los nombres de los atributos coinciden con los alias de la query para que DbUtils los mapee.
 * plazasLibres solo descuenta las inscripciones pagadas y precio = créditos * PRECIO_CREDITO.
 * seleccionada no sale de la query: la marca la vista al elegirla.
 */
@Getter @Setter
public class OfertaMicrocredencialDTO {
	private String microcredencialId;
	private String anioAcademicoId;
	private String nombre;
	private String ramaConocimiento;
	private Double creditos;
	private Integer plazas;
	private Integer plazasLibres;
	private String inicioMatriculacion;
	private String finMatriculacion;
	private String inicioImparticion;
	private String finImparticion;
	private Double precio;
	private boolean seleccionada;

	public OfertaMicrocredencialDTO() {
		super();
	}
}
