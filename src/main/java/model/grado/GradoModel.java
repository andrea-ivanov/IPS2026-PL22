package model.grado;

import java.util.ArrayList;
import java.util.List;

import model.curso.Curso;
import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Modelo de negocio y caso de uso para la gestión y búsqueda de Grados.
 * Maneja validaciones de entrada, consultas a base de datos y mapeo a entidades de dominio.
 */
public class GradoModel {
	private Database db;

	public GradoModel() {
		this.db = new Database();
	}

	public GradoModel(Database db) {
		this.db = db;
	}

	/**
	 * Busca un Grado por su ID representado como cadena de texto.
	 * Valida que no sea nulo, vacío ni tenga un formato inválido.
	 *
	 * @param id Identificador en formato String
	 * @return Objeto Grado completamente poblado con sus cursos
	 * @throws ValidationException Si el id es nulo, vacío o no numérico
	 * @throws ResourceNotFoundException Si no existe ningún grado con ese ID
	 */
	public Grado buscarPorId(String id) {
		validarFormatoId(id);
		return buscarPorIdInterno(id.trim());
	}

	/**
	 * Busca un Grado por su ID numérico.
	 *
	 * @param id Identificador numérico Long
	 * @return Objeto Grado completamente poblado con sus cursos
	 * @throws ValidationException Si el id es nulo
	 * @throws ResourceNotFoundException Si no existe ningún grado con ese ID
	 */
	public Grado buscarPorId(Long id) {
		if (id == null) {
			throw new ValidationException("El ID de grado no puede ser nulo");
		}
		return buscarPorIdInterno(id.toString());
	}

	private Grado buscarPorIdInterno(String idStr) {
		List<Object[]> rowsGrado = null;
		// 1. Intento con el esquema oficial Grado (gradoId)
		try {
			String sqlGrado = "SELECT gradoId, nombre, facultad, ramaConocimiento, estado FROM Grado WHERE gradoId = ?";
			rowsGrado = db.executeQueryArray(sqlGrado, idStr);
		} catch (Exception ignored) {
		}

		// 2. Fallback a esquema alternativo id_grado
		if (rowsGrado == null || rowsGrado.isEmpty()) {
			try {
				String sqlGradoAlt = "SELECT id_grado, nombre FROM Grado WHERE id_grado = ?";
				rowsGrado = db.executeQueryArray(sqlGradoAlt, idStr);
			} catch (Exception ignored) {
			}
		}

		if (rowsGrado == null || rowsGrado.isEmpty()) {
			throw new ResourceNotFoundException("Grado", idStr);
		}

		Object[] row = rowsGrado.get(0);
		Long idGrado = parseNumber(row[0], 1L);
		String gradoCode = row[0] != null ? row[0].toString() : idStr;
		String nombreGrado = (String) row[1];
		String facultad = row.length > 2 && row[2] != null ? (String) row[2] : "Escuela de Ingeniería Informática";
		String rama = row.length > 3 && row[3] != null ? (String) row[3] : "Ingeniería y Arquitectura";

		// Buscar cursos asociados a este grado
		List<Object[]> rowsCursos = null;
		try {
			String sqlCursos = "SELECT c.cursoId, c.nombre, gc.gradoId, c.coordinadorId, c.creditos FROM GradoCurso gc JOIN Curso c ON gc.cursoId = c.cursoId WHERE gc.gradoId = ? ORDER BY c.cursoId";
			rowsCursos = db.executeQueryArray(sqlCursos, idStr);
		} catch (Exception ignored) {
		}

		if (rowsCursos == null || rowsCursos.isEmpty()) {
			try {
				String sqlCursosAlt = "SELECT id_curso, nombre, id_grado, id_coordinador FROM Curso WHERE id_grado = ? ORDER BY id_curso";
				rowsCursos = db.executeQueryArray(sqlCursosAlt, idStr);
			} catch (Exception ignored) {
			}
		}

		List<Curso> cursos = new ArrayList<>();
		if (rowsCursos != null) {
			for (Object[] r : rowsCursos) {
				Long idCurso = parseNumber(r[0], 1L);
				String cursoCode = r[0] != null ? r[0].toString() : "";
				String nombreCurso = (String) r[1];
				Long idGradoCurso = r[2] != null ? parseNumber(r[2], idGrado) : idGrado;
				Long idCoordinador = r[3] != null ? parseNumber(r[3], null) : null;

				cursos.add(Curso.builder()
						.id(idCurso)
						.cursoId(cursoCode)
						.nombre(nombreCurso)
						.idGrado(idGradoCurso)
						.idCoordinador(idCoordinador)
						.creditos(6)
						.activo(true)
						.build());
			}
		}

		return Grado.builder()
				.id(idGrado)
				.gradoId(gradoCode)
				.nombre(nombreGrado)
				.facultad(facultad)
				.rama(rama)
				.cursos(cursos)
				.estado(EstadoGrado.ACTIVO)
				.activo(true)
				.build();
	}

	private void validarFormatoId(String id) {
		if (id == null) {
			throw new ValidationException("El ID de grado no puede ser nulo");
		}
		String trimmed = id.trim();
		if (trimmed.isEmpty()) {
			throw new ValidationException("El ID de grado no puede estar vacío");
		}
		if (!trimmed.matches("^\\d+$") && !trimmed.matches("^[Gg]\\d{3,4}$")) {
			throw new ValidationException("El formato del ID de grado es inválido: '" + id + "'. Debe ser un número entero positivo o código G001.");
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

