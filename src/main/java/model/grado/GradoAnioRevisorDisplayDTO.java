package model.grado;

import lombok.*;

/**
 * Métricas de rendimiento de un grado en un año académico, para la vista del revisor externo (HU 36411).
 * Las tasas son porcentajes con 1 decimal; son nulas cuando no se pueden calcular.
 */
@Getter
@Setter
@Builder
@ToString
public class GradoAnioRevisorDisplayDTO {
	private String idGrado;
	private String nombreGrado;
	private String rama;
	private String idAnioAcademico;
	private Double tasaExito;    // null si no hay ningún presentado
	private Double tasaAbandono; // null si todavía no se puede medir
	private int aprobados;
	private int matriculados;

	public GradoAnioRevisorDisplayDTO() {
		super();
	}

	public GradoAnioRevisorDisplayDTO(String idGrado, String nombreGrado, String rama, String idAnioAcademico,
			Double tasaExito, Double tasaAbandono, int aprobados, int matriculados) {
		super();
		this.idGrado = idGrado;
		this.nombreGrado = nombreGrado;
		this.rama = rama;
		this.idAnioAcademico = idAnioAcademico;
		this.tasaExito = tasaExito;
		this.tasaAbandono = tasaAbandono;
		this.aprobados = aprobados;
		this.matriculados = matriculados;
	}
}
