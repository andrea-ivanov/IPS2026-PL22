package repository;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.anoacademico.AnoAcademico;
import util.Database;
import util.Util;

/**
 * Acceso a datos del año académico (tabla AnioAcademico).
 * Solo SQL y mapeo de filas a DTO; las reglas de negocio están en AnoAcademicoModel.
 */
public class AnoAcademicoRepository {
	private final Database db;

	public AnoAcademicoRepository(Database db) {
		this.db = db;
	}

	/** Id del año académico actual: el último configurado (el de mayor fecha de inicio). Nulo si no hay ninguno */
	public String findActualId() {
		List<Object[]> rows = db.executeQueryArray(
				"SELECT anioAcademicoId FROM AnioAcademico ORDER BY fechaInicio DESC LIMIT 1");
		return rows.isEmpty() || rows.get(0)[0] == null ? null : rows.get(0)[0].toString();
	}

	/** Id del año académico cuyo rango de fechas contiene el día de hoy. Nulo si no hay ninguno */
	public String findVigenteId() {
		List<Object[]> rows = db.executeQueryArray("SELECT anioAcademicoId FROM AnioAcademico "
				+ "WHERE date('now') BETWEEN fechaInicio AND fechaFin LIMIT 1");
		return rows.isEmpty() || rows.get(0)[0] == null ? null : rows.get(0)[0].toString();
	}

	/**
	 * Id del último año que ya ha empezado en la fecha indicada, o el más antiguo si todavía no ha empezado
	 * ninguno. Nulo si no hay años
	 */
	public String findEnCursoId(String fechaHoy) {
		List<Object[]> rows = db.executeQueryArray("SELECT anioAcademicoId FROM AnioAcademico "
				+ "ORDER BY CASE WHEN fechaInicio <= ? THEN 0 ELSE 1 END, "
				+ "CASE WHEN fechaInicio <= ? THEN fechaInicio END DESC, fechaInicio LIMIT 1", fechaHoy, fechaHoy);
		return rows.isEmpty() ? null : rows.get(0)[0].toString();
	}

	/**
	 * Todos los años académicos del más antiguo al más reciente, como pares {anioAcademicoId, finMatriculacion}
	 * (finMatriculacion en ISO, puede ser nulo)
	 */
	public List<String[]> findIdsYFinMatriculacion() {
		List<String[]> anios = new ArrayList<>();
		for (Object[] fila : db.executeQueryArray(
				"SELECT anioAcademicoId, finMatriculacion FROM AnioAcademico ORDER BY anioAcademicoId")) {
			anios.add(new String[] { fila[0].toString(), fila[1] == null ? null : fila[1].toString() });
		}
		return anios;
	}

	/** Ids de todos los años académicos, del más reciente al más antiguo */
	public List<String> findAllIds() {
		List<String> ids = new ArrayList<>();
		for (Object[] fila : db.executeQueryArray(
				"SELECT anioAcademicoId FROM AnioAcademico ORDER BY anioAcademicoId DESC")) {
			ids.add(fila[0].toString());
		}
		return ids;
	}

	/** Año académico por su id, o nulo si no existe */
	public AnoAcademico findById(String id) {
		List<Object[]> rows = db.executeQueryArray("SELECT anioAcademicoId, fechaInicio, fechaFin, "
				+ "inicioMatriculacion, finMatriculacion FROM AnioAcademico WHERE anioAcademicoId = ?", id);
		if (rows == null || rows.isEmpty()) {
			return null;
		}
		Object[] r = rows.get(0);
		String anioId = (String) r[0];
		return new AnoAcademico(parsearNumeroDeId(anioId, 1L), anioId, anioId, mapearFecha(r[1]), mapearFecha(r[2]),
				mapearFecha(r[3]), mapearFecha(r[4]), true);
	}

	/** Id del año académico actual (el de mayor fecha de inicio) dentro de una transacción. Nulo si no hay ninguno */
	public String findActualId(Connection conn) throws SQLException {
		return new QueryRunner().query(conn,
				"SELECT anioAcademicoId FROM AnioAcademico ORDER BY fechaInicio DESC LIMIT 1",
				new ScalarHandler<String>());
	}

	/** Fecha de fin (ISO) del año académico inmediatamente anterior al que empieza en fechaInicio. Nulo si no hay */
	public String findFechaFinAnterior(Connection conn, String fechaInicio) throws SQLException {
		Object fin = new QueryRunner().query(conn,
				"SELECT fechaFin FROM AnioAcademico WHERE fechaInicio < ? ORDER BY fechaInicio DESC LIMIT 1",
				new ScalarHandler<Object>(), fechaInicio);
		return fin == null ? null : fin.toString();
	}

	/** Fecha de fin (ISO) del último año académico configurado, o nulo si no hay ninguno */
	public String findUltimaFechaFin(Connection conn) throws SQLException {
		Object fin = new QueryRunner().query(conn, "SELECT MAX(fechaFin) FROM AnioAcademico",
				new ScalarHandler<Object>());
		return fin == null ? null : fin.toString();
	}

	public boolean existe(Connection conn, String anioAcademicoId) throws SQLException {
		Number existentes = new QueryRunner().query(conn,
				"SELECT COUNT(*) FROM AnioAcademico WHERE anioAcademicoId = ?", new ScalarHandler<Number>(),
				anioAcademicoId);
		return existentes.intValue() > 0;
	}

	public void insertar(Connection conn, String anioId, String inicio, String fin, String inicioMatriculacion,
			String finMatriculacion) throws SQLException {
		update(conn, "INSERT INTO AnioAcademico (anioAcademicoId, fechaInicio, fechaFin, inicioMatriculacion, finMatriculacion) "
				+ "VALUES (?, ?, ?, ?, ?)", anioId, inicio, fin, inicioMatriculacion, finMatriculacion);
	}

	/**
	 * Pasa de estado los grados cuya vigencia ha caducado: los que están en estadoOrigen y tienen aniosVigencia o
	 * más años académicos registrados que empiezan en su fechaInicioVigencia o después pasan a estadoDestino
	 */
	public void actualizarGradosVigenciaCaducada(Connection conn, int aniosVigencia, String estadoOrigen,
			String estadoDestino) throws SQLException {
		update(conn, "UPDATE Grado SET estado = ? WHERE estado = ? AND (SELECT COUNT(*) FROM AnioAcademico a "
				+ "WHERE a.fechaInicio >= Grado.fechaInicioVigencia) >= ?",
				estadoDestino, estadoOrigen, aniosVigencia);
	}

	/**
	 * Cambia de estadoOrigen a estadoDestino los cursos de los grados que estén en alguno de los estados de grado
	 * indicados
	 */
	public void actualizarCursosDeGradosEnEstados(Connection conn, List<String> estadosGrado, String estadoOrigen,
			String estadoDestino) throws SQLException {
		String marcas = String.join(", ", java.util.Collections.nCopies(estadosGrado.size(), "?"));
		List<Object> params = new ArrayList<>();
		params.add(estadoDestino);
		params.add(estadoOrigen);
		params.addAll(estadosGrado);
		update(conn, "UPDATE GradoCurso SET estado = ? WHERE estado = ? "
				+ "AND gradoId IN (SELECT gradoId FROM Grado WHERE estado IN (" + marcas + "))", params.toArray());
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
		throw new IllegalStateException("Tipo de fecha incompatible: " + fechaObj.getClass());
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
