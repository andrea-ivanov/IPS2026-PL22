package repository;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.contrato.ContratoAlumnoDTO;
import util.Database;

/**
 * Acceso a datos de los contratos de los alumnos (HU 37045 y requisito de contrato de la HU 37046):
 * solo SQL y mapeo a DTO. Las reglas de negocio están en ContratoAlumnoModel.
 */
public class ContratoAlumnoRepository {
	private final Database db;

	public ContratoAlumnoRepository(Database db) {
		this.db = db;
	}

	/** Historial de contratos del alumno, del que empieza más tarde al que empieza antes */
	public List<ContratoAlumnoDTO> findByAlumno(String alumnoId) {
		return db.executeQueryPojo(ContratoAlumnoDTO.class,
				"SELECT contratoId, alumnoId, descripcion, fechaInicio, fechaFin FROM ContratoAlumno "
						+ "WHERE alumnoId = ? ORDER BY fechaInicio DESC, contratoId DESC",
				alumnoId);
	}

	/** Si el alumno tiene algún contrato vigente en la fecha indicada (empieza ese día o antes y termina ese día o después) */
	public boolean existeContratoVigente(String alumnoId, String fecha) {
		return !db.executeQueryArray("SELECT 1 FROM ContratoAlumno WHERE alumnoId = ? AND fechaInicio <= ? AND fechaFin >= ?",
				alumnoId, fecha, fecha).isEmpty();
	}

	/** Número del siguiente contrato: 5 si el último es CT0004 */
	public int siguienteNumero(Connection conn) throws SQLException {
		Number max = new QueryRunner().query(conn,
				"SELECT MAX(CAST(SUBSTR(contratoId, 3) AS INTEGER)) FROM ContratoAlumno WHERE contratoId LIKE 'CT%'",
				new ScalarHandler<Number>());
		return max == null ? 1 : max.intValue() + 1;
	}

	public void insertar(Connection conn, String contratoId, String alumnoId, String descripcion, String fechaInicio,
			String fechaFin) throws SQLException {
		update(conn, "INSERT INTO ContratoAlumno (contratoId, alumnoId, descripcion, fechaInicio, fechaFin) "
				+ "VALUES (?, ?, ?, ?, ?)", contratoId, alumnoId, descripcion, fechaInicio, fechaFin);
	}
}
