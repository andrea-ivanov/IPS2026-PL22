package model.microcredencial;

import static util.Util.parsearCreditos;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import repository.MicrocredencialRepository;
import util.Database;
import util.exception.ValidationException;

/**
 * Lógica de negocio de las microcredenciales que propone un profesor (HU 35988).
 * - El profesor indica nombre, rama, plazas, créditos, inicio de la matriculación y fechas de impartición
 * - La matriculación dura siempre DIAS_MATRICULACION_MICROCREDENCIAL días: el fin se calcula, no se escribe
 * - Una propuesta válida se guarda como PROPUESTA en el año académico actual, con la fecha de hoy
 * - El nombre no puede repetirse en el año académico (uq_microcredencial_nombre, sin distinguir mayúsculas)
 */
public class MicrocredencialModel {
	/** Provisional: la HU 35988 habla de un "máximo estipulado" sin valor (docs/DATABASE.md, 6) */
	public static final double MAX_CREDITOS_MICROCREDENCIAL = 15;
	public static final int DIAS_MATRICULACION_MICROCREDENCIAL = 14;
	/** Porcentaje mínimo de plazas pagadas exigido para no cancelar por baja demanda (HU 36410) */
	public static final int PORCENTAJE_MINIMO_PLAZAS_MICROCREDENCIAL = 10;
	public static final int MAX_NOMBRE = 150;
	/** Las cinco ramas oficiales de ck_microcredencial_rama, tal y como se guardan (sin tildes) */
	public static final List<String> RAMAS = List.of("Artes y Humanidades", "Ciencias", "Ciencias de la Salud",
			"Ciencias Sociales y Juridicas", "Ingenieria y Arquitectura");
	private static final String PREFIJO_ID = "MC";

	private Database db;
	private MicrocredencialRepository repository;

	public MicrocredencialModel(Database db) {
		this.db = db;
		this.repository = new MicrocredencialRepository(db);
	}

	// =========================================================================
	// Consulta
	// =========================================================================

	/**
	 * Microcredenciales del profesor ("Mis microcredenciales"), de la más reciente a la más antigua.
	 */
	public List<MicrocredencialDisplayDTO> getMicrocredenciales(String profesorId) {
		return repository.findByProfesor(profesorId);
	}

	/**
	 * Fin de la matriculación para un inicio escrito por el usuario (inicio + 14 días), en formato AAAA-MM-DD.
	 * @return el fin calculado, o cadena vacía si el inicio no es una fecha válida
	 */
	public static String calcularFinMatriculacion(String inicioMatriculacion) {
		try {
			return LocalDate.parse(inicioMatriculacion.trim()).plusDays(DIAS_MATRICULACION_MICROCREDENCIAL).toString();
		} catch (DateTimeParseException | NullPointerException e) {
			return "";
		}
	}

	// =========================================================================
	// Alta
	// =========================================================================

	/**
	 * Crea una microcredencial PROPUESTA del profesor en el año académico indicado, con la fecha fechaHoy.
	 * @return el identificador de la microcredencial creada
	 * @throws ValidationException si algún dato no es válido
	 */
	public String crearMicrocredencial(String profesorId, String anioAcademicoId, String nombre, String rama,
			String plazas, String creditos, String inicioMatriculacion, String inicioImparticion, String finImparticion,
			String fechaHoy) {
		validarMicrocredencial(anioAcademicoId, nombre, rama, plazas, creditos, inicioMatriculacion, inicioImparticion,
				finImparticion, fechaHoy);
		String nombreLimpio = nombre.trim();
		int valorPlazas = parsearPlazas(plazas);
		double valorCreditos = parsearCreditos(creditos);
		String inicioMat = inicioMatriculacion.trim();
		String finMat = calcularFinMatriculacion(inicioMat);

		return db.enTransaccion(conn -> {
			String microcredencialId = String.format("%s%03d", PREFIJO_ID, repository.siguienteNumero(conn));
			repository.insertar(conn, microcredencialId, anioAcademicoId, profesorId, nombreLimpio, rama,
					valorCreditos, valorPlazas, inicioMat, finMat, inicioImparticion.trim(), finImparticion.trim(),
					fechaHoy);
			return microcredencialId;
		});
	}

	/**
	 * Comprueba los datos de una propuesta sin guardar nada: campos obligatorios, rama oficial, plazas enteras
	 * mayores que 0, créditos entre 0 y el máximo, fechas AAAA-MM-DD con la matriculación desde hoy y la
	 * impartición después de la matriculación, y nombre no repetido en el año académico.
	 * @throws ValidationException con el primer problema encontrado
	 */
	public void validarMicrocredencial(String anioAcademicoId, String nombre, String rama, String plazas,
			String creditos, String inicioMatriculacion, String inicioImparticion, String finImparticion,
			String fechaHoy) {
		if (anioAcademicoId == null) {
			throw new ValidationException("No hay ningún año académico configurado");
		}
		validarObligatorio(nombre, "El nombre de la microcredencial es obligatorio");
		validarObligatorio(rama, "La rama de conocimiento es obligatoria");
		validarObligatorio(plazas, "Las plazas son obligatorias");
		validarObligatorio(creditos, "Los créditos son obligatorios");
		validarObligatorio(inicioMatriculacion, "El inicio de la matriculación es obligatorio");
		validarObligatorio(inicioImparticion, "El inicio de la impartición es obligatorio");
		validarObligatorio(finImparticion, "El fin de la impartición es obligatorio");

		String nombreLimpio = nombre.trim();
		if (nombreLimpio.length() > MAX_NOMBRE) {
			throw new ValidationException("El nombre no puede superar los " + MAX_NOMBRE + " caracteres");
		}
		if (!RAMAS.contains(rama)) {
			throw new ValidationException("La rama de conocimiento no es válida: '" + rama + "'");
		}
		parsearPlazas(plazas);
		if (parsearCreditos(creditos) > MAX_CREDITOS_MICROCREDENCIAL) {
			throw new ValidationException("Los créditos no pueden superar el máximo de "
					+ formatearCreditos(MAX_CREDITOS_MICROCREDENCIAL));
		}

		LocalDate inicioMat = parsearFecha(inicioMatriculacion, "inicio de la matriculación");
		LocalDate finMat = inicioMat.plusDays(DIAS_MATRICULACION_MICROCREDENCIAL);
		LocalDate inicioImp = parsearFecha(inicioImparticion, "inicio de la impartición");
		LocalDate finImp = parsearFecha(finImparticion, "fin de la impartición");
		if (inicioMat.isBefore(LocalDate.parse(fechaHoy))) {
			throw new ValidationException("El inicio de la matriculación no puede ser anterior a hoy (" + fechaHoy + ")");
		}
		if (!inicioImp.isAfter(finMat)) {
			throw new ValidationException("El inicio de la impartición debe ser posterior al fin de la matriculación ("
					+ finMat + ")");
		}
		if (!finImp.isAfter(inicioImp)) {
			throw new ValidationException("El fin de la impartición debe ser posterior a su inicio");
		}

		if (repository.existeNombreEnAnio(anioAcademicoId, nombreLimpio)) {
			throw new ValidationException("Ya existe una microcredencial llamada '" + nombreLimpio + "' en el año "
					+ anioAcademicoId);
		}
	}

	// =========================================================================
	// Lote de cancelación por baja demanda (HU 36410)
	// =========================================================================

	/**
	 * Proceso por lotes (HU 36410): el día antes de que cierre el periodo de matriculación
	 * (date(finMatriculacion, '-1 day') <= fechaHoy), cancela las inscripciones no pagadas por IMPAGO
	 * y comprueba si las inscripciones pagadas cubren al menos el 10% de las plazas ofertadas.
	 * Si no alcanzan el 10%, cambia la microcredencial a CANCELADA_BAJA_DEMANDA y cancela sus
	 * inscripciones con motivo BAJA_DEMANDA.
	 */
	public void cancelarPorBajaDemanda(String fechaHoy) {
		validarObligatorio(fechaHoy, "La fecha actual es obligatoria");
		parsearFecha(fechaHoy, "evaluación de baja demanda");
		String fecha = fechaHoy.trim();
		db.enTransaccion(conn -> {
			repository.cancelarInscripcionesImpagadas(conn, fecha);
			repository.cancelarMicrocredencialesBajaDemanda(conn, fecha,
					PORCENTAJE_MINIMO_PLAZAS_MICROCREDENCIAL);
			repository.cancelarInscripcionesPorBajaDemanda(conn);
			return null;
		});
	}

	// =========================================================================
	// Auxiliares
	// =========================================================================

	/** Plazas: número entero mayor que 0 */
	private static int parsearPlazas(String plazas) {
		int valor;
		try {
			valor = Integer.parseInt(plazas.trim());
		} catch (NumberFormatException e) {
			throw new ValidationException("Las plazas deben ser un número entero: '" + plazas.trim() + "'");
		}
		if (valor <= 0) {
			throw new ValidationException("Las plazas deben ser mayores que 0");
		}
		return valor;
	}

	private static LocalDate parsearFecha(String fecha, String campo) {
		try {
			return LocalDate.parse(fecha.trim());
		} catch (DateTimeParseException e) {
			throw new ValidationException("La fecha de " + campo + " debe tener el formato AAAA-MM-DD: '"
					+ fecha.trim() + "'");
		}
	}

	/** 15 en lugar de 15.0 */
	public static String formatearCreditos(double creditos) {
		return creditos == Math.floor(creditos) ? String.valueOf((long) creditos) : String.valueOf(creditos);
	}

	private void validarObligatorio(String valor, String mensaje) {
		if (valor == null || valor.trim().isEmpty()) {
			throw new ValidationException(mensaje);
		}
	}

}
