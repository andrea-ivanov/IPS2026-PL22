package model.microcredencial;

import lombok.*;

/**
 * Una microcredencial de un profesor, para el listado "Mis microcredenciales" (HU 35988).
 * Los nombres de los atributos coinciden con los alias de la query para que DbUtils los mapee.
 */
@Getter @Setter
public class MicrocredencialDisplayDTO {
	private String microcredencialId;
	private String anioAcademicoId;
	private String nombre;
	private String ramaConocimiento;
	private Double creditos;
	private Integer plazas;
	private String inicioMatriculacion;
	private String finMatriculacion;
	private String inicioImparticion;
	private String finImparticion;
	private String fechaPropuesta;
	private String estado;

	public MicrocredencialDisplayDTO() {
		super();
	}
}
