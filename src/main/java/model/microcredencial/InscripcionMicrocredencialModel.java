package model.microcredencial;

import java.util.ArrayList;
import java.util.List;

import model.anoacademico.AnoAcademicoModel;
import model.gestionmatricula.GestionMatriculaModel;
import model.matriculacion.MatriculacionModel;
import repository.MicrocredencialRepository;
import util.Database;
import util.exception.ValidationException;

/**
 * Lógica de negocio de la inscripción del alumno en microcredenciales (HU 35987, docs/DATABASE.md 6.10).
 * - Las ramas habilitadas son las de los grados con matrícula no cancelada del alumno en el año actual
 * - La oferta son las microcredenciales ACEPTADAS de esas ramas con el periodo de matriculación abierto hoy,
 *   con plazas libres (solo cuentan las inscripciones pagadas) y en las que aún no está inscrito
 * - Precio = créditos * PRECIO_CREDITO
 */
public class InscripcionMicrocredencialModel {
	private Database db;
	private MicrocredencialRepository repository;
	private AnoAcademicoModel anoAcademicoModel;

	public InscripcionMicrocredencialModel(Database db) {
		this.db = db;
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

	/**
	 * Inscripciones en microcredenciales del estudiante para la vista "Mis Matrículas"
	 * (HU 35232, HU 36410 y HU 36459).
	 */
	public List<MatriculaMicrocredencialDisplayDTO> getMatriculasAlumno(String alumnoId) {
		if (alumnoId == null || alumnoId.trim().isEmpty()) {
			return new ArrayList<>();
		}
		return repository.findMatriculasByAlumno(alumnoId.trim());
	}

	private String anioActual() {
		return anoAcademicoModel.buscarActual().getAnioAcademicoId();
	}

	/**
	 * Inscribe al alumno en las microcredenciales indicadas, todas o ninguna. Cada una debe estar en su oferta de hoy.
	 * Con tarjeta la inscripción queda INSCRITA y con el pago validado; con otro método queda SOLICITADA.
	 * @throws ValidationException si la lista está vacía, el método de pago no es válido o alguna no es inscribible
	 */
	public void inscribir(String alumnoId, List<String> microcredencialIds, String metodoPago, String fechaHoy) {
		if (microcredencialIds == null || microcredencialIds.isEmpty()) {
			throw new ValidationException("Selecciona al menos una microcredencial");
		}
		if (metodoPago == null || !GestionMatriculaModel.METODOS_PAGO.contains(metodoPago)) {
			throw new ValidationException("Debe seleccionar un método de pago");
		}
		String anio = anioActual();
		List<OfertaMicrocredencialDTO> oferta = getOferta(alumnoId, fechaHoy);
		boolean pagada = "TARJETA".equals(metodoPago);

		// Primero se valida todo; después se guarda en una sola transacción
		List<OfertaMicrocredencialDTO> elegidas = new ArrayList<>();
		List<String> matriculaIds = new ArrayList<>();
		for (String id : microcredencialIds) {
			OfertaMicrocredencialDTO mc = oferta.stream().filter(o -> o.getMicrocredencialId().equals(id))
					.findFirst().orElse(null);
			if (mc == null) {
				throw new ValidationException(motivoNoInscribible(alumnoId, anio, id));
			}
			String matriculaId = repository.findMatriculaParaRama(alumnoId, anio, mc.getRamaConocimiento());
			if (matriculaId == null) {
				throw new ValidationException("No tienes una matrícula activa en la rama " + mc.getRamaConocimiento()
						+ " para inscribirte en la microcredencial " + mc.getNombre());
			}
			elegidas.add(mc);
			matriculaIds.add(matriculaId);
		}

		db.enTransaccion(conn -> {
			for (int i = 0; i < elegidas.size(); i++) {
				OfertaMicrocredencialDTO mc = elegidas.get(i);
				repository.insertarInscripcion(conn, matriculaIds.get(i), anio, mc.getMicrocredencialId(), fechaHoy,
						pagada ? "INSCRITA" : "SOLICITADA", mc.getPrecio(), metodoPago, fechaHoy, pagada ? 1 : 0);
			}
			return null;
		});
	}

	private String motivoNoInscribible(String alumnoId, String anio, String id) {
		OfertaMicrocredencialDTO info = repository.findNombreYPlazasLibres(id, anio);
		String nombre = info != null ? info.getNombre() : id;
		if (repository.existeInscripcion(alumnoId, id)) {
			return "Ya estás inscrito en la microcredencial " + nombre;
		}
		if (info != null && info.getPlazasLibres() <= 0) {
			return "La microcredencial " + nombre + " no tiene plazas libres";
		}
		return "La microcredencial " + nombre + " no está disponible para ti";
	}
}
