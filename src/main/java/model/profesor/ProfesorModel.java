package model.profesor;

import java.util.List;

import model.asignaciondocente.AsignacionDocenteDisplayDTO;
import repository.ProfesorRepository;
import util.Database;

/**
 * Modelo de la parte del profesor: lista de profesores y los grupos que imparte cada uno.
 */
public class ProfesorModel {
	private final ProfesorRepository repository;

	public ProfesorModel(Database db) {
		this.repository = new ProfesorRepository(db);
	}

	/** Profesores para los combos, ordenados por id */
	public List<ProfesorDTO> getProfesores() {
		return repository.getProfesores();
	}

	/** Grupos que imparte el profesor, con su rol (COORDINADOR o DOCENTE) */
	public List<AsignacionDocenteDisplayDTO> getAsignacionesDocentes(String profesorId) {
		return repository.getAsignacionesDocentes(profesorId);
	}
}
