package repository;

import model.matriculacion.Matriculacion;
import model.matriculacion.MatriculacionModel;
import util.Database;

/**
 * Repositorio para el acceso a datos de Matriculación.
 * Permite integrarse fácilmente tanto con la arquitectura basada en repositorios
 * como con la arquitectura de modelos de GIIS.
 */
public class MatriculacionRepository {
	private final MatriculacionModel matriculacionModel;

	public MatriculacionRepository() {
		this.matriculacionModel = new MatriculacionModel();
	}

	public MatriculacionRepository(Database db) {
		this.matriculacionModel = new MatriculacionModel(db);
	}

	public Matriculacion findById(String id) {
		return matriculacionModel.buscarPorId(id);
	}

	public Matriculacion findById(Long id) {
		return matriculacionModel.buscarPorId(id);
	}
}

