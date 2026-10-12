package model.anoacademico;

import java.util.Date;
import java.util.List;

import util.Database;
import util.Util;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Modelo de negocio y caso de uso para la gestión del Año Académico y sus periodos.
 */
public class AnoAcademicoModel {
	private Database db;

	public AnoAcademicoModel() {
		this.db = new Database();
	}

	public AnoAcademicoModel(Database db) {
		this.db = db;
	}

	/**
	 * Busca el año académico marcado como actual en la base de datos.
	 *
	 * @return Objeto AnoAcademico actual
	 * @throws ResourceNotFoundException si no existe ningún año académico actual
	 */
	public AnoAcademico buscarActual() {
		String sql = "SELECT id_ano_academico, nombre, inicio_ano_academico, fin_ano_academico, " +
				"inicio_periodo_matriculacion, fin_periodo_matriculacion, actual " +
				"FROM AnoAcademico WHERE actual = 1 LIMIT 1";
		List<Object[]> rows = db.executeQueryArray(sql);
		if (rows == null || rows.isEmpty()) {
			// Intento secundario: obtener el último registrado si ninguno tiene actual=1
			String sqlUltimo = "SELECT id_ano_academico, nombre, inicio_ano_academico, fin_ano_academico, " +
					"inicio_periodo_matriculacion, fin_periodo_matriculacion, actual " +
					"FROM AnoAcademico ORDER BY id_ano_academico DESC LIMIT 1";
			rows = db.executeQueryArray(sqlUltimo);
			if (rows == null || rows.isEmpty()) {
				throw new ResourceNotFoundException("AnoAcademico", "actual");
			}
		}

		return mapearFila(rows.get(0));
	}

	/**
	 * Busca un Año Académico por su identificador en formato String.
	 */
	public AnoAcademico buscarPorId(String id) {
		validarFormatoId(id);
		return buscarPorId(Long.parseLong(id.trim()));
	}

	/**
	 * Busca un Año Académico por su identificador numérico Long.
	 */
	public AnoAcademico buscarPorId(Long id) {
		if (id == null) {
			throw new ValidationException("El ID de año académico no puede ser nulo");
		}

		String sql = "SELECT id_ano_academico, nombre, inicio_ano_academico, fin_ano_academico, " +
				"inicio_periodo_matriculacion, fin_periodo_matriculacion, actual " +
				"FROM AnoAcademico WHERE id_ano_academico = ?";
		List<Object[]> rows = db.executeQueryArray(sql, id);
		if (rows == null || rows.isEmpty()) {
			throw new ResourceNotFoundException("AnoAcademico", id);
		}

		return mapearFila(rows.get(0));
	}

	/**
	 * Actualiza las fechas de inicio y fin del año académico.
	 */
	public void actualizarFechasAnoAcademico(Long id, Date inicio, Date fin) {
		if (id == null) {
			throw new ValidationException("El ID de año académico no puede ser nulo");
		}
		if (inicio == null || fin == null) {
			throw new ValidationException("Las fechas de inicio y fin de año académico son obligatorias");
		}
		if (inicio.after(fin)) {
			throw new ValidationException("La fecha de inicio de año académico debe ser anterior o igual a la fecha de fin");
		}

		String sql = "UPDATE AnoAcademico SET inicio_ano_academico = ?, fin_ano_academico = ? WHERE id_ano_academico = ?";
		db.executeUpdate(sql, Util.dateToIsoString(inicio), Util.dateToIsoString(fin), id);
	}

	/**
	 * Actualiza las fechas del periodo de matriculación.
	 */
	public void actualizarFechasMatriculacion(Long id, Date inicio, Date fin) {
		if (id == null) {
			throw new ValidationException("El ID de año académico no puede ser nulo");
		}
		if (inicio == null || fin == null) {
			throw new ValidationException("Las fechas de inicio y fin de periodo de matriculación son obligatorias");
		}
		if (inicio.after(fin)) {
			throw new ValidationException("La fecha de inicio del periodo de matriculación debe ser anterior o igual a la de fin");
		}

		String sql = "UPDATE AnoAcademico SET inicio_periodo_matriculacion = ?, fin_periodo_matriculacion = ? WHERE id_ano_academico = ?";
		db.executeUpdate(sql, Util.dateToIsoString(inicio), Util.dateToIsoString(fin), id);
	}

	/**
	 * Marca el año académico indicado como actual y desmarca los demás.
	 */
	public void establecerActual(Long id) {
		if (id == null) {
			throw new ValidationException("El ID de año académico no puede ser nulo");
		}
		db.executeUpdate("UPDATE AnoAcademico SET actual = 0");
		db.executeUpdate("UPDATE AnoAcademico SET actual = 1 WHERE id_ano_academico = ?", id);
	}

	private AnoAcademico mapearFila(Object[] r) {
		Long id = ((Number) r[0]).longValue();
		String nombre = (String) r[1];
		Date inicioAno = mapearFecha(r[2]);
		Date finAno = mapearFecha(r[3]);
		Date inicioMat = mapearFecha(r[4]);
		Date finMat = mapearFecha(r[5]);
		boolean actual = false;
		if (r[6] != null) {
			if (r[6] instanceof Boolean) {
				actual = (Boolean) r[6];
			} else if (r[6] instanceof Number) {
				actual = ((Number) r[6]).intValue() == 1;
			}
		}

		return AnoAcademico.builder()
				.id(id)
				.nombre(nombre)
				.inicioAnoAcademico(inicioAno)
				.finAnoAcademico(finAno)
				.inicioPeriodoMatriculacion(inicioMat)
				.finPeriodoMatriculacion(finMat)
				.actual(actual)
				.build();
	}

	private Date mapearFecha(Object fechaObj) {
		if (fechaObj == null) {
			return null;
		}
		if (fechaObj instanceof Date) {
			return (Date) fechaObj;
		}
		if (fechaObj instanceof java.sql.Date) {
			return new Date(((java.sql.Date) fechaObj).getTime());
		}
		if (fechaObj instanceof String) {
			return Util.isoStringToDate((String) fechaObj);
		}
		throw new ValidationException("Tipo de fecha incompatible: " + fechaObj.getClass());
	}

	private void validarFormatoId(String id) {
		if (id == null) {
			throw new ValidationException("El ID de año académico no puede ser nulo");
		}
		String trimmed = id.trim();
		if (trimmed.isEmpty()) {
			throw new ValidationException("El ID de año académico no puede estar vacío");
		}
		if (!trimmed.matches("^\\d+$")) {
			throw new ValidationException("El formato del ID de año académico es inválido: '" + id + "'. Debe ser un número entero positivo.");
		}
	}
}

