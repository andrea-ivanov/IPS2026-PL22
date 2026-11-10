package repository;

import java.time.LocalDate;
import java.util.List;

import model.grado.GradoAnioRevisorDisplayDTO;
import model.grado.MetricasGradoModel;
import util.Database;

/**
 * Repositorio de las métricas de rendimiento de los grados para el revisor externo (HU 36411).
 */
public class MetricasGradoRepository {
	private final MetricasGradoModel metricasGradoModel;

	public MetricasGradoRepository() {
		this(new Database());
	}

	public MetricasGradoRepository(Database db) {
		this.metricasGradoModel = new MetricasGradoModel(db);
	}

	public List<GradoAnioRevisorDisplayDTO> getMetricasGrados(String rama, String nombreGrado, String anioAcademicoId) {
		return metricasGradoModel.getMetricasGrados(rama, nombreGrado, anioAcademicoId, LocalDate.now().toString());
	}

	public String getAnioAcademicoPorDefecto() {
		return metricasGradoModel.getAnioAcademicoPorDefecto();
	}
}
