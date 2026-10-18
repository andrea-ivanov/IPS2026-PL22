package model.matriculacion;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import util.Database;
import util.UnexpectedException;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Lógica de negocio de la Secretaría sobre las matrículas.
 * HU 35226: consulta de matriculaciones de cursos, grados y grupos
 * HU 35229: ciclo de vida (aceptar solicitudes, modificar, asignar grupos, cancelar, activar al inicio
 * del año y finalizar al terminarlo)
 * Las operaciones que cambian el estado actualizan Matricula y MatriculaCurso en la misma transacción.
 */
public class GestionMatriculaModel {
	public static final List<String> MOTIVOS_CANCELACION = Arrays.asList("IMPAGO", "FUERZA_MAYOR", "INCOMPATIBILIDAD");
	public static final List<String> METODOS_PAGO = Arrays.asList("TARJETA", "TRANSFERENCIA", "CARTA_PAGO");

	private static final String SQL_SELECT = "SELECT mc.matriculaId, mc.cursoId, m.anioAcademicoId, a.alumnoId, a.dni, "
			+ "a.nombre || ' ' || a.apellidos AS estudiante, a.email, g.gradoId, g.nombre AS grado, c.nombre AS curso, "
			+ "COALESCE((SELECT GROUP_CONCAT(mg.grupoCodigo, ', ') FROM MatriculaGrupo mg "
			+ "WHERE mg.matriculaId = mc.matriculaId AND mg.cursoId = mc.cursoId), 'Sin asignar') AS grupo, "
			+ "mc.estado, m.estado AS estadoMatricula, CAST(mc.importe AS REAL) AS importe, mc.metodoPago, mc.fechaPago, "
			+ "mc.pagoValidado, mc.motivoCancelacion, mc.fechaCancelacion, an.fechaInicio AS fechaInicioAnio, "
			+ "(SELECT COUNT(DISTINCT gr.tipo) FROM Grupo gr WHERE gr.cursoId = mc.cursoId) "
			+ "- (SELECT COUNT(*) FROM MatriculaGrupo mg2 WHERE mg2.matriculaId = mc.matriculaId "
			+ "AND mg2.cursoId = mc.cursoId) AS gruposPendientes "
			+ "FROM MatriculaCurso mc "
			+ "JOIN Matricula m ON mc.matriculaId = m.matriculaId "
			+ "JOIN AnioAcademico an ON m.anioAcademicoId = an.anioAcademicoId "
			+ "JOIN Alumno a ON m.alumnoId = a.alumnoId "
			+ "JOIN Grado g ON mc.gradoId = g.gradoId "
			+ "JOIN Curso c ON mc.cursoId = c.cursoId ";

	private Database db;

	public GestionMatriculaModel(Database db) {
		this.db = db;
	}

	// =========================================================================
	// HU 35226: consulta
	// =========================================================================

	/**
	 * Listado de todos los cursos matriculados, del año académico más reciente al más antiguo.
	 */
	public List<MatriculaSecretariaDTO> getMatriculas() {
		return db.executeQueryPojo(MatriculaSecretariaDTO.class,
				SQL_SELECT + "ORDER BY m.anioAcademicoId DESC, mc.matriculaId, mc.cursoId");
	}

	/**
	 * Detalle de un curso matriculado.
	 * @throws ResourceNotFoundException si no existe
	 */
	public MatriculaSecretariaDTO getMatricula(String matriculaId, String cursoId) {
		List<MatriculaSecretariaDTO> result = db.executeQueryPojo(MatriculaSecretariaDTO.class,
				SQL_SELECT + "WHERE mc.matriculaId = ? AND mc.cursoId = ?", matriculaId, cursoId);
		if (result.isEmpty()) {
			throw new ResourceNotFoundException("No existe la matrícula " + matriculaId + " en el curso " + cursoId);
		}
		return result.get(0);
	}

	/**
	 * Años académicos, del más reciente al más antiguo.
	 */
	public List<String> getAniosAcademicos() {
		List<String> anios = new ArrayList<>();
		for (Object[] fila : db.executeQueryArray("SELECT anioAcademicoId FROM AnioAcademico ORDER BY anioAcademicoId DESC")) {
			anios.add(fila[0].toString());
		}
		return anios;
	}

	/**
	 * Año académico en curso: el último que ya ha empezado en la fecha indicada,
	 * o el más antiguo si todavía no ha empezado ninguno. Nulo si no hay años.
	 */
	public String getAnioAcademicoActual(String fechaHoy) {
		List<Object[]> result = db.executeQueryArray("SELECT anioAcademicoId FROM AnioAcademico "
				+ "ORDER BY CASE WHEN fechaInicio <= ? THEN 0 ELSE 1 END, "
				+ "CASE WHEN fechaInicio <= ? THEN fechaInicio END DESC, fechaInicio LIMIT 1", fechaHoy, fechaHoy);
		return result.isEmpty() ? null : result.get(0)[0].toString();
	}

	// =========================================================================
	// HU 35226: consulta de grupos
	// =========================================================================

	/**
	 * Todos los grupos con los grados de su curso, sus profesores y las plazas ocupadas en el año indicado.
	 * Solo ocupan plaza los cursos PAGADOS o ACTIVOS.
	 */
	public List<GrupoConsultaDTO> getGruposConsulta(String anioAcademicoId) {
		return db.executeQueryPojo(GrupoConsultaDTO.class, "SELECT g.cursoId, c.nombre AS curso, "
				+ "COALESCE((SELECT GROUP_CONCAT(gr.nombre, '; ') FROM GradoCurso gc JOIN Grado gr ON gr.gradoId = gc.gradoId "
				+ "WHERE gc.cursoId = g.cursoId), '-') AS grados, "
				+ "g.grupoCodigo, g.tipo, g.capacidad, "
				+ "(SELECT COUNT(*) FROM MatriculaGrupo mg "
				+ "JOIN MatriculaCurso mc ON mc.matriculaId = mg.matriculaId AND mc.cursoId = mg.cursoId "
				+ "JOIN Matricula m ON m.matriculaId = mc.matriculaId "
				+ "WHERE mg.cursoId = g.cursoId AND mg.grupoCodigo = g.grupoCodigo AND mc.estado IN ('PAGADA', 'ACTIVA') "
				+ "AND m.anioAcademicoId = ?) AS ocupadas, "
				+ "COALESCE((SELECT GROUP_CONCAT(p.nombre || ' ' || p.apellidos, ', ') FROM Imparte i "
				+ "JOIN Profesor p ON p.profesorId = i.profesorId "
				+ "WHERE i.cursoId = g.cursoId AND i.grupoCodigo = g.grupoCodigo), 'Sin asignar') AS profesores "
				+ "FROM Grupo g JOIN Curso c ON c.cursoId = g.cursoId "
				+ "ORDER BY c.nombre, g.tipo DESC, g.grupoCodigo", anioAcademicoId);
	}

	/**
	 * Cursos matriculados asignados a un grupo en un año académico (en cualquier estado).
	 */
	public List<MatriculaSecretariaDTO> getMatriculasGrupo(String cursoId, String grupoCodigo, String anioAcademicoId) {
		return db.executeQueryPojo(MatriculaSecretariaDTO.class, SQL_SELECT
				+ "WHERE mc.cursoId = ? AND m.anioAcademicoId = ? AND EXISTS (SELECT 1 FROM MatriculaGrupo mg3 "
				+ "WHERE mg3.matriculaId = mc.matriculaId AND mg3.cursoId = mc.cursoId AND mg3.grupoCodigo = ?) "
				+ "ORDER BY a.apellidos, a.nombre", cursoId, anioAcademicoId, grupoCodigo);
	}

	// =========================================================================
	// HU 35229: ciclo de vida
	// =========================================================================

	/**
	 * Mantiene al día el estado de todas las matrículas según la fecha: activa lo pagado de los años
	 * ya iniciados y finaliza lo activo de los años terminados. Se ejecuta al arrancar la aplicación.
	 * @param fechaHoy fecha de referencia en formato yyyy-MM-dd
	 * @return número de cursos que han cambiado de estado
	 */
	public int actualizarCicloVida(String fechaHoy) {
		return enTransaccion(conn -> activarMatriculasIniciadas(conn, fechaHoy) + finalizarMatriculasTerminadas(conn, fechaHoy));
	}

	/**
	 * Al iniciar el año académico los cursos PAGADOS pasan a ACTIVA, y la matrícula
	 * pasa a ACTIVA si tiene algún curso activo. Los SOLICITADOS (sin pagar) no cambian.
	 * @param fechaHoy fecha de referencia en formato yyyy-MM-dd
	 * @return número de cursos activados
	 */
	public int activarMatriculasIniciadas(String fechaHoy) {
		return enTransaccion(conn -> activarMatriculasIniciadas(conn, fechaHoy));
	}

	/**
	 * Al terminar el año académico los cursos ACTIVOS y sus matrículas activas pasan a FINALIZADA.
	 * @param fechaHoy fecha de referencia en formato yyyy-MM-dd
	 * @return número de cursos finalizados
	 */
	public int finalizarMatriculasTerminadas(String fechaHoy) {
		return enTransaccion(conn -> finalizarMatriculasTerminadas(conn, fechaHoy));
	}

	// =========================================================================
	// HU 35229: grupos
	// =========================================================================

	/**
	 * Grupos del curso con las plazas ocupadas en el año académico de la matrícula.
	 * Solo ocupan plaza los cursos PAGADOS o ACTIVOS.
	 */
	public List<GrupoPlazasDTO> getGrupos(String matriculaId, String cursoId) {
		return db.executeQueryPojo(GrupoPlazasDTO.class, "SELECT g.grupoCodigo, g.tipo, g.capacidad, "
				+ "(SELECT COUNT(*) FROM MatriculaGrupo mg "
				+ "JOIN MatriculaCurso mc ON mc.matriculaId = mg.matriculaId AND mc.cursoId = mg.cursoId "
				+ "JOIN Matricula m ON m.matriculaId = mc.matriculaId "
				+ "WHERE mg.cursoId = g.cursoId AND mg.grupoCodigo = g.grupoCodigo AND mc.estado IN ('PAGADA', 'ACTIVA') "
				+ "AND m.anioAcademicoId = (SELECT anioAcademicoId FROM Matricula WHERE matriculaId = ?)) AS ocupadas, "
				+ "(SELECT COUNT(*) FROM MatriculaGrupo mg WHERE mg.matriculaId = ? AND mg.cursoId = g.cursoId "
				+ "AND mg.grupoCodigo = g.grupoCodigo) AS asignado "
				+ "FROM Grupo g WHERE g.cursoId = ? ORDER BY g.tipo DESC, g.grupoCodigo", matriculaId, matriculaId, cursoId);
	}

	/**
	 * Asigna (o cambia) los grupos de un curso matriculado: uno de cada tipo que tenga el curso,
	 * solo a cursos PAGADOS o ACTIVOS y sin superar la capacidad del grupo.
	 * @param grupoPorTipo tipo de grupo (TE, PA, PL) → código del grupo elegido
	 */
	public void asignarGrupos(String matriculaId, String cursoId, Map<String, String> grupoPorTipo) {
		MatriculaSecretariaDTO actual = getMatricula(matriculaId, cursoId);
		if (!"PAGADA".equals(actual.getEstado()) && !"ACTIVA".equals(actual.getEstado())) {
			throw new ValidationException("Solo se asignan grupos a cursos PAGADOS o ACTIVOS (estado actual: "
					+ actual.getEstado() + ")");
		}
		List<GrupoPlazasDTO> grupos = getGrupos(matriculaId, cursoId);
		for (GrupoPlazasDTO g : grupos) {
			if (!grupoPorTipo.containsKey(g.getTipo())) {
				throw new ValidationException("Debe asignarse un grupo de cada tipo; falta el tipo " + g.getTipo());
			}
		}
		for (Map.Entry<String, String> e : grupoPorTipo.entrySet()) {
			GrupoPlazasDTO elegido = grupos.stream().filter(g -> g.getGrupoCodigo().equals(e.getValue()))
					.findFirst().orElseThrow(() -> new ValidationException(
							"El grupo " + e.getValue() + " no existe en el curso " + actual.getCurso()));
			if (!elegido.getTipo().equals(e.getKey())) {
				throw new ValidationException("El grupo " + elegido.getGrupoCodigo() + " es de tipo "
						+ elegido.getTipo() + ", no " + e.getKey());
			}
			if (!elegido.estaAsignado() && elegido.getPlazasLibres() <= 0) {
				throw new ValidationException("El grupo " + elegido.getGrupoCodigo() + " está completo ("
						+ elegido.getCapacidad() + " plazas)");
			}
		}

		enTransaccion(conn -> {
			for (Map.Entry<String, String> e : grupoPorTipo.entrySet()) {
				update(conn, "DELETE FROM MatriculaGrupo WHERE matriculaId = ? AND cursoId = ? AND tipo = ?",
						matriculaId, cursoId, e.getKey());
				update(conn, "INSERT INTO MatriculaGrupo (matriculaId, cursoId, tipo, grupoCodigo) VALUES (?, ?, ?, ?)",
						matriculaId, cursoId, e.getKey(), e.getValue());
			}
			return 0;
		});
	}

	/**
	 * Acepta una solicitud pendiente de pago: valida el pago y el curso pasa a PAGADA
	 * (o directamente a ACTIVA si el año académico ya ha comenzado, o a FINALIZADA si ya ha terminado).
	 * @param metodoPago método de pago a registrar; puede ser nulo si la solicitud ya lo tenía
	 * @return el estado final del curso
	 */
	public String aceptarSolicitud(String matriculaId, String cursoId, String metodoPago, String fechaHoy) {
		MatriculaSecretariaDTO actual = getMatricula(matriculaId, cursoId);
		if (!"SOLICITADA".equals(actual.getEstado())) {
			throw new ValidationException("Solo se pueden aceptar solicitudes en estado SOLICITADA (estado actual: "
					+ actual.getEstado() + ")");
		}
		String metodo = metodoPago != null ? metodoPago : actual.getMetodoPago();
		validarMetodoPago(metodo);
		String fechaPago = actual.getFechaPago() != null ? actual.getFechaPago() : fechaHoy;

		enTransaccion(conn -> {
			update(conn, "UPDATE MatriculaCurso SET estado = 'PAGADA', metodoPago = ?, fechaPago = ?, pagoValidado = 1 "
					+ "WHERE matriculaId = ? AND cursoId = ? AND estado = 'SOLICITADA'",
					metodo, fechaPago, matriculaId, cursoId);
			return activarMatriculasIniciadas(conn, fechaHoy) + finalizarMatriculasTerminadas(conn, fechaHoy);
		});
		return getMatricula(matriculaId, cursoId).getEstado();
	}

	/**
	 * Modifica la información asociada a un curso matriculado: método de pago y email de contacto.
	 * El método de pago solo se puede cambiar mientras el pago no esté validado; al registrarlo en una
	 * solicitud sin pago queda con fecha de pago y pendiente de que el secretario lo valide.
	 * @param metodoPago nuevo método de pago, o nulo para mantener el actual
	 */
	public void modificarMatricula(String matriculaId, String cursoId, String metodoPago, String email, String fechaHoy) {
		MatriculaSecretariaDTO actual = getMatricula(matriculaId, cursoId);
		if ("CANCELADA".equals(actual.getEstado()) || "FINALIZADA".equals(actual.getEstado())) {
			throw new ValidationException("No se puede modificar una matrícula " + actual.getEstado());
		}
		if (metodoPago != null) {
			validarMetodoPago(metodoPago);
			if (actual.getPagoValidado() == 1 && !metodoPago.equals(actual.getMetodoPago())) {
				throw new ValidationException("El pago por " + actual.getMetodoPago()
						+ " ya está validado: no se puede cambiar su método de pago");
			}
		}
		String fechaPago = metodoPago != null && actual.getFechaPago() == null ? fechaHoy : actual.getFechaPago();
		if (email == null || !email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
			throw new ValidationException("El email introducido no es válido");
		}
		String emailLimpio = email.trim();
		List<Object[]> otros = db.executeQueryArray(
				"SELECT alumnoId FROM Alumno WHERE email = ? AND alumnoId <> ?", emailLimpio, actual.getAlumnoId());
		if (!otros.isEmpty()) {
			throw new ValidationException("El email " + emailLimpio + " ya pertenece a otro estudiante");
		}

		enTransaccion(conn -> {
			update(conn, "UPDATE MatriculaCurso SET metodoPago = COALESCE(?, metodoPago), fechaPago = ? "
					+ "WHERE matriculaId = ? AND cursoId = ?", metodoPago, fechaPago, matriculaId, cursoId);
			update(conn, "UPDATE Alumno SET email = ? WHERE alumnoId = ?", emailLimpio, actual.getAlumnoId());
			return 0;
		});
	}

	/**
	 * Cancela un curso o la matrícula completa. La matrícula no se borra: queda registrada como CANCELADA.
	 * Cancelar la matrícula cancela todos sus cursos no finalizados, y cancelar el último curso
	 * no cancelado cancela la matrícula.
	 * @param todaLaMatricula true para cancelar la matrícula completa, false solo el curso indicado
	 */
	public void cancelar(String matriculaId, String cursoId, String motivo, String fechaHoy, boolean todaLaMatricula) {
		if (motivo == null || !MOTIVOS_CANCELACION.contains(motivo)) {
			throw new ValidationException("Motivo de cancelación no válido: " + motivo);
		}
		MatriculaSecretariaDTO actual = getMatricula(matriculaId, cursoId);
		if ("CANCELADA".equals(actual.getEstado()) || "FINALIZADA".equals(actual.getEstado())) {
			throw new ValidationException("No se puede cancelar una matrícula " + actual.getEstado());
		}

		enTransaccion(conn -> {
			if (todaLaMatricula) {
				update(conn, "UPDATE MatriculaCurso SET estado = 'CANCELADA', motivoCancelacion = ?, fechaCancelacion = ? "
						+ "WHERE matriculaId = ? AND estado NOT IN ('CANCELADA', 'FINALIZADA')", motivo, fechaHoy, matriculaId);
			} else {
				update(conn, "UPDATE MatriculaCurso SET estado = 'CANCELADA', motivoCancelacion = ?, fechaCancelacion = ? "
						+ "WHERE matriculaId = ? AND cursoId = ?", motivo, fechaHoy, matriculaId, cursoId);
			}
			Number restantes = new QueryRunner().query(conn,
					"SELECT count(*) FROM MatriculaCurso WHERE matriculaId = ? AND estado <> 'CANCELADA'",
					new ScalarHandler<Number>(), matriculaId);
			if (todaLaMatricula || restantes.intValue() == 0) {
				update(conn, "UPDATE Matricula SET estado = 'CANCELADA', motivoCancelacion = ?, fechaCancelacion = ? "
						+ "WHERE matriculaId = ?", motivo, fechaHoy, matriculaId);
			}
			return 0;
		});
	}

	// =========================================================================
	// Auxiliares
	// =========================================================================

	/**
	 * Activa lo pagado de cualquier año ya iniciado, aunque ya haya terminado: así un curso pagado
	 * nunca se queda PAGADO, y a continuación finalizarMatriculasTerminadas lo pasa a FINALIZADA.
	 */
	private int activarMatriculasIniciadas(Connection conn, String fechaHoy) throws SQLException {
		int activadas = update(conn, "UPDATE MatriculaCurso SET estado = 'ACTIVA' WHERE estado = 'PAGADA' "
				+ "AND matriculaId IN (SELECT m.matriculaId FROM Matricula m "
				+ "JOIN AnioAcademico an ON m.anioAcademicoId = an.anioAcademicoId "
				+ "WHERE an.fechaInicio <= ?)", fechaHoy);
		update(conn, "UPDATE Matricula SET estado = 'ACTIVA' WHERE estado = 'SOLICITADA' "
				+ "AND EXISTS (SELECT 1 FROM MatriculaCurso mc WHERE mc.matriculaId = Matricula.matriculaId "
				+ "AND mc.estado = 'ACTIVA')");
		return activadas;
	}

	private int finalizarMatriculasTerminadas(Connection conn, String fechaHoy) throws SQLException {
		int finalizadas = update(conn, "UPDATE MatriculaCurso SET estado = 'FINALIZADA' WHERE estado = 'ACTIVA' "
				+ "AND matriculaId IN (SELECT m.matriculaId FROM Matricula m "
				+ "JOIN AnioAcademico an ON m.anioAcademicoId = an.anioAcademicoId WHERE an.fechaFin < ?)", fechaHoy);
		update(conn, "UPDATE Matricula SET estado = 'FINALIZADA' WHERE estado = 'ACTIVA' "
				+ "AND anioAcademicoId IN (SELECT anioAcademicoId FROM AnioAcademico WHERE fechaFin < ?)", fechaHoy);
		return finalizadas;
	}

	private void validarMetodoPago(String metodoPago) {
		if (metodoPago == null || !METODOS_PAGO.contains(metodoPago)) {
			throw new ValidationException("Debe indicarse un método de pago válido " + METODOS_PAGO);
		}
	}

	private int update(Connection conn, String sql, Object... params) throws SQLException {
		return new QueryRunner().update(conn, sql, params);
	}

	@FunctionalInterface
	private interface OperacionBD {
		int ejecutar(Connection conn) throws SQLException;
	}

	/** Ejecuta la operación en una única transacción: o se aplican todos los cambios o ninguno. */
	private int enTransaccion(OperacionBD operacion) {
		Connection conn = null;
		try {
			conn = db.getConnection();
			conn.setAutoCommit(false);
			int result = operacion.ejecutar(conn);
			conn.commit();
			return result;
		} catch (SQLException e) {
			DbUtils.rollbackQuietly(conn);
			throw new UnexpectedException(e);
		} finally {
			DbUtils.closeQuietly(conn);
		}
	}
}
