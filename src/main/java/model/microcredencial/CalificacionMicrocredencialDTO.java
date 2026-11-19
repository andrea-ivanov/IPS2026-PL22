package model.microcredencial;

import lombok.Getter;
import lombok.Setter;

/**
 * Un alumno en el acta de una microcredencial, con su nota (HU 36459).
 * Los nombres de los atributos coinciden con los alias de la query para que DbUtils los mapee.
 */
@Getter @Setter
public class CalificacionMicrocredencialDTO {
	private String matriculaId;
	private String alumnoId;
	private String alumno;
	private String dni;
	/** INSCRITA con el acta abierta, FINALIZADA con el acta cerrada */
	private String estado;
	/** Nula: sin nota (no presentado si el acta está cerrada) */
	private Double nota;

	public CalificacionMicrocredencialDTO() {
		super();
	}

	/**
	 * Nota para mostrar en el acta: vacía si no tiene nota y el acta sigue abierta, NP si no tiene nota y el acta
	 * está cerrada, y el número sin ".0" si es entero (4 en lugar de 4.0).
	 */
	public String getNotaTexto() {
		if (nota == null) {
			return "FINALIZADA".equals(estado) ? "NP" : "";
		}
		return nota == Math.floor(nota) ? String.valueOf(nota.longValue()) : String.valueOf(nota);
	}
}
