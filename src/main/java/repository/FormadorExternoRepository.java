package repository;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.profesor.ProfesorDTO;
import util.Database;

/**
 * Acceso a datos de los formadores externos que imparten FIP (HU 37166).
 * Solo SQL; las reglas de negocio están en FormadorExternoModel.
 */
public class FormadorExternoRepository {
	private Database db;

	public FormadorExternoRepository(Database db) {
		this.db = db;
	}

	/** Formadores externos para los combos, como ProfesorDTO marcados como externos, ordenados por id */
	public List<ProfesorDTO> findAll() {
		return db.executeQueryPojo(ProfesorDTO.class, "SELECT formadorExternoId AS profesorId, nombre, apellidos, "
				+ "1 AS esExterno FROM FormadorExterno ORDER BY formadorExternoId");
	}

	/** Si existe un formador externo con ese id (dentro de la transacción del modelo) */
	public boolean existe(Connection conn, String formadorExternoId) throws SQLException {
		Number existentes = new QueryRunner().query(conn,
				"SELECT COUNT(*) FROM FormadorExterno WHERE formadorExternoId = ?", new ScalarHandler<Number>(),
				formadorExternoId);
		return existentes.intValue() > 0;
	}

	/** Si algún formador externo o profesor de plantilla tiene ya ese correo, sin distinguir mayúsculas */
	public boolean existeEmail(Connection conn, String email) throws SQLException {
		String buscado = email.trim().toLowerCase();
		Number existentes = new QueryRunner().query(conn,
				"SELECT (SELECT COUNT(*) FROM FormadorExterno WHERE lower(trim(email)) = ?)"
						+ " + (SELECT COUNT(*) FROM Profesor WHERE lower(trim(email)) = ?)",
				new ScalarHandler<Number>(), buscado, buscado);
		return existentes.intValue() > 0;
	}

	/** Número del siguiente formador externo: 4 si el último es FE003 */
	public int siguienteNumero(Connection conn) throws SQLException {
		Number max = new QueryRunner().query(conn, "SELECT MAX(CAST(SUBSTR(formadorExternoId, 3) AS INTEGER)) "
				+ "FROM FormadorExterno WHERE formadorExternoId LIKE 'FE%'", new ScalarHandler<Number>());
		return max == null ? 1 : max.intValue() + 1;
	}

	/** Inserta un formador externo (dentro de la transacción del modelo) */
	public void insertar(Connection conn, String formadorExternoId, String nombre, String apellidos, String email)
			throws SQLException {
		update(conn, "INSERT INTO FormadorExterno (formadorExternoId, nombre, apellidos, email) VALUES (?, ?, ?, ?)",
				formadorExternoId, nombre, apellidos, email);
	}
}
