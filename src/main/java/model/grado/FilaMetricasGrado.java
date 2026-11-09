package model.grado;

/**
 * Datos en bruto de un grado en un año académico, tal como salen de la BD, para calcular sus métricas
 * (HU 36411). El modelo los convierte en GradoAnioRevisorDisplayDTO.
 */
public record FilaMetricasGrado(String gradoId, String nombre, String facultad, String ramaConocimiento,
		String anioAcademicoId, int matriculados, int aprobados, double creditosPresentados,
		double creditosSuperados) {
}
