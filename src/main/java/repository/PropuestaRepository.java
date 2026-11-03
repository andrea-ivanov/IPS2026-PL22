package repository;

import java.util.List;

import model.grado.GradoDisplayDTO;
import model.propuesta.PropuestaDisplayDTO;
import model.propuesta.PropuestaModel;
import util.Database;

/**
 * Repositorio de propuestas de asignaturas (HU 35243). La lógica de negocio está en PropuestaModel.
 */
public class PropuestaRepository {
	private PropuestaModel propuestaModel;

	public PropuestaRepository(Database db) {
		this.propuestaModel = new PropuestaModel(db);
	}

	public List<GradoDisplayDTO> getGradosProfesor(String profesorId) {
		return propuestaModel.getGradosProfesor(profesorId);
	}

	public List<PropuestaDisplayDTO> getPropuestas(String profesorId) {
		return propuestaModel.getPropuestas(profesorId);
	}

	public void validarPropuestas(String profesorId, List<String> gradoIds, String nombre, String anio,
			String creditos, String descripcion) {
		propuestaModel.validarPropuestas(profesorId, gradoIds, nombre, anio, creditos, descripcion);
	}

	public List<String> crearPropuestas(String profesorId, List<String> gradoIds, String nombre, String anio,
			String creditos, String descripcion, String fechaHoy) {
		return propuestaModel.crearPropuestas(profesorId, gradoIds, nombre, anio, creditos, descripcion, fechaHoy);
	}
}
