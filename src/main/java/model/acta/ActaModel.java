package model.acta;

import java.util.List;
import java.util.Map;

import model.asignaciondocente.RolDocente;
import model.matriculacion.MatriculacionModel;
import repository.ActaRepository;
import util.Database;
import util.exception.ValidationException;

/**
 * Lógica de negocio de las actas y las notas (HU 35239).
 * - Solo el COORDINADOR de la asignatura pone notas y cierra el acta; el DOCENTE solo las consulta
 * - Solo se califica en actas ABIERTAS; una vez cerrada, las notas son de solo lectura
 * - La nota es un número entre 0 y 10, NP (no presentado, calificación sin nota) o vacía (sin calificar)
 * - Las actas abiertas cuya convocatoria ha terminado las cierra el sistema (cierreAutomatico = 1),
 *   y cada una es una notificación para secretaría
 */
public class ActaModel {
	public static final String NO_PRESENTADO = "NP";

	private final Database db;
	private final ActaRepository actaRepository;

	public ActaModel(Database db) {
		this.db = db;
		this.actaRepository = new ActaRepository(db);
	}

	// =========================================================================
	// Consulta
	// =========================================================================

	/**
	 * Actas de la asignatura, de la más reciente a la más antigua.
	 */
	public List<ActaDisplayDTO> getActasCurso(String cursoId) {
		return actaRepository.findActasCurso(cursoId);
	}

	/**
	 * Alumnos del acta con su nota. En un acta abierta salen los que entran en ella;
	 * en una cerrada solo los que tienen calificación (al cerrar, los que no tenían nota pasan a NP).
	 * La nota sale vacía si no tiene calificación y NP si la calificación no tiene nota.
	 */
	public List<CalificacionActaDisplayDTO> getCalificaciones(String cursoId, String anioAcademicoId, String tipoConvocatoria) {
		return actaRepository.findCalificaciones(cursoId, anioAcademicoId, tipoConvocatoria, MatriculacionModel.NOTA_APROBADO);
	}

	/**
	 * Notificaciones para secretaría: actas que ha cerrado el sistema, de la más reciente a la más antigua.
	 * Columnas: curso, asignatura, año, convocatoria, fecha de cierre y coordinador.
	 */
	public List<Object[]> getActasCerradasAutomaticamente() {
		return actaRepository.findActasCerradasAutomaticamente();
	}

	// =========================================================================
	// Reglas
	// =========================================================================

	/**
	 * Las notas solo se pueden editar (y el acta cerrar) si el profesor es COORDINADOR y el acta está ABIERTA.
	 */
	public static boolean puedeEditarNotas(RolDocente rol, EstadoActa estadoActa) {
		return rol == RolDocente.COORDINADOR && estadoActa == EstadoActa.ABIERTA;
	}

	/**
	 * Convierte el texto de la tabla en nota: null si es vacío o NP; admite coma o punto decimal.
	 * @throws ValidationException si no es un número entre 0 y 10
	 */
	public static Double parseNota(String matriculaId, String texto) {
		texto = texto == null ? "" : texto.trim();
		if (texto.isEmpty() || NO_PRESENTADO.equalsIgnoreCase(texto)) {
			return null;
		}
		try {
			double nota = Double.parseDouble(texto.replace(',', '.'));
			if (nota >= 0 && nota <= 10) {
				return nota;
			}
		} catch (NumberFormatException e) {
			// se trata igual que una nota fuera de rango
		}
		throw new ValidationException("Nota no válida en la matrícula " + matriculaId + ": '" + texto
				+ "'. La nota debe ser un número entre 0 y 10 o NP");
	}

	// =========================================================================
	// Notas y cierre
	// =========================================================================

	/**
	 * Guarda las notas de un acta (clave: matriculaId, valor: nota escrita en la tabla).
	 * Se validan todas antes de guardar y se guardan en una única transacción: o todas o ninguna.
	 * Nota vacía: sin calificar (se borra la calificación); NP: no presentado (calificación sin nota).
	 * @throws ValidationException si el profesor no es el coordinador, el acta está cerrada o alguna nota no es válida
	 */
	public void guardarNotas(String profesorId, String cursoId, String anioAcademicoId, String tipoConvocatoria,
			Map<String, String> notas) {
		comprobarCoordinador(profesorId, cursoId);
		comprobarActaAbierta(cursoId, anioAcademicoId, tipoConvocatoria);
		for (Map.Entry<String, String> n : notas.entrySet()) {
			parseNota(n.getKey(), n.getValue());
		}

		db.enTransaccion(conn -> {
			int filas = 0;
			for (Map.Entry<String, String> n : notas.entrySet()) {
				String texto = n.getValue() == null ? "" : n.getValue().trim();
				if (texto.isEmpty()) {
					filas += actaRepository.borrarCalificacion(conn, n.getKey(), cursoId, anioAcademicoId, tipoConvocatoria);
				} else {
					filas += actaRepository.guardarCalificacion(conn, n.getKey(), cursoId, anioAcademicoId,
							tipoConvocatoria, parseNota(n.getKey(), texto));
				}
			}
			return filas;
		});
	}

	/**
	 * Cierre manual del acta por el coordinador (cierreAutomatico = 0).
	 * Los alumnos que no tienen nota quedan como no presentados (NP), en la misma transacción.
	 * @throws ValidationException si el profesor no es el coordinador o el acta ya está cerrada
	 */
	public void cerrarActa(String profesorId, String cursoId, String anioAcademicoId, String tipoConvocatoria, String fechaHoy) {
		comprobarCoordinador(profesorId, cursoId);
		comprobarActaAbierta(cursoId, anioAcademicoId, tipoConvocatoria);
		db.enTransaccion(conn -> actaRepository.insertarNoPresentados(conn, cursoId, anioAcademicoId, tipoConvocatoria,
				MatriculacionModel.NOTA_APROBADO)
				+ actaRepository.cerrarActa(conn, cursoId, anioAcademicoId, tipoConvocatoria, fechaHoy, false));
	}

	/**
	 * Cierra las actas abiertas cuya convocatoria terminó antes de fechaHoy (cierreAutomatico = 1).
	 * Cada acta cerrada así es una notificación para secretaría (ver getActasCerradasAutomaticamente).
	 * Como en el cierre manual, los alumnos sin nota quedan como NP.
	 * @return las actas que ha cerrado
	 */
	public List<ActaDisplayDTO> cerrarActasVencidas(String fechaHoy) {
		List<ActaDisplayDTO> vencidas = actaRepository.findActasVencidas(EstadoActa.ABIERTA.name(), fechaHoy);
		if (vencidas.isEmpty()) {
			return vencidas;
		}
		db.enTransaccion(conn -> {
			int filas = 0;
			for (ActaDisplayDTO a : vencidas) {
				filas += actaRepository.insertarNoPresentados(conn, a.getCursoId(), a.getAnioAcademicoId(),
						a.getTipoConvocatoria(), MatriculacionModel.NOTA_APROBADO);
				filas += actaRepository.cerrarActa(conn, a.getCursoId(), a.getAnioAcademicoId(), a.getTipoConvocatoria(),
						fechaHoy, true);
			}
			return filas;
		});
		return vencidas;
	}

	// =========================================================================
	// Auxiliares
	// =========================================================================

	private void comprobarCoordinador(String profesorId, String cursoId) {
		if (!actaRepository.esCoordinador(profesorId, cursoId)) {
			throw new ValidationException("Solo el coordinador de la asignatura puede modificar las notas");
		}
	}

	private void comprobarActaAbierta(String cursoId, String anioAcademicoId, String tipoConvocatoria) {
		if (!actaRepository.existeActaEnEstado(cursoId, anioAcademicoId, tipoConvocatoria, EstadoActa.ABIERTA.name())) {
			throw new ValidationException("El acta está cerrada: las notas son de solo lectura");
		}
	}

}
