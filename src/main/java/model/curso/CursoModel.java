package model.curso;

import java.util.List;

import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Modelo de negocio y caso de uso para la gestión y búsqueda de Cursos / Asignaturas.
 */
public class CursoModel {
	private Database db;

	public CursoModel() {
		this.db = new Database();
	}

	public CursoModel(Database db) {
		this.db = db;
	}

	/**
	 * Busca un Curso por su ID en formato String tras validar formato y nulidad.
	 *
	 * @param id Identificador en formato String
	 * @return Objeto Curso correspondiente
	 * @throws ValidationException Si el id es nulo, vacío o con formato no numérico
	 * @throws ResourceNotFoundException Si no existe en la base de datos
	 */
	public Curso buscarPorId(String id) {
		validarFormatoId(id);
		return buscarPorIdInterno(id.trim());
	}

	/**
	 * Busca un Curso por su ID numérico.
	 *
	 * @param id Identificador numérico Long
	 * @return Objeto Curso correspondiente
	 * @throws ValidationException Si el id es nulo
	 * @throws ResourceNotFoundException Si no existe en la base de datos
	 */
	public Curso buscarPorId(Long id) {
		if (id == null) {
			throw new ValidationException("El ID de curso no puede ser nulo");
		}
		return buscarPorIdInterno(id.toString());
	}

	private Curso buscarPorIdInterno(String idStr) {
		List<Object[]> rows = null;
		try {
			String sql = "SELECT c.cursoId, c.nombre, gc.gradoId, c.coordinadorId, c.creditos FROM Curso c LEFT JOIN GradoCurso gc ON c.cursoId = gc.cursoId WHERE c.cursoId = ? LIMIT 1";
			rows = db.executeQueryArray(sql, idStr);
		} catch (Exception ignored) {
		}

		if (rows == null || rows.isEmpty()) {
			try {
				String sqlAlt = "SELECT id_curso, nombre, id_grado, id_coordinador FROM Curso WHERE id_curso = ?";
				rows = db.executeQueryArray(sqlAlt, idStr);
			} catch (Exception ignored) {
			}
		}

		if (rows == null || rows.isEmpty()) {
			throw new ResourceNotFoundException("Curso", idStr);
		}

		Object[] row = rows.get(0);
		Long idCurso = parseNumber(row[0], 1L);
		String cursoCode = row[0] != null ? row[0].toString() : idStr;
		String nombre = (String) row[1];
		Long idGrado = row[2] != null ? parseNumber(row[2], null) : null;
		Long idCoordinador = row[3] != null ? parseNumber(row[3], null) : null;
		int creditos = row.length > 4 && row[4] != null ? ((Number) row[4]).intValue() : 6;

		return Curso.builder()
				.id(idCurso)
				.cursoId(cursoCode)
				.nombre(nombre)
				.idGrado(idGrado)
				.idCoordinador(idCoordinador)
				.creditos(creditos)
				.activo(true)
				.build();
	}

	private void validarFormatoId(String id) {
		if (id == null) {
			throw new ValidationException("El ID de curso no puede ser nulo");
		}
		String trimmed = id.trim();
		if (trimmed.isEmpty()) {
			throw new ValidationException("El ID de curso no puede estar vacío");
		}
		if (!trimmed.matches("^\\d+$") && !trimmed.matches("^[Cc]\\d{3,4}$")) {
			throw new ValidationException("El formato del ID de curso es inválido: '" + id + "'. Debe ser un número entero positivo o código C001.");
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

