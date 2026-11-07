package repository;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import model.acta.ActaDisplayDTO;
import model.acta.CalificacionActaDisplayDTO;
import model.acta.EstadoActa;
import util.Database;

/**
 * Acceso a datos de actas y notas (tablas Acta, Calificacion y Convocatoria).
 * Solo SQL y mapeo de filas a DTO; las reglas de negocio están en ActaModel.
 */
public class ActaRepository {
	private static final String SQL_SELECT_ACTA = "SELECT a.cursoId, a.anioAcademicoId, a.tipoConvocatoria, cv.fechaFin, a.estado "
			+ "FROM Acta a "
			+ "JOIN Convocatoria cv ON cv.anioAcademicoId = a.anioAcademicoId AND cv.tipoConvocatoria = a.tipoConvocatoria ";

	/**
	 * Alumnos que entran en el acta "a": matriculados en la asignatura ese año (PAGADA, ACTIVA o FINALIZADA)
	 * que no la han aprobado ya en una convocatoria anterior del mismo año (p. ej. aprobado en enero no va a julio).
	 * Alias: a (Acta), m (Matricula), mc (MatriculaCurso). Lleva un parámetro: la nota mínima de aprobado.
	 */
	private static final String SQL_ALUMNOS_ACTA = "FROM Acta a "
			+ "JOIN Matricula m ON m.anioAcademicoId = a.anioAcademicoId "
			+ "JOIN MatriculaCurso mc ON mc.matriculaId = m.matriculaId AND mc.cursoId = a.cursoId "
			+ "AND mc.estado IN ('PAGADA', 'ACTIVA', 'FINALIZADA') "
			+ "AND NOT EXISTS (SELECT 1 FROM Calificacion ap "
			+ "JOIN Convocatoria cvAp ON cvAp.anioAcademicoId = ap.anioAcademicoId AND cvAp.tipoConvocatoria = ap.tipoConvocatoria "
			+ "JOIN Convocatoria cvActa ON cvActa.anioAcademicoId = a.anioAcademicoId AND cvActa.tipoConvocatoria = a.tipoConvocatoria "
			+ "WHERE ap.matriculaId = mc.matriculaId AND ap.cursoId = mc.cursoId AND ap.anioAcademicoId = a.anioAcademicoId "
			+ "AND ap.nota >= ? AND cvAp.fechaFin < cvActa.fechaFin) ";

	private final Database db;

	public ActaRepository(Database db) {
		this.db = db;
	}

	/** Actas de la asignatura, de la más reciente a la más antigua */
	public List<ActaDisplayDTO> findActasCurso(String cursoId) {
		return db.executeQueryPojo(ActaDisplayDTO.class, SQL_SELECT_ACTA + "WHERE a.cursoId = ? ORDER BY cv.fechaFin DESC",
				cursoId);
	}

	/** Actas en el estado indicado cuya convocatoria terminó antes de la fecha */
	public List<ActaDisplayDTO> findActasVencidas(String estado, String fechaHoy) {
		return db.executeQueryPojo(ActaDisplayDTO.class,
				SQL_SELECT_ACTA + "WHERE a.estado = ? AND cv.fechaFin < ?", estado, fechaHoy);
	}

	/**
	 * Alumnos del acta con su nota. En un acta abierta salen los que entran en ella (ver SQL_ALUMNOS_ACTA);
	 * en una cerrada solo los que tienen calificación. La nota sale vacía si no tiene calificación y NP si no tiene nota.
	 */
	public List<CalificacionActaDisplayDTO> findCalificaciones(String cursoId, String anioAcademicoId,
			String tipoConvocatoria, double notaAprobado) {
		return db.executeQueryPojo(CalificacionActaDisplayDTO.class,
				"SELECT mc.matriculaId, al.alumnoId, al.nombre || ' ' || al.apellidos AS alumno, al.dni, "
						+ "CASE WHEN ca.matriculaId IS NULL THEN '' WHEN ca.nota IS NULL THEN 'NP' ELSE CAST(ca.nota AS TEXT) END AS nota "
						+ SQL_ALUMNOS_ACTA
						+ "JOIN Alumno al ON al.alumnoId = m.alumnoId "
						+ "LEFT JOIN Calificacion ca ON ca.matriculaId = mc.matriculaId AND ca.cursoId = a.cursoId "
						+ "AND ca.anioAcademicoId = a.anioAcademicoId AND ca.tipoConvocatoria = a.tipoConvocatoria "
						+ "WHERE a.cursoId = ? AND a.anioAcademicoId = ? AND a.tipoConvocatoria = ? "
						+ "AND (a.estado = ? OR ca.matriculaId IS NOT NULL) "
						+ "ORDER BY al.apellidos, al.nombre",
				notaAprobado, cursoId, anioAcademicoId, tipoConvocatoria, EstadoActa.ABIERTA.name());
	}

	/**
	 * Actas que ha cerrado el sistema, de la más reciente a la más antigua.
	 * Columnas: curso, asignatura, año, convocatoria, fecha de cierre y coordinador.
	 */
	public List<Object[]> findActasCerradasAutomaticamente() {
		return db.executeQueryArray("SELECT a.cursoId, c.nombre, a.anioAcademicoId, a.tipoConvocatoria, a.fechaCierre, "
				+ "COALESCE(p.nombre || ' ' || p.apellidos, '(sin coordinador)') "
				+ "FROM Acta a "
				+ "JOIN Curso c ON c.cursoId = a.cursoId "
				+ "LEFT JOIN Profesor p ON p.profesorId = c.coordinadorId "
				+ "WHERE a.cierreAutomatico = 1 "
				+ "ORDER BY a.fechaCierre DESC, a.cursoId");
	}

	/** El profesor es el coordinador de la asignatura */
	public boolean esCoordinador(String profesorId, String cursoId) {
		return !db.executeQueryArray("SELECT 1 FROM Curso WHERE cursoId = ? AND coordinadorId = ?", cursoId, profesorId)
				.isEmpty();
	}

	/** El acta existe y está en el estado indicado */
	public boolean existeActaEnEstado(String cursoId, String anioAcademicoId, String tipoConvocatoria, String estado) {
		return !db.executeQueryArray("SELECT 1 FROM Acta WHERE cursoId = ? AND anioAcademicoId = ? "
				+ "AND tipoConvocatoria = ? AND estado = ?", cursoId, anioAcademicoId, tipoConvocatoria, estado).isEmpty();
	}

	/** Borra la calificación de la matrícula en el acta (sin calificar). Devuelve las filas afectadas */
	public int borrarCalificacion(Connection conn, String matriculaId, String cursoId, String anioAcademicoId,
			String tipoConvocatoria) throws SQLException {
		return update(conn, "DELETE FROM Calificacion "
				+ "WHERE matriculaId = ? AND cursoId = ? AND anioAcademicoId = ? AND tipoConvocatoria = ?",
				matriculaId, cursoId, anioAcademicoId, tipoConvocatoria);
	}

	/** Inserta o sustituye la calificación de la matrícula en el acta. Nota nula = no presentado */
	public int guardarCalificacion(Connection conn, String matriculaId, String cursoId, String anioAcademicoId,
			String tipoConvocatoria, Double nota) throws SQLException {
		return update(conn, "INSERT OR REPLACE INTO Calificacion "
				+ "(matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) VALUES (?, ?, ?, ?, ?)",
				matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota);
	}

	/**
	 * Los alumnos que entran en el acta y no tienen calificación quedan como NP (nota nula),
	 * que no consume convocatoria. Devuelve las filas insertadas.
	 */
	public int insertarNoPresentados(Connection conn, String cursoId, String anioAcademicoId, String tipoConvocatoria,
			double notaAprobado) throws SQLException {
		return update(conn, "INSERT INTO Calificacion (matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) "
				+ "SELECT mc.matriculaId, a.cursoId, a.anioAcademicoId, a.tipoConvocatoria, NULL "
				+ SQL_ALUMNOS_ACTA
				+ "WHERE a.cursoId = ? AND a.anioAcademicoId = ? AND a.tipoConvocatoria = ? "
				+ "AND NOT EXISTS (SELECT 1 FROM Calificacion ca WHERE ca.matriculaId = mc.matriculaId "
				+ "AND ca.cursoId = a.cursoId AND ca.anioAcademicoId = a.anioAcademicoId "
				+ "AND ca.tipoConvocatoria = a.tipoConvocatoria)",
				notaAprobado, cursoId, anioAcademicoId, tipoConvocatoria);
	}

	/** Cierra el acta (si sigue ABIERTA) con la fecha y el tipo de cierre indicados */
	public int cerrarActa(Connection conn, String cursoId, String anioAcademicoId, String tipoConvocatoria,
			String fechaHoy, boolean cierreAutomatico) throws SQLException {
		return update(conn, "UPDATE Acta SET estado = ?, fechaCierre = ?, cierreAutomatico = ? "
				+ "WHERE cursoId = ? AND anioAcademicoId = ? AND tipoConvocatoria = ? AND estado = ?",
				EstadoActa.CERRADA.name(), fechaHoy, cierreAutomatico ? 1 : 0, cursoId, anioAcademicoId,
				tipoConvocatoria, EstadoActa.ABIERTA.name());
	}
}
