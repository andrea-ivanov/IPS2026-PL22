package model.grado;

import java.util.ArrayList;
import java.util.List;

import model.curso.Curso;
import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Modelo y caso de uso para la gestión y búsqueda de Grados.
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
	 * @return Objeto Grado completamente poblado
	 * @throws ValidationException Si el id es nulo, vacío o no tiene formato numérico
	 * @throws ResourceNotFoundException Si no existe ningún grado con ese ID
	 */
	public Grado buscarPorId(String id) {
		validarFormatoId(id);
		long idNumerico = Long.parseLong(id.trim());
		return buscarPorId(idNumerico);
	}

	/**
	 * Busca un Grado por su ID numérico.
	 *
	 * @param id Identificador numérico
	 * @return Objeto Grado completamente poblado
	 * @throws ValidationException Si el id es nulo
	 * @throws ResourceNotFoundException Si no existe ningún grado con ese ID
	 */
	public Grado buscarPorId(Long id) {
		if (id == null) {
			throw new ValidationException("El ID de grado no puede ser nulo");
		}

		String sqlGrado = "SELECT id_grado, nombre FROM Grado WHERE id_grado = ?";
		List<Object[]> rowsGrado = db.executeQueryArray(sqlGrado, id);
		if (rowsGrado == null || rowsGrado.isEmpty()) {
			throw new ResourceNotFoundException("Grado", id);
		}

		Object[] row = rowsGrado.get(0);
		Long idGrado = ((Number) row[0]).longValue();
		String nombreGrado = (String) row[1];

		// Buscar cursos asociados a este grado
		String sqlCursos = "SELECT id_curso, nombre, id_grado, id_coordinador FROM Curso WHERE id_grado = ? ORDER BY id_curso";
		List<Object[]> rowsCursos = db.executeQueryArray(sqlCursos, id);
		List<Curso> cursos = new ArrayList<>();
		if (rowsCursos != null) {
			for (Object[] r : rowsCursos) {
				Long idCurso = ((Number) r[0]).longValue();
				String nombreCurso = (String) r[1];
				Long idGradoCurso = r[2] != null ? ((Number) r[2]).longValue() : null;
				Long idCoordinador = r[3] != null ? ((Number) r[3]).longValue() : null;

				cursos.add(Curso.builder()
						.id(idCurso)
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
				.nombre(nombreGrado)
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
		if (!trimmed.matches("^\\d+$")) {
			throw new ValidationException("El formato del ID de grado es inválido: '" + id + "'. Debe ser un número entero positivo.");
		}
	}
}

