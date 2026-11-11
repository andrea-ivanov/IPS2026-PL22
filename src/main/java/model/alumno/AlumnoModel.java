package model.alumno;

import java.util.List;

import repository.AlumnoRepository;
import util.Database;

/**
 * Modelo del alumno: lista para el login y resolución de su identificador y su nombre.
 */
public class AlumnoModel {
	private final AlumnoRepository repository;

	public AlumnoModel(Database db) {
		this.repository = new AlumnoRepository(db);
	}

	/** Lista de alumnos para el combo del login con formato "UOxxxxx - Nombre Apellidos" */
	public List<Object[]> getListaAlumnos() {
		return repository.getListaAlumnos();
	}

	/**
	 * Resuelve el identificador oficial de un alumno en la base de datos (p. ej. "AL001")
	 * tanto si se le pasa con prefijo UO ("UO001"), AL ("AL001") o numérico ("1").
	 * Garantiza el acceso coherente a los datos de la base de datos.
	 *
	 * @param uoOrId Código UO o identificador del alumno
	 * @return Identificador exacto en la tabla Alumno
	 */
	public String resolverAlumnoId(String uoOrId) {
		if (uoOrId == null || uoOrId.trim().isEmpty()) {
			return "";
		}
		String clean = uoOrId.trim();
		if (clean.contains(" - ")) {
			clean = clean.split(" - ")[0].trim();
		}
		// 1. Coincidencia directa
		if (repository.existeAlumnoId(clean)) {
			return clean;
		}
		// 2. Si empieza por UO / uo, probar con AL
		if (clean.toUpperCase().startsWith("UO")) {
			String alt = "AL" + clean.substring(2);
			if (repository.existeAlumnoId(alt)) {
				return alt;
			}
		}
		// 3. Si empieza por AL / al, probar con UO
		if (clean.toUpperCase().startsWith("AL")) {
			String alt = "UO" + clean.substring(2);
			if (repository.existeAlumnoId(alt)) {
				return alt;
			}
		}
		return clean;
	}

	/**
	 * Obtiene el nombre de pila del alumno a partir de su UO o alumnoId o cadena "UO - Nombre".
	 *
	 * @param uoOrId Código UO, ID del alumno o etiqueta "UOxxxxx - Nombre"
	 * @return Nombre del estudiante, o cadena vacía si no se encuentra
	 */
	public String obtenerNombreAlumno(String uoOrId) {
		if (uoOrId != null && uoOrId.contains(" - ")) {
			String[] parts = uoOrId.split(" - ", 2);
			if (parts.length > 1 && !parts[1].trim().isEmpty()) {
				return parts[1].trim();
			}
		}
		String nombre = repository.findNombre(resolverAlumnoId(uoOrId));
		return nombre == null ? "" : nombre;
	}
}
