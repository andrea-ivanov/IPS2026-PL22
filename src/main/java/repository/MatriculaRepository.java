package repository;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.gestionmatricula.FiltroMatriculasSecretaria;
import model.gestionmatricula.MatriculaSecretariaDTO;
import model.matriculacion.CalificacionDisplayDTO;
import model.matriculacion.MatriculacionDisplayDTO;
import model.matriculacion.OfertaCursoDTO;
import model.grado.GradoDisplayDTO;
import util.Database;

/**
 * Acceso a datos de la matrícula del estudiante (tablas Matricula, MatriculaCurso y Calificacion del alumno)
 * y de la oferta de grados y cursos en la que se puede matricular, y de la gestión de matrículas de Secretaría.
 * Solo SQL y mapeo de filas a DTO; las reglas de negocio están en MatriculacionModel y GestionMatriculaModel.
 * Los filtros opcionales (grado, año) se omiten si el valor es nulo o vacío.
 */
public class MatriculaRepository {
	// Listado de Secretaría: una fila por MatriculaCurso
	private static final String SQL_COLUMNAS = "SELECT mc.matriculaId, mc.cursoId, m.anioAcademicoId, a.alumnoId, a.dni, "
			+ "a.nombre || ' ' || a.apellidos AS estudiante, a.email, g.gradoId, g.nombre AS grado, c.nombre AS curso, "
			+ "COALESCE((SELECT GROUP_CONCAT(mg.grupoCodigo, ', ') FROM MatriculaGrupo mg "
			+ "WHERE mg.matriculaId = mc.matriculaId AND mg.cursoId = mc.cursoId), 'Sin asignar') AS grupo, "
			+ "mc.estado, m.estado AS estadoMatricula, CAST(mc.importe AS REAL) AS importe, mc.metodoPago, mc.fechaPago, "
			+ "mc.pagoValidado, mc.motivoCancelacion, mc.fechaCancelacion, an.fechaInicio AS fechaInicioAnio, "
			+ "(SELECT COUNT(DISTINCT gr.tipo) FROM Grupo gr WHERE gr.cursoId = mc.cursoId) "
			+ "- (SELECT COUNT(*) FROM MatriculaGrupo mg2 WHERE mg2.matriculaId = mc.matriculaId "
			+ "AND mg2.cursoId = mc.cursoId) AS gruposPendientes ";
	private static final String SQL_FROM = "FROM MatriculaCurso mc "
			+ "JOIN Matricula m ON mc.matriculaId = m.matriculaId "
			+ "JOIN AnioAcademico an ON m.anioAcademicoId = an.anioAcademicoId "
			+ "JOIN Alumno a ON m.alumnoId = a.alumnoId "
			+ "JOIN Grado g ON mc.gradoId = g.gradoId "
			+ "JOIN Curso c ON mc.cursoId = c.cursoId ";
	private static final String SQL_SELECT = SQL_COLUMNAS + SQL_FROM;
	private static final String SQL_ORDEN = "ORDER BY m.anioAcademicoId DESC, mc.matriculaId, mc.cursoId ";

	// Tipos de grupo del curso que aún no tiene asignados (la misma cuenta que gruposPendientes)
	private static final String SQL_GRUPOS_PENDIENTES = "((SELECT COUNT(DISTINCT gr.tipo) FROM Grupo gr "
			+ "WHERE gr.cursoId = mc.cursoId) - (SELECT COUNT(*) FROM MatriculaGrupo mg2 "
			+ "WHERE mg2.matriculaId = mc.matriculaId AND mg2.cursoId = mc.cursoId))";

	private final Database db;

	public MatriculaRepository(Database db) {
		this.db = db;
	}

	private static boolean informado(String valor) {
		return valor != null && !valor.isEmpty();
	}

	/** Primer valor de la consulta como entero (0 si no hay filas o es nulo) */
	private int contar(String sql, Object... params) {
		List<Object[]> rows = db.executeQueryArray(sql, params);
		if (rows != null && !rows.isEmpty() && rows.get(0)[0] != null) {
			return ((Number) rows.get(0)[0]).intValue();
		}
		return 0;
	}

	// =========================================================================
	// Consultas del alumno
	// =========================================================================

	/** Matrículas del alumno con el nombre del grado */
	public List<MatriculacionDisplayDTO> findMatriculasAlumno(String alumnoId) {
		String sql = "SELECT m.matriculaId AS id, "
				+ "       g.nombre AS grado, "
				+ "       m.estado AS estado, "
				+ "       m.fechaSolicitud AS fecha "
				+ "FROM Matricula m "
				+ "JOIN Grado g ON m.gradoId = g.gradoId "
				+ "WHERE m.alumnoId = ?";
		return db.executeQueryPojo(MatriculacionDisplayDTO.class, sql, alumnoId);
	}

	/** Grados en los que está o ha estado matriculado el alumno, con formato "G001 - Nombre" */
	public List<Object[]> findGradosAlumno(String alumnoId) {
		String sql = "SELECT DISTINCT g.gradoId || ' - ' || g.nombre "
				+ "FROM Matricula m "
				+ "JOIN Grado g ON m.gradoId = g.gradoId "
				+ "WHERE m.alumnoId = ? "
				+ "ORDER BY g.gradoId";
		return db.executeQueryArray(sql, alumnoId);
	}

	/**
	 * Asignaturas matriculadas por el alumno (una fila por MatriculaCurso) con el número de vez que se matricula
	 * de cada una (consulta de referencia de la HU 35232). Opcionalmente solo las del grado indicado.
	 */
	public List<MatriculacionDisplayDTO> findCursosMatriculados(String alumnoId, String gradoId) {
		String sql = "SELECT m.matriculaId AS id, "
				+ "       m.anioAcademicoId, "
				+ "       c.nombre AS curso, "
				+ "       gc.anio, "
				+ "       mc.estado, "
				+ "       (SELECT COUNT(*) FROM MatriculaCurso mc2 "
				+ "        JOIN Matricula m2 ON m2.matriculaId = mc2.matriculaId "
				+ "        WHERE m2.alumnoId = m.alumnoId AND mc2.cursoId = mc.cursoId "
				+ "          AND m2.anioAcademicoId <= m.anioAcademicoId) AS numMatricula "
				+ "FROM MatriculaCurso mc "
				+ "JOIN Matricula m ON m.matriculaId = mc.matriculaId "
				+ "JOIN Curso c ON c.cursoId = mc.cursoId "
				+ "JOIN GradoCurso gc ON gc.gradoId = mc.gradoId AND gc.cursoId = mc.cursoId "
				+ "WHERE m.alumnoId = ? ";
		String orden = "ORDER BY gc.anio ASC, numMatricula DESC, c.nombre ASC";
		if (informado(gradoId)) {
			return db.executeQueryPojo(MatriculacionDisplayDTO.class, sql + "AND m.gradoId = ? " + orden,
					alumnoId, gradoId);
		}
		return db.executeQueryPojo(MatriculacionDisplayDTO.class, sql + orden, alumnoId);
	}

	/**
	 * Créditos superados en cada matrícula del alumno, de la más reciente a la más antigua.
	 * Columnas: matriculaId, anioAcademicoId, estado y créditos superados (nota >= notaAprobado).
	 */
	public List<Object[]> findCreditosSuperadosPorMatricula(String alumnoId, String gradoId, double notaAprobado) {
		String sql = "SELECT m.matriculaId, m.anioAcademicoId, m.estado, "
				+ "       COALESCE(SUM(c.creditos), 0) AS creditosSuperados "
				+ "FROM Matricula m "
				+ "LEFT JOIN (SELECT DISTINCT matriculaId, cursoId FROM Calificacion WHERE nota >= ?) a "
				+ "       ON a.matriculaId = m.matriculaId "
				+ "LEFT JOIN Curso c ON c.cursoId = a.cursoId "
				+ "WHERE m.alumnoId = ? "
				+ (informado(gradoId) ? "AND m.gradoId = ? " : "")
				+ "GROUP BY m.matriculaId, m.anioAcademicoId, m.estado "
				+ "ORDER BY m.anioAcademicoId DESC";
		return informado(gradoId)
				? db.executeQueryArray(sql, notaAprobado, alumnoId, gradoId)
				: db.executeQueryArray(sql, notaAprobado, alumnoId);
	}

	/**
	 * Años matriculados en el grado y créditos superados acumulados (cada asignatura cuenta una vez).
	 * Columnas: aniosEnGrado y creditosTotales.
	 */
	public List<Object[]> findAniosYCreditosTotales(String alumnoId, String gradoId, double notaAprobado) {
		String sql = "SELECT COUNT(*) AS aniosEnGrado, "
				+ "       COALESCE((SELECT SUM(c.creditos) "
				+ "                 FROM (SELECT DISTINCT ca.cursoId "
				+ "                       FROM Calificacion ca "
				+ "                       JOIN Matricula m2 ON m2.matriculaId = ca.matriculaId "
				+ "                       WHERE m2.alumnoId = ? "
				+ (informado(gradoId) ? "AND m2.gradoId = ? " : "")
				+ "                         AND ca.nota >= ?) s "
				+ "                 JOIN Curso c ON c.cursoId = s.cursoId), 0) AS creditosTotales "
				+ "FROM Matricula "
				+ "WHERE alumnoId = ? "
				+ (informado(gradoId) ? "AND gradoId = ?" : "");
		return informado(gradoId)
				? db.executeQueryArray(sql, alumnoId, gradoId, notaAprobado, alumnoId, gradoId)
				: db.executeQueryArray(sql, alumnoId, notaAprobado, alumnoId);
	}

	/** Calificaciones del alumno en todas sus convocatorias. Opcionalmente solo las del grado indicado. */
	public List<CalificacionDisplayDTO> findCalificaciones(String alumnoId, String gradoId) {
		String sql = "SELECT ca.anioAcademicoId, "
				+ "       ca.cursoId, "
				+ "       c.nombre AS curso, "
				+ "       ca.tipoConvocatoria, "
				+ "       ca.nota "
				+ "FROM Calificacion ca "
				+ "JOIN Matricula m ON m.matriculaId = ca.matriculaId "
				+ "JOIN Curso c ON c.cursoId = ca.cursoId "
				+ "WHERE m.alumnoId = ? ";
		String orden = "ORDER BY ca.anioAcademicoId DESC, c.nombre ASC, ca.tipoConvocatoria ASC";
		if (informado(gradoId)) {
			return db.executeQueryPojo(CalificacionDisplayDTO.class, sql + "AND m.gradoId = ? " + orden,
					alumnoId, gradoId);
		}
		return db.executeQueryPojo(CalificacionDisplayDTO.class, sql + orden, alumnoId);
	}

	// =========================================================================
	// Comprobaciones de la solicitud de matrícula (HU 35235)
	// =========================================================================

	/** Si el alumno tiene una matrícula no cancelada en el grado y año indicados */
	public boolean existeMatriculaNoCancelada(String alumnoId, String gradoId, String anioAcademicoId) {
		return contar("SELECT COUNT(*) FROM Matricula "
				+ "WHERE alumnoId = ? AND gradoId = ? AND anioAcademicoId = ? "
				+ "  AND estado != 'CANCELADA'", alumnoId, gradoId, anioAcademicoId) > 0;
	}

	/** Convocatorias con nota (no NP) del alumno en la asignatura, en todos sus años y grados */
	public int contarConvocatoriasConsumidas(String alumnoId, String cursoId) {
		return contar("SELECT COUNT(*) AS convocatoriasConsumidas "
				+ "FROM Calificacion ca "
				+ "JOIN Matricula m ON m.matriculaId = ca.matriculaId "
				+ "WHERE m.alumnoId = ? AND ca.cursoId = ? "
				+ "  AND ca.nota IS NOT NULL", alumnoId, cursoId);
	}

	/** Asignaturas SOLICITADAS sin pagar del alumno en años académicos ya iniciados */
	public int contarCursosSinPagarIniciados(String alumnoId) {
		return contar("SELECT COUNT(*) FROM MatriculaCurso mc "
				+ "JOIN Matricula m ON m.matriculaId = mc.matriculaId "
				+ "JOIN AnioAcademico a ON a.anioAcademicoId = m.anioAcademicoId "
				+ "WHERE m.alumnoId = ? AND mc.estado = 'SOLICITADA' "
				+ "  AND (mc.metodoPago IS NULL OR mc.pagoValidado = 0) "
				+ "  AND a.fechaInicio <= date('now')", alumnoId);
	}

	/** Cancelaciones por IMPAGO del alumno (de la matrícula entera o de alguna asignatura) */
	public int contarCancelacionesPorImpago(String alumnoId) {
		return contar("SELECT COUNT(*) FROM Matricula m "
				+ "LEFT JOIN MatriculaCurso mc ON mc.matriculaId = m.matriculaId "
				+ "WHERE m.alumnoId = ? AND (m.motivoCancelacion = 'IMPAGO' OR mc.motivoCancelacion = 'IMPAGO')",
				alumnoId);
	}

	// =========================================================================
	// Oferta de grados y cursos (HU 35235)
	// =========================================================================

	/** Grados VIGENTE o PENDIENTE_RENOVACION, por nombre */
	public List<GradoDisplayDTO> findGradosOfertados() {
		String sql = "SELECT gradoId AS id, nombre, facultad, ramaConocimiento AS rama, estado "
				+ "FROM Grado "
				+ "WHERE estado IN ('VIGENTE', 'PENDIENTE_RENOVACION') "
				+ "ORDER BY nombre ASC";
		return db.executeQueryPojo(GradoDisplayDTO.class, sql);
	}

	/** Cursos ACTIVO del grado, por año y nombre (sin precio ni convocatorias, que calcula el modelo) */
	public List<OfertaCursoDTO> findCursosOfertados(String gradoId) {
		String sql = "SELECT c.cursoId, c.nombre, gc.anio, c.creditos, gc.itinerario "
				+ "FROM GradoCurso gc "
				+ "JOIN Curso c ON c.cursoId = gc.cursoId "
				+ "WHERE gc.gradoId = ? AND gc.estado = 'ACTIVO' "
				+ "ORDER BY gc.anio ASC, c.nombre ASC";
		return db.executeQueryPojo(OfertaCursoDTO.class, sql, gradoId);
	}

	/** dni, nombre, apellidos y email del alumno; nulo si no existe */
	public Object[] findDatosAlumno(String alumnoId) {
		List<Object[]> rows = db.executeQueryArray("SELECT dni, nombre, apellidos, email FROM Alumno WHERE alumnoId = ?",
				alumnoId);
		return rows != null && !rows.isEmpty() ? rows.get(0) : null;
	}

	/** Nombre del grado; nulo si no existe */
	public String findNombreGrado(String gradoId) {
		List<Object[]> rows = db.executeQueryArray("SELECT nombre FROM Grado WHERE gradoId = ?", gradoId);
		return rows != null && !rows.isEmpty() && rows.get(0)[0] != null ? rows.get(0)[0].toString() : null;
	}

	// =========================================================================
	// Registro de la solicitud (dentro de la transacción del modelo)
	// =========================================================================

	/** Número de la siguiente matrícula: 2101 si la última es M02100 (1 si no hay ninguna) */
	public int siguienteNumeroMatricula(Connection conn) throws SQLException {
		Number max = new QueryRunner().query(conn,
				"SELECT MAX(CAST(SUBSTR(matriculaId, 2) AS INTEGER)) FROM Matricula WHERE matriculaId LIKE 'M%'",
				new ScalarHandler<Number>());
		return max == null ? 1 : max.intValue() + 1;
	}

	/** Inserta la cabecera de la matrícula en estado SOLICITADA */
	public void insertarMatricula(Connection conn, String matriculaId, String alumnoId, String gradoId,
			String anioAcademicoId, String fechaSolicitud) throws SQLException {
		update(conn, "INSERT INTO Matricula (matriculaId, alumnoId, gradoId, anioAcademicoId, fechaSolicitud, estado, motivoCancelacion, fechaCancelacion) "
				+ "VALUES (?, ?, ?, ?, ?, 'SOLICITADA', NULL, NULL)",
				matriculaId, alumnoId, gradoId, anioAcademicoId, fechaSolicitud);
	}

	/** Inserta una asignatura de la matrícula */
	public void insertarCurso(Connection conn, String matriculaId, String gradoId, String cursoId, String estado,
			double importe, String metodoPago, String fechaPago, int pagoValidado) throws SQLException {
		update(conn, "INSERT INTO MatriculaCurso (matriculaId, gradoId, cursoId, estado, motivoCancelacion, fechaCancelacion, importe, metodoPago, fechaPago, pagoValidado) "
				+ "VALUES (?, ?, ?, ?, NULL, NULL, ?, ?, ?, ?)",
				matriculaId, gradoId, cursoId, estado, importe, metodoPago, fechaPago, pagoValidado);
	}

	// =========================================================================
	// Alumnos de un curso (AdminOfertaView)
	// =========================================================================

	/** Alumnos matriculados en el curso, opcionalmente solo en el año académico indicado */
	public List<MatriculacionDisplayDTO> findAlumnosCurso(String cursoId, String anioAcademicoId) {
		String sql = "SELECT a.alumnoId AS id, " +
				"       a.nombre || ' ' || a.apellidos AS nombreAlumno, " +
				"       mc.cursoId AS idCurso, " +
				"       a.email AS email, " +
				"       mc.estado AS estado " +
				"FROM MatriculaCurso mc " +
				"JOIN Matricula m ON mc.matriculaId = m.matriculaId " +
				"JOIN Alumno a ON m.alumnoId = a.alumnoId " +
				"WHERE mc.cursoId = ? ";
		String orden = "ORDER BY a.alumnoId";
		if (informado(anioAcademicoId)) {
			return db.executeQueryPojo(MatriculacionDisplayDTO.class, sql + "AND m.anioAcademicoId = ? " + orden,
					cursoId, anioAcademicoId);
		}
		return db.executeQueryPojo(MatriculacionDisplayDTO.class, sql + orden, cursoId);
	}

	// =========================================================================
	// Secretaría: consulta de matriculaciones (HU 35226)
	// =========================================================================

	/** Todos los cursos matriculados, del año académico más reciente al más antiguo */
	public List<MatriculaSecretariaDTO> findMatriculasSecretaria() {
		return db.executeQueryPojo(MatriculaSecretariaDTO.class, SQL_SELECT + SQL_ORDEN);
	}

	/** Número de cursos matriculados que cumplen los filtros */
	public int contarMatriculasSecretaria(FiltroMatriculasSecretaria filtro) {
		List<Object> params = new ArrayList<>();
		String where = whereMatriculas(filtro, params);
		return ((Number) db.executeQueryArray("SELECT COUNT(*) " + SQL_FROM + where, params.toArray()).get(0)[0])
				.intValue();
	}

	/**
	 * Una página de los cursos matriculados que cumplen los filtros, en el mismo orden que findMatriculasSecretaria.
	 * Primero se buscan solo las claves de la página y después se calculan los datos (grupos, pendientes...)
	 * de esas filas, para no calcularlos para todo el listado.
	 */
	public List<MatriculaSecretariaDTO> findMatriculasSecretaria(FiltroMatriculasSecretaria filtro, int offset,
			int limite) {
		List<Object> params = new ArrayList<>();
		String where = whereMatriculas(filtro, params);
		params.add(limite);
		params.add(offset);
		return db.executeQueryPojo(MatriculaSecretariaDTO.class, SQL_SELECT
				+ "WHERE (mc.matriculaId, mc.cursoId) IN (SELECT mc.matriculaId, mc.cursoId " + SQL_FROM + where
				+ SQL_ORDEN + "LIMIT ? OFFSET ?) " + SQL_ORDEN, params.toArray());
	}

	/** Años académicos que tienen matrículas, del más reciente al más antiguo */
	public List<String> findAniosConMatriculas() {
		return columna("SELECT DISTINCT anioAcademicoId FROM Matricula ORDER BY anioAcademicoId DESC");
	}

	/** Nombres de los grados que tienen cursos matriculados */
	public List<String> findGradosConMatriculas() {
		return columna("SELECT DISTINCT g.nombre FROM MatriculaCurso mc JOIN Grado g ON g.gradoId = mc.gradoId "
				+ "ORDER BY g.nombre");
	}

	/** Nombres de los cursos que tienen alguna matrícula */
	public List<String> findCursosConMatriculas() {
		return columna("SELECT DISTINCT c.nombre FROM MatriculaCurso mc JOIN Curso c ON c.cursoId = mc.cursoId "
				+ "ORDER BY c.nombre");
	}

	/** Detalle de un curso matriculado; nulo si no existe */
	public MatriculaSecretariaDTO findMatriculaSecretaria(String matriculaId, String cursoId) {
		List<MatriculaSecretariaDTO> result = db.executeQueryPojo(MatriculaSecretariaDTO.class,
				SQL_SELECT + "WHERE mc.matriculaId = ? AND mc.cursoId = ?", matriculaId, cursoId);
		return result.isEmpty() ? null : result.get(0);
	}

	/** Cursos matriculados asignados a un grupo en un año académico (en cualquier estado) */
	public List<MatriculaSecretariaDTO> findMatriculasGrupo(String cursoId, String grupoCodigo, String anioAcademicoId) {
		return db.executeQueryPojo(MatriculaSecretariaDTO.class, SQL_SELECT
				+ "WHERE mc.cursoId = ? AND m.anioAcademicoId = ? AND EXISTS (SELECT 1 FROM MatriculaGrupo mg3 "
				+ "WHERE mg3.matriculaId = mc.matriculaId AND mg3.cursoId = mc.cursoId AND mg3.grupoCodigo = ?) "
				+ "ORDER BY a.apellidos, a.nombre", cursoId, anioAcademicoId, grupoCodigo);
	}

	/** Condiciones WHERE de los filtros del listado; añade a params los valores de sus parámetros */
	private static String whereMatriculas(FiltroMatriculasSecretaria filtro, List<Object> params) {
		StringBuilder where = new StringBuilder("WHERE 1 = 1 ");
		if (hayValor(filtro.getEstudiante())) {
			String patron = "%" + filtro.getEstudiante().trim().toLowerCase() + "%";
			where.append("AND (LOWER(a.nombre || ' ' || a.apellidos) LIKE ? OR LOWER(a.dni) LIKE ?) ");
			params.add(patron);
			params.add(patron);
		}
		if (hayValor(filtro.getAnioAcademicoId())) {
			where.append("AND m.anioAcademicoId = ? ");
			params.add(filtro.getAnioAcademicoId());
		}
		if (hayValor(filtro.getGrado())) {
			where.append("AND g.nombre = ? ");
			params.add(filtro.getGrado());
		}
		if (hayValor(filtro.getCurso())) {
			where.append("AND c.nombre = ? ");
			params.add(filtro.getCurso());
		}
		if (hayValor(filtro.getGrupo())) {
			where.append("AND EXISTS (SELECT 1 FROM MatriculaGrupo mgf WHERE mgf.matriculaId = mc.matriculaId "
					+ "AND mgf.cursoId = mc.cursoId AND UPPER(mgf.grupoCodigo) = UPPER(?)) ");
			params.add(filtro.getGrupo().trim());
		}
		if (hayValor(filtro.getEstado())) {
			where.append("AND mc.estado = ? ");
			params.add(filtro.getEstado());
		}
		switch (filtro.getSituacion()) {
			case PAGO_PENDIENTE:
				where.append("AND mc.estado = 'SOLICITADA' AND mc.metodoPago IS NOT NULL AND mc.pagoValidado = 0 ");
				break;
			case SIN_PAGAR:
				where.append("AND mc.estado = 'SOLICITADA' AND mc.metodoPago IS NULL AND an.fechaInicio <= ? ");
				params.add(filtro.getFechaHoy());
				break;
			case FALTAN_GRUPOS:
				where.append("AND mc.estado IN ('PAGADA', 'ACTIVA') AND " + SQL_GRUPOS_PENDIENTES + " > 0 ");
				break;
			default:
				break;
		}
		return where.toString();
	}

	/** Valor informado en un filtro de Secretaría (sin contar los espacios) */
	private static boolean hayValor(String filtro) {
		return filtro != null && !filtro.trim().isEmpty();
	}

	/** Primera columna de cada fila como texto */
	private List<String> columna(String sql, Object... params) {
		List<String> valores = new ArrayList<>();
		for (Object[] fila : db.executeQueryArray(sql, params)) {
			valores.add(fila[0].toString());
		}
		return valores;
	}

	// =========================================================================
	// Secretaría: ciclo de vida (HU 35229), dentro de la transacción del modelo
	// =========================================================================

	/** El curso SOLICITADO pasa a PAGADA con el pago validado */
	public void validarPagoCurso(Connection conn, String matriculaId, String cursoId, String metodoPago,
			String fechaPago) throws SQLException {
		update(conn, "UPDATE MatriculaCurso SET estado = 'PAGADA', metodoPago = ?, fechaPago = ?, pagoValidado = 1 "
				+ "WHERE matriculaId = ? AND cursoId = ? AND estado = 'SOLICITADA'",
				metodoPago, fechaPago, matriculaId, cursoId);
	}

	/** Cambia el método de pago (nulo para mantener el actual) y la fecha de pago del curso */
	public void actualizarPagoCurso(Connection conn, String matriculaId, String cursoId, String metodoPago,
			String fechaPago) throws SQLException {
		update(conn, "UPDATE MatriculaCurso SET metodoPago = COALESCE(?, metodoPago), fechaPago = ? "
				+ "WHERE matriculaId = ? AND cursoId = ?", metodoPago, fechaPago, matriculaId, cursoId);
	}

	/** Cancela todos los cursos de la matrícula que no estén cancelados ni finalizados */
	public void cancelarCursosAbiertos(Connection conn, String matriculaId, String motivo, String fechaCancelacion)
			throws SQLException {
		update(conn, "UPDATE MatriculaCurso SET estado = 'CANCELADA', motivoCancelacion = ?, fechaCancelacion = ? "
				+ "WHERE matriculaId = ? AND estado NOT IN ('CANCELADA', 'FINALIZADA')", motivo, fechaCancelacion,
				matriculaId);
	}

	/** Cancela un curso de la matrícula */
	public void cancelarCurso(Connection conn, String matriculaId, String cursoId, String motivo,
			String fechaCancelacion) throws SQLException {
		update(conn, "UPDATE MatriculaCurso SET estado = 'CANCELADA', motivoCancelacion = ?, fechaCancelacion = ? "
				+ "WHERE matriculaId = ? AND cursoId = ?", motivo, fechaCancelacion, matriculaId, cursoId);
	}

	/** Número de cursos de la matrícula que no están cancelados */
	public int contarCursosNoCancelados(Connection conn, String matriculaId) throws SQLException {
		Number restantes = new QueryRunner().query(conn,
				"SELECT count(*) FROM MatriculaCurso WHERE matriculaId = ? AND estado <> 'CANCELADA'",
				new ScalarHandler<Number>(), matriculaId);
		return restantes.intValue();
	}

	/** La cabecera de la matrícula pasa a CANCELADA */
	public void cancelarMatricula(Connection conn, String matriculaId, String motivo, String fechaCancelacion)
			throws SQLException {
		update(conn, "UPDATE Matricula SET estado = 'CANCELADA', motivoCancelacion = ?, fechaCancelacion = ? "
				+ "WHERE matriculaId = ?", motivo, fechaCancelacion, matriculaId);
	}

	/**
	 * Los cursos PAGADOS de cualquier año ya iniciado en la fecha pasan a ACTIVA.
	 * @return número de cursos activados
	 */
	public int activarCursosPagadosIniciados(Connection conn, String fechaHoy) throws SQLException {
		return update(conn, "UPDATE MatriculaCurso SET estado = 'ACTIVA' WHERE estado = 'PAGADA' "
				+ "AND matriculaId IN (SELECT m.matriculaId FROM Matricula m "
				+ "JOIN AnioAcademico an ON m.anioAcademicoId = an.anioAcademicoId "
				+ "WHERE an.fechaInicio <= ?)", fechaHoy);
	}

	/** Las matrículas SOLICITADAS con algún curso ACTIVO pasan a ACTIVA */
	public void activarMatriculasConCursoActivo(Connection conn) throws SQLException {
		update(conn, "UPDATE Matricula SET estado = 'ACTIVA' WHERE estado = 'SOLICITADA' "
				+ "AND EXISTS (SELECT 1 FROM MatriculaCurso mc WHERE mc.matriculaId = Matricula.matriculaId "
				+ "AND mc.estado = 'ACTIVA')");
	}

	/**
	 * Los cursos ACTIVOS de los años terminados antes de la fecha pasan a FINALIZADA.
	 * @return número de cursos finalizados
	 */
	public int finalizarCursosActivosTerminados(Connection conn, String fechaHoy) throws SQLException {
		return update(conn, "UPDATE MatriculaCurso SET estado = 'FINALIZADA' WHERE estado = 'ACTIVA' "
				+ "AND matriculaId IN (SELECT m.matriculaId FROM Matricula m "
				+ "JOIN AnioAcademico an ON m.anioAcademicoId = an.anioAcademicoId WHERE an.fechaFin < ?)", fechaHoy);
	}

	/** Las matrículas ACTIVAS de los años terminados antes de la fecha pasan a FINALIZADA */
	public void finalizarMatriculasActivasTerminadas(Connection conn, String fechaHoy) throws SQLException {
		update(conn, "UPDATE Matricula SET estado = 'FINALIZADA' WHERE estado = 'ACTIVA' "
				+ "AND anioAcademicoId IN (SELECT anioAcademicoId FROM AnioAcademico WHERE fechaFin < ?)", fechaHoy);
	}
}
