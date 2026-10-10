package model.alumno;

import java.util.List;

import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Modelo de negocio y caso de uso para la gestión y búsqueda de Alumnos.
 */
public class AlumnoModel {
	private Database db;

	public AlumnoModel() {
		this.db = new Database();
	}

	public AlumnoModel(Database db) {
		this.db = db;
	}

	/**
	 * Busca un Alumno por su identificador en formato String tras validar nulidad y formato.
	 *
	 * @param id Identificador en formato String
	 * @return Objeto Alumno correspondiente
	 * @throws ValidationException Si el id es nulo, vacío o no numérico
	 * @throws ResourceNotFoundException Si el alumno no existe en la BD
	 */
	public Alumno buscarPorId(String id) {
		validarFormatoId(id);
		long idNumerico = Long.parseLong(id.trim());
		return buscarPorId(idNumerico);
	}

	/**
	 * Busca un Alumno por su ID numérico.
	 *
	 * @param id Identificador numérico Long
	 * @return Objeto Alumno correspondiente
	 * @throws ValidationException Si el id es nulo
	 * @throws ResourceNotFoundException Si el alumno no existe en la BD
	 */
	public Alumno buscarPorId(Long id) {
		if (id == null) {
			throw new ValidationException("El ID de alumno no puede ser nulo");
		}

		String sql = "SELECT id_alumno, nombre, email FROM Alumno WHERE id_alumno = ?";
		List<Object[]> rows = db.executeQueryArray(sql, id);
		if (rows == null || rows.isEmpty()) {
			throw new ResourceNotFoundException("Alumno", id);
		}

		Object[] row = rows.get(0);
		Long idAlumno = ((Number) row[0]).longValue();
		String nombre = (String) row[1];
		String email = (String) row[2];

		return Alumno.builder()
				.id(idAlumno)
				.nombre(nombre)
				.email(email)
				.build();
	}

	private void validarFormatoId(String id) {
		if (id == null) {
			throw new ValidationException("El ID de alumno no puede ser nulo");
		}
		String trimmed = id.trim();
		if (trimmed.isEmpty()) {
			throw new ValidationException("El ID de alumno no puede estar vacío");
		}
		if (!trimmed.matches("^\\d+$")) {
			throw new ValidationException("El formato del ID de alumno es inválido: '" + id + "'. Debe ser un número entero positivo.");
		}
	}
}

