package repository;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.microcredencial.EstadoMicrocredencial;
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
						+ "fechaPropuesta, estado "
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
	 * libres (solo cuentan las inscripciones pagadas) y en las que no está ya inscrito. Ordenadas por rama y nombre.
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
						+ "WHERE x.microcredencialId = mc.microcredencialId AND x.matriculaId = m.matriculaId) "
						+ "AND (SELECT COUNT(*) FROM MatriculaMicrocredencial x "
						+ "WHERE x.microcredencialId = mc.microcredencialId AND x.pagoValidado = 1) < mc.plazas "
						+ "ORDER BY mc.ramaConocimiento, mc.nombre",
				precioCredito, alumnoId, anioAcademicoId, fechaHoy);
	}
}
