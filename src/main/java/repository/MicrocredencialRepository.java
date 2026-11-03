package repository;

import java.util.List;

import model.microcredencial.MicrocredencialDisplayDTO;
import model.microcredencial.MicrocredencialModel;
import util.Database;

/**
 * Repositorio de microcredenciales del profesor (HU 35988). La lógica de negocio está en MicrocredencialModel.
 */
public class MicrocredencialRepository {
	private MicrocredencialModel microcredencialModel;

	public MicrocredencialRepository(Database db) {
		this.microcredencialModel = new MicrocredencialModel(db);
	}

	public List<MicrocredencialDisplayDTO> getMicrocredenciales(String profesorId) {
		return microcredencialModel.getMicrocredenciales(profesorId);
	}

	public String crearMicrocredencial(String profesorId, String anioAcademicoId, String nombre, String rama,
			String plazas, String creditos, String inicioMatriculacion, String inicioImparticion, String finImparticion,
			String fechaHoy) {
		return microcredencialModel.crearMicrocredencial(profesorId, anioAcademicoId, nombre, rama, plazas, creditos,
				inicioMatriculacion, inicioImparticion, finImparticion, fechaHoy);
	}
}
