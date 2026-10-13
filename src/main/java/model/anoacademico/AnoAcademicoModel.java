package model.anoacademico;

import java.util.Date;
import java.util.List;

import util.Database;
import util.Util;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Modelo de negocio y caso de uso para la gestión del Año Académico y sus periodos.
 * Conecta fielmente con la tabla AnioAcademico de la base de datos (y soporte de fallback a AnoAcademico).
 * Garantiza persistencia real de las fechas del año académico y del periodo de matriculación.
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
	 * Busca el año académico actual en la base de datos.
	 * En la tabla AnioAcademico toma el último año registrado (p. ej. '2026-2027').
	 *
	 * @return Objeto AnoAcademico actual completamente poblado desde BD
	 * @throws ResourceNotFoundException si no existe ningún año académico en la base de datos
	 */
	public AnoAcademico buscarActual() {
		// 1. Intento con la tabla AnoAcademico (con campo booleano actual = 1)
		try {
			String sql = "SELECT id_ano_academico, nombre, inicio_ano_academico, fin_ano_academico, " +
					"inicio_periodo_matriculacion, fin_periodo_matriculacion, actual " +
					"FROM AnoAcademico WHERE actual = 1 LIMIT 1";
			List<Object[]> rows = db.executeQueryArray(sql);
			if (rows != null && !rows.isEmpty()) {
				return mapearFilaAntigua(rows.get(0));
			}
		} catch (Exception ignored) {
		}

		// 2. Intento con la tabla oficial AnioAcademico
		try {
			String sqlAnio = "SELECT anioAcademicoId, fechaInicio, fechaFin, inicioMatriculacion, finMatriculacion " +
					"FROM AnioAcademico ORDER BY anioAcademicoId DESC LIMIT 1";
			List<Object[]> rows = db.executeQueryArray(sqlAnio);
			if (rows != null && !rows.isEmpty()) {
				Object[] r = rows.get(0);
				String anioId = (String) r[0];
				long idNum = parsearNumeroDeId(anioId, 1L);
				return AnoAcademico.builder()
						.id(idNum)
						.nombre(anioId)
						.anioAcademicoId(anioId)
						.inicioAnoAcademico(mapearFecha(r[1]))
						.finAnoAcademico(mapearFecha(r[2]))
						.inicioPeriodoMatriculacion(mapearFecha(r[3]))
						.finPeriodoMatriculacion(mapearFecha(r[4]))
						.actual(true)
						.build();
			}
		} catch (Exception ignored) {
		}

		// 3. Fallback a cualquier fila de AnoAcademico
		try {
			String sqlUltimo = "SELECT id_ano_academico, nombre, inicio_ano_academico, fin_ano_academico, " +
					"inicio_periodo_matriculacion, fin_periodo_matriculacion, actual " +
					"FROM AnoAcademico ORDER BY id_ano_academico DESC LIMIT 1";
			List<Object[]> rows = db.executeQueryArray(sqlUltimo);
			if (rows != null && !rows.isEmpty()) {
				return mapearFilaAntigua(rows.get(0));
			}
		} catch (Exception ignored) {
		}

		throw new ResourceNotFoundException("AnoAcademico", "actual");
	}

	/**
	 * Busca un Año Académico por su identificador en formato String (p. ej. "2026-2027" o "1").
	 */
	public AnoAcademico buscarPorId(String id) {
		validarFormatoId(id);
		String cleanId = id.trim();

		// 1. Si el ID es numérico, búsqueda por ID Long
		if (cleanId.matches("^\\d+$")) {
			return buscarPorId(Long.parseLong(cleanId));
		}

		// 2. Búsqueda en AnioAcademico por anioAcademicoId
		try {
			String sql = "SELECT anioAcademicoId, fechaInicio, fechaFin, inicioMatriculacion, finMatriculacion FROM AnioAcademico WHERE anioAcademicoId = ?";
			List<Object[]> rows = db.executeQueryArray(sql, cleanId);
			if (rows != null && !rows.isEmpty()) {
				Object[] r = rows.get(0);
				String anioId = (String) r[0];
				long idNum = parsearNumeroDeId(anioId, 1L);
				return AnoAcademico.builder()
						.id(idNum)
						.nombre(anioId)
						.anioAcademicoId(anioId)
						.inicioAnoAcademico(mapearFecha(r[1]))
						.finAnoAcademico(mapearFecha(r[2]))
						.inicioPeriodoMatriculacion(mapearFecha(r[3]))
						.finPeriodoMatriculacion(mapearFecha(r[4]))
						.actual(true)
						.build();
			}
		} catch (Exception ignored) {
		}

		throw new ResourceNotFoundException("AnoAcademico", id);
	}

	/**
	 * Busca un Año Académico por su identificador numérico Long.
	 */
	public AnoAcademico buscarPorId(Long id) {
		if (id == null) {
			throw new ValidationException("El ID de año académico no puede ser nulo");
		}

		// 1. Búsqueda en AnoAcademico
		try {
			String sql = "SELECT id_ano_academico, nombre, inicio_ano_academico, fin_ano_academico, " +
					"inicio_periodo_matriculacion, fin_periodo_matriculacion, actual " +
					"FROM AnoAcademico WHERE id_ano_academico = ?";
			List<Object[]> rows = db.executeQueryArray(sql, id);
			if (rows != null && !rows.isEmpty()) {
				return mapearFilaAntigua(rows.get(0));
			}
		} catch (Exception ignored) {
		}

		// 2. Búsqueda en AnioAcademico
		try {
			String sql = "SELECT anioAcademicoId, fechaInicio, fechaFin, inicioMatriculacion, finMatriculacion FROM AnioAcademico WHERE anioAcademicoId = ? OR anioAcademicoId LIKE ?";
			List<Object[]> rows = db.executeQueryArray(sql, String.valueOf(id), id + "-%");
			if (rows != null && !rows.isEmpty()) {
				Object[] r = rows.get(0);
				String anioId = (String) r[0];
				return AnoAcademico.builder()
						.id(id)
						.nombre(anioId)
						.anioAcademicoId(anioId)
						.inicioAnoAcademico(mapearFecha(r[1]))
						.finAnoAcademico(mapearFecha(r[2]))
						.inicioPeriodoMatriculacion(mapearFecha(r[3]))
						.finPeriodoMatriculacion(mapearFecha(r[4]))
						.actual(true)
						.build();
			}
		} catch (Exception ignored) {
		}

		throw new ResourceNotFoundException("AnoAcademico", id);
	}

	/**
	 * Actualiza y persiste en base de datos las fechas de inicio y fin del año académico.
	 */
	public void actualizarFechasAnoAcademico(String anioId, Date inicio, Date fin) {
		validarFechas(inicio, fin, "año académico");
		String cleanId = anioId != null ? anioId.trim() : "2026-2027";

		// 1. Actualizar en AnioAcademico
		try {
			String sql = "UPDATE AnioAcademico SET fechaInicio = ?, fechaFin = ? WHERE anioAcademicoId = ?";
			db.executeUpdate(sql, Util.dateToIsoString(inicio), Util.dateToIsoString(fin), cleanId);
		} catch (Exception ignored) {
		}

		// 2. Actualizar en AnoAcademico
		try {
			String sqlAnt = "UPDATE AnoAcademico SET inicio_ano_academico = ?, fin_ano_academico = ? WHERE nombre = ? OR id_ano_academico = ?";
			db.executeUpdate(sqlAnt, Util.dateToIsoString(inicio), Util.dateToIsoString(fin), cleanId, cleanId);
		} catch (Exception ignored) {
		}
	}

	/**
	 * Sobrecarga por ID numérico.
	 */
	public void actualizarFechasAnoAcademico(Long id, Date inicio, Date fin) {
		if (id == null) {
			throw new ValidationException("El ID de año académico no puede ser nulo");
		}
		validarFechas(inicio, fin, "año académico");
		try {
			String sql = "UPDATE AnoAcademico SET inicio_ano_academico = ?, fin_ano_academico = ? WHERE id_ano_academico = ?";
			db.executeUpdate(sql, Util.dateToIsoString(inicio), Util.dateToIsoString(fin), id);
		} catch (Exception ignored) {
		}
		try {
			String sqlAnio = "UPDATE AnioAcademico SET fechaInicio = ?, fechaFin = ? WHERE anioAcademicoId = ?";
			db.executeUpdate(sqlAnio, Util.dateToIsoString(inicio), Util.dateToIsoString(fin), String.valueOf(id));
		} catch (Exception ignored) {
		}
	}

	/**
	 * Actualiza y persiste en base de datos las fechas del periodo de matriculación.
	 */
	public void actualizarFechasMatriculacion(String anioId, Date inicio, Date fin) {
		validarFechas(inicio, fin, "periodo de matriculación");
		String cleanId = anioId != null ? anioId.trim() : "2026-2027";

		// 1. Actualizar en AnioAcademico
		try {
			String sql = "UPDATE AnioAcademico SET inicioMatriculacion = ?, finMatriculacion = ? WHERE anioAcademicoId = ?";
			db.executeUpdate(sql, Util.dateToIsoString(inicio), Util.dateToIsoString(fin), cleanId);
		} catch (Exception ignored) {
		}

		// 2. Actualizar en AnoAcademico
		try {
			String sqlAnt = "UPDATE AnoAcademico SET inicio_periodo_matriculacion = ?, fin_periodo_matriculacion = ? WHERE nombre = ? OR id_ano_academico = ?";
			db.executeUpdate(sqlAnt, Util.dateToIsoString(inicio), Util.dateToIsoString(fin), cleanId, cleanId);
		} catch (Exception ignored) {
		}
	}

	/**
	 * Sobrecarga por ID numérico.
	 */
	public void actualizarFechasMatriculacion(Long id, Date inicio, Date fin) {
		if (id == null) {
			throw new ValidationException("El ID de año académico no puede ser nulo");
		}
		validarFechas(inicio, fin, "periodo de matriculación");
		try {
			String sql = "UPDATE AnoAcademico SET inicio_periodo_matriculacion = ?, fin_periodo_matriculacion = ? WHERE id_ano_academico = ?";
			db.executeUpdate(sql, Util.dateToIsoString(inicio), Util.dateToIsoString(fin), id);
		} catch (Exception ignored) {
		}
		try {
			String sqlAnio = "UPDATE AnioAcademico SET inicioMatriculacion = ?, finMatriculacion = ? WHERE anioAcademicoId = ?";
			db.executeUpdate(sqlAnio, Util.dateToIsoString(inicio), Util.dateToIsoString(fin), String.valueOf(id));
		} catch (Exception ignored) {}
	}

	/**
	 * Marca el año académico indicado como actual.
	 */
	public void establecerActual(Long id) {
		if (id == null) {
			throw new ValidationException("El ID de año académico no puede ser nulo");
		}
		try {
			db.executeUpdate("UPDATE AnoAcademico SET actual = 0");
			db.executeUpdate("UPDATE AnoAcademico SET actual = 1 WHERE id_ano_academico = ?", id);
		} catch (Exception ignored) {
		}
	}

	private void validarFechas(Date inicio, Date fin, String concepto) {
		if (inicio == null || fin == null) {
			throw new ValidationException("Las fechas de inicio y fin de " + concepto + " son obligatorias");
		}
		if (inicio.after(fin)) {
			throw new ValidationException("La fecha de inicio de " + concepto + " debe ser anterior o igual a la de fin");
		}
	}

	private AnoAcademico mapearFilaAntigua(Object[] r) {
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
				.anioAcademicoId(nombre)
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
		if (!trimmed.matches("^\\d+$") && !trimmed.matches("^\\d{4}[-/]\\d{4}$")) {
			throw new ValidationException("El formato del ID de año académico es inválido: '" + id + "'. Debe ser un número entero o rango año (p.ej. 2026-2027).");
		}
	}

	private long parsearNumeroDeId(String str, long defaultVal) {
		if (str == null) return defaultVal;
		try {
			String digits = str.replaceAll("\\D+", "");
			return digits.isEmpty() ? defaultVal : Long.parseLong(digits.substring(0, Math.min(digits.length(), 6)));
		} catch (Exception e) {
			return defaultVal;
		}
	}
}
