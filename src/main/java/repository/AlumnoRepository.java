package repository;

import model.alumno.Alumno;
import model.alumno.AlumnoModel;
import util.Database;

/**
 * Repositorio para el acceso a datos de Alumno.
 * Permite integrarse fácilmente tanto con la arquitectura basada en repositorios
 * como con la arquitectura de modelos de GIIS.
 */
public class AlumnoRepository {
	private final AlumnoModel alumnoModel;

	public AlumnoRepository() {
		this.alumnoModel = new AlumnoModel();
	}

	public AlumnoRepository(Database db) {
		this.alumnoModel = new AlumnoModel(db);
	}

	public Alumno findById(String id) {
		return alumnoModel.buscarPorId(id);
	}

	public Alumno findById(Long id) {
		return alumnoModel.buscarPorId(id);
	}
}

