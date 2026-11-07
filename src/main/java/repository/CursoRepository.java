package repository;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.curso.CursoDisplayDTO;
import model.curso.CursoEdicionDTO;
import model.curso.EstadoCurso;
import util.Database;

/**
 * Acceso a datos de Curso / Asignatura (tablas Curso y GradoCurso).
 * Solo SQL y mapeo de filas a DTO; las reglas de negocio están en CursoModel.
 */
public class CursoRepository {
	private final Database db;

	public CursoRepository(Database db) {
		this.db = db;
	}

	/**
	 * Obtiene los cursos asociados a un grado específico para la tabla de AdminOfertaView.
	 */
	public List<CursoDisplayDTO> findByGrado(String idGrado) {
		if (idGrado == null) {
			return new ArrayList<>();
		}
		String sql = "SELECT c.cursoId AS id, " +
					 "       c.nombre AS nombre, " +
					 "       CAST(gc.anio AS TEXT) || 'º' AS ano, " +
					 "       COALESCE(gc.itinerario, 'Común') AS itinerario, " +
					 "       COALESCE(p.nombre || ' ' || p.apellidos, 'Sin asignar') AS profesor, " +
					 "       gc.estado AS estado, " +
					 "       gc.gradoId AS idGrado " +
					 "FROM GradoCurso gc " +
					 "JOIN Curso c ON gc.cursoId = c.cursoId " +
					 "LEFT JOIN Profesor p ON c.coordinadorId = p.profesorId " +
					 "WHERE gc.gradoId = ? " +
					 "ORDER BY gc.anio, c.cursoId";
		return db.executeQueryPojo(CursoDisplayDTO.class, sql, idGrado);
	}

	/** Datos actuales del curso en el grado, sin transformar. Nulo si el curso no pertenece al grado */
	public CursoEdicionDTO findDatosEdicion(String gradoId, String cursoId) {
		List<CursoEdicionDTO> datos = db.executeQueryPojo(CursoEdicionDTO.class,
				"SELECT gc.anio, gc.itinerario, c.creditos, c.nombre, c.coordinadorId "
						+ "FROM GradoCurso gc JOIN Curso c ON c.cursoId = gc.cursoId WHERE gc.gradoId = ? AND gc.cursoId = ?",
				gradoId, cursoId);
		return datos.isEmpty() ? null : datos.get(0);
	}

	/** Número de grados en los que está el curso */
	public int contarGrados(String cursoId) {
		return ((Number) db.executeQueryArray("SELECT COUNT(*) FROM GradoCurso WHERE cursoId = ?", cursoId).get(0)[0])
				.intValue();
	}

	public boolean existe(Connection conn, String cursoId) throws SQLException {
		Number existentes = new QueryRunner().query(conn, "SELECT COUNT(*) FROM Curso WHERE cursoId = ?",
				new ScalarHandler<Number>(), cursoId);
		return existentes.intValue() > 0;
	}

	/** Identificadores de los cursos con formato C seguido de dígitos (candidatos a calcular el siguiente id) */
	public List<String> findIdsNumerados(Connection conn) throws SQLException {
		return new QueryRunner().query(conn, "SELECT cursoId FROM Curso WHERE cursoId GLOB 'C[0-9]*'",
				new ColumnListHandler<String>());
	}

	public void insertarCurso(Connection conn, String cursoId, String coordinadorId, String nombre, double creditos)
			throws SQLException {
		update(conn, "INSERT INTO Curso (cursoId, coordinadorId, nombre, creditos) VALUES (?, ?, ?, ?)",
				cursoId, coordinadorId, nombre, creditos);
	}

	public void insertarEnGrado(Connection conn, String gradoId, String cursoId, int anio, String itinerario,
			String estado) throws SQLException {
		update(conn, "INSERT INTO GradoCurso (gradoId, cursoId, anio, itinerario, estado) VALUES (?, ?, ?, ?, ?)",
				gradoId, cursoId, anio, itinerario, estado);
	}

	/** Actualiza año e itinerario del curso en el grado. Devuelve las filas afectadas (0 si no pertenece al grado) */
	public int actualizarEnGrado(Connection conn, String gradoId, String cursoId, int anio, String itinerario)
			throws SQLException {
		return update(conn, "UPDATE GradoCurso SET anio = ?, itinerario = ? WHERE gradoId = ? AND cursoId = ?",
				anio, itinerario, gradoId, cursoId);
	}

	public void actualizarCreditos(Connection conn, String cursoId, double creditos) throws SQLException {
		update(conn, "UPDATE Curso SET creditos = ? WHERE cursoId = ?", creditos, cursoId);
	}

	/** Estado del curso en el grado tal y como está en la base de datos. Nulo si no pertenece al grado */
	public String findEstadoEnGrado(Connection conn, String gradoId, String cursoId) throws SQLException {
		return new QueryRunner().query(conn,
				"SELECT estado FROM GradoCurso WHERE gradoId = ? AND cursoId = ?",
				new ScalarHandler<String>(), gradoId, cursoId);
	}

	/** Cuenta las matrículas del curso en el grado y año académico que no están canceladas */
	public int contarMatriculasActivas(Connection conn, String gradoId, String cursoId, String anioAcademicoId)
			throws SQLException {
		Number matriculas = new QueryRunner().query(conn,
				"SELECT COUNT(*) FROM MatriculaCurso mc JOIN Matricula m ON m.matriculaId = mc.matriculaId "
						+ "WHERE mc.gradoId = ? AND mc.cursoId = ? AND m.anioAcademicoId = ? AND mc.estado <> 'CANCELADA'",
				new ScalarHandler<Number>(), gradoId, cursoId, anioAcademicoId);
		return matriculas.intValue();
	}

	/** Cambia el estado del curso solo en ese grado */
	public void actualizarEstadoEnGrado(Connection conn, String gradoId, String cursoId, String estado)
			throws SQLException {
		update(conn, "UPDATE GradoCurso SET estado = ? WHERE gradoId = ? AND cursoId = ?", estado, gradoId, cursoId);
	}
}
