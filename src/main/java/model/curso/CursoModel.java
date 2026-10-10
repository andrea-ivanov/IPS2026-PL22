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
		long idNumerico = Long.parseLong(id.trim());
		return buscarPorId(idNumerico);
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

		String sql = "SELECT id_curso, nombre, id_grado, id_coordinador FROM Curso WHERE id_curso = ?";
		List<Object[]> rows = db.executeQueryArray(sql, id);
		if (rows == null || rows.isEmpty()) {
			throw new ResourceNotFoundException("Curso", id);
		}

		Object[] row = rows.get(0);
		Long idCurso = ((Number) row[0]).longValue();
		String nombre = (String) row[1];
		Long idGrado = row[2] != null ? ((Number) row[2]).longValue() : null;
		Long idCoordinador = row[3] != null ? ((Number) row[3]).longValue() : null;

		return Curso.builder()
				.id(idCurso)
				.nombre(nombre)
				.idGrado(idGrado)
				.idCoordinador(idCoordinador)
				.creditos(6)
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
		if (!trimmed.matches("^\\d+$")) {
			throw new ValidationException("El formato del ID de curso es inválido: '" + id + "'. Debe ser un número entero positivo.");
		}
	}
}

