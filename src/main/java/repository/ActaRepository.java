package repository;

import java.util.List;
import java.util.Map;

import model.acta.ActaDisplayDTO;
import model.acta.ActaModel;
import model.acta.CalificacionActaDisplayDTO;
import util.Database;

/**
 * Repositorio de actas y notas (HU 35239). La lógica de negocio está en ActaModel.
 */
public class ActaRepository {
	private ActaModel actaModel;

	public ActaRepository() {
		this(new Database());
	}

	public ActaRepository(Database db) {
		this.actaModel = new ActaModel(db);
	}

	public List<ActaDisplayDTO> getActasCurso(String cursoId) {
		return actaModel.getActasCurso(cursoId);
	}

	public List<CalificacionActaDisplayDTO> getCalificaciones(String cursoId, String anioAcademicoId, String tipoConvocatoria) {
		return actaModel.getCalificaciones(cursoId, anioAcademicoId, tipoConvocatoria);
	}

	public void guardarNotas(String profesorId, String cursoId, String anioAcademicoId, String tipoConvocatoria,
			Map<String, String> notas) {
		actaModel.guardarNotas(profesorId, cursoId, anioAcademicoId, tipoConvocatoria, notas);
	}

	public void cerrarActa(String profesorId, String cursoId, String anioAcademicoId, String tipoConvocatoria, String fechaHoy) {
		actaModel.cerrarActa(profesorId, cursoId, anioAcademicoId, tipoConvocatoria, fechaHoy);
	}

	public List<ActaDisplayDTO> cerrarActasVencidas(String fechaHoy) {
		return actaModel.cerrarActasVencidas(fechaHoy);
	}

	public List<Object[]> getActasCerradasAutomaticamente() {
		return actaModel.getActasCerradasAutomaticamente();
	}
}
