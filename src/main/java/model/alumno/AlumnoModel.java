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
		return buscarPorIdInterno(id.trim());
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
		return buscarPorIdInterno(id.toString());
	}

	private Alumno buscarPorIdInterno(String idStr) {
		List<Object[]> rows = null;
		try {
			String sql = "SELECT alumnoId, nombre, email FROM Alumno WHERE alumnoId = ?";
			rows = db.executeQueryArray(sql, idStr);
		} catch (Exception ignored) {
		}

		if (rows == null || rows.isEmpty()) {
			try {
				String sqlAlt = "SELECT id_alumno, nombre, email FROM Alumno WHERE id_alumno = ?";
				rows = db.executeQueryArray(sqlAlt, idStr);
			} catch (Exception ignored) {
			}
		}

		if (rows == null || rows.isEmpty()) {
			throw new ResourceNotFoundException("Alumno", idStr);
		}

		Object[] row = rows.get(0);
		Long idAlumno = parseNumber(row[0], 1L);
		String alumnoCode = row[0] != null ? row[0].toString() : idStr;
		String nombre = (String) row[1];
		String email = (String) row[2];

		return Alumno.builder()
				.id(idAlumno)
				.alumnoId(alumnoCode)
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
		if (!trimmed.matches("^\\d+$") && !trimmed.matches("^(AL|al|UO|uo)\\d+$")) {
			throw new ValidationException("El formato del ID de alumno es inválido: '" + id + "'. Debe ser un número entero positivo o código AL001.");
		}
	}

	private Long parseNumber(Object val, Long defaultVal) {
		if (val == null) return defaultVal;
		if (val instanceof Number) return ((Number) val).longValue();
		String s = val.toString().trim();
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			String digits = s.replaceAll("\\D+", "");
			return digits.isEmpty() ? defaultVal : Long.parseLong(digits.substring(0, Math.min(digits.length(), 6)));
		}
	}
}

