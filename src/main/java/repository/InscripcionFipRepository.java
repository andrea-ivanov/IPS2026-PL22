package repository;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.fip.FIPDTO;
import model.fip.InscritoFipDTO;
import util.Database;

/**
 * Acceso a datos de la matrícula presencial en FIP (HU 37046): tablas Fip e InscripcionFip.
 * Solo SQL y mapeo a DTO; las reglas de negocio están en InscripcionFIPModel.
 */
public class InscripcionFipRepository {
	// f.* y no la lista de columnas: así se lee también el máximo de plazas en cuanto la tabla Fip tenga la columna plazas
	private static final String SELECT_FIP = "SELECT f.*, f.profesorId AS profesorResponsableId, "
			+ "(SELECT COUNT(*) FROM InscripcionFip i WHERE i.fipId = f.fipId) AS plazasOcupadas "
			+ "FROM Fip f ";

	private final Database db;

	public InscripcionFipRepository(Database db) {
		this.db = db;
	}

	/** Todas las FIP, de la que empieza más tarde a la que empezó antes */
	public List<FIPDTO> findFips() {
		return db.executeQueryPojo(FIPDTO.class, SELECT_FIP + "ORDER BY f.fechaInicio DESC, f.fipId DESC");
	}

	/** La FIP con su responsable interno (nulo si es un formador externo) y sus inscritos; nula si no existe */
	public FIPDTO findFip(Connection conn, String fipId) throws SQLException {
		return new QueryRunner().query(conn, SELECT_FIP + "WHERE f.fipId = ?", new BeanHandler<>(FIPDTO.class), fipId);
	}

	/** Inscritos en la FIP con su nombre y tipo, ordenados por fecha de inscripción */
	public List<InscritoFipDTO> findInscritos(String fipId) {
		return db.executeQueryPojo(InscritoFipDTO.class,
				"SELECT i.inscripcionId, COALESCE(i.alumnoId, i.profesorId) AS usuarioId, "
						+ "COALESCE(a.nombre || ' ' || a.apellidos, p.nombre || ' ' || p.apellidos) AS usuario, "
						+ "CASE WHEN i.alumnoId IS NOT NULL THEN 'ALUMNO' ELSE 'PROFESOR' END AS tipo, "
						+ "i.fechaInscripcion, i.resultado "
						+ "FROM InscripcionFip i "
						+ "LEFT JOIN Alumno a ON a.alumnoId = i.alumnoId "
						+ "LEFT JOIN Profesor p ON p.profesorId = i.profesorId "
						+ "WHERE i.fipId = ? ORDER BY i.fechaInscripcion, i.inscripcionId",
				fipId);
	}

	/** Si el alumno o el profesor ya está inscrito en la FIP */
	public boolean existeInscripcion(Connection conn, String fipId, String alumnoId, String profesorId)
			throws SQLException {
		Number n = new QueryRunner().query(conn,
				"SELECT COUNT(*) FROM InscripcionFip WHERE fipId = ? AND (alumnoId = ? OR profesorId = ?)",
				new ScalarHandler<Number>(), fipId, alumnoId, profesorId);
		return n != null && n.intValue() > 0;
	}

	public boolean existeProfesor(String profesorId) {
		return !db.executeQueryArray("SELECT 1 FROM Profesor WHERE profesorId = ?", profesorId).isEmpty();
	}

	/** Número de la siguiente inscripción: 17 si la última es IF00016 */
	public int siguienteNumero(Connection conn) throws SQLException {
		Number max = new QueryRunner().query(conn,
				"SELECT MAX(CAST(SUBSTR(inscripcionId, 3) AS INTEGER)) FROM InscripcionFip WHERE inscripcionId LIKE 'IF%'",
				new ScalarHandler<Number>());
		return max == null ? 1 : max.intValue() + 1;
	}

	/** Inscribe a un alumno o a un profesor (el otro va nulo) */
	public void insertar(Connection conn, String inscripcionId, String fipId, String alumnoId, String profesorId,
			String fechaInscripcion, String resultado) throws SQLException {
		update(conn, "INSERT INTO InscripcionFip (inscripcionId, fipId, alumnoId, profesorId, fechaInscripcion, resultado) "
				+ "VALUES (?, ?, ?, ?, ?, ?)", inscripcionId, fipId, alumnoId, profesorId, fechaInscripcion, resultado);
	}
}
