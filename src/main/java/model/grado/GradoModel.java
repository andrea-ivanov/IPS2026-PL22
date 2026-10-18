package model.grado;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.curso.Curso;
import model.curso.EstadoCurso;
import util.Database;
import util.UnexpectedException;
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
		EstadoGrado estado = EstadoGrado.fromString(row.length > 4 ? (String) row[4] : null);

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
				.estado(estado)
				.activo(true)
				.build();
	}

	/**
	 * Crea un grado VIGENTE cuya vigencia empieza con el año académico indicado.
	 *
	 * @return Identificador generado (G001, G002...)
	 * @throws ValidationException Si falta algún dato obligatorio o el año académico no existe
	 */
	public String crearGrado(String nombre, String facultad, String ramaConocimiento, String anioAcademicoId) {
		validarObligatorio(nombre, "El nombre del grado es obligatorio");
		validarObligatorio(facultad, "La facultad del grado es obligatoria");
		validarObligatorio(ramaConocimiento, "La rama de conocimiento del grado es obligatoria");
		validarObligatorio(anioAcademicoId, "No hay un año académico actual configurado");

		return enTransaccion(conn -> {
			Object fechaInicio = new QueryRunner().query(conn,
					"SELECT fechaInicio FROM AnioAcademico WHERE anioAcademicoId = ?",
					new ScalarHandler<Object>(), anioAcademicoId.trim());
			if (fechaInicio == null) {
				throw new ValidationException("El año académico " + anioAcademicoId + " no existe");
			}
			String gradoId = generarId(conn);
			Number existentes = new QueryRunner().query(conn, "SELECT COUNT(*) FROM Grado WHERE gradoId = ?",
					new ScalarHandler<Number>(), gradoId);
			if (existentes.intValue() > 0) {
				throw new ValidationException("Ya existe un grado con el código " + gradoId);
			}
			update(conn, "INSERT INTO Grado (gradoId, nombre, facultad, ramaConocimiento, estado, fechaInicioVigencia) "
					+ "VALUES (?, ?, ?, ?, ?, ?)",
					gradoId, nombre.trim(), facultad.trim(), ramaConocimiento.trim(), EstadoGrado.VIGENTE.name(),
					fechaInicio.toString());
			return gradoId;
		});
	}

	/**
	 * Modifica los datos editables del grado (nombre, facultad y rama), en cualquier estado.
	 *
	 * @throws ValidationException Si falta algún dato obligatorio
	 * @throws ResourceNotFoundException Si no existe el grado
	 */
	public void modificarGrado(String gradoId, String nombre, String facultad, String ramaConocimiento) {
		validarObligatorio(gradoId, "Debe indicarse el grado a modificar");
		validarObligatorio(nombre, "El nombre del grado es obligatorio");
		validarObligatorio(facultad, "La facultad del grado es obligatoria");
		validarObligatorio(ramaConocimiento, "La rama de conocimiento del grado es obligatoria");

		enTransaccion(conn -> {
			int filas = update(conn, "UPDATE Grado SET nombre = ?, facultad = ?, ramaConocimiento = ? WHERE gradoId = ?",
					nombre.trim(), facultad.trim(), ramaConocimiento.trim(), gradoId);
			if (filas == 0) {
				throw new ResourceNotFoundException("Grado", gradoId);
			}
			return null;
		});
	}

	/**
	 * Comprueba que el grado se puede eliminar en el año académico indicado.
	 *
	 * @throws ValidationException Si el grado ya está en extinción o alguno de sus cursos tiene matrículas en el año
	 */
	public void validarEliminacion(String gradoId, String anioAcademicoId) {
		enTransaccion(conn -> {
			comprobarEliminable(conn, gradoId, anioAcademicoId);
			return null;
		});
	}

	/**
	 * Elimina lógicamente el grado: pasa a EN_EXTINCION y todos sus cursos a INACTIVO.
	 *
	 * @throws ValidationException Si el grado no se puede eliminar (ver validarEliminacion)
	 */
	public void eliminarGrado(String gradoId, String anioAcademicoId) {
		enTransaccion(conn -> {
			comprobarEliminable(conn, gradoId, anioAcademicoId);
			update(conn, "UPDATE Grado SET estado = ? WHERE gradoId = ?", EstadoGrado.EN_EXTINCION.name(), gradoId);
			update(conn, "UPDATE GradoCurso SET estado = ? WHERE gradoId = ?", EstadoCurso.INACTIVO.name(), gradoId);
			return null;
		});
	}

	private void comprobarEliminable(Connection conn, String gradoId, String anioAcademicoId) throws SQLException {
		validarObligatorio(anioAcademicoId, "No hay un año académico actual configurado");
		validarObligatorio(gradoId, "Debe indicarse el grado a eliminar");

		String estado = new QueryRunner().query(conn, "SELECT estado FROM Grado WHERE gradoId = ?",
				new ScalarHandler<String>(), gradoId);
		if (estado == null) {
			throw new ResourceNotFoundException("Grado", gradoId);
		}
		if (EstadoGrado.fromString(estado) == EstadoGrado.EN_EXTINCION) {
			throw new ValidationException("El grado " + gradoId + " ya está en extinción");
		}

		Number matriculas = new QueryRunner().query(conn,
				"SELECT COUNT(*) FROM MatriculaCurso mc JOIN Matricula m ON m.matriculaId = mc.matriculaId "
						+ "WHERE mc.gradoId = ? AND m.anioAcademicoId = ? AND mc.estado <> 'CANCELADA'",
				new ScalarHandler<Number>(), gradoId, anioAcademicoId);
		if (matriculas.intValue() > 0) {
			throw new ValidationException("No se puede eliminar el grado " + gradoId
					+ ": tiene cursos con matrículas en el año académico " + anioAcademicoId);
		}
	}

	private String generarId(Connection conn) throws SQLException {
		List<String> ids = new QueryRunner().query(conn, "SELECT gradoId FROM Grado WHERE gradoId GLOB 'G[0-9]*'",
				new ColumnListHandler<String>());
		int max = 0;
		for (String id : ids) {
			if (id.matches("^G\\d+$")) {
				max = Math.max(max, Integer.parseInt(id.substring(1)));
			}
		}
		return String.format("G%03d", max + 1);
	}

	private void validarObligatorio(String valor, String mensaje) {
		if (valor == null || valor.trim().isEmpty()) {
			throw new ValidationException(mensaje);
		}
	}

	private int update(Connection conn, String sql, Object... params) throws SQLException {
		return new QueryRunner().update(conn, sql, params);
	}

	@FunctionalInterface
	private interface OperacionBD<T> {
		T ejecutar(Connection conn) throws SQLException;
	}

	/** Ejecuta la operación en una única transacción: o se aplican todos los cambios o ninguno. */
	private <T> T enTransaccion(OperacionBD<T> operacion) {
		Connection conn = null;
		try {
			conn = db.getConnection();
			conn.setAutoCommit(false);
			T result = operacion.ejecutar(conn);
			conn.commit();
			return result;
		} catch (SQLException e) {
			DbUtils.rollbackQuietly(conn);
			throw new UnexpectedException(e);
		} catch (RuntimeException e) {
			DbUtils.rollbackQuietly(conn);
			throw e;
		} finally {
			DbUtils.closeQuietly(conn);
		}
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

