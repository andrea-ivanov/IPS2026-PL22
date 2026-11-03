package repository;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.curso.EstadoCurso;
import model.grado.EstadoGrado;
import model.grado.GradoDisplayDTO;
import util.Database;

/**
 * Acceso a datos de Grado (tabla Grado y sus cursos en GradoCurso).
 * Solo SQL y mapeo de filas a DTO; las reglas de negocio están en GradoModel.
 */
public class GradoRepository {
	private final Database db;

	public GradoRepository(Database db) {
		this.db = db;
	}

	/**
	 * Obtiene todos los grados académicos registrados en la base de datos
	 * adaptados para la visualización en la tabla de AdminOfertaView.
	 */
	public List<GradoDisplayDTO> findAll() {
		String sql = "SELECT gradoId AS id, nombre, facultad, ramaConocimiento AS rama, estado FROM Grado ORDER BY gradoId";
		List<GradoDisplayDTO> list = db.executeQueryPojo(GradoDisplayDTO.class, sql);
		for (GradoDisplayDTO g : list) {
			g.setEstado(EstadoGrado.fromString(g.getEstado()).name());
		}
		return list;
	}

	/** Fecha de inicio del año académico indicado. Nulo si no existe */
	public String findFechaInicioAnio(Connection conn, String anioAcademicoId) throws SQLException {
		Object fechaInicio = new QueryRunner().query(conn,
				"SELECT fechaInicio FROM AnioAcademico WHERE anioAcademicoId = ?",
				new ScalarHandler<Object>(), anioAcademicoId);
		return fechaInicio == null ? null : fechaInicio.toString();
	}

	/** Estado del grado tal y como está en la base de datos. Nulo si no existe */
	public String findEstado(Connection conn, String gradoId) throws SQLException {
		return new QueryRunner().query(conn, "SELECT estado FROM Grado WHERE gradoId = ?",
				new ScalarHandler<String>(), gradoId);
	}

	public boolean existe(Connection conn, String gradoId) throws SQLException {
		Number existentes = new QueryRunner().query(conn, "SELECT COUNT(*) FROM Grado WHERE gradoId = ?",
				new ScalarHandler<Number>(), gradoId);
		return existentes.intValue() > 0;
	}

	/** Identificadores de los grados con formato G seguido de dígitos (candidatos a calcular el siguiente id) */
	public List<String> findIdsNumerados(Connection conn) throws SQLException {
		return new QueryRunner().query(conn, "SELECT gradoId FROM Grado WHERE gradoId GLOB 'G[0-9]*'",
				new ColumnListHandler<String>());
	}

	public void insertar(Connection conn, String gradoId, String nombre, String facultad, String ramaConocimiento,
			String estado, String fechaInicioVigencia) throws SQLException {
		update(conn, "INSERT INTO Grado (gradoId, nombre, facultad, ramaConocimiento, estado, fechaInicioVigencia) "
				+ "VALUES (?, ?, ?, ?, ?, ?)",
				gradoId, nombre, facultad, ramaConocimiento, estado, fechaInicioVigencia);
	}

	/** Actualiza nombre, facultad y rama. Devuelve las filas afectadas (0 si no existe el grado) */
	public int actualizar(Connection conn, String gradoId, String nombre, String facultad, String ramaConocimiento)
			throws SQLException {
		return update(conn, "UPDATE Grado SET nombre = ?, facultad = ?, ramaConocimiento = ? WHERE gradoId = ?",
				nombre, facultad, ramaConocimiento, gradoId);
	}

	/** Cuenta las matrículas de curso no canceladas del grado en el año académico */
	public int contarMatriculasActivas(Connection conn, String gradoId, String anioAcademicoId) throws SQLException {
		Number matriculas = new QueryRunner().query(conn,
				"SELECT COUNT(*) FROM MatriculaCurso mc JOIN Matricula m ON m.matriculaId = mc.matriculaId "
						+ "WHERE mc.gradoId = ? AND m.anioAcademicoId = ? AND mc.estado <> 'CANCELADA'",
				new ScalarHandler<Number>(), gradoId, anioAcademicoId);
		return matriculas.intValue();
	}

	public void marcarEnExtincion(Connection conn, String gradoId) throws SQLException {
		update(conn, "UPDATE Grado SET estado = ? WHERE gradoId = ?", EstadoGrado.EN_EXTINCION.name(), gradoId);
	}

	/** Pasa a INACTIVO todos los cursos del grado */
	public void inactivarCursos(Connection conn, String gradoId) throws SQLException {
		update(conn, "UPDATE GradoCurso SET estado = ? WHERE gradoId = ?", EstadoCurso.INACTIVO.name(), gradoId);
	}
}
