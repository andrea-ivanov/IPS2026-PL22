package model.grado;

import lombok.*;

/**
 * Datos de un grado para la pantalla de revisión del revisor externo (HU 36455):
 * su estado, la decisión que se muestra y la fecha del último dictamen.
 */
@Getter
@Setter
@ToString
public class GradoRevisionDisplayDTO {
	private String idGrado;
	private String nombre;
	private String facultad;
	private String rama;
	private String estado;
	private String decision;            // FAVORABLE / DESFAVORABLE; null si no hay nada que mostrar
	private String fechaUltimaRevision; // ISO; null si el grado no tiene ningún dictamen

	public GradoRevisionDisplayDTO() {
		super();
	}
}
