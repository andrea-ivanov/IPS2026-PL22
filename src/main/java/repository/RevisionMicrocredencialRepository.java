package repository;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.microcredencial.PropuestaMicrocredencialDTO;
import util.Database;

/**
 * Acceso a datos de la revisión de microcredenciales por el secretario (HU 35994): solo SQL y mapeo a DTO.
 * Las reglas de negocio están en RevisionMicrocredencialModel.
 */
public class RevisionMicrocredencialRepository {
	private final Database db;

	public RevisionMicrocredencialRepository(Database db) {
		this.db = db;
	}

	/** Microcredenciales en el estado indicado, con el nombre del profesor, de la propuesta más antigua a la más reciente */
	public List<PropuestaMicrocredencialDTO> getPorEstado(String estado) {
		return db.executeQueryPojo(PropuestaMicrocredencialDTO.class,
				"SELECT mc.microcredencialId, mc.anioAcademicoId, mc.nombre, mc.profesorId, "
						+ "p.nombre || ' ' || p.apellidos AS profesor, mc.ramaConocimiento, "
						+ "CAST(mc.creditos AS REAL) AS creditos, mc.plazas, mc.inicioImparticion, mc.finImparticion, "
						+ "mc.fechaPropuesta "
						+ "FROM Microcredencial mc "
						+ "JOIN Profesor p ON p.profesorId = mc.profesorId "
						+ "WHERE mc.estado = ? "
						+ "ORDER BY mc.fechaPropuesta, mc.microcredencialId",
				estado);
	}

	/** Estado de la microcredencial, o null si no existe */
	public String getEstado(Connection conn, String microcredencialId) throws SQLException {
		return new QueryRunner().query(conn, "SELECT estado FROM Microcredencial WHERE microcredencialId = ?",
				new ScalarHandler<String>(), microcredencialId);
	}

	/**
	 * Resuelve la microcredencial: cambia el estado y guarda la fecha y el motivo de rechazo (nulo si se acepta),
	 * solo si sigue en estadoActual.
	 * @return filas actualizadas (0 si ya no estaba en estadoActual)
	 */
	public int resolver(Connection conn, String microcredencialId, String estadoActual, String estadoNuevo,
			String fechaResolucion, String motivoRechazo) throws SQLException {
		return update(conn, "UPDATE Microcredencial SET estado = ?, fechaResolucion = ?, motivoRechazo = ? "
				+ "WHERE microcredencialId = ? AND estado = ?", estadoNuevo, fechaResolucion, motivoRechazo,
				microcredencialId, estadoActual);
	}
}
