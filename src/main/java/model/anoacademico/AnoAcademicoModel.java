package model.anoacademico;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

import repository.AnoAcademicoRepository;
import util.Database;
import util.Util;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Modelo de negocio y caso de uso para la gestión del Año Académico y sus periodos.
 * Conecta con la tabla AnioAcademico de la base de datos.
 */
public class AnoAcademicoModel {
	private Database db;
	private AnoAcademicoRepository repository;

	public AnoAcademicoModel(Database db) {
		this.db = db;
		this.repository = new AnoAcademicoRepository(db);
	}

	/**
	 * Busca el año académico actual: el último configurado (el de mayor fecha de inicio).
	 *
	 * @return Objeto AnoAcademico actual completamente poblado desde BD
	 * @throws ResourceNotFoundException si no existe ningún año académico en la base de datos
	 */
	public AnoAcademico buscarActual() {
		String id = repository.findActualId();
		if (id == null) {
			throw new ResourceNotFoundException("AnoAcademico", "actual");
		}
		return buscarPorId(id);
	}

	/**
	 * Busca un Año Académico por su identificador (p. ej. "2026-2027").
	 */
	public AnoAcademico buscarPorId(String id) {
		validarFormatoId(id);
		String cleanId = id.trim();

		AnoAcademico anio = repository.findById(cleanId);
		if (anio == null) {
			throw new ResourceNotFoundException("AnoAcademico", id);
		}
		return anio;
	}

	/**
	 * Comprueba que el año académico anterior ya ha terminado en la fecha indicada (hoy posterior a su fecha de fin).
	 *
	 * @param fechaHoy Fecha de referencia en formato ISO (AAAA-MM-DD)
	 * @throws ValidationException Si el año anterior todavía no ha terminado
	 */
	public void validarAnoAnteriorTerminado(String fechaHoy) {
		db.enTransaccion(conn -> {
			comprobarAnoAnteriorTerminado(conn, fechaHoy);
			return null;
		});
	}

	/**
	 * Comprueba que se puede configurar un nuevo año académico con las fechas indicadas, sin modificar nada.
	 *
	 * @param fechaHoy Fecha de referencia en formato ISO (AAAA-MM-DD)
	 * @return Identificador del año que se creará (AAAA-AAAA)
	 * @throws ValidationException Si el año anterior no ha terminado o alguna fecha falta o no cumple las reglas
	 */
	public String validarNuevoAnoAcademico(String fechaHoy, Date inicioAnio, Date finAnio, Date inicioMatriculacion,
			Date finMatriculacion) {
		return db.enTransaccion(conn -> comprobarNuevoAno(conn, fechaHoy, inicioAnio, finAnio, inicioMatriculacion,
				finMatriculacion));
	}

	/**
	 * Configura un nuevo año académico, que pasa a ser el actual. En la misma transacción:
	 * los grados VIGENTES con la vigencia de 4 años caducada pasan a PENDIENTE_RENOVACION
	 * y los cursos de grados en extinción pasan a INACTIVO.
	 *
	 * @param fechaHoy Fecha de referencia en formato ISO (AAAA-MM-DD)
	 * @return Identificador del año creado (AAAA-AAAA)
	 * @throws ValidationException Si el año anterior no ha terminado o alguna fecha falta o no cumple las reglas
	 */
	public String iniciarNuevoAnoAcademico(String fechaHoy, Date inicioAnio, Date finAnio, Date inicioMatriculacion,
			Date finMatriculacion) {
		return db.enTransaccion(conn -> {
			String anioId = comprobarNuevoAno(conn, fechaHoy, inicioAnio, finAnio, inicioMatriculacion,
					finMatriculacion);
			String inicio = Util.dateToIsoString(inicioAnio);
			repository.insertar(conn, anioId, inicio, Util.dateToIsoString(finAnio),
					Util.dateToIsoString(inicioMatriculacion), Util.dateToIsoString(finMatriculacion));
			repository.actualizarEstadosAlIniciarAno(conn, inicio);
			return anioId;
		});
	}

	private void comprobarAnoAnteriorTerminado(Connection conn, String fechaHoy) throws SQLException {
		if (fechaHoy == null || fechaHoy.trim().isEmpty()) {
			throw new ValidationException("Debe indicarse la fecha de hoy");
		}
		String finAnterior = repository.findUltimaFechaFin(conn);
		if (finAnterior != null && fechaHoy.trim().compareTo(finAnterior) <= 0) {
			throw new ValidationException("No se puede configurar un nuevo año académico hasta que termine el actual ("
					+ finAnterior + ")");
		}
	}

	private String comprobarNuevoAno(Connection conn, String fechaHoy, Date inicioAnio, Date finAnio,
			Date inicioMatriculacion, Date finMatriculacion) throws SQLException {
		comprobarAnoAnteriorTerminado(conn, fechaHoy);
		if (inicioAnio == null || finAnio == null || inicioMatriculacion == null || finMatriculacion == null) {
			throw new ValidationException("Las fechas de inicio y fin del año académico y del periodo de matriculación "
					+ "son obligatorias (dd/mm/aa)");
		}
		String inicio = Util.dateToIsoString(inicioAnio);
		String fin = Util.dateToIsoString(finAnio);
		String inicioMat = Util.dateToIsoString(inicioMatriculacion);
		String finMat = Util.dateToIsoString(finMatriculacion);

		if (fin.compareTo(inicio) <= 0) {
			throw new ValidationException("La fecha de fin del año académico debe ser posterior a la de inicio");
		}
		if (finMat.compareTo(inicioMat) <= 0) {
			throw new ValidationException("La fecha de fin de matriculación debe ser posterior a la de inicio");
		}
		if (finMat.compareTo(fin) >= 0) {
			throw new ValidationException("El periodo de matriculación debe terminar antes que el año académico");
		}

		String finAnterior = repository.findUltimaFechaFin(conn);
		if (finAnterior != null) {
			if (inicio.compareTo(finAnterior) <= 0) {
				throw new ValidationException("El año académico debe empezar después de que termine el anterior ("
						+ finAnterior + ")");
			}
			if (inicioMat.compareTo(finAnterior) <= 0) {
				throw new ValidationException("El periodo de matriculación debe empezar después de que termine el año anterior ("
						+ finAnterior + ")");
			}
		}

		String anioId = inicio.substring(0, 4) + "-" + fin.substring(0, 4);
		if (repository.existe(conn, anioId)) {
			throw new ValidationException("Ya existe el año académico " + anioId);
		}
		return anioId;
	}

	private void validarFormatoId(String id) {
		if (id == null) {
			throw new ValidationException("El ID de año académico no puede ser nulo");
		}
		String trimmed = id.trim();
		if (trimmed.isEmpty()) {
			throw new ValidationException("El ID de año académico no puede estar vacío");
		}
		if (!trimmed.matches("^\\d+$") && !trimmed.matches("^\\d{4}[-/]\\d{4}$")) {
			throw new ValidationException("El formato del ID de año académico es inválido: '" + id + "'. Debe ser un número entero o rango año (p.ej. 2026-2027).");
		}
	}
}
