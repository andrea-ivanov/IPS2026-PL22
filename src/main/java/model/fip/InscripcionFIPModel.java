package model.fip;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import model.contrato.ContratoAlumnoModel;
import util.Database;
import util.exception.ValidationException;

/**
 * Matrícula presencial de profesores y alumnos en una FIP por el secretario (HU 37046).
 * De momento contiene las reglas que no dependen de la tabla FIP (HU-01 del administrador):
 * - Plazo: solo se matricula hasta el día anterior al inicio de la FIP
 * - Un alumno necesita un contrato vigente en la fecha de inicio de la FIP (contratos de la HU 37045)
 * Pendiente cuando exista la tabla FIP: plazas máximas, no duplicar la matrícula, el responsable y los formadores
 * externos no se matriculan, alta con resultado PENDIENTE y anulación antes del inicio.
 */
public class InscripcionFIPModel {
	private final ContratoAlumnoModel contratoModel;

	public InscripcionFIPModel(Database db) {
		this.contratoModel = new ContratoAlumnoModel(db);
	}

	/**
	 * Comprueba que todavía se puede matricular: hoy tiene que ser anterior al inicio de la FIP
	 * (desde el día de inicio no se admiten nuevas altas).
	 * @throws ValidationException si el plazo ha terminado o las fechas no son válidas
	 */
	public void comprobarPlazo(String fechaInicioFip, String fechaHoy) {
		LocalDate inicio = parsearFecha(fechaInicioFip, "inicio de la FIP");
		LocalDate hoy = parsearFecha(fechaHoy, "hoy");
		if (!hoy.isBefore(inicio)) {
			throw new ValidationException("El plazo de matrícula terminó el día anterior al inicio de la FIP ("
					+ inicio.minusDays(1) + ")");
		}
	}

	/**
	 * Comprueba que el alumno tiene un contrato vigente en la fecha de inicio de la FIP.
	 * @throws ValidationException si no lo tiene
	 */
	public void comprobarContratoAlumno(String alumnoId, String fechaInicioFip) {
		String inicio = parsearFecha(fechaInicioFip, "inicio de la FIP").toString();
		if (!contratoModel.tieneContratoVigente(alumnoId, inicio)) {
			throw new ValidationException("El alumno " + alumnoId.trim() + " no tiene ningún contrato vigente el "
					+ inicio + " (fecha de inicio de la FIP)");
		}
	}

	private LocalDate parsearFecha(String fecha, String cual) {
		if (fecha == null || fecha.trim().isEmpty()) {
			throw new ValidationException("Falta la fecha de " + cual);
		}
		try {
			return LocalDate.parse(fecha.trim());
		} catch (DateTimeParseException e) {
			throw new ValidationException("La fecha de " + cual + " no es válida (formato AAAA-MM-DD): '" + fecha.trim() + "'");
		}
	}
}
