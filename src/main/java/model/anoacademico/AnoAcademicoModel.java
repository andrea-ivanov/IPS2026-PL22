package model.anoacademico;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.curso.EstadoCurso;
import model.grado.EstadoGrado;
import util.Database;
import util.UnexpectedException;
import util.Util;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Modelo de negocio y caso de uso para la gestión del Año Académico y sus periodos.
 * Conecta con la tabla AnioAcademico de la base de datos.
 */
public class AnoAcademicoModel {
	private Database db;

	public AnoAcademicoModel() {
		this.db = new Database();
	}

	public AnoAcademicoModel(Database db) {
		this.db = db;
	}

	/**
	 * Busca el año académico actual: el último configurado (el de mayor fecha de inicio).
	 *
	 * @return Objeto AnoAcademico actual completamente poblado desde BD
	 * @throws ResourceNotFoundException si no existe ningún año académico en la base de datos
	 */
	public AnoAcademico buscarActual() {
		List<Object[]> rows = db.executeQueryArray(
				"SELECT anioAcademicoId FROM AnioAcademico ORDER BY fechaInicio DESC LIMIT 1");
		if (rows.isEmpty()) {
			throw new ResourceNotFoundException("AnoAcademico", "actual");
		}
		return buscarPorId((String) rows.get(0)[0]);
	}

	/**
	 * Busca un Año Académico por su identificador (p. ej. "2026-2027").
	 */
	public AnoAcademico buscarPorId(String id) {
		validarFormatoId(id);
		String cleanId = id.trim();

		String sql = "SELECT anioAcademicoId, fechaInicio, fechaFin, inicioMatriculacion, finMatriculacion FROM AnioAcademico WHERE anioAcademicoId = ?";
		List<Object[]> rows = db.executeQueryArray(sql, cleanId);
		if (rows == null || rows.isEmpty()) {
			throw new ResourceNotFoundException("AnoAcademico", id);
		}
		Object[] r = rows.get(0);
		String anioId = (String) r[0];
		long idNum = parsearNumeroDeId(anioId, 1L);
		return new AnoAcademico(idNum,anioId,anioId,mapearFecha(r[1]),mapearFecha(r[2]),mapearFecha(r[3]),mapearFecha(r[4]),true);
	}

	/**
	 * Comprueba que el año académico anterior ya ha terminado en la fecha indicada (hoy posterior a su fecha de fin).
	 *
	 * @param fechaHoy Fecha de referencia en formato ISO (AAAA-MM-DD)
	 * @throws ValidationException Si el año anterior todavía no ha terminado
	 */
	public void validarAnoAnteriorTerminado(String fechaHoy) {
		enTransaccion(conn -> {
			comprobarAnoAnteriorTerminado(conn, fechaHoy);
			return null;
		});
	}

	/**
	 * Comprueba que se puede configurar un nuevo año académico con las fechas indicadas, sin modificar nada.
	 *
	 * @param fechaHoy Fecha de referencia en formato ISO (AAAA-MM-DD)
	 * @return Identificador del año que se creará (AAAA-AAAA)
	 * @throws ValidationException Si el año anterior no ha terminado o alguna fecha falta o no cumple las reglas
	 */
	public String validarNuevoAnoAcademico(String fechaHoy, Date inicioAnio, Date finAnio, Date inicioMatriculacion,
			Date finMatriculacion) {
		return enTransaccion(conn -> comprobarNuevoAno(conn, fechaHoy, inicioAnio, finAnio, inicioMatriculacion,
				finMatriculacion));
	}

	/**
	 * Configura un nuevo año académico, que pasa a ser el actual. En la misma transacción:
	 * los grados VIGENTES con la vigencia de 4 años caducada pasan a PENDIENTE_RENOVACION
	 * y los cursos de grados en extinción pasan a INACTIVO.
	 *
	 * @param fechaHoy Fecha de referencia en formato ISO (AAAA-MM-DD)
	 * @return Identificador del año creado (AAAA-AAAA)
	 * @throws ValidationException Si el año anterior no ha terminado o alguna fecha falta o no cumple las reglas
	 */
	public String iniciarNuevoAnoAcademico(String fechaHoy, Date inicioAnio, Date finAnio, Date inicioMatriculacion,
			Date finMatriculacion) {
		return enTransaccion(conn -> {
			String anioId = comprobarNuevoAno(conn, fechaHoy, inicioAnio, finAnio, inicioMatriculacion,
					finMatriculacion);
			String inicio = Util.dateToIsoString(inicioAnio);
			update(conn, "INSERT INTO AnioAcademico (anioAcademicoId, fechaInicio, fechaFin, inicioMatriculacion, finMatriculacion) "
					+ "VALUES (?, ?, ?, ?, ?)",
					anioId, inicio, Util.dateToIsoString(finAnio), Util.dateToIsoString(inicioMatriculacion),
					Util.dateToIsoString(finMatriculacion));
			update(conn, "UPDATE Grado SET estado = ? WHERE estado = ? AND date(fechaInicioVigencia, '+4 years') <= ?",
					EstadoGrado.PENDIENTE_RENOVACION.name(), EstadoGrado.VIGENTE.name(), inicio);
			update(conn, "UPDATE GradoCurso SET estado = ? WHERE estado = ? "
					+ "AND gradoId IN (SELECT gradoId FROM Grado WHERE estado IN ('EN_EXTINCION', 'EXTINTO'))",
					EstadoCurso.INACTIVO.name(), EstadoCurso.ACTIVO.name());
			return anioId;
		});
	}

	private void comprobarAnoAnteriorTerminado(Connection conn, String fechaHoy) throws SQLException {
		if (fechaHoy == null || fechaHoy.trim().isEmpty()) {
			throw new ValidationException("Debe indicarse la fecha de hoy");
		}
		Object finAnterior = new QueryRunner().query(conn, "SELECT MAX(fechaFin) FROM AnioAcademico",
				new ScalarHandler<Object>());
		if (finAnterior != null && fechaHoy.trim().compareTo(finAnterior.toString()) <= 0) {
			throw new ValidationException("No se puede configurar un nuevo año académico hasta que termine el actual ("
					+ finAnterior + ")");
		}
	}

	private String comprobarNuevoAno(Connection conn, String fechaHoy, Date inicioAnio, Date finAnio,
			Date inicioMatriculacion, Date finMatriculacion) throws SQLException {
		comprobarAnoAnteriorTerminado(conn, fechaHoy);
		if (inicioAnio == null || finAnio == null || inicioMatriculacion == null || finMatriculacion == null) {
			throw new ValidationException("Las fechas de inicio y fin del año académico y del periodo de matriculación "
					+ "son obligatorias (dd/mm/aa)");
		}
		String inicio = Util.dateToIsoString(inicioAnio);
		String fin = Util.dateToIsoString(finAnio);
		String inicioMat = Util.dateToIsoString(inicioMatriculacion);
		String finMat = Util.dateToIsoString(finMatriculacion);

		if (fin.compareTo(inicio) <= 0) {
			throw new ValidationException("La fecha de fin del año académico debe ser posterior a la de inicio");
		}
		if (finMat.compareTo(inicioMat) <= 0) {
			throw new ValidationException("La fecha de fin de matriculación debe ser posterior a la de inicio");
		}
		if (finMat.compareTo(fin) >= 0) {
			throw new ValidationException("El periodo de matriculación debe terminar antes que el año académico");
		}

		Object finAnterior = new QueryRunner().query(conn, "SELECT MAX(fechaFin) FROM AnioAcademico",
				new ScalarHandler<Object>());
		if (finAnterior != null) {
			if (inicio.compareTo(finAnterior.toString()) <= 0) {
				throw new ValidationException("El año académico debe empezar después de que termine el anterior ("
						+ finAnterior + ")");
			}
			if (inicioMat.compareTo(finAnterior.toString()) <= 0) {
				throw new ValidationException("El periodo de matriculación debe empezar después de que termine el año anterior ("
						+ finAnterior + ")");
			}
		}

		String anioId = inicio.substring(0, 4) + "-" + fin.substring(0, 4);
		Number existentes = new QueryRunner().query(conn,
				"SELECT COUNT(*) FROM AnioAcademico WHERE anioAcademicoId = ?", new ScalarHandler<Number>(), anioId);
		if (existentes.intValue() > 0) {
			throw new ValidationException("Ya existe el año académico " + anioId);
		}
		return anioId;
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

	private Date mapearFecha(Object fechaObj) {
		if (fechaObj == null) {
			return null;
		}
		if (fechaObj instanceof Date) {
			return (Date) fechaObj;
		}
		if (fechaObj instanceof java.sql.Date) {
			return new Date(((java.sql.Date) fechaObj).getTime());
		}
		if (fechaObj instanceof String) {
			return Util.isoStringToDate((String) fechaObj);
		}
		throw new ValidationException("Tipo de fecha incompatible: " + fechaObj.getClass());
	}

	private void validarFormatoId(String id) {
		if (id == null) {
			throw new ValidationException("El ID de año académico no puede ser nulo");
		}
		String trimmed = id.trim();
		if (trimmed.isEmpty()) {
			throw new ValidationException("El ID de año académico no puede estar vacío");
		}
		if (!trimmed.matches("^\\d+$") && !trimmed.matches("^\\d{4}[-/]\\d{4}$")) {
			throw new ValidationException("El formato del ID de año académico es inválido: '" + id + "'. Debe ser un número entero o rango año (p.ej. 2026-2027).");
		}
	}

	private long parsearNumeroDeId(String str, long defaultVal) {
		if (str == null) return defaultVal;
		try {
			String digits = str.replaceAll("\\D+", "");
			return digits.isEmpty() ? defaultVal : Long.parseLong(digits.substring(0, Math.min(digits.length(), 6)));
		} catch (Exception e) {
			return defaultVal;
		}
	}
}
