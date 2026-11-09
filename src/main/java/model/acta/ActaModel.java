package model.acta;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;


import model.asignaciondocente.RolDocente;
import model.matriculacion.MatriculacionModel;
import util.Database;
import util.exception.ValidationException;

/**
 * Lógica de negocio de las actas y las notas (HU 35239).
 * - Solo el COORDINADOR de la asignatura pone notas y cierra el acta; el DOCENTE solo las consulta
 * - Solo se califica en actas ABIERTAS; una vez cerrada, las notas son de solo lectura
 * - La nota es un número entre 0 y 10, NP (no presentado, calificación sin nota) o vacía (sin calificar)
 * - Las actas abiertas cuya convocatoria ha terminado las cierra el sistema (cierreAutomatico = 1),
 *   y cada una es una notificación para secretaría
 */
public class ActaModel {
	public static final String NO_PRESENTADO = "NP";

	private static final String SQL_SELECT_ACTA = "SELECT a.cursoId, a.anioAcademicoId, a.tipoConvocatoria, cv.fechaFin, a.estado "
			+ "FROM Acta a "
			+ "JOIN Convocatoria cv ON cv.anioAcademicoId = a.anioAcademicoId AND cv.tipoConvocatoria = a.tipoConvocatoria ";

	/**
	 * Alumnos que entran en el acta "a": matriculados en la asignatura ese año (PAGADA, ACTIVA o FINALIZADA)
	 * que no la han aprobado ya en una convocatoria anterior del mismo año (p. ej. aprobado en enero no va a julio).
	 * Alias: a (Acta), m (Matricula), mc (MatriculaCurso).
	 */
	private static final String SQL_ALUMNOS_ACTA = "FROM Acta a "
			+ "JOIN Matricula m ON m.anioAcademicoId = a.anioAcademicoId "
			+ "JOIN MatriculaCurso mc ON mc.matriculaId = m.matriculaId AND mc.cursoId = a.cursoId "
			+ "AND mc.estado IN ('PAGADA', 'ACTIVA', 'FINALIZADA') "
			+ "AND NOT EXISTS (SELECT 1 FROM Calificacion ap "
			+ "JOIN Convocatoria cvAp ON cvAp.anioAcademicoId = ap.anioAcademicoId AND cvAp.tipoConvocatoria = ap.tipoConvocatoria "
			+ "JOIN Convocatoria cvActa ON cvActa.anioAcademicoId = a.anioAcademicoId AND cvActa.tipoConvocatoria = a.tipoConvocatoria "
			+ "WHERE ap.matriculaId = mc.matriculaId AND ap.cursoId = mc.cursoId AND ap.anioAcademicoId = a.anioAcademicoId "
			+ "AND ap.nota >= " + MatriculacionModel.NOTA_APROBADO + " AND cvAp.fechaFin < cvActa.fechaFin) ";

	private Database db;

	public ActaModel(Database db) {
		this.db = db;
	}

	// =========================================================================
	// Consulta
	// =========================================================================

	/**
	 * Actas de la asignatura, de la más reciente a la más antigua.
	 */
	public List<ActaDisplayDTO> getActasCurso(String cursoId) {
		return db.executeQueryPojo(ActaDisplayDTO.class, SQL_SELECT_ACTA + "WHERE a.cursoId = ? ORDER BY cv.fechaFin DESC",
				cursoId);
	}

	/**
	 * Alumnos del acta con su nota. En un acta abierta salen los que entran en ella (ver SQL_ALUMNOS_ACTA);
	 * en una cerrada solo los que tienen calificación (al cerrar, los que no tenían nota pasan a NP).
	 * La nota sale vacía si no tiene calificación y NP si la calificación no tiene nota.
	 */
	public List<CalificacionActaDisplayDTO> getCalificaciones(String cursoId, String anioAcademicoId, String tipoConvocatoria) {
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
				cursoId, anioAcademicoId, tipoConvocatoria, EstadoActa.ABIERTA.name());
	}

	/**
	 * Notificaciones para secretaría: actas que ha cerrado el sistema, de la más reciente a la más antigua.
	 * Columnas: curso, asignatura, año, convocatoria, fecha de cierre y coordinador.
	 */
	public List<Object[]> getActasCerradasAutomaticamente() {
		return db.executeQueryArray("SELECT a.cursoId, c.nombre, a.anioAcademicoId, a.tipoConvocatoria, a.fechaCierre, "
				+ "COALESCE(p.nombre || ' ' || p.apellidos, '(sin coordinador)') "
				+ "FROM Acta a "
				+ "JOIN Curso c ON c.cursoId = a.cursoId "
				+ "LEFT JOIN Profesor p ON p.profesorId = c.coordinadorId "
				+ "WHERE a.cierreAutomatico = 1 "
				+ "ORDER BY a.fechaCierre DESC, a.cursoId");
	}

	// =========================================================================
	// Reglas
	// =========================================================================

	/**
	 * Las notas solo se pueden editar (y el acta cerrar) si el profesor es COORDINADOR y el acta está ABIERTA.
	 */
	public static boolean puedeEditarNotas(RolDocente rol, EstadoActa estadoActa) {
		return rol == RolDocente.COORDINADOR && estadoActa == EstadoActa.ABIERTA;
	}

	/**
	 * Convierte el texto de la tabla en nota: null si es vacío o NP; admite coma o punto decimal.
	 * @throws ValidationException si no es un número entre 0 y 10
	 */
	public static Double parseNota(String matriculaId, String texto) {
		texto = texto == null ? "" : texto.trim();
		if (texto.isEmpty() || NO_PRESENTADO.equalsIgnoreCase(texto)) {
			return null;
		}
		try {
			double nota = Double.parseDouble(texto.replace(',', '.'));
			if (nota >= 0 && nota <= 10) {
				return nota;
			}
		} catch (NumberFormatException e) {
			// se trata igual que una nota fuera de rango
		}
		throw new ValidationException("Nota no válida en la matrícula " + matriculaId + ": '" + texto
				+ "'. La nota debe ser un número entre 0 y 10 o NP");
	}

	// =========================================================================
	// Notas y cierre
	// =========================================================================

	/**
	 * Guarda las notas de un acta (clave: matriculaId, valor: nota escrita en la tabla).
	 * Se validan todas antes de guardar y se guardan en una única transacción: o todas o ninguna.
	 * Nota vacía: sin calificar (se borra la calificación); NP: no presentado (calificación sin nota).
	 * @throws ValidationException si el profesor no es el coordinador, el acta está cerrada o alguna nota no es válida
	 */
	public void guardarNotas(String profesorId, String cursoId, String anioAcademicoId, String tipoConvocatoria,
			Map<String, String> notas) {
		comprobarCoordinador(profesorId, cursoId);
		comprobarActaAbierta(cursoId, anioAcademicoId, tipoConvocatoria);
		for (Map.Entry<String, String> n : notas.entrySet()) {
			parseNota(n.getKey(), n.getValue());
		}

		db.enTransaccion(conn -> {
			int filas = 0;
			for (Map.Entry<String, String> n : notas.entrySet()) {
				String texto = n.getValue() == null ? "" : n.getValue().trim();
				if (texto.isEmpty()) {
					filas += update(conn, "DELETE FROM Calificacion "
							+ "WHERE matriculaId = ? AND cursoId = ? AND anioAcademicoId = ? AND tipoConvocatoria = ?",
							n.getKey(), cursoId, anioAcademicoId, tipoConvocatoria);
				} else {
					filas += update(conn, "INSERT OR REPLACE INTO Calificacion "
							+ "(matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) VALUES (?, ?, ?, ?, ?)",
							n.getKey(), cursoId, anioAcademicoId, tipoConvocatoria, parseNota(n.getKey(), texto));
				}
			}
			return filas;
		});
	}

	/**
	 * Cierre manual del acta por el coordinador (cierreAutomatico = 0).
	 * Los alumnos que no tienen nota quedan como no presentados (NP), en la misma transacción.
	 * @throws ValidationException si el profesor no es el coordinador o el acta ya está cerrada
	 */
	public void cerrarActa(String profesorId, String cursoId, String anioAcademicoId, String tipoConvocatoria, String fechaHoy) {
		comprobarCoordinador(profesorId, cursoId);
		comprobarActaAbierta(cursoId, anioAcademicoId, tipoConvocatoria);
		db.enTransaccion(conn -> insertarNoPresentados(conn, cursoId, anioAcademicoId, tipoConvocatoria)
				+ update(conn, "UPDATE Acta SET estado = ?, fechaCierre = ?, cierreAutomatico = 0 "
						+ "WHERE cursoId = ? AND anioAcademicoId = ? AND tipoConvocatoria = ?",
						EstadoActa.CERRADA.name(), fechaHoy, cursoId, anioAcademicoId, tipoConvocatoria));
	}

	/**
	 * Cierra las actas abiertas cuya convocatoria terminó antes de fechaHoy (cierreAutomatico = 1).
	 * Cada acta cerrada así es una notificación para secretaría (ver getActasCerradasAutomaticamente).
	 * Como en el cierre manual, los alumnos sin nota quedan como NP.
	 * @return las actas que ha cerrado
	 */
	public List<ActaDisplayDTO> cerrarActasVencidas(String fechaHoy) {
		List<ActaDisplayDTO> vencidas = db.executeQueryPojo(ActaDisplayDTO.class,
				SQL_SELECT_ACTA + "WHERE a.estado = ? AND cv.fechaFin < ?", EstadoActa.ABIERTA.name(), fechaHoy);
		if (vencidas.isEmpty()) {
			return vencidas;
		}
		db.enTransaccion(conn -> {
			int filas = 0;
			for (ActaDisplayDTO a : vencidas) {
				filas += insertarNoPresentados(conn, a.getCursoId(), a.getAnioAcademicoId(), a.getTipoConvocatoria());
				filas += update(conn, "UPDATE Acta SET estado = ?, fechaCierre = ?, cierreAutomatico = 1 "
						+ "WHERE cursoId = ? AND anioAcademicoId = ? AND tipoConvocatoria = ? AND estado = ?",
						EstadoActa.CERRADA.name(), fechaHoy, a.getCursoId(), a.getAnioAcademicoId(),
						a.getTipoConvocatoria(), EstadoActa.ABIERTA.name());
			}
			return filas;
		});
		return vencidas;
	}

	// =========================================================================
	// Auxiliares
	// =========================================================================

	private void comprobarCoordinador(String profesorId, String cursoId) {
		if (db.executeQueryArray("SELECT 1 FROM Curso WHERE cursoId = ? AND coordinadorId = ?", cursoId, profesorId).isEmpty()) {
			throw new ValidationException("Solo el coordinador de la asignatura puede modificar las notas");
		}
	}

	private void comprobarActaAbierta(String cursoId, String anioAcademicoId, String tipoConvocatoria) {
		if (db.executeQueryArray("SELECT 1 FROM Acta WHERE cursoId = ? AND anioAcademicoId = ? "
				+ "AND tipoConvocatoria = ? AND estado = ?", cursoId, anioAcademicoId, tipoConvocatoria,
				EstadoActa.ABIERTA.name()).isEmpty()) {
			throw new ValidationException("El acta está cerrada: las notas son de solo lectura");
		}
	}

	/**
	 * Al cerrar un acta, los alumnos que entran en ella y no tienen calificación quedan como NP (nota nula),
	 * que no consume convocatoria. Así siguen apareciendo en el acta cerrada.
	 */
	private int insertarNoPresentados(Connection conn, String cursoId, String anioAcademicoId, String tipoConvocatoria)
			throws SQLException {
		return update(conn, "INSERT INTO Calificacion (matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) "
				+ "SELECT mc.matriculaId, a.cursoId, a.anioAcademicoId, a.tipoConvocatoria, NULL "
				+ SQL_ALUMNOS_ACTA
				+ "WHERE a.cursoId = ? AND a.anioAcademicoId = ? AND a.tipoConvocatoria = ? "
				+ "AND NOT EXISTS (SELECT 1 FROM Calificacion ca WHERE ca.matriculaId = mc.matriculaId "
				+ "AND ca.cursoId = a.cursoId AND ca.anioAcademicoId = a.anioAcademicoId "
				+ "AND ca.tipoConvocatoria = a.tipoConvocatoria)",
				cursoId, anioAcademicoId, tipoConvocatoria);
	}

}
