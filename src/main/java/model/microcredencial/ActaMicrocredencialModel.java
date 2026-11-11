package model.microcredencial;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import model.acta.ActaModel;
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

	private Database db;
	private ActaMicrocredencialRepository repository;

	public ActaMicrocredencialModel(Database db) {
		this.db = db;
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
	// Notas
	// =========================================================================

	/**
	 * Guarda las notas del acta (clave: matriculaId, valor: nota escrita en la tabla). Vacío o NP: sin nota.
	 * Se validan todas antes de guardar y se guardan en una única transacción: o todas o ninguna.
	 * @throws ValidationException si el profesor no la imparte, el acta está cerrada, no ha empezado la
	 *         impartición, alguna nota no es válida o alguna matrícula no está en el acta
	 */
	public void guardarNotas(String profesorId, String microcredencialId, Map<String, String> notas, String fechaHoy) {
		MicrocredencialDisplayDTO mc = getMicrocredencialDelProfesor(profesorId, microcredencialId);
		comprobarActaAbierta(mc);
		if (!puedeEditarNotas(mc, fechaHoy)) {
			throw new ValidationException("La microcredencial todavía no ha empezado (inicio: "
					+ mc.getInicioImparticion() + ")");
		}
		for (Map.Entry<String, String> n : notas.entrySet()) {
			ActaModel.parseNota(n.getKey(), n.getValue());
		}

		db.enTransaccion(conn -> {
			for (Map.Entry<String, String> n : notas.entrySet()) {
				Double nota = ActaModel.parseNota(n.getKey(), n.getValue());
				if (repository.guardarNota(conn, microcredencialId, n.getKey(), nota, INSCRITA) == 0) {
					// se lanza dentro de la transacción para deshacer las notas ya guardadas
					throw new ValidationException("La matrícula " + n.getKey() + " no está en el acta");
				}
			}
			return null;
		});
	}

	// =========================================================================
	// Cierre
	// =========================================================================

	/**
	 * Cierre manual del acta por el profesor (cierreAutomatico = 0), desde el fin de la impartición hasta 15 días
	 * después. Las inscripciones INSCRITAS y la microcredencial pasan a FINALIZADA en la misma transacción;
	 * quien no tiene nota queda como no presentado.
	 * @throws ValidationException si el profesor no la imparte, el acta ya está cerrada o está fuera de plazo
	 */
	public void cerrarActa(String profesorId, String microcredencialId, String fechaHoy) {
		MicrocredencialDisplayDTO mc = getMicrocredencialDelProfesor(profesorId, microcredencialId);
		comprobarActaAbierta(mc);
		if (fechaHoy.compareTo(mc.getFinImparticion()) < 0) {
			throw new ValidationException("No se puede cerrar el acta hasta el fin de la microcredencial ("
					+ mc.getFinImparticion() + ")");
		}
		if (fechaHoy.compareTo(finPlazoCierre(mc)) > 0) {
			throw new ValidationException("El plazo para cerrar el acta terminó el " + finPlazoCierre(mc)
					+ ": la cerrará el sistema");
		}
		db.enTransaccion(conn -> repository.cambiarEstadoInscripciones(conn, microcredencialId, INSCRITA, FINALIZADA)
				+ repository.cerrarActa(conn, microcredencialId, EstadoMicrocredencial.ACEPTADA.name(),
						EstadoMicrocredencial.FINALIZADA.name(), fechaHoy, false));
	}

	/**
	 * Cierre automático (cierreAutomatico = 1): las actas abiertas que el profesor no cerró en los
	 * DIAS_CIERRE_ACTA_MICROCREDENCIAL días siguientes al fin de la impartición se cierran con las notas que haya,
	 * con fecha fin + 15 días aunque el sistema lo haga más tarde. Se ejecuta al arrancar y al recargar la BD.
	 * @return el número de actas cerradas
	 */
	public int cerrarActasVencidas(String fechaHoy) {
		String abierta = EstadoMicrocredencial.ACEPTADA.name();
		return db.enTransaccion(conn -> {
			repository.cambiarEstadoInscripcionesVencidas(conn, fechaHoy, DIAS_CIERRE_ACTA_MICROCREDENCIAL, abierta,
					INSCRITA, FINALIZADA);
			return repository.cerrarActasVencidas(conn, fechaHoy, DIAS_CIERRE_ACTA_MICROCREDENCIAL, abierta,
					EstadoMicrocredencial.FINALIZADA.name());
		});
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

	private static void comprobarActaAbierta(MicrocredencialDisplayDTO mc) {
		if (!actaAbierta(mc)) {
			throw new ValidationException("El acta está cerrada: las notas ya no se pueden cambiar");
		}
	}

	/** Último día en que el profesor puede cerrar el acta: fin de la impartición + 15 días */
	public static String finPlazoCierre(MicrocredencialDisplayDTO mc) {
		return LocalDate.parse(mc.getFinImparticion()).plusDays(DIAS_CIERRE_ACTA_MICROCREDENCIAL).toString();
	}
}
