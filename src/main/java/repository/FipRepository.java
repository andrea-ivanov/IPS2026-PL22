package repository;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.fip.FipDisplayDTO;
import util.Database;

/**
 * Acceso a datos de las Formaciones Internas del Personal (FIP): listado y alta por el administrador (HU 37165).
 * Solo SQL; las reglas de negocio están en FipModel.
 */
public class FipRepository {
	private Database db;

	public FipRepository(Database db) {
		this.db = db;
	}

	/**
	 * Todas las FIP, de la que empieza más tarde a la que empieza antes, con el nombre y los apellidos y el correo
	 * de quien la imparte (profesor o formador externo)
	 */
	public List<FipDisplayDTO> findAll() {
		return db.executeQueryPojo(FipDisplayDTO.class,
				"SELECT f.fipId, f.nombre, f.fechaInicio, f.fechaFin, "
						+ "COALESCE(p.nombre || ' ' || p.apellidos, fe.nombre || ' ' || fe.apellidos) AS responsable, "
						+ "COALESCE(p.email, fe.email) AS email "
						+ "FROM Fip f "
						+ "LEFT JOIN Profesor p ON p.profesorId = f.profesorId "
						+ "LEFT JOIN FormadorExterno fe ON fe.formadorExternoId = f.formadorExternoId "
						+ "ORDER BY f.fechaInicio DESC, f.fipId DESC");
	}

	/** Número de la siguiente FIP: 6 si la última es FIP005 */
	public int siguienteNumero(Connection conn) throws SQLException {
		Number max = new QueryRunner().query(conn, "SELECT MAX(CAST(SUBSTR(fipId, 4) AS INTEGER)) "
				+ "FROM Fip WHERE fipId LIKE 'FIP%'", new ScalarHandler<Number>());
		return max == null ? 1 : max.intValue() + 1;
	}

	/** Inserta una FIP impartida por un profesor, sin formador externo ni acta (dentro de la transacción del modelo) */
	public void insertar(Connection conn, String fipId, String nombre, String profesorId, String fechaCreacion,
			String fechaInicio, String fechaFin) throws SQLException {
		update(conn, "INSERT INTO Fip (fipId, nombre, profesorId, fechaCreacion, fechaInicio, fechaFin) "
				+ "VALUES (?, ?, ?, ?, ?, ?)",
				fipId, nombre, profesorId, fechaCreacion, fechaInicio, fechaFin);
	}
}
