package repository;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.grado.GradoDisplayDTO;
import model.propuesta.EstadoPropuesta;
import model.propuesta.PropuestaDisplayDTO;
import util.Database;

/**
 * Acceso a datos de las propuestas de asignaturas (tabla PropuestaCurso).
 * Solo SQL y mapeo de filas a DTO; las reglas de negocio están en PropuestaModel.
 */
public class PropuestaRepository {
	private final Database db;

	public PropuestaRepository(Database db) {
		this.db = db;
	}

	/**
	 * Grados en los que el profesor imparte docencia (algún grupo de una asignatura del grado), con su estado.
	 * Salen también los no vigentes.
	 */
	public List<GradoDisplayDTO> findGradosProfesor(String profesorId) {
		return db.executeQueryPojo(GradoDisplayDTO.class,
				"SELECT DISTINCT g.gradoId AS id, g.nombre, g.facultad, g.ramaConocimiento AS rama, g.estado "
						+ "FROM Imparte i "
						+ "JOIN GradoCurso gc ON gc.cursoId = i.cursoId "
						+ "JOIN Grado g ON g.gradoId = gc.gradoId "
						+ "WHERE i.profesorId = ? "
						+ "ORDER BY g.nombre",
				profesorId);
	}

	/** Propuestas del profesor, de la más reciente a la más antigua */
	public List<PropuestaDisplayDTO> findPropuestas(String profesorId) {
		return db.executeQueryPojo(PropuestaDisplayDTO.class,
				"SELECT p.propuestaId, p.gradoId, g.nombre AS grado, p.nombre, p.anio, CAST(p.creditos AS REAL) AS creditos, "
						+ "p.descripcion, p.fechaPropuesta, p.estado "
						+ "FROM PropuestaCurso p "
						+ "JOIN Grado g ON g.gradoId = p.gradoId "
						+ "WHERE p.profesorId = ? "
						+ "ORDER BY p.fechaPropuesta DESC, p.propuestaId DESC",
				profesorId);
	}

	/** Nombre repetido en el grado, sin distinguir mayúsculas: como asignatura del grado o como propuesta no rechazada */
	public boolean existeNombreEnGrado(String gradoId, String nombre) {
		return !db.executeQueryArray("SELECT 1 FROM GradoCurso gc JOIN Curso c ON c.cursoId = gc.cursoId "
				+ "WHERE gc.gradoId = ? AND LOWER(TRIM(c.nombre)) = LOWER(?) "
				+ "UNION ALL "
				+ "SELECT 1 FROM PropuestaCurso p WHERE p.gradoId = ? AND LOWER(TRIM(p.nombre)) = LOWER(?) AND p.estado <> ?",
				gradoId, nombre, gradoId, nombre, EstadoPropuesta.RECHAZADA.name()).isEmpty();
	}

	/** Número de la mayor propuesta (30 si la última es PR030). Nulo si no hay ninguna */
	public Integer findMaxNumero(Connection conn) throws SQLException {
		Number max = new QueryRunner().query(conn, "SELECT MAX(CAST(SUBSTR(propuestaId, 3) AS INTEGER)) "
				+ "FROM PropuestaCurso WHERE propuestaId LIKE 'PR%'", new ScalarHandler<Number>());
		return max == null ? null : max.intValue();
	}

	public int insertar(Connection conn, String propuestaId, String profesorId, String gradoId, String nombre, int anio,
			double creditos, String descripcion, String fechaPropuesta, String estado) throws SQLException {
		return update(conn, "INSERT INTO PropuestaCurso (propuestaId, profesorId, gradoId, nombre, anio, creditos, "
				+ "descripcion, fechaPropuesta, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
				propuestaId, profesorId, gradoId, nombre, anio, creditos, descripcion, fechaPropuesta, estado);
	}
}
