package model.microcredencial;

import java.util.List;

import repository.RevisionMicrocredencialRepository;
import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Revisión de las microcredenciales propuestas por los profesores, por parte del secretario (HU 35994).
 * - El secretario ve las PROPUESTAS con nombre, profesor proponente, créditos y periodo de impartición
 * - Al aceptarla pasa a ACEPTADA con la fecha de resolución: queda registrada en la oferta
 * - No se guarda qué secretario la acepta (docs/DATABASE.md, 4.5)
 * El SQL está en RevisionMicrocredencialRepository.
 */
public class RevisionMicrocredencialModel {
	public static final String PROPUESTA = "PROPUESTA";
	public static final String ACEPTADA = "ACEPTADA";

	private final Database db;
	private final RevisionMicrocredencialRepository repository;

	public RevisionMicrocredencialModel(Database db) {
		this.db = db;
		this.repository = new RevisionMicrocredencialRepository(db);
	}

	/** Microcredenciales PROPUESTAS pendientes de revisión, de la propuesta más antigua a la más reciente */
	public List<PropuestaMicrocredencialDTO> getPropuestasPendientes() {
		return repository.getPorEstado(PROPUESTA);
	}

	/**
	 * Acepta una microcredencial PROPUESTA: pasa a ACEPTADA con fechaResolucion = fechaHoy.
	 * @throws ResourceNotFoundException si no existe
	 * @throws ValidationException si ya no está PROPUESTA (ya se aceptó o se rechazó)
	 */
	public void aceptarPropuesta(String microcredencialId, String fechaHoy) {
		validarObligatorio(microcredencialId, "Selecciona la microcredencial que quieres aceptar");
		validarObligatorio(fechaHoy, "Falta la fecha de la resolución");
		String id = microcredencialId.trim();
		db.enTransaccion(conn -> {
			String estado = repository.getEstado(conn, id);
			if (estado == null) {
				throw new ResourceNotFoundException("Microcredencial", id);
			}
			if (!PROPUESTA.equals(estado)) {
				throw new ValidationException("Solo se pueden aceptar microcredenciales PROPUESTAS: " + id + " está " + estado);
			}
			// La condición sobre el estado evita aceptar dos veces si otro secretario la ha resuelto a la vez
			if (repository.resolver(conn, id, PROPUESTA, ACEPTADA, fechaHoy) != 1) {
				throw new ValidationException("La microcredencial " + id + " ya no está pendiente de revisión");
			}
			return 1;
		});
	}

	private void validarObligatorio(String valor, String mensaje) {
		if (valor == null || valor.trim().isEmpty()) {
			throw new ValidationException(mensaje);
		}
	}
}
