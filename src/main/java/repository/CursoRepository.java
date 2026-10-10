package repository;

import model.curso.Curso;
import model.curso.CursoModel;
import util.Database;

/**
 * Repositorio para el acceso a datos de Curso / Asignatura.
 * Permite integrarse fácilmente tanto con la arquitectura basada en repositorios
 * como con la arquitectura de modelos de GIIS.
 */
public class CursoRepository {
	private final CursoModel cursoModel;

	public CursoRepository() {
		this.cursoModel = new CursoModel();
	}

	public CursoRepository(Database db) {
		this.cursoModel = new CursoModel(db);
	}

	public Curso findById(String id) {
		return cursoModel.buscarPorId(id);
	}

	public Curso findById(Long id) {
		return cursoModel.buscarPorId(id);
	}
}

