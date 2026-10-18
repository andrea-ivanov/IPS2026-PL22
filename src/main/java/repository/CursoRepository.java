package repository;

import java.util.ArrayList;
import java.util.List;

import model.curso.Curso;
import model.curso.CursoDisplayDTO;
import model.curso.CursoEdicionDTO;
import model.curso.CursoModel;
import util.Database;

/**
 * Repositorio para el acceso a datos de Curso / Asignatura.
 * Permite integrarse fácilmente tanto con la arquitectura basada en repositorios
 * como con la arquitectura de modelos de GIIS.
 */
public class CursoRepository {
	private final CursoModel cursoModel;
	private Database db;

	public CursoRepository() {
		try {
			this.db = new Database();
		} catch (Exception ignored) {
		}
		this.cursoModel = this.db != null ? new CursoModel(this.db) : new CursoModel();
	}

	public CursoRepository(Database db) {
		this.db = db;
		this.cursoModel = new CursoModel(db);
	}

	public Curso findById(String id) {
		return cursoModel.buscarPorId(id);
	}

	public Curso findById(Long id) {
		return cursoModel.buscarPorId(id);
	}

	/**
	 * Obtiene los cursos asociados a un grado específico para la tabla de AdminOfertaView.
	 */
	public List<CursoDisplayDTO> getCursosPorGrado(String idGrado) {
		if (db != null && idGrado != null) {
			try {
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
				List<CursoDisplayDTO> list = db.executeQueryPojo(CursoDisplayDTO.class, sql, idGrado);
				if (list != null) {
					return list;
				}
			} catch (Exception ex) {
				System.err.println("Aviso al consultar BD en getCursosPorGrado: " + ex.getMessage());
			}
		}
		return new ArrayList<>();
	}

	public String crearCurso(String gradoId, String nombre, String creditos, String anio, String coordinadorId,
			String itinerario) {
		return cursoModel.crearCurso(gradoId, nombre, creditos, anio, coordinadorId, itinerario);
	}

	public void modificarCurso(String gradoId, String cursoId, String anio, String itinerario, String creditos) {
		cursoModel.modificarCurso(gradoId, cursoId, anio, itinerario, creditos);
	}

	public CursoEdicionDTO buscarDatosEdicion(String gradoId, String cursoId) {
		return cursoModel.buscarDatosEdicion(gradoId, cursoId);
	}

	public int contarGradosDelCurso(String cursoId) {
		return cursoModel.contarGradosDelCurso(cursoId);
	}

	public void validarEliminacion(String gradoId, String cursoId, String anioAcademicoId) {
		cursoModel.validarEliminacion(gradoId, cursoId, anioAcademicoId);
	}

	public void eliminarCurso(String gradoId, String cursoId, String anioAcademicoId) {
		cursoModel.eliminarCurso(gradoId, cursoId, anioAcademicoId);
	}
}

