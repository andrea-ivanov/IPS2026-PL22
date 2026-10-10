package repository;

import model.grado.Grado;
import model.grado.GradoModel;
import util.Database;

/**
 * Repositorio para el acceso a datos de Grado.
 * Permite integrarse fácilmente tanto con la arquitectura basada en repositorios
 * como con la arquitectura de modelos de GIIS.
 */
public class GradoRepository {
	private final GradoModel gradoModel;

	public GradoRepository() {
		this.gradoModel = new GradoModel();
	}

	public GradoRepository(Database db) {
		this.gradoModel = new GradoModel(db);
	}

	public Grado findById(String id) {
		return gradoModel.buscarPorId(id);
	}

	public Grado findById(Long id) {
		return gradoModel.buscarPorId(id);
	}
}
