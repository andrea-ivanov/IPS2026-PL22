package model.microcredencial;

import java.time.LocalDate;
import java.util.List;

import repository.ActaMicrocredencialRepository;
import util.Database;
import util.exception.ValidationException;

/**
 * Lógica de negocio del acta de una microcredencial (HU 36459).
 * - Solo el profesor que imparte la microcredencial ve su acta, pone notas y la cierra
 * - En el acta salen solo las inscripciones pagadas (INSCRITA o FINALIZADA); ni abandonadas ni canceladas
 * - El acta está abierta mientras la microcredencial está ACEPTADA (fechaCierreActa nula)
 * - Las notas (0 a 10; sin nota = no presentado) se ponen desde el inicio de la impartición hasta cerrar el acta
 * - El acta se cierra desde el fin de la impartición (confirmado por el PO) y, si el profesor no la cierra en
 *   DIAS_CIERRE_ACTA_MICROCREDENCIAL días (aprobado por el cliente), la cierra el sistema
 */
public class ActaMicrocredencialModel {
	public static final int DIAS_CIERRE_ACTA_MICROCREDENCIAL = 15;
	static final String INSCRITA = "INSCRITA";
	static final String FINALIZADA = "FINALIZADA";

	private ActaMicrocredencialRepository repository;

	public ActaMicrocredencialModel(Database db) {
		this.repository = new ActaMicrocredencialRepository(db);
	}

	// =========================================================================
	// Consulta
	// =========================================================================

	/**
	 * La microcredencial, si la imparte el profesor.
	 * @throws ValidationException si no existe o la imparte otro profesor
	 */
	public MicrocredencialDisplayDTO getMicrocredencialDelProfesor(String profesorId, String microcredencialId) {
		MicrocredencialDisplayDTO mc = repository.findByIdYProfesor(microcredencialId, profesorId);
		if (mc == null) {
			throw new ValidationException("Solo el profesor que imparte la microcredencial puede ver y calificar su acta");
		}
		return mc;
	}

	/**
	 * Alumnos del acta con su nota, por apellidos.
	 * @throws ValidationException si el profesor no imparte la microcredencial
	 */
	public List<CalificacionMicrocredencialDTO> getCalificaciones(String profesorId, String microcredencialId) {
		getMicrocredencialDelProfesor(profesorId, microcredencialId);
		return repository.findCalificaciones(microcredencialId, List.of(INSCRITA, FINALIZADA));
	}

	// =========================================================================
	// Reglas
	// =========================================================================

	/** El acta está abierta mientras la microcredencial está ACEPTADA */
	public static boolean actaAbierta(MicrocredencialDisplayDTO mc) {
		return EstadoMicrocredencial.ACEPTADA.name().equals(mc.getEstado());
	}

	/** Notas: con el acta abierta y desde el inicio de la impartición */
	public static boolean puedeEditarNotas(MicrocredencialDisplayDTO mc, String fechaHoy) {
		return actaAbierta(mc) && fechaHoy.compareTo(mc.getInicioImparticion()) >= 0;
	}

	/** Cierre manual: con el acta abierta, desde el fin de la impartición hasta el último día del plazo */
	public static boolean puedeCerrarActa(MicrocredencialDisplayDTO mc, String fechaHoy) {
		return actaAbierta(mc) && fechaHoy.compareTo(mc.getFinImparticion()) >= 0
				&& fechaHoy.compareTo(finPlazoCierre(mc)) <= 0;
	}

	/** Último día en que el profesor puede cerrar el acta: fin de la impartición + 15 días */
	public static String finPlazoCierre(MicrocredencialDisplayDTO mc) {
		return LocalDate.parse(mc.getFinImparticion()).plusDays(DIAS_CIERRE_ACTA_MICROCREDENCIAL).toString();
	}
}
