package repository;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.gestionmatricula.GrupoConsultaDTO;
import model.gestionmatricula.GrupoPlazasDTO;
import util.Database;

/**
 * Acceso a datos de los grupos de los cursos (tablas Grupo, MatriculaGrupo e Imparte) para la Secretaría.
 * Solo SQL y mapeo de filas a DTO; las reglas de negocio están en GestionMatriculaModel.
 * Solo ocupan plaza en un grupo los cursos matriculados PAGADOS o ACTIVOS.
 */
public class GrupoRepository {
	private static final String SQL_COLUMNAS_GRUPO = "SELECT g.cursoId, c.nombre AS curso, "
			+ "COALESCE((SELECT GROUP_CONCAT(gr.nombre, '; ') FROM GradoCurso gc JOIN Grado gr ON gr.gradoId = gc.gradoId "
			+ "WHERE gc.cursoId = g.cursoId), '-') AS grados, "
			+ "g.grupoCodigo, g.tipo, g.capacidad, "
			+ "(SELECT COUNT(*) FROM MatriculaGrupo mg "
			+ "JOIN MatriculaCurso mc ON mc.matriculaId = mg.matriculaId AND mc.cursoId = mg.cursoId "
			+ "JOIN Matricula m ON m.matriculaId = mc.matriculaId "
			+ "WHERE mg.cursoId = g.cursoId AND mg.grupoCodigo = g.grupoCodigo AND mc.estado IN ('PAGADA', 'ACTIVA') "
			+ "AND m.anioAcademicoId = ?) AS ocupadas, "
			+ "COALESCE((SELECT GROUP_CONCAT(p.nombre || ' ' || p.apellidos, ', ') FROM Imparte i "
			+ "JOIN Profesor p ON p.profesorId = i.profesorId "
			+ "WHERE i.cursoId = g.cursoId AND i.grupoCodigo = g.grupoCodigo), 'Sin asignar') AS profesores ";
	private static final String SQL_FROM_GRUPO = "FROM Grupo g JOIN Curso c ON c.cursoId = g.cursoId ";
	private static final String SQL_ORDEN_GRUPO = "ORDER BY c.nombre, g.tipo DESC, g.grupoCodigo ";

	private final Database db;

	public GrupoRepository(Database db) {
		this.db = db;
	}

	// =========================================================================
	// Consulta de grupos (HU 35226)
	// =========================================================================

	/** Todos los grupos con los grados de su curso, sus profesores y las plazas ocupadas en el año indicado */
	public List<GrupoConsultaDTO> findGruposConsulta(String anioAcademicoId) {
		return db.executeQueryPojo(GrupoConsultaDTO.class,
				SQL_COLUMNAS_GRUPO + SQL_FROM_GRUPO + SQL_ORDEN_GRUPO, anioAcademicoId);
	}

	/** Número de grupos del grado y curso indicados (nulo o vacío para no filtrar) */
	public int contarGruposConsulta(String grado, String curso) {
		List<Object> params = new ArrayList<>();
		String where = whereGrupos(grado, curso, params);
		return ((Number) db.executeQueryArray("SELECT COUNT(*) " + SQL_FROM_GRUPO + where, params.toArray()).get(0)[0])
				.intValue();
	}

	/**
	 * Una página de los grupos del grado y curso indicados, en el mismo orden que findGruposConsulta.
	 * La ocupación y los profesores solo se calculan para los grupos de la página.
	 */
	public List<GrupoConsultaDTO> findGruposConsulta(String anioAcademicoId, String grado, String curso, int offset,
			int limite) {
		List<Object> params = new ArrayList<>();
		params.add(anioAcademicoId);
		String where = whereGrupos(grado, curso, params);
		params.add(limite);
		params.add(offset);
		return db.executeQueryPojo(GrupoConsultaDTO.class, SQL_COLUMNAS_GRUPO + SQL_FROM_GRUPO
				+ "WHERE (g.cursoId, g.grupoCodigo) IN (SELECT g.cursoId, g.grupoCodigo " + SQL_FROM_GRUPO + where
				+ SQL_ORDEN_GRUPO + "LIMIT ? OFFSET ?) " + SQL_ORDEN_GRUPO, params.toArray());
	}

	/** Nombres de los grados que tienen algún curso con grupos */
	public List<String> findGradosConGrupos() {
		return columna("SELECT DISTINCT gr.nombre FROM GradoCurso gc JOIN Grado gr ON gr.gradoId = gc.gradoId "
				+ "WHERE EXISTS (SELECT 1 FROM Grupo g WHERE g.cursoId = gc.cursoId) ORDER BY gr.nombre");
	}

	/** Nombres de los cursos que tienen grupos */
	public List<String> findCursosConGrupos() {
		return columna("SELECT DISTINCT c.nombre FROM Grupo g JOIN Curso c ON c.cursoId = g.cursoId ORDER BY c.nombre");
	}

	private static String whereGrupos(String grado, String curso, List<Object> params) {
		StringBuilder where = new StringBuilder("WHERE 1 = 1 ");
		if (hayValor(grado)) {
			where.append("AND EXISTS (SELECT 1 FROM GradoCurso gcf JOIN Grado grf ON grf.gradoId = gcf.gradoId "
					+ "WHERE gcf.cursoId = g.cursoId AND grf.nombre = ?) ");
			params.add(grado);
		}
		if (hayValor(curso)) {
			where.append("AND c.nombre = ? ");
			params.add(curso);
		}
		return where.toString();
	}

	/** Valor informado en un filtro (sin contar los espacios) */
	private static boolean hayValor(String filtro) {
		return filtro != null && !filtro.trim().isEmpty();
	}

	/** Primera columna de cada fila como texto */
	private List<String> columna(String sql, Object... params) {
		List<String> valores = new ArrayList<>();
		for (Object[] fila : db.executeQueryArray(sql, params)) {
			valores.add(fila[0].toString());
		}
		return valores;
	}

	// =========================================================================
	// Grupos de un curso matriculado (HU 35229)
	// =========================================================================

	/** Grupos del curso con las plazas ocupadas en el año académico de la matrícula y si la matrícula está en él */
	public List<GrupoPlazasDTO> findGruposMatricula(String matriculaId, String cursoId) {
		return db.executeQueryPojo(GrupoPlazasDTO.class, "SELECT g.grupoCodigo, g.tipo, g.capacidad, "
				+ "(SELECT COUNT(*) FROM MatriculaGrupo mg "
				+ "JOIN MatriculaCurso mc ON mc.matriculaId = mg.matriculaId AND mc.cursoId = mg.cursoId "
				+ "JOIN Matricula m ON m.matriculaId = mc.matriculaId "
				+ "WHERE mg.cursoId = g.cursoId AND mg.grupoCodigo = g.grupoCodigo AND mc.estado IN ('PAGADA', 'ACTIVA') "
				+ "AND m.anioAcademicoId = (SELECT anioAcademicoId FROM Matricula WHERE matriculaId = ?)) AS ocupadas, "
				+ "(SELECT COUNT(*) FROM MatriculaGrupo mg WHERE mg.matriculaId = ? AND mg.cursoId = g.cursoId "
				+ "AND mg.grupoCodigo = g.grupoCodigo) AS asignado "
				+ "FROM Grupo g WHERE g.cursoId = ? ORDER BY g.tipo DESC, g.grupoCodigo", matriculaId, matriculaId, cursoId);
	}

	/** Quita la asignación del curso matriculado al grupo del tipo indicado (dentro de la transacción del modelo) */
	public void quitarGrupo(Connection conn, String matriculaId, String cursoId, String tipo) throws SQLException {
		update(conn, "DELETE FROM MatriculaGrupo WHERE matriculaId = ? AND cursoId = ? AND tipo = ?",
				matriculaId, cursoId, tipo);
	}

	/** Asigna el curso matriculado a un grupo del tipo indicado (dentro de la transacción del modelo) */
	public void asignarGrupo(Connection conn, String matriculaId, String cursoId, String tipo, String grupoCodigo)
			throws SQLException {
		update(conn, "INSERT INTO MatriculaGrupo (matriculaId, cursoId, tipo, grupoCodigo) VALUES (?, ?, ?, ?)",
				matriculaId, cursoId, tipo, grupoCodigo);
	}
}
