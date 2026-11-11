package model.grado;

/**
 * Datos en bruto de un grado en un año académico, tal como salen de la BD, para calcular sus métricas
 * (HU 36411). El modelo los convierte en GradoAnioRevisorDisplayDTO.
 * Un grado sin métricas (HU 36455) tiene anioAcademicoId nulo y los contadores a 0.
 */
public record FilaMetricasGrado(String gradoId, String nombre, String facultad, String ramaConocimiento,
		String estado, String anioAcademicoId, int matriculados, int aprobados, double creditosPresentados,
		double creditosSuperados) {
}
