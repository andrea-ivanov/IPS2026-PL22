package model.matriculacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;

import util.Database;
import util.UnexpectedException;
import util.exception.ValidationException;

/**
 * Modelo de negocio y caso de uso para la gestión y búsqueda de
 * Matriculaciones.
 * Integra la resolución de Alumno, asignaturas/cursos matriculados y el mapeo
 * tipado
 * de fechas, enums (EstadoMatricula) y booleanos.
 */
public class MatriculacionModel {
	// Constantes de negocio según docs/DATABASE.md (Sección 6.1)
	public static final double PRECIO_CREDITO = 20.0;
	public static final int CREDITOS_MINIMOS_ANIO = 12;
	public static final int CREDITOS_MINIMOS_TERCER_ANIO = 90;
	public static final int MAX_CONVOCATORIAS = 6;
	public static final double NOTA_APROBADO = 5.0;

	private Database db;

	public MatriculacionModel(Database db) {
		this.db = db;
	}

	/**
	 * Obtiene la lista de matrículas de un estudiante dado su UO o alumnoId.
	 * Cruza la tabla Matricula con Grado para poblar MatriculacionDisplayDTO.
	 *
	 * @param uo Identificador del alumno (p. ej. "AL001", "1")
	 * @return Lista de MatriculacionDisplayDTO correspondientes al alumno
	 */
	public List<MatriculacionDisplayDTO> getMatriculasPorEstudiante(String uo) {
		if (uo == null || uo.trim().isEmpty()) {
			return new ArrayList<>();
		}
		String sql = "SELECT m.matriculaId AS id, "
				+ "       g.nombre AS grado, "
				+ "       m.estado AS estado, "
				+ "       m.fechaSolicitud AS fecha "
				+ "FROM Matricula m "
				+ "JOIN Grado g ON m.gradoId = g.gradoId "
				+ "WHERE m.alumnoId = ?";
		return db.executeQueryPojo(MatriculacionDisplayDTO.class, sql, resolverAlumnoId(uo));
	}

	/**
	 * Resuelve el identificador oficial de un alumno en la base de datos (p. ej. "AL001")
	 * tanto si se le pasa con prefijo UO ("UO001"), AL ("AL001") o numérico ("1").
	 * Garantiza el acceso coherente a los datos de la base de datos.
	 *
	 * @param uoOrId Código UO o identificador del alumno
	 * @return Identificador exacto en la tabla Alumno
	 */
	public String resolverAlumnoId(String uoOrId) {
		if (uoOrId == null || uoOrId.trim().isEmpty()) {
			return "";
		}
		String clean = uoOrId.trim();
		if (clean.contains(" - ")) {
			clean = clean.split(" - ")[0].trim();
		}
		// 1. Coincidencia directa
		List<Object[]> r1 = db.executeQueryArray("SELECT alumnoId FROM Alumno WHERE alumnoId = ?", clean);
		if (r1 != null && !r1.isEmpty() && r1.get(0)[0] != null) {
			return r1.get(0)[0].toString();
		}
		// 2. Si empieza por UO / uo, probar con AL
		if (clean.toUpperCase().startsWith("UO")) {
			String alt = "AL" + clean.substring(2);
			List<Object[]> r2 = db.executeQueryArray("SELECT alumnoId FROM Alumno WHERE alumnoId = ?", alt);
			if (r2 != null && !r2.isEmpty() && r2.get(0)[0] != null) {
				return r2.get(0)[0].toString();
			}
		}
		// 3. Si empieza por AL / al, probar con UO
		if (clean.toUpperCase().startsWith("AL")) {
			String alt = "UO" + clean.substring(2);
			List<Object[]> r3 = db.executeQueryArray("SELECT alumnoId FROM Alumno WHERE alumnoId = ?", alt);
			if (r3 != null && !r3.isEmpty() && r3.get(0)[0] != null) {
				return r3.get(0)[0].toString();
			}
		}
		return clean;
	}

	/**
	 * Obtiene el nombre de pila del alumno a partir de su UO o alumnoId o cadena "UO - Nombre".
	 *
	 * @param uoOrId Código UO, ID del alumno o etiqueta "UOxxxxx - Nombre"
	 * @return Nombre del estudiante, o cadena vacía si no se encuentra
	 */
	public String obtenerNombreAlumno(String uoOrId) {
		if (uoOrId != null && uoOrId.contains(" - ")) {
			String[] parts = uoOrId.split(" - ", 2);
			if (parts.length > 1 && !parts[1].trim().isEmpty()) {
				return parts[1].trim();
			}
		}
		String idReal = resolverAlumnoId(uoOrId);
		List<Object[]> rows = db.executeQueryArray("SELECT nombre FROM Alumno WHERE alumnoId = ?", idReal);
		if (rows != null && !rows.isEmpty() && rows.get(0)[0] != null) {
			return rows.get(0)[0].toString();
		}
		return "";
	}

	/**
	 * Obtiene los grados en los que está o ha estado matriculado el estudiante.
	 * Formato para el combo: "G001 - Ingenieria Informatica del Software".
	 *
	 * @param uo Identificador del alumno (p. ej. "AL001")
	 * @return Lista de Object[] para poblar el combo de selección de grado
	 */
	public List<Object[]> getGradosPorEstudiante(String uo) {
		if (uo == null || uo.trim().isEmpty()) {
			return new ArrayList<>();
		}
		String sql = "SELECT DISTINCT g.gradoId || ' - ' || g.nombre "
				+ "FROM Matricula m "
				+ "JOIN Grado g ON m.gradoId = g.gradoId "
				+ "WHERE m.alumnoId = ? "
				+ "ORDER BY g.gradoId";
		return db.executeQueryArray(sql, resolverAlumnoId(uo));
	}

	/**
	 * Obtiene las asignaturas matriculadas de un estudiante filtradas por el grado
	 * consultado.
	 * Implementa la consulta de referencia de la HU 35232 de docs/DATABASE.md.
	 *
	 * @param uo      Identificador del alumno (p. ej. "AL001")
	 * @param gradoId Identificador del grado (p. ej. "G001") o null para todos
	 * @return Lista de MatriculacionDisplayDTO con anioAcademicoId, anio, curso,
	 *         numMatricula y estado
	 */
	public List<MatriculacionDisplayDTO> getMatriculasPorEstudianteYGrado(String uo, String gradoId) {
		if (uo == null || uo.trim().isEmpty()) {
			return new ArrayList<>();
		}
		String cleanUo = resolverAlumnoId(uo);
		String sql = "SELECT m.matriculaId AS id, "
				+ "       m.anioAcademicoId, "
				+ "       c.nombre AS curso, "
				+ "       gc.anio, "
				+ "       mc.estado, "
				+ "       (SELECT COUNT(*) FROM MatriculaCurso mc2 "
				+ "        JOIN Matricula m2 ON m2.matriculaId = mc2.matriculaId "
				+ "        WHERE m2.alumnoId = m.alumnoId AND mc2.cursoId = mc.cursoId "
				+ "          AND m2.anioAcademicoId <= m.anioAcademicoId) AS numMatricula "
				+ "FROM MatriculaCurso mc "
				+ "JOIN Matricula m ON m.matriculaId = mc.matriculaId "
				+ "JOIN Curso c ON c.cursoId = mc.cursoId "
				+ "JOIN GradoCurso gc ON gc.gradoId = mc.gradoId AND gc.cursoId = mc.cursoId "
				+ "WHERE m.alumnoId = ? ";
		String orden = "ORDER BY gc.anio ASC, numMatricula DESC, c.nombre ASC";
		if (gradoId != null && !gradoId.trim().isEmpty()) {
			return db.executeQueryPojo(MatriculacionDisplayDTO.class, sql + "AND m.gradoId = ? " + orden,
					cleanUo, gradoId.trim());
		}
		return db.executeQueryPojo(MatriculacionDisplayDTO.class, sql + orden, cleanUo);
	}

	/**
	 * Obtiene el resumen de hitos del régimen de permanencia del estudiante (HU 35232 - Paso 3).
	 * Compara:
	 * 1. Créditos superados en el último año evaluado frente a 12 créditos.
	 * 2. Créditos superados acumulados en el grado frente a 90 créditos (al tercer año).
	 *
	 * @param uo Identificador del alumno (p. ej. "AL001")
	 * @param gradoId Identificador del grado (p. ej. "G001")
	 * @return Objeto HitosPermanenciaDTO con el cálculo realizado
	 */
	public HitosPermanenciaDTO getHitosPermanencia(String uo, String gradoId) {
		if (uo == null || uo.trim().isEmpty()) {
			return new HitosPermanenciaDTO("", "", null, 0, 0, 0);
		}
		String cleanUo = resolverAlumnoId(uo);
		String cleanGrado = gradoId != null ? gradoId.trim() : "";

		String ultimoAnoEvaluadoId = null;
		int creditosAnuales = 0;
		int creditosTotales = 0;
		int aniosEnGrado = 0;

		// 1. Créditos superados en cada año matriculado
		String sqlAnual = "SELECT m.matriculaId, m.anioAcademicoId, m.estado, "
				+ "       COALESCE(SUM(c.creditos), 0) AS creditosSuperados "
				+ "FROM Matricula m "
				+ "LEFT JOIN (SELECT DISTINCT matriculaId, cursoId FROM Calificacion WHERE nota >= " + NOTA_APROBADO + ") a "
				+ "       ON a.matriculaId = m.matriculaId "
				+ "LEFT JOIN Curso c ON c.cursoId = a.cursoId "
				+ "WHERE m.alumnoId = ? "
				+ (cleanGrado.isEmpty() ? "" : "AND m.gradoId = ? ")
				+ "GROUP BY m.matriculaId, m.anioAcademicoId, m.estado "
				+ "ORDER BY m.anioAcademicoId DESC";

		List<Object[]> rowsAnual = cleanGrado.isEmpty()
				? db.executeQueryArray(sqlAnual, cleanUo)
				: db.executeQueryArray(sqlAnual, cleanUo, cleanGrado);

		if (rowsAnual != null && !rowsAnual.isEmpty()) {
			Object[] selectedRow = rowsAnual.get(0);
			if (rowsAnual.size() > 1) {
				int creds0 = selectedRow[3] != null ? ((Number) selectedRow[3]).intValue() : 0;
				String estado0 = selectedRow[2] != null ? selectedRow[2].toString() : "";
				if (creds0 == 0 && ("ACTIVA".equalsIgnoreCase(estado0) || "SOLICITADA".equalsIgnoreCase(estado0))) {
					selectedRow = rowsAnual.get(1); // Usamos el último año cerrado / con evaluaciones
				}
			}
			ultimoAnoEvaluadoId = selectedRow[1] != null ? selectedRow[1].toString() : null;
			creditosAnuales = selectedRow[3] != null ? ((Number) selectedRow[3]).intValue() : 0;
		}

		// 2. Años en el grado y créditos totales acumulados (frente a 90)
		String sqlTotal = "SELECT COUNT(*) AS aniosEnGrado, "
				+ "       COALESCE((SELECT SUM(c.creditos) "
				+ "                 FROM (SELECT DISTINCT ca.cursoId "
				+ "                       FROM Calificacion ca "
				+ "                       JOIN Matricula m2 ON m2.matriculaId = ca.matriculaId "
				+ "                       WHERE m2.alumnoId = ? "
				+ (cleanGrado.isEmpty() ? "" : "AND m2.gradoId = ? ")
				+ "                         AND ca.nota >= " + NOTA_APROBADO + ") s "
				+ "                 JOIN Curso c ON c.cursoId = s.cursoId), 0) AS creditosTotales "
				+ "FROM Matricula "
				+ "WHERE alumnoId = ? "
				+ (cleanGrado.isEmpty() ? "" : "AND gradoId = ?");

		List<Object[]> rowsTotal = cleanGrado.isEmpty()
				? db.executeQueryArray(sqlTotal, cleanUo, cleanUo)
				: db.executeQueryArray(sqlTotal, cleanUo, cleanGrado, cleanUo, cleanGrado);

		if (rowsTotal != null && !rowsTotal.isEmpty()) {
			Object[] r = rowsTotal.get(0);
			aniosEnGrado = r[0] != null ? ((Number) r[0]).intValue() : 0;
			creditosTotales = r[1] != null ? ((Number) r[1]).intValue() : 0;
		}

		return new HitosPermanenciaDTO(cleanUo, cleanGrado, ultimoAnoEvaluadoId, creditosAnuales, creditosTotales, aniosEnGrado);
	}

	/**
	 * Obtiene el historial de calificaciones obtenidas por un estudiante en las convocatorias presentadas (HU 35232 - Paso 4).
	 *
	 * @param uo Identificador del alumno
	 * @param gradoId Identificador del grado (opcional)
	 * @return Lista de CalificacionDisplayDTO
	 */
	public List<CalificacionDisplayDTO> getCalificacionesPorEstudianteYGrado(String uo, String gradoId) {
		if (uo == null || uo.trim().isEmpty()) {
			return new ArrayList<>();
		}
		String cleanUo = resolverAlumnoId(uo);
		String sql = "SELECT ca.anioAcademicoId, "
				+ "       ca.cursoId, "
				+ "       c.nombre AS curso, "
				+ "       ca.tipoConvocatoria, "
				+ "       ca.nota "
				+ "FROM Calificacion ca "
				+ "JOIN Matricula m ON m.matriculaId = ca.matriculaId "
				+ "JOIN Curso c ON c.cursoId = ca.cursoId "
				+ "WHERE m.alumnoId = ? ";
		String orden = "ORDER BY ca.anioAcademicoId DESC, c.nombre ASC, ca.tipoConvocatoria ASC";
		if (gradoId != null && !gradoId.trim().isEmpty()) {
			return db.executeQueryPojo(CalificacionDisplayDTO.class, sql + "AND m.gradoId = ? " + orden,
					cleanUo, gradoId.trim());
		}
		return db.executeQueryPojo(CalificacionDisplayDTO.class, sql + orden, cleanUo);
	}

	/**
	 * Comprueba si el estudiante ya cuenta con una matrícula registrada (no cancelada)
	 * para el grado y año académico indicados (impide solicitudes duplicadas - HU 35235).
	 *
	 * @param alumnoId Identificador del alumno (p. ej. "AL001")
	 * @param gradoId Identificador del grado (p. ej. "G001")
	 * @param anioAcademicoId Identificador del año académico (p. ej. "2026-2027")
	 * @return true si ya existe una matrícula previa no cancelada
	 */
	public boolean comprobarMatriculaExistente(String alumnoId, String gradoId, String anioAcademicoId) {
		if (alumnoId == null || gradoId == null || anioAcademicoId == null) {
			return false;
		}
		String cleanAlumno = resolverAlumnoId(alumnoId);
		String sql = "SELECT COUNT(*) FROM Matricula "
				+ "WHERE alumnoId = ? AND gradoId = ? AND anioAcademicoId = ? "
				+ "  AND estado != 'CANCELADA'";
		List<Object[]> rows = db.executeQueryArray(sql, cleanAlumno, gradoId.trim(), anioAcademicoId.trim());
		if (rows != null && !rows.isEmpty() && rows.get(0)[0] != null) {
			return ((Number) rows.get(0)[0]).intValue() > 0;
		}
		return false;
	}

	/**
	 * Devuelve el número de convocatorias con calificación numérica consumidas por el estudiante
	 * en una asignatura a lo largo de todos sus años y grados (HU 35235 / Regla 6.3).
	 * Las notas nulas (no presentado) no computan.
	 *
	 * @param alumnoId Identificador del alumno
	 * @param cursoId Identificador de la asignatura (p. ej. "C005")
	 * @return Cantidad de convocatorias consumidas (0 a 6)
	 */
	public int getConvocatoriasConsumidas(String alumnoId, String cursoId) {
		if (alumnoId == null || cursoId == null) {
			return 0;
		}
		String cleanAlumno = resolverAlumnoId(alumnoId);
		String sql = "SELECT COUNT(*) AS convocatoriasConsumidas "
				+ "FROM Calificacion ca "
				+ "JOIN Matricula m ON m.matriculaId = ca.matriculaId "
				+ "WHERE m.alumnoId = ? AND ca.cursoId = ? "
				+ "  AND ca.nota IS NOT NULL";
		List<Object[]> rows = db.executeQueryArray(sql, cleanAlumno, cursoId.trim());
		if (rows != null && !rows.isEmpty() && rows.get(0)[0] != null) {
			return ((Number) rows.get(0)[0]).intValue();
		}
		return 0;
	}

	/**
	 * Comprueba si el alumno se encuentra al corriente de pago (HU 35235 / Regla 6.4).
	 * No debe tener asignaturas SOLICITADAS sin pagar en años académicos ya iniciados,
	 * ni cancelaciones previas por motivo de IMPAGO.
	 *
	 * @param alumnoId Identificador del alumno
	 * @return true si está al corriente de pago, false si tiene deudas o impagos pendientes
	 */
	public boolean comprobarAlCorrienteDePago(String alumnoId) {
		if (alumnoId == null || alumnoId.trim().isEmpty()) {
			return true;
		}
		String cleanId = resolverAlumnoId(alumnoId);
		// 1. Asignaturas sin pagar con el año ya iniciado
		String sqlSinPagar = "SELECT COUNT(*) FROM MatriculaCurso mc "
				+ "JOIN Matricula m ON m.matriculaId = mc.matriculaId "
				+ "JOIN AnioAcademico a ON a.anioAcademicoId = m.anioAcademicoId "
				+ "WHERE m.alumnoId = ? AND mc.estado = 'SOLICITADA' "
				+ "  AND (mc.metodoPago IS NULL OR mc.pagoValidado = 0) "
				+ "  AND a.fechaInicio <= date('now')";
		List<Object[]> r1 = db.executeQueryArray(sqlSinPagar, cleanId);
		if (r1 != null && !r1.isEmpty() && r1.get(0)[0] != null && ((Number) r1.get(0)[0]).intValue() > 0) {
			return false;
		}

		// 2. Cancelaciones por IMPAGO
		String sqlImpago = "SELECT COUNT(*) FROM Matricula m "
				+ "LEFT JOIN MatriculaCurso mc ON mc.matriculaId = m.matriculaId "
				+ "WHERE m.alumnoId = ? AND (m.motivoCancelacion = 'IMPAGO' OR mc.motivoCancelacion = 'IMPAGO')";
		List<Object[]> r2 = db.executeQueryArray(sqlImpago, cleanId);
		if (r2 != null && !r2.isEmpty() && r2.get(0)[0] != null && ((Number) r2.get(0)[0]).intValue() > 0) {
			return false;
		}
		return true;
	}

	/**
	 * Verifica si el estudiante cumple el régimen de permanencia en el grado (HU 35235 / Regla 6.3):
	 * 1. Mínimo anual: si la matrícula anterior en este grado no fue cancelada, debió superar >= 12 créditos.
	 * 2. Mínimo al tercer año: si ya acumula >= 3 años en el grado, debe sumar >= 90 créditos totales superados.
	 *
	 * @param alumnoId Identificador del alumno
	 * @param gradoId Identificador del grado
	 * @return true si cumple el régimen de permanencia
	 */
	public boolean comprobarCumplimientoPermanencia(String alumnoId, String gradoId) {
		HitosPermanenciaDTO hitos = getHitosPermanencia(alumnoId, gradoId);
		if (hitos == null) {
			return true;
		}

		// Si tuvo año evaluado previo, debe superar el mínimo anual de 12 créditos
		if (hitos.getUltimoAnoEvaluadoId() != null && !hitos.getUltimoAnoEvaluadoId().isEmpty()) {
			if (!hitos.isCumpleHitoAnual()) {
				return false;
			}
		}

		// Si lleva 3 o más años, debe sumar al menos 90 créditos
		if (!hitos.isCumpleHitoTercerAnio()) {
			return false;
		}

		return true;
	}

	/**
	 * Valida integralmente una solicitud de matrícula frente a todas las reglas de negocio (HU 35235).
	 * Lanza ValidationException con un mensaje descriptivo si alguna regla es vulnerada.
	 *
	 * @param alumnoId Identificador del alumno
	 * @param gradoId Identificador del grado solicitado
	 * @param anioAcademicoId Año académico actual
	 * @param cursoIds Lista de asignaturas seleccionadas
	 * @throws ValidationException si alguna regla no se cumple
	 */
	public void validarSolicitudMatricula(String alumnoId, String gradoId, String anioAcademicoId, List<String> cursoIds) {
		if (alumnoId == null || alumnoId.trim().isEmpty()) {
			throw new ValidationException("Debe especificarse el identificador del alumno.");
		}
		if (gradoId == null || gradoId.trim().isEmpty()) {
			throw new ValidationException("Debe seleccionar un grado para la matrícula.");
		}
		if (anioAcademicoId == null || anioAcademicoId.trim().isEmpty()) {
			throw new ValidationException("Debe especificarse el año académico actual.");
		}
		if (cursoIds == null || cursoIds.isEmpty()) {
			throw new ValidationException("Debe seleccionar al menos una asignatura para matricularse.");
		}

		// 1. Validar al corriente de pago
		if (!comprobarAlCorrienteDePago(alumnoId)) {
			throw new ValidationException("No es posible solicitar la matrícula: el estudiante tiene pagos pendientes o deudas previas.");
		}

		// 2. Validar solicitud duplicada
		if (comprobarMatriculaExistente(alumnoId, gradoId, anioAcademicoId)) {
			throw new ValidationException("Ya existe una matrícula activa o en trámite para este grado en el año académico " + anioAcademicoId + ".");
		}

		// 3. Validar régimen de permanencia
		if (!comprobarCumplimientoPermanencia(alumnoId, gradoId)) {
			throw new ValidationException("La matrícula no cumple con los requisitos del régimen de permanencia en este grado.");
		}

		// 4. Validar límite de convocatorias por cada asignatura solicitada
		for (String cId : cursoIds) {
			int consumidas = getConvocatoriasConsumidas(alumnoId, cId);
			if (consumidas >= MAX_CONVOCATORIAS) {
				throw new ValidationException("No es posible matricularse en la asignatura '" + cId + "': se ha alcanzado el límite de " + MAX_CONVOCATORIAS + " convocatorias.");
			}
		}
	}

	/**
	 * Consulta la oferta de grados disponibles para nueva matrícula (HU 35235 / Fase 2).
	 * Solo incluye grados en estado 'VIGENTE' o 'PENDIENTE_RENOVACION',
	 * excluyendo los grados 'EN_EXTINCION' y 'EXTINTO'.
	 *
	 * @return Lista de OfertaGradoDTO disponibles
	 */
	public List<OfertaGradoDTO> getOfertaGradosDisponibles() {
		String sql = "SELECT gradoId, nombre, facultad, ramaConocimiento, estado "
				+ "FROM Grado "
				+ "WHERE estado IN ('VIGENTE', 'PENDIENTE_RENOVACION') "
				+ "ORDER BY nombre ASC";
		return db.executeQueryPojo(OfertaGradoDTO.class, sql);
	}

	/**
	 * Consulta las asignaturas activas ofertadas para un grado seleccionado (HU 35235 / Fase 2).
	 * Incluye el cálculo del precio (20.0 €/crédito) y, si se facilita el identificador del alumno,
	 * el número de convocatorias consumidas con calificación en cada asignatura.
	 *
	 * @param gradoId Identificador del grado (p. ej. "G001")
	 * @param alumnoId Identificador del alumno (p. ej. "AL001", opcional para cálculo de convocatorias)
	 * @return Lista de OfertaCursoDTO activos en dicho grado
	 */
	public List<OfertaCursoDTO> getOfertaCursosPorGrado(String gradoId, String alumnoId) {
		if (gradoId == null || gradoId.trim().isEmpty()) {
			return new ArrayList<>();
		}
		String sql = "SELECT c.cursoId, c.nombre, gc.anio, c.creditos, gc.itinerario "
				+ "FROM GradoCurso gc "
				+ "JOIN Curso c ON c.cursoId = gc.cursoId "
				+ "WHERE gc.gradoId = ? AND gc.estado = 'ACTIVO' "
				+ "ORDER BY gc.anio ASC, c.nombre ASC";
		List<OfertaCursoDTO> list = db.executeQueryPojo(OfertaCursoDTO.class, sql, gradoId.trim());
		for (OfertaCursoDTO curso : list) {
			if (curso.getPrecio() <= 0 && curso.getCreditos() > 0) {
				curso.setPrecio(curso.getCreditos() * PRECIO_CREDITO);
			}
			if (alumnoId != null && !alumnoId.trim().isEmpty()) {
				curso.setConvocatoriasConsumidas(getConvocatoriasConsumidas(alumnoId, curso.getCursoId()));
			}
		}
		return list;
	}

	/**
	 * Sobrecarga de conveniencia para consultar las asignaturas ofertadas sin contextualizar alumno.
	 *
	 * @param gradoId Identificador del grado
	 * @return Lista de OfertaCursoDTO activos en dicho grado
	 */
	public List<OfertaCursoDTO> getOfertaCursosPorGrado(String gradoId) {
		return getOfertaCursosPorGrado(gradoId, null);
	}

	/**
	 * Obtiene el identificador del año académico en curso o más reciente (HU 35235 / Fase 2).
	 * Comprueba primero si la fecha actual está dentro del rango oficial (fechaInicio - fechaFin).
	 *
	 * @return Identificador del año académico (p. ej. "2026-2027")
	 */
	public String getAnioAcademicoActual() {
		String sqlVigente = "SELECT anioAcademicoId FROM AnioAcademico "
				+ "WHERE date('now') BETWEEN fechaInicio AND fechaFin LIMIT 1";
		List<Object[]> rows = db.executeQueryArray(sqlVigente);
		if (rows != null && !rows.isEmpty() && rows.get(0)[0] != null) {
			return rows.get(0)[0].toString();
		}

		String sqlMasReciente = "SELECT anioAcademicoId FROM AnioAcademico ORDER BY fechaInicio DESC LIMIT 1";
		List<Object[]> rowsReciente = db.executeQueryArray(sqlMasReciente);
		if (rowsReciente != null && !rowsReciente.isEmpty() && rowsReciente.get(0)[0] != null) {
			return rowsReciente.get(0)[0].toString();
		}
		return "2026-2027";
	}

	/**
	 * Alias explícito para comprobar las convocatorias consumidas con calificación en una asignatura
	 * (solicitado según la nomenclatura de la Fase 2 del plan de trabajo).
	 *
	 * @param alumnoId Identificador del alumno
	 * @param cursoId Identificador de la asignatura
	 * @return Número de convocatorias consumidas (con nota != null)
	 */
	public int comprobarConvocatorias(String alumnoId, String cursoId) {
		return getConvocatoriasConsumidas(alumnoId, cursoId);
	}

	/**
	 * Alias explícito para comprobar el cumplimiento y los hitos del régimen de permanencia
	 * (solicitado según la nomenclatura de la Fase 2 del plan de trabajo).
	 *
	 * @param alumnoId Identificador del alumno
	 * @param gradoId Identificador del grado
	 * @return Objeto HitosPermanenciaDTO con el desglose de permanencia
	 */
	public HitosPermanenciaDTO comprobarPermanencia(String alumnoId, String gradoId) {
		return getHitosPermanencia(alumnoId, gradoId);
	}

	/**
	 * Genera de forma segura el siguiente identificador correlativo para una matrícula (p. ej. "M02101").
	 * Utiliza la conexión proporcionada para operar dentro de una transacción.
	 *
	 * @param conn Conexión JDBC activa
	 * @return Nuevo identificador formateado como "M%05d", el mismo que data.sql
	 * @throws SQLException si ocurre un error SQL
	 */
	public String generarSiguienteMatriculaId(Connection conn) throws SQLException {
		String sql = "SELECT MAX(CAST(SUBSTR(matriculaId, 2) AS INTEGER)) FROM Matricula WHERE matriculaId LIKE 'M%'";
		try (Statement stmt = conn.createStatement();
			 ResultSet rs = stmt.executeQuery(sql)) {
			int maxNum = 0;
			if (rs.next()) {
				maxNum = rs.getInt(1);
			}
			return String.format("M%05d", maxNum + 1);
		}
	}

	/**
	 * Registra de forma transaccional una nueva solicitud de matrícula en la base de datos (HU 35235 / Fase 2).
	 * <p>
	 * Reglas de persistencia:
	 * 1. Valida previamente permanencia, convocatorias, al corriente de pago y ausencia de duplicidad.
	 * 2. Inserta el registro en {@code Matricula} con estado {@code 'SOLICITADA'}.
	 * 3. Inserta cada asignatura en {@code MatriculaCurso}:
	 *    - Si el pago es con {@code TARJETA}: estado {@code 'PAGADA'}, pagoValidado {@code = 1}.
	 *    - Si es {@code TRANSFERENCIA} o {@code CARTA_PAGO}: estado {@code 'SOLICITADA'}, pagoValidado {@code = 0} (requiere aprobación manual de secretaría).
	 * </p>
	 *
	 * @param solicitud DTO consolidado con toda la información de la solicitud y método de pago
	 * @return El identificador de la nueva matrícula registrada (p. ej. "M02101")
	 * @throws ValidationException si alguna validación de negocio falla
	 * @throws UnexpectedException si ocurre algún fallo transaccional en base de datos
	 */
	public String registrarSolicitudMatricula(ResumenSolicitudDTO solicitud) {
		if (solicitud == null) {
			throw new ValidationException("La información de la solicitud de matrícula no puede ser nula.");
		}

		String alumnoId = resolverAlumnoId(solicitud.getAlumnoId());
		String gradoId = solicitud.getGradoId();
		String anioAcademicoId = solicitud.getAnioAcademicoId();
		if (anioAcademicoId == null || anioAcademicoId.trim().isEmpty()) {
			anioAcademicoId = getAnioAcademicoActual();
			solicitud.setAnioAcademicoId(anioAcademicoId);
		}

		List<OfertaCursoDTO> asignaturas = solicitud.getAsignaturas();
		if (asignaturas == null || asignaturas.isEmpty()) {
			throw new ValidationException("Debe seleccionar al menos una asignatura para matricularse.");
		}

		List<String> cursoIds = new ArrayList<>();
		for (OfertaCursoDTO c : asignaturas) {
			if (c != null && c.getCursoId() != null) {
				cursoIds.add(c.getCursoId().trim());
			}
		}

		// Validar reglas de negocio completas (permanencia, convocatorias, duplicados, al corriente de pago)
		validarSolicitudMatricula(alumnoId, gradoId, anioAcademicoId, cursoIds);

		// Validar método de pago admitido
		String metodoPago = solicitud.getMetodoPago();
		if (metodoPago == null || metodoPago.trim().isEmpty()) {
			throw new ValidationException("Debe seleccionar un método de pago (TARJETA, TRANSFERENCIA o CARTA_PAGO).");
		}
		metodoPago = metodoPago.trim().toUpperCase();
		if (!"TARJETA".equals(metodoPago) && !"TRANSFERENCIA".equals(metodoPago) && !"CARTA_PAGO".equals(metodoPago)) {
			throw new ValidationException("Método de pago '" + metodoPago + "' no reconocido. Opciones válidas: TARJETA, TRANSFERENCIA o CARTA_PAGO.");
		}

		// Fecha de la solicitud (formato ISO YYYY-MM-DD)
		String fecha = solicitud.getFechaSolicitud();
		if (fecha == null || fecha.trim().isEmpty()) {
			fecha = LocalDate.now().toString();
			solicitud.setFechaSolicitud(fecha);
		} else {
			fecha = fecha.trim();
		}

		Connection conn = null;
		try {
			conn = db.getConnection();
			// Asegurar enforcement de claves foráneas en la conexión SQLite
			try (Statement pragmaStmt = conn.createStatement()) {
				pragmaStmt.execute("PRAGMA foreign_keys = ON;");
			}

			// Iniciar transacción
			conn.setAutoCommit(false);

			// 1. Obtener nuevo ID correlativo de matrícula
			String matriculaId = generarSiguienteMatriculaId(conn);

			// 2. Insertar cabecera en Matricula (siempre SOLICITADA)
			String sqlMatricula = "INSERT INTO Matricula (matriculaId, alumnoId, gradoId, anioAcademicoId, fechaSolicitud, estado, motivoCancelacion, fechaCancelacion) "
					+ "VALUES (?, ?, ?, ?, ?, 'SOLICITADA', NULL, NULL)";
			try (PreparedStatement psMatricula = conn.prepareStatement(sqlMatricula)) {
				psMatricula.setString(1, matriculaId);
				psMatricula.setString(2, alumnoId.trim());
				psMatricula.setString(3, gradoId.trim());
				psMatricula.setString(4, anioAcademicoId.trim());
				psMatricula.setString(5, fecha);
				psMatricula.executeUpdate();
			}

			// 3. Determinar estado y pago según método elegido
			boolean esTarjeta = "TARJETA".equals(metodoPago);
			String estadoCurso = esTarjeta ? "PAGADA" : "SOLICITADA";
			int pagoValidado = esTarjeta ? 1 : 0;

			// 4. Insertar cada asignatura en MatriculaCurso
			String sqlCurso = "INSERT INTO MatriculaCurso (matriculaId, gradoId, cursoId, estado, motivoCancelacion, fechaCancelacion, importe, metodoPago, fechaPago, pagoValidado) "
					+ "VALUES (?, ?, ?, ?, NULL, NULL, ?, ?, ?, ?)";
			try (PreparedStatement psCurso = conn.prepareStatement(sqlCurso)) {
				for (OfertaCursoDTO c : asignaturas) {
					double importe = c.getPrecio() > 0 ? c.getPrecio() : c.getCreditos() * PRECIO_CREDITO;
					psCurso.setString(1, matriculaId);
					psCurso.setString(2, gradoId.trim());
					psCurso.setString(3, c.getCursoId().trim());
					psCurso.setString(4, estadoCurso);
					psCurso.setDouble(5, importe);
					psCurso.setString(6, metodoPago);
					psCurso.setString(7, fecha);
					psCurso.setInt(8, pagoValidado);
					psCurso.executeUpdate();
				}
			}

			// Confirmar transacción
			conn.commit();
			return matriculaId;

		} catch (SQLException ex) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (SQLException rollbackEx) {
					System.err.println("Error al revertir transacción de matrícula: " + rollbackEx.getMessage());
				}
			}
			if (ex.getMessage() != null && ex.getMessage().contains("UNIQUE constraint failed")) {
				throw new ValidationException("Ya existe una matrícula registrada para este estudiante en el grado y año indicados.");
			}
			throw new UnexpectedException(ex);
		} finally {
			DbUtils.closeQuietly(conn);
		}
	}

	/**
	 * Sobrecarga de conveniencia para registrar solicitud a partir de los datos atómicos.
	 *
	 * @param alumnoId Identificador del alumno
	 * @param gradoId Identificador del grado
	 * @param anioAcademicoId Año académico
	 * @param asignaturas Asignaturas seleccionadas
	 * @param metodoPago Método de pago ("TARJETA", "TRANSFERENCIA", "CARTA_PAGO")
	 * @return El ID de la matrícula creada
	 */
	public String registrarSolicitudMatricula(String alumnoId, String gradoId, String anioAcademicoId,
			List<OfertaCursoDTO> asignaturas, String metodoPago) {
		ResumenSolicitudDTO dto = new ResumenSolicitudDTO();
		dto.setAlumnoId(alumnoId);
		dto.setGradoId(gradoId);
		dto.setAnioAcademicoId(anioAcademicoId);
		dto.setAsignaturas(asignaturas);
		dto.setMetodoPago(metodoPago);
		return registrarSolicitudMatricula(dto);
	}

	/**
	 * Carga los datos informativos del alumno y del grado seleccionado para precargar
	 * el objeto ResumenSolicitudDTO que se presentará en la vista resumen (HU 35235).
	 *
	 * @param alumnoId Identificador del alumno
	 * @param gradoId Identificador del grado (opcional)
	 * @return DTO resumen precargado con datos del alumno, año académico y grado
	 */
	public ResumenSolicitudDTO obtenerDatosEstudianteParaSolicitud(String alumnoId, String gradoId) {
		ResumenSolicitudDTO resumen = new ResumenSolicitudDTO();
		String cleanAlumnoId = resolverAlumnoId(alumnoId);
		resumen.setAlumnoId(cleanAlumnoId);
		resumen.setGradoId(gradoId);
		resumen.setAnioAcademicoId(getAnioAcademicoActual());
		resumen.setFechaSolicitud(LocalDate.now().toString());

		if (cleanAlumnoId != null && !cleanAlumnoId.trim().isEmpty()) {
			String sqlAlumno = "SELECT dni, nombre, apellidos, email FROM Alumno WHERE alumnoId = ?";
			List<Object[]> rows = db.executeQueryArray(sqlAlumno, cleanAlumnoId.trim());
			if (rows != null && !rows.isEmpty()) {
				Object[] r = rows.get(0);
				resumen.setDni(r[0] != null ? r[0].toString() : "");
				String nom = r[1] != null ? r[1].toString() : "";
				String ape = r[2] != null ? r[2].toString() : "";
				resumen.setNombreCompleto((nom + " " + ape).trim());
				resumen.setEmail(r[3] != null ? r[3].toString() : "");
			}
		}

		if (gradoId != null && !gradoId.trim().isEmpty()) {
			String sqlGrado = "SELECT nombre FROM Grado WHERE gradoId = ?";
			List<Object[]> rows = db.executeQueryArray(sqlGrado, gradoId.trim());
			if (rows != null && !rows.isEmpty() && rows.get(0)[0] != null) {
				resumen.setNombreGrado(rows.get(0)[0].toString());
			}
		}

		return resumen;
	}

	/**
	 * Obtiene las matrículas (alumnos) asociadas a un curso específico para la tabla de AdminOfertaView.
	 */
	public List<MatriculacionDisplayDTO> getMatriculacionesPorCurso(String idCurso) {
		return getMatriculacionesPorCursoYAnoActual(idCurso, null);
	}

	/**
     * Obtiene las matrículas (alumnos) asociadas a un curso específico
     * filtradas por el año académico indicado.
     */
    public List<MatriculacionDisplayDTO> getMatriculacionesPorCursoYAnoActual(String idCurso, String anioAcademicoId) {
        if (idCurso == null) {
            return new ArrayList<>();
        }
        String sql = "SELECT a.alumnoId AS id, " +
                     "       a.nombre || ' ' || a.apellidos AS nombreAlumno, " +
                     "       mc.cursoId AS idCurso, " +
                     "       a.email AS email, " +
                     "       mc.estado AS estado " +
                     "FROM MatriculaCurso mc " +
                     "JOIN Matricula m ON mc.matriculaId = m.matriculaId " +
                     "JOIN Alumno a ON m.alumnoId = a.alumnoId " +
                     "WHERE mc.cursoId = ? ";
        String orden = "ORDER BY a.alumnoId";
        if (anioAcademicoId != null && !anioAcademicoId.isEmpty()) {
            return db.executeQueryPojo(MatriculacionDisplayDTO.class, sql + "AND m.anioAcademicoId = ? " + orden,
                    idCurso, anioAcademicoId);
        }
        return db.executeQueryPojo(MatriculacionDisplayDTO.class, sql + orden, idCurso);
    }
}
