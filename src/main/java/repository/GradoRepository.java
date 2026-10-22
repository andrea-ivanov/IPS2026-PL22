package repository;

import java.util.List;

import model.grado.EstadoGrado;
import model.grado.GradoDisplayDTO;
import model.grado.GradoModel;
import util.Database;

/**
 * Repositorio para el acceso a datos de Grado.
 * Permite integrarse fácilmente tanto con la arquitectura basada en repositorios
 * como con la arquitectura de modelos de GIIS.
 */
public class GradoRepository {
	private final GradoModel gradoModel;
	private Database db;

	public GradoRepository() {
		this(new Database());
	}

	public GradoRepository(Database db) {
		this.db = db;
		this.gradoModel = new GradoModel(db);
	}

	/**
	 * Obtiene todos los grados académicos registrados en la base de datos
	 * adaptados para la visualización en la tabla de AdminOfertaView.
	 */
	public List<GradoDisplayDTO> getTodosLosGrados() {
		String sql = "SELECT gradoId AS id, nombre, facultad, ramaConocimiento AS rama, estado FROM Grado ORDER BY gradoId";
		List<GradoDisplayDTO> list = db.executeQueryPojo(GradoDisplayDTO.class, sql);
		for (GradoDisplayDTO g : list) {
			g.setEstado(EstadoGrado.fromString(g.getEstado()).name());
		}
		return list;
	}

	public String crearGrado(String nombre, String facultad, String ramaConocimiento, String anioAcademicoId) {
		return gradoModel.crearGrado(nombre, facultad, ramaConocimiento, anioAcademicoId);
	}

	public void modificarGrado(String gradoId, String nombre, String facultad, String ramaConocimiento) {
		gradoModel.modificarGrado(gradoId, nombre, facultad, ramaConocimiento);
	}

	public void validarEliminacion(String gradoId, String anioAcademicoId) {
		gradoModel.validarEliminacion(gradoId, anioAcademicoId);
	}

	public void eliminarGrado(String gradoId, String anioAcademicoId) {
		gradoModel.eliminarGrado(gradoId, anioAcademicoId);
	}
}
