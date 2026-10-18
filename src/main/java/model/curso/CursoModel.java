package model.curso;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.grado.EstadoGrado;
import util.Database;
import util.UnexpectedException;
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

	/**
	 * Crea un curso nuevo y lo asocia ACTIVO al grado indicado, que debe estar VIGENTE.
	 *
	 * @return Identificador generado (C001, C002...)
	 * @throws ValidationException Si falta algún dato, los créditos o el año no son válidos o el grado no está VIGENTE
	 */
	public String crearCurso(String gradoId, String nombre, String creditos, String anio, String coordinadorId,
			String itinerario) {
		validarObligatorio(gradoId, "Debe indicarse el grado del curso");
		validarObligatorio(nombre, "El nombre del curso es obligatorio");
		validarObligatorio(creditos, "Los créditos del curso son obligatorios");
		validarObligatorio(anio, "El año del curso es obligatorio");
		validarObligatorio(coordinadorId, "El coordinador del curso es obligatorio");
		validarObligatorio(itinerario, "El itinerario del curso es obligatorio");
		double valorCreditos = parsearCreditos(creditos);
		int valorAnio = parsearAnio(anio);

		return enTransaccion(conn -> {
			String estadoGrado = new QueryRunner().query(conn, "SELECT estado FROM Grado WHERE gradoId = ?",
					new ScalarHandler<String>(), gradoId);
			if (estadoGrado == null) {
				throw new ResourceNotFoundException("Grado", gradoId);
			}
			if (!EstadoGrado.VIGENTE.name().equals(estadoGrado)) {
				throw new ValidationException("Solo se pueden añadir cursos a grados vigentes");
			}
			String cursoId = generarId(conn);
			Number existentes = new QueryRunner().query(conn, "SELECT COUNT(*) FROM Curso WHERE cursoId = ?",
					new ScalarHandler<Number>(), cursoId);
			if (existentes.intValue() > 0) {
				throw new ValidationException("Ya existe un curso con el código " + cursoId);
			}
			update(conn, "INSERT INTO Curso (cursoId, coordinadorId, nombre, creditos) VALUES (?, ?, ?, ?)",
					cursoId, coordinadorId.trim(), nombre.trim(), valorCreditos);
			update(conn, "INSERT INTO GradoCurso (gradoId, cursoId, anio, itinerario, estado) VALUES (?, ?, ?, ?, ?)",
					gradoId, cursoId, valorAnio, itinerario.trim(), EstadoCurso.ACTIVO.name());
			return cursoId;
		});
	}

	/**
	 * Modifica los datos editables del curso en el grado, en cualquier estado. El año y el itinerario
	 * cambian solo en ese grado; los créditos son de la asignatura y cambian en todos sus grados.
	 *
	 * @throws ValidationException Si falta algún dato o los créditos o el año no son válidos
	 * @throws ResourceNotFoundException Si el curso no pertenece a ese grado
	 */
	public void modificarCurso(String gradoId, String cursoId, String anio, String itinerario, String creditos) {
		validarObligatorio(gradoId, "Debe indicarse el grado del curso");
		validarObligatorio(cursoId, "Debe indicarse el curso a modificar");
		validarObligatorio(anio, "El año del curso es obligatorio");
		validarObligatorio(itinerario, "El itinerario del curso es obligatorio");
		validarObligatorio(creditos, "Los créditos del curso son obligatorios");
		double valorCreditos = parsearCreditos(creditos);
		int valorAnio = parsearAnio(anio);

		enTransaccion(conn -> {
			int filas = update(conn, "UPDATE GradoCurso SET anio = ?, itinerario = ? WHERE gradoId = ? AND cursoId = ?",
					valorAnio, itinerario.trim(), gradoId, cursoId);
			if (filas == 0) {
				throw new ResourceNotFoundException("Curso", cursoId);
			}
			update(conn, "UPDATE Curso SET creditos = ? WHERE cursoId = ?", valorCreditos, cursoId);
			return null;
		});
	}

	/**
	 * Obtiene los datos actuales del curso en el grado, sin transformar, para precargar su modificación.
	 *
	 * @throws ResourceNotFoundException Si el curso no pertenece a ese grado
	 */
	public CursoEdicionDTO buscarDatosEdicion(String gradoId, String cursoId) {
		List<CursoEdicionDTO> datos = db.executeQueryPojo(CursoEdicionDTO.class,
				"SELECT gc.anio, gc.itinerario, c.creditos, c.nombre, c.coordinadorId "
						+ "FROM GradoCurso gc JOIN Curso c ON c.cursoId = gc.cursoId WHERE gc.gradoId = ? AND gc.cursoId = ?",
				gradoId, cursoId);
		if (datos.isEmpty()) {
			throw new ResourceNotFoundException("Curso", cursoId);
		}
		return datos.get(0);
	}

	/**
	 * Cuenta en cuántos grados está el curso.
	 */
	public int contarGradosDelCurso(String cursoId) {
		return ((Number) db.executeQueryArray("SELECT COUNT(*) FROM GradoCurso WHERE cursoId = ?", cursoId).get(0)[0])
				.intValue();
	}

	/**
	 * Comprueba que el curso se puede eliminar del grado en el año académico indicado.
	 *
	 * @throws ValidationException Si el curso ya está INACTIVO o tiene matrículas en el año en ese grado
	 */
	public void validarEliminacion(String gradoId, String cursoId, String anioAcademicoId) {
		enTransaccion(conn -> {
			comprobarEliminable(conn, gradoId, cursoId, anioAcademicoId);
			return null;
		});
	}

	/**
	 * Elimina lógicamente el curso del grado: pasa a INACTIVO solo en ese grado.
	 *
	 * @throws ValidationException Si el curso no se puede eliminar (ver validarEliminacion)
	 */
	public void eliminarCurso(String gradoId, String cursoId, String anioAcademicoId) {
		enTransaccion(conn -> {
			comprobarEliminable(conn, gradoId, cursoId, anioAcademicoId);
			update(conn, "UPDATE GradoCurso SET estado = ? WHERE gradoId = ? AND cursoId = ?",
					EstadoCurso.INACTIVO.name(), gradoId, cursoId);
			return null;
		});
	}

	private void comprobarEliminable(Connection conn, String gradoId, String cursoId, String anioAcademicoId)
			throws SQLException {
		validarObligatorio(anioAcademicoId, "No hay un año académico actual configurado");
		validarObligatorio(gradoId, "Debe indicarse el grado del curso");
		validarObligatorio(cursoId, "Debe indicarse el curso a eliminar");

		String estado = new QueryRunner().query(conn,
				"SELECT estado FROM GradoCurso WHERE gradoId = ? AND cursoId = ?",
				new ScalarHandler<String>(), gradoId, cursoId);
		if (estado == null) {
			throw new ResourceNotFoundException("Curso", cursoId);
		}
		if (EstadoCurso.fromString(estado) == EstadoCurso.INACTIVO) {
			throw new ValidationException("El curso " + cursoId + " ya está inactivo");
		}

		Number matriculas = new QueryRunner().query(conn,
				"SELECT COUNT(*) FROM MatriculaCurso mc JOIN Matricula m ON m.matriculaId = mc.matriculaId "
						+ "WHERE mc.gradoId = ? AND mc.cursoId = ? AND m.anioAcademicoId = ? AND mc.estado <> 'CANCELADA'",
				new ScalarHandler<Number>(), gradoId, cursoId, anioAcademicoId);
		if (matriculas.intValue() > 0) {
			throw new ValidationException("No se puede eliminar el curso " + cursoId
					+ ": tiene matrículas en el año académico " + anioAcademicoId);
		}
	}

	private double parsearCreditos(String creditos) {
		double valor;
		try {
			valor = Double.parseDouble(creditos.trim());
		} catch (NumberFormatException e) {
			throw new ValidationException("Los créditos deben ser un número: '" + creditos + "'");
		}
		if (valor <= 0) {
			throw new ValidationException("Los créditos deben ser mayores que 0");
		}
		return valor;
	}

	private int parsearAnio(String anio) {
		int valor;
		try {
			valor = Integer.parseInt(anio.trim());
		} catch (NumberFormatException e) {
			throw new ValidationException("El año debe ser un número entero: '" + anio + "'");
		}
		if (valor < 1 || valor > 6) {
			throw new ValidationException("El año debe estar entre 1 y 6");
		}
		return valor;
	}

	private String generarId(Connection conn) throws SQLException {
		List<String> ids = new QueryRunner().query(conn, "SELECT cursoId FROM Curso WHERE cursoId GLOB 'C[0-9]*'",
				new ColumnListHandler<String>());
		int max = 0;
		for (String id : ids) {
			if (id.matches("^C\\d+$")) {
				max = Math.max(max, Integer.parseInt(id.substring(1)));
			}
		}
		return String.format("C%03d", max + 1);
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

