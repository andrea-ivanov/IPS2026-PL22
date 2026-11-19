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
 * - Al rechazarla pasa a RECHAZADA con la fecha y el motivo, que es obligatorio. Queda invalidada:
 *   si el profesor quiere volver a intentarlo crea una propuesta nueva
 * - No se guarda qué secretario la resuelve (docs/DATABASE.md, 4.5)
 * El SQL está en RevisionMicrocredencialRepository.
 */
public class RevisionMicrocredencialModel {
	// Microcredencial.motivoRechazo es VARCHAR(500)
	public static final int MAX_MOTIVO_RECHAZO = 500;

	private final Database db;
	private final RevisionMicrocredencialRepository repository;

	public RevisionMicrocredencialModel(Database db) {
		this.db = db;
		this.repository = new RevisionMicrocredencialRepository(db);
	}

	/** Microcredenciales PROPUESTAS pendientes de revisión, de la propuesta más antigua a la más reciente */
	public List<PropuestaMicrocredencialDTO> getPropuestasPendientes() {
		return repository.getPorEstado(EstadoMicrocredencial.PROPUESTA.name());
	}

	/**
	 * Acepta una microcredencial PROPUESTA: pasa a ACEPTADA con fechaResolucion = fechaHoy.
	 * @throws ResourceNotFoundException si no existe
	 * @throws ValidationException si ya no está PROPUESTA (ya se aceptó o se rechazó)
	 */
	public void aceptarPropuesta(String microcredencialId, String fechaHoy) {
		validarObligatorio(microcredencialId, "Selecciona la microcredencial que quieres aceptar");
		resolver(microcredencialId.trim(), EstadoMicrocredencial.ACEPTADA, null, fechaHoy, "aceptar");
	}

	/**
	 * Rechaza una microcredencial PROPUESTA: pasa a RECHAZADA con fechaResolucion = fechaHoy y el motivo.
	 * @throws ResourceNotFoundException si no existe
	 * @throws ValidationException si falta el motivo, es demasiado largo o ya no está PROPUESTA
	 */
	public void rechazarPropuesta(String microcredencialId, String motivoRechazo, String fechaHoy) {
		validarObligatorio(microcredencialId, "Selecciona la microcredencial que quieres rechazar");
		String motivo = validarMotivoRechazo(motivoRechazo);
		resolver(microcredencialId.trim(), EstadoMicrocredencial.RECHAZADA, motivo, fechaHoy, "rechazar");
	}

	/**
	 * Comprueba el motivo sin guardar nada (el controlador la usa antes de cerrar el diálogo).
	 * @return el motivo sin espacios en los extremos
	 * @throws ValidationException si falta o es demasiado largo
	 */
	public String validarMotivoRechazo(String motivoRechazo) {
		validarObligatorio(motivoRechazo, "El motivo del rechazo es obligatorio");
		String motivo = motivoRechazo.trim();
		if (motivo.length() > MAX_MOTIVO_RECHAZO) {
			throw new ValidationException("El motivo del rechazo no puede superar los " + MAX_MOTIVO_RECHAZO + " caracteres");
		}
		return motivo;
	}

	/** Pasa la microcredencial de PROPUESTA al estado indicado, en una transacción. */
	private void resolver(String id, EstadoMicrocredencial estadoNuevo, String motivo, String fechaHoy, String accion) {
		validarObligatorio(fechaHoy, "Falta la fecha de la resolución");
		String propuesta = EstadoMicrocredencial.PROPUESTA.name();
		db.enTransaccion(conn -> {
			String estado = repository.getEstado(conn, id);
			if (estado == null) {
				throw new ResourceNotFoundException("Microcredencial", id);
			}
			if (!propuesta.equals(estado)) {
				throw new ValidationException("Solo se pueden " + accion + " microcredenciales PROPUESTAS: " + id + " está " + estado);
			}
			// La condición sobre el estado evita resolverla dos veces si otro secretario lo ha hecho a la vez
			if (repository.resolver(conn, id, propuesta, estadoNuevo.name(), fechaHoy, motivo) != 1) {
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
