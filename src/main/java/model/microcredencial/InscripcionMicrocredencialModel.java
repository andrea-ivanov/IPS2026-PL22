package model.microcredencial;

import java.util.List;

import model.anoacademico.AnoAcademicoModel;
import model.matriculacion.MatriculacionModel;
import repository.MicrocredencialRepository;
import util.Database;

/**
 * Lógica de negocio de la inscripción del alumno en microcredenciales (HU 35987, docs/DATABASE.md 6.10).
 * - Las ramas habilitadas son las de los grados con matrícula no cancelada del alumno en el año actual
 * - La oferta son las microcredenciales ACEPTADAS de esas ramas con el periodo de matriculación abierto hoy,
 *   con plazas libres (solo cuentan las inscripciones pagadas) y en las que aún no está inscrito
 * - Precio = créditos * PRECIO_CREDITO
 */
public class InscripcionMicrocredencialModel {
	private MicrocredencialRepository repository;
	private AnoAcademicoModel anoAcademicoModel;

	public InscripcionMicrocredencialModel(Database db) {
		this.repository = new MicrocredencialRepository(db);
		this.anoAcademicoModel = new AnoAcademicoModel(db);
	}

	/** Ramas de conocimiento en las que el alumno puede inscribirse este año, ordenadas */
	public List<String> getRamasHabilitadas(String alumnoId) {
		return repository.findRamasHabilitadas(alumnoId, anioActual());
	}

	/** Microcredenciales que el alumno puede solicitar hoy (fechaHoy en formato AAAA-MM-DD), por rama y nombre */
	public List<OfertaMicrocredencialDTO> getOferta(String alumnoId, String fechaHoy) {
		return repository.findOferta(alumnoId, anioActual(), fechaHoy, MatriculacionModel.PRECIO_CREDITO);
	}

	private String anioActual() {
		return anoAcademicoModel.buscarActual().getAnioAcademicoId();
	}
}
