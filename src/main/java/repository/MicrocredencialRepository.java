package repository;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.microcredencial.EstadoMicrocredencial;
import model.microcredencial.MatriculaMicrocredencialDisplayDTO;
import model.microcredencial.MicrocredencialDisplayDTO;
import model.microcredencial.OfertaMicrocredencialDTO;
import util.Database;

/**
 * Acceso a datos de las microcredenciales: propuestas del profesor (HU 35988) y oferta para el alumno (HU 35987).
 * Solo SQL y mapeo de filas a DTO; las reglas de negocio están en MicrocredencialModel e
 * InscripcionMicrocredencialModel.
 */
public class MicrocredencialRepository {
	private Database db;

	public MicrocredencialRepository(Database db) {
		this.db = db;
	}

	// =========================================================================
	// Propuestas del profesor (HU 35988)
	// =========================================================================

	/** Microcredenciales del profesor, de la más reciente a la más antigua */
	public List<MicrocredencialDisplayDTO> findByProfesor(String profesorId) {
		return db.executeQueryPojo(MicrocredencialDisplayDTO.class,
				"SELECT microcredencialId, anioAcademicoId, nombre, ramaConocimiento, CAST(creditos AS REAL) AS creditos, "
						+ "plazas, inicioMatriculacion, finMatriculacion, inicioImparticion, finImparticion, "
						+ "fechaPropuesta, fechaResolucion, motivoRechazo, fechaCancelacion, estado "
						+ "FROM Microcredencial "
						+ "WHERE profesorId = ? "
						+ "ORDER BY fechaPropuesta DESC, microcredencialId DESC",
				profesorId);
	}

	/**
	 * Nombre repetido en el año académico, sin distinguir mayúsculas y en cualquier estado:
	 * la restricción uq_microcredencial_nombre incluye también las rechazadas
	 */
	public boolean existeNombreEnAnio(String anioAcademicoId, String nombre) {
		return !db.executeQueryArray("SELECT 1 FROM Microcredencial "
				+ "WHERE anioAcademicoId = ? AND LOWER(TRIM(nombre)) = LOWER(?)",
				anioAcademicoId, nombre).isEmpty();
	}

	/** Número de la siguiente microcredencial: 53 si la última es MC052 */
	public int siguienteNumero(Connection conn) throws SQLException {
		Number max = new QueryRunner().query(conn, "SELECT MAX(CAST(SUBSTR(microcredencialId, 3) AS INTEGER)) "
				+ "FROM Microcredencial WHERE microcredencialId LIKE 'MC%'", new ScalarHandler<Number>());
		return max == null ? 1 : max.intValue() + 1;
	}

	/** Inserta una microcredencial PROPUESTA, sin resolución ni acta (dentro de la transacción del modelo) */
	public void insertar(Connection conn, String id, String anioAcademicoId, String profesorId, String nombre,
			String rama, double creditos, int plazas, String inicioMat, String finMat, String inicioImp,
			String finImp, String fechaHoy) throws SQLException {
		update(conn, "INSERT INTO Microcredencial (microcredencialId, anioAcademicoId, profesorId, nombre, "
				+ "ramaConocimiento, creditos, plazas, inicioMatriculacion, finMatriculacion, inicioImparticion, "
				+ "finImparticion, estado, fechaPropuesta, cierreAutomatico) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
				id, anioAcademicoId, profesorId, nombre, rama, creditos, plazas, inicioMat, finMat, inicioImp, finImp,
				EstadoMicrocredencial.PROPUESTA.name(), fechaHoy);
	}

	// =========================================================================
	// Oferta para el alumno (HU 35987)
	// =========================================================================

	/** Ramas de los grados en los que el alumno tiene una matrícula no cancelada ese año, ordenadas */
	public List<String> findRamasHabilitadas(String alumnoId, String anioAcademicoId) {
		List<Object[]> rows = db.executeQueryArray("SELECT DISTINCT g.ramaConocimiento "
				+ "FROM Matricula m "
				+ "JOIN Grado g ON g.gradoId = m.gradoId "
				+ "WHERE m.alumnoId = ? AND m.anioAcademicoId = ? AND m.estado <> 'CANCELADA' "
				+ "ORDER BY g.ramaConocimiento",
				alumnoId, anioAcademicoId);
		List<String> ramas = new ArrayList<>();
		for (Object[] row : rows) {
			ramas.add((String) row[0]);
		}
		return ramas;
	}

	/**
	 * Microcredenciales que el alumno puede solicitar ese año (docs/DATABASE.md, HU 35987): ACEPTADAS, de la rama
	 * del grado de una matrícula suya no cancelada, con fechaHoy dentro del periodo de matriculación, con plazas
	 * libres (solo cuentan las inscripciones pagadas) y en las que no está ya inscrito desde ninguna de sus
	 * matrículas. Ordenadas por rama y nombre.
	 */
	public List<OfertaMicrocredencialDTO> findOferta(String alumnoId, String anioAcademicoId, String fechaHoy,
			double precioCredito) {
		return db.executeQueryPojo(OfertaMicrocredencialDTO.class,
				"SELECT DISTINCT mc.microcredencialId, mc.anioAcademicoId, mc.nombre, mc.ramaConocimiento, "
						+ "CAST(mc.creditos AS REAL) AS creditos, mc.plazas, "
						+ "mc.plazas - (SELECT COUNT(*) FROM MatriculaMicrocredencial x "
						+ "WHERE x.microcredencialId = mc.microcredencialId AND x.pagoValidado = 1) AS plazasLibres, "
						+ "mc.inicioMatriculacion, mc.finMatriculacion, mc.inicioImparticion, mc.finImparticion, "
						+ "CAST(mc.creditos * ? AS REAL) AS precio "
						+ "FROM Microcredencial mc "
						+ "JOIN Matricula m ON m.anioAcademicoId = mc.anioAcademicoId "
						+ "JOIN Grado g ON g.gradoId = m.gradoId AND g.ramaConocimiento = mc.ramaConocimiento "
						+ "WHERE m.alumnoId = ? AND m.estado <> 'CANCELADA' AND mc.anioAcademicoId = ? "
						+ "AND mc.estado = 'ACEPTADA' "
						+ "AND ? BETWEEN mc.inicioMatriculacion AND mc.finMatriculacion "
						+ "AND NOT EXISTS (SELECT 1 FROM MatriculaMicrocredencial x "
						+ "JOIN Matricula xm ON xm.matriculaId = x.matriculaId "
						+ "WHERE x.microcredencialId = mc.microcredencialId AND xm.alumnoId = m.alumnoId) "
						+ "AND (SELECT COUNT(*) FROM MatriculaMicrocredencial x "
						+ "WHERE x.microcredencialId = mc.microcredencialId AND x.pagoValidado = 1) < mc.plazas "
						+ "ORDER BY mc.ramaConocimiento, mc.nombre",
				precioCredito, alumnoId, anioAcademicoId, fechaHoy);
	}

	// =========================================================================
	// Inscripción del alumno (HU 35987)
	// =========================================================================

	/**
	 * matriculaId de la primera matrícula no cancelada del alumno en ese año cuyo grado es de esa rama;
	 * null si no hay
	 */
	public String findMatriculaParaRama(String alumnoId, String anioAcademicoId, String rama) {
		List<Object[]> rows = db.executeQueryArray("SELECT m.matriculaId FROM Matricula m "
				+ "JOIN Grado g ON g.gradoId = m.gradoId "
				+ "WHERE m.alumnoId = ? AND m.anioAcademicoId = ? AND m.estado <> 'CANCELADA' "
				+ "AND g.ramaConocimiento = ? ORDER BY m.matriculaId LIMIT 1",
				alumnoId, anioAcademicoId, rama);
		return rows.isEmpty() ? null : (String) rows.get(0)[0];
	}

	/** Si el alumno ya tiene una inscripción en la microcredencial desde cualquiera de sus matrículas */
	public boolean existeInscripcion(String alumnoId, String microcredencialId) {
		return !db.executeQueryArray("SELECT 1 FROM MatriculaMicrocredencial x "
				+ "JOIN Matricula m ON m.matriculaId = x.matriculaId "
				+ "WHERE m.alumnoId = ? AND x.microcredencialId = ?",
				alumnoId, microcredencialId).isEmpty();
	}

	/** Nombre y plazas libres (solo cuentan las pagadas) de una microcredencial; null si no existe */
	public OfertaMicrocredencialDTO findNombreYPlazasLibres(String microcredencialId, String anioAcademicoId) {
		List<OfertaMicrocredencialDTO> rows = db.executeQueryPojo(OfertaMicrocredencialDTO.class,
				"SELECT mc.microcredencialId, mc.nombre, mc.plazas, "
						+ "mc.plazas - (SELECT COUNT(*) FROM MatriculaMicrocredencial x "
						+ "WHERE x.microcredencialId = mc.microcredencialId AND x.pagoValidado = 1) AS plazasLibres "
						+ "FROM Microcredencial mc WHERE mc.microcredencialId = ? AND mc.anioAcademicoId = ?",
				microcredencialId, anioAcademicoId);
		return rows.isEmpty() ? null : rows.get(0);
	}

	/** Inserta una inscripción (dentro de la transacción del modelo); nota, motivo y baja quedan nulos */
	public void insertarInscripcion(Connection conn, String matriculaId, String anioAcademicoId,
			String microcredencialId, String fechaHoy, String estado, double importe, String metodoPago,
			String fechaPago, int pagoValidado) throws SQLException {
		update(conn, "INSERT INTO MatriculaMicrocredencial (matriculaId, anioAcademicoId, microcredencialId, "
				+ "fechaSolicitud, estado, importe, metodoPago, fechaPago, pagoValidado) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
				matriculaId, anioAcademicoId, microcredencialId, fechaHoy, estado, importe, metodoPago, fechaPago,
				pagoValidado);
	}

	/**
	 * Inscripciones en microcredenciales de un estudiante para la vista "Mis Matrículas"
	 * (HU 35232, HU 36410 y HU 36459). La nota solo se devuelve cuando el acta está cerrada y el estado es FINALIZADA.
	 */
	public List<MatriculaMicrocredencialDisplayDTO> findMatriculasByAlumno(String alumnoId) {
		return db.executeQueryPojo(MatriculaMicrocredencialDisplayDTO.class,
				"SELECT mc.microcredencialId, mc.anioAcademicoId, mc.nombre, mc.ramaConocimiento, "
						+ "CAST(mc.creditos AS REAL) AS creditos, mc.inicioImparticion, mc.finImparticion, "
						+ "CAST(mm.importe AS REAL) AS importe, mm.estado, mm.motivoCancelacion, mm.fechaBaja, "
						+ "CASE WHEN mc.fechaCierreActa IS NULL OR mm.estado <> 'FINALIZADA' THEN NULL "
						+ "     WHEN mm.nota IS NULL THEN 'NP' ELSE CAST(mm.nota AS TEXT) END AS nota "
						+ "FROM MatriculaMicrocredencial mm "
						+ "JOIN Matricula m ON m.matriculaId = mm.matriculaId "
						+ "JOIN Microcredencial mc ON mc.microcredencialId = mm.microcredencialId "
						+ "WHERE m.alumnoId = ? "
						+ "ORDER BY mc.inicioImparticion DESC, mc.microcredencialId DESC",
				alumnoId);
	}

	// =========================================================================
	// Lote de cancelación por baja demanda (HU 36410)
	// =========================================================================

	/**
	 * Cancela por IMPAGO las inscripciones SOLICITADAS (sin pago validado) de las microcredenciales
	 * ACEPTADAS cuyo periodo de evaluación previo al cierre de matriculación ya se ha cumplido
	 * (date(finMatriculacion, '-1 day') <= fechaHoy).
	 */
	public void cancelarInscripcionesImpagadas(Connection conn, String fechaHoy) throws SQLException {
		update(conn, "UPDATE MatriculaMicrocredencial "
				+ "SET estado = 'CANCELADA', motivoCancelacion = 'IMPAGO', "
				+ "    fechaBaja = (SELECT date(mc.finMatriculacion, '-1 day') FROM Microcredencial mc "
				+ "                 WHERE mc.microcredencialId = MatriculaMicrocredencial.microcredencialId) "
				+ "WHERE estado = 'SOLICITADA' "
				+ "  AND microcredencialId IN (SELECT microcredencialId FROM Microcredencial "
				+ "                            WHERE estado = ? AND date(finMatriculacion, '-1 day') <= ?)",
				EstadoMicrocredencial.ACEPTADA.name(), fechaHoy);
	}

	/**
	 * Cambia a CANCELADA_BAJA_DEMANDA las microcredenciales ACEPTADAS en fecha de evaluación
	 * (date(finMatriculacion, '-1 day') <= fechaHoy) cuyas inscripciones pagadas (pagoValidado = 1)
	 * no alcancen el porcentaje mínimo de plazas ofertadas (porcentajeMinimo, p. ej. 10%).
	 */
	public void cancelarMicrocredencialesBajaDemanda(Connection conn, String fechaHoy, int porcentajeMinimo)
			throws SQLException {
		update(conn, "UPDATE Microcredencial "
				+ "SET estado = ?, fechaCancelacion = date(finMatriculacion, '-1 day') "
				+ "WHERE estado = ? AND date(finMatriculacion, '-1 day') <= ? "
				+ "  AND (SELECT COUNT(*) FROM MatriculaMicrocredencial mm "
				+ "       WHERE mm.microcredencialId = Microcredencial.microcredencialId AND mm.pagoValidado = 1) * 100 < plazas * ?",
				EstadoMicrocredencial.CANCELADA_BAJA_DEMANDA.name(),
				EstadoMicrocredencial.ACEPTADA.name(),
				fechaHoy, porcentajeMinimo);
	}

	/**
	 * Cancela con motivo BAJA_DEMANDA todas las inscripciones INSCRITAS de las microcredenciales
	 * que hayan pasado a CANCELADA_BAJA_DEMANDA.
	 */
	public void cancelarInscripcionesPorBajaDemanda(Connection conn) throws SQLException {
		update(conn, "UPDATE MatriculaMicrocredencial "
				+ "SET estado = 'CANCELADA', motivoCancelacion = 'BAJA_DEMANDA', "
				+ "    fechaBaja = (SELECT mc.fechaCancelacion FROM Microcredencial mc "
				+ "                 WHERE mc.microcredencialId = MatriculaMicrocredencial.microcredencialId) "
				+ "WHERE estado = 'INSCRITA' "
				+ "  AND microcredencialId IN (SELECT microcredencialId FROM Microcredencial WHERE estado = ?)",
				EstadoMicrocredencial.CANCELADA_BAJA_DEMANDA.name());
	}
}
