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
import model.grado.GradoRevisionDisplayDTO;
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

	/** Inicio del periodo de matriculación del año académico indicado. Nulo si no existe o no está configurado */
	public String findInicioMatriculacionAnio(Connection conn, String anioAcademicoId) throws SQLException {
		Object inicio = new QueryRunner().query(conn,
				"SELECT inicioMatriculacion FROM AnioAcademico WHERE anioAcademicoId = ?",
				new ScalarHandler<Object>(), anioAcademicoId);
		return inicio == null ? null : inicio.toString();
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

	/** Nombre del grado; nulo si no existe */
	public String findNombre(String gradoId) {
		List<Object[]> rows = db.executeQueryArray("SELECT nombre FROM Grado WHERE gradoId = ?", gradoId);
		return rows != null && !rows.isEmpty() && rows.get(0)[0] != null ? rows.get(0)[0].toString() : null;
	}

	/** Cambia el estado del grado */
	public void actualizarEstado(Connection conn, String gradoId, String estado) throws SQLException {
		update(conn, "UPDATE Grado SET estado = ? WHERE gradoId = ?", estado, gradoId);
	}

	/**
	 * Los grados en estadoOrigen con dictamen del resultado indicado para el año académico pasan a estadoDestino, con
	 * la vigencia desde fechaInicioVigencia
	 */
	public void renovarPendientesConDictamen(Connection conn, String anioAcademicoId, String resultado,
			String estadoOrigen, String estadoDestino, String fechaInicioVigencia) throws SQLException {
		update(conn, "UPDATE Grado SET estado = ?, fechaInicioVigencia = ? WHERE estado = ? AND EXISTS ("
				+ "SELECT 1 FROM RevisionGrado r WHERE r.gradoId = Grado.gradoId AND r.anioAcademicoId = ? "
				+ "AND r.resultado = ?)", estadoDestino, fechaInicioVigencia, estadoOrigen, anioAcademicoId, resultado);
	}

	/**
	 * Datos del grado para la pantalla de revisión: decision es el resultado de su dictamen para el año indicado
	 * (nulo si no lo tiene) y fechaUltimaRevision la fecha del dictamen de su año más reciente. Nulo si no existe
	 */
	public GradoRevisionDisplayDTO findRevision(String gradoId, String anioAcademicoId) {
		List<GradoRevisionDisplayDTO> filas = db.executeQueryPojo(GradoRevisionDisplayDTO.class,
				"SELECT g.gradoId AS idGrado, g.nombre, g.facultad, g.ramaConocimiento AS rama, g.estado, "
						+ "(SELECT r.resultado FROM RevisionGrado r WHERE r.gradoId = g.gradoId "
						+ "AND r.anioAcademicoId = ?) AS decision, "
						+ "(SELECT r.fechaRevision FROM RevisionGrado r WHERE r.gradoId = g.gradoId "
						+ "ORDER BY r.anioAcademicoId DESC LIMIT 1) AS fechaUltimaRevision "
						+ "FROM Grado g WHERE g.gradoId = ?",
				anioAcademicoId, gradoId);
		return filas.isEmpty() ? null : filas.get(0);
	}

	/** Resultado del dictamen del grado para el año académico; nulo si no tiene */
	public String findResultadoRevision(Connection conn, String gradoId, String anioAcademicoId) throws SQLException {
		return new QueryRunner().query(conn,
				"SELECT resultado FROM RevisionGrado WHERE gradoId = ? AND anioAcademicoId = ?",
				new ScalarHandler<String>(), gradoId, anioAcademicoId);
	}

	public void insertarRevision(Connection conn, String gradoId, String anioAcademicoId, String resultado,
			String fechaRevision) throws SQLException {
		update(conn, "INSERT INTO RevisionGrado (gradoId, anioAcademicoId, resultado, fechaRevision) VALUES (?, ?, ?, ?)",
				gradoId, anioAcademicoId, resultado, fechaRevision);
	}

	/** Sobrescribe el resultado y la fecha del dictamen del grado para el año académico */
	public void actualizarRevision(Connection conn, String gradoId, String anioAcademicoId, String resultado,
			String fechaRevision) throws SQLException {
		update(conn, "UPDATE RevisionGrado SET resultado = ?, fechaRevision = ? WHERE gradoId = ? AND anioAcademicoId = ?",
				resultado, fechaRevision, gradoId, anioAcademicoId);
	}

	/** Identificadores de los grados que están en alguno de los estados indicados */
	public List<String> findIdsPorEstados(Connection conn, List<String> estados) throws SQLException {
		String marcas = String.join(", ", java.util.Collections.nCopies(estados.size(), "?"));
		return new QueryRunner().query(conn,
				"SELECT gradoId FROM Grado WHERE estado IN (" + marcas + ") ORDER BY gradoId",
				new ColumnListHandler<String>(), estados.toArray());
	}

	/**
	 * Identificadores de los grados VIGENTES que no tenían, en la fecha de inicio del año académico, ninguna matrícula
	 * de ese año con alguna asignatura pagada (pago validado y no cancelada antes de esa fecha)
	 */
	public List<String> findIdsVigentesSinMatriculaPagada(Connection conn, String anioAcademicoId, String fechaInicio)
			throws SQLException {
		return new QueryRunner().query(conn,
				"SELECT g.gradoId FROM Grado g WHERE g.estado = 'VIGENTE' AND NOT EXISTS ("
						+ "SELECT 1 FROM MatriculaCurso mc JOIN Matricula m ON m.matriculaId = mc.matriculaId "
						+ "WHERE m.gradoId = g.gradoId AND m.anioAcademicoId = ? AND mc.pagoValidado = 1 "
						+ "AND (mc.estado <> 'CANCELADA' OR mc.fechaCancelacion >= ?)) ORDER BY g.gradoId",
				new ColumnListHandler<String>(), anioAcademicoId, fechaInicio);
	}

	/** Cambia el estado de todos los cursos del grado */
	public void actualizarEstadoCursos(Connection conn, String gradoId, String estado) throws SQLException {
		update(conn, "UPDATE GradoCurso SET estado = ? WHERE gradoId = ?", estado, gradoId);
	}
}
