package model.grado;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.matriculacion.MatriculacionModel;
import util.Database;

/**
 * Métricas de rendimiento académico de los grados para el revisor externo (HU 36411).
 * No se guardan: se calculan al consultar a partir de las matrículas y las calificaciones.
 * - Hay una fila por cada grado y año académico con alumnos matriculados (matrícula ACTIVA o FINALIZADA)
 * - Los grados EXTINTO no aparecen; los EN_EXTINCION sí
 * - Los PENDIENTE_RENOVACION y EN_EXTINCION aparecen siempre, aunque no tengan métricas (HU 36455)
 */
public class MetricasGradoModel {
	// solo cuentan las matrículas que han superado la solicitud y no se han cancelado
	private static final String MATRICULA_VALIDA = "('ACTIVA', 'FINALIZADA')";
	/** Grados que requieren acción del revisor: aparecen siempre, aunque no tengan métricas (HU 36455) */
	private static final List<String> ESTADOS_REQUIEREN_ACCION = List.of(EstadoGrado.PENDIENTE_RENOVACION.name(),
			EstadoGrado.EN_EXTINCION.name());

	private Database db;

	public MetricasGradoModel() {
		this.db = new Database();
	}

	public MetricasGradoModel(Database db) {
		this.db = db;
	}

	/**
	 * Métricas de cada grado en cada año académico, ordenadas por nombre del grado, facultad
	 * y año académico de más reciente a más antiguo.
	 * Los grados que requieren acción del revisor (HU 36455) aparecen siempre: si no tienen ninguna fila (o ninguna
	 * en el año pedido), aparecen una vez sin métricas, con el año pedido (null si son todos) y los contadores a 0.
	 *
	 * @param rama Parte de la rama de conocimiento, sin distinguir mayúsculas (null o vacío: todas)
	 * @param nombreGrado Parte del nombre del grado, sin distinguir mayúsculas (null o vacío: todos)
	 * @param anioAcademicoId Año académico exacto (null: todos)
	 * @param fechaHoy Fecha de referencia en formato ISO (AAAA-MM-DD)
	 */
	public List<GradoAnioRevisorDisplayDTO> getMetricasGrados(String rama, String nombreGrado, String anioAcademicoId,
			String fechaHoy) {
		List<Object> params = new ArrayList<>();
		// a: cada asignatura no cancelada de la matrícula, presentada (alguna nota) y superada (alguna nota >= 5) ese año
		String sql = "SELECT g.gradoId, g.nombre, g.facultad, g.ramaConocimiento, g.estado, m.anioAcademicoId, "
				+ "COUNT(DISTINCT m.alumnoId), COALESCE(SUM(a.superada), 0), "
				+ "COALESCE(SUM(a.presentada * a.creditos), 0), COALESCE(SUM(a.superada * a.creditos), 0) "
				+ "FROM Matricula m "
				+ "JOIN Grado g ON g.gradoId = m.gradoId "
				+ "LEFT JOIN (SELECT mc.matriculaId, c.creditos, "
				+ "                  MAX(CASE WHEN ca.nota IS NOT NULL THEN 1 ELSE 0 END) AS presentada, "
				+ "                  MAX(CASE WHEN ca.nota >= " + MatriculacionModel.NOTA_APROBADO + " THEN 1 ELSE 0 END) AS superada "
				+ "           FROM MatriculaCurso mc "
				+ "           JOIN Matricula ma ON ma.matriculaId = mc.matriculaId "
				+ "           JOIN Curso c ON c.cursoId = mc.cursoId "
				+ "           LEFT JOIN Calificacion ca ON ca.matriculaId = mc.matriculaId AND ca.cursoId = mc.cursoId "
				+ "                AND ca.anioAcademicoId = ma.anioAcademicoId "
				+ "           WHERE mc.estado <> 'CANCELADA' "
				+ "           GROUP BY mc.matriculaId, mc.cursoId) a ON a.matriculaId = m.matriculaId "
				+ "WHERE m.estado IN " + MATRICULA_VALIDA + " AND g.estado <> '" + EstadoGrado.EXTINTO.name() + "' ";
		if (rama != null && !rama.trim().isEmpty()) {
			sql += "AND LOWER(g.ramaConocimiento) LIKE ? ";
			params.add("%" + rama.trim().toLowerCase() + "%");
		}
		if (nombreGrado != null && !nombreGrado.trim().isEmpty()) {
			sql += "AND LOWER(g.nombre) LIKE ? ";
			params.add("%" + nombreGrado.trim().toLowerCase() + "%");
		}
		if (anioAcademicoId != null) {
			sql += "AND m.anioAcademicoId = ? ";
			params.add(anioAcademicoId);
		}
		sql += "GROUP BY g.gradoId, m.anioAcademicoId "
				+ "ORDER BY g.nombre, g.facultad, g.gradoId, m.anioAcademicoId DESC";

		List<Object[]> anios = db.executeQueryArray(
				"SELECT anioAcademicoId, finMatriculacion FROM AnioAcademico ORDER BY anioAcademicoId");
		List<GradoAnioRevisorDisplayDTO> metricas = new ArrayList<>();
		Set<String> gradosConFilas = new HashSet<>();
		for (Object[] r : db.executeQueryArray(sql, params.toArray())) {
			double creditosPresentados = ((Number) r[8]).doubleValue();
			double creditosSuperados = ((Number) r[9]).doubleValue();
			gradosConFilas.add((String) r[0]);
			metricas.add(new GradoAnioRevisorDisplayDTO((String) r[0], (String) r[1], (String) r[2], (String) r[3],
					(String) r[4], (String) r[5], porcentaje(creditosSuperados, creditosPresentados),
					tasaAbandono((String) r[0], (String) r[5], anios, fechaHoy),
					((Number) r[7]).intValue(), ((Number) r[6]).intValue()));
		}

		List<Object> paramsAccion = new ArrayList<>(ESTADOS_REQUIEREN_ACCION);
		String sqlAccion = "SELECT gradoId, nombre, facultad, ramaConocimiento, estado FROM Grado WHERE estado IN (?, ?) ";
		if (rama != null && !rama.trim().isEmpty()) {
			sqlAccion += "AND LOWER(ramaConocimiento) LIKE ? ";
			paramsAccion.add("%" + rama.trim().toLowerCase() + "%");
		}
		if (nombreGrado != null && !nombreGrado.trim().isEmpty()) {
			sqlAccion += "AND LOWER(nombre) LIKE ? ";
			paramsAccion.add("%" + nombreGrado.trim().toLowerCase() + "%");
		}
		for (Object[] g : db.executeQueryArray(sqlAccion, paramsAccion.toArray())) {
			if (!gradosConFilas.contains((String) g[0])) {
				metricas.add(new GradoAnioRevisorDisplayDTO((String) g[0], (String) g[1], (String) g[2], (String) g[3],
						(String) g[4], anioAcademicoId, null, null, 0, 0));
			}
		}
		metricas.sort(Comparator.comparing(GradoAnioRevisorDisplayDTO::getNombreGrado)
				.thenComparing(GradoAnioRevisorDisplayDTO::getFacultad)
				.thenComparing(GradoAnioRevisorDisplayDTO::getIdGrado)
				.thenComparing(GradoAnioRevisorDisplayDTO::getIdAnioAcademico,
						Comparator.nullsLast(Comparator.reverseOrder())));
		return metricas;
	}

	/**
	 * Métricas de un año a partir de la lista de todos los años (sin volver a consultar), en el mismo orden:
	 * las filas de ese año y, por cada grado que requiere acción (PENDIENTE_RENOVACION o EN_EXTINCION) sin fila en ese
	 * año, una fila sin métricas (tasas nulas y contadores a 0).
	 *
	 * @param todas Métricas de todos los años (getMetricasGrados con año nulo)
	 * @param anioAcademicoId Año académico
	 */
	public List<GradoAnioRevisorDisplayDTO> metricasDelAnio(List<GradoAnioRevisorDisplayDTO> todas,
			String anioAcademicoId) {
		Map<String, GradoAnioRevisorDisplayDTO> filaDelAnio = new LinkedHashMap<>();
		for (GradoAnioRevisorDisplayDTO m : todas) {
			filaDelAnio.putIfAbsent(m.getIdGrado(), null);
			if (anioAcademicoId.equals(m.getIdAnioAcademico())) {
				filaDelAnio.put(m.getIdGrado(), m);
			}
		}
		List<GradoAnioRevisorDisplayDTO> metricas = new ArrayList<>();
		for (Map.Entry<String, GradoAnioRevisorDisplayDTO> e : filaDelAnio.entrySet()) {
			if (e.getValue() != null) {
				metricas.add(e.getValue());
			} else {
				GradoAnioRevisorDisplayDTO g = todas.stream().filter(m -> m.getIdGrado().equals(e.getKey()))
						.findFirst().get();
				if (ESTADOS_REQUIEREN_ACCION.contains(g.getEstado())) {
					metricas.add(new GradoAnioRevisorDisplayDTO(g.getIdGrado(), g.getNombreGrado(), g.getFacultad(),
							g.getRama(), g.getEstado(), anioAcademicoId, null, null, 0, 0));
				}
			}
		}
		return metricas;
	}

	/**
	 * Abandonos registrados en el año Z / matriculados del grado en Z-2. Abandona quien:
	 * - tiene matrícula en el grado en Z-2 y no la tiene ni en Z-1 ni en Z (una cancelada no cuenta)
	 * - no ha superado todos los créditos del grado (la suma de todas sus asignaturas)
	 * - no tiene matrícula en Z-1 ni en Z en otro grado que comparta alguna asignatura con este
	 * Es null si no existe Z-2, si no ha terminado el periodo de matrícula de Z o si no hubo matriculados en Z-2.
	 *
	 * @param anios Años académicos ordenados (anioAcademicoId, finMatriculacion)
	 */
	private Double tasaAbandono(String gradoId, String anioId, List<Object[]> anios, String fechaHoy) {
		int z = 0;
		while (!anios.get(z)[0].equals(anioId)) {
			z++;
		}
		Object finMatriculacion = anios.get(z)[1];
		if (z < 2 || finMatriculacion == null || finMatriculacion.toString().compareTo(fechaHoy) >= 0) {
			return null;
		}
		String anioMenos2 = (String) anios.get(z - 2)[0];
		String anioMenos1 = (String) anios.get(z - 1)[0];

		Number matriculados = (Number) db.executeQueryArray(
				"SELECT COUNT(DISTINCT alumnoId) FROM Matricula "
						+ "WHERE gradoId = ? AND anioAcademicoId = ? AND estado IN " + MATRICULA_VALIDA,
				gradoId, anioMenos2).get(0)[0];
		Number abandonos = (Number) db.executeQueryArray(
				"SELECT COUNT(DISTINCT m.alumnoId) FROM Matricula m "
						+ "WHERE m.gradoId = ? AND m.anioAcademicoId = ? AND m.estado IN " + MATRICULA_VALIDA + " "
						+ "AND NOT EXISTS (SELECT 1 FROM Matricula n "
						+ "     WHERE n.alumnoId = m.alumnoId AND n.anioAcademicoId IN (?, ?) AND n.estado IN " + MATRICULA_VALIDA + " "
						+ "       AND (n.gradoId = m.gradoId "
						+ "            OR EXISTS (SELECT 1 FROM GradoCurso gm JOIN GradoCurso gn ON gn.cursoId = gm.cursoId "
						+ "                       WHERE gm.gradoId = m.gradoId AND gn.gradoId = n.gradoId))) "
						+ "AND (SELECT COALESCE(SUM(c.creditos), 0) FROM Curso c WHERE c.cursoId IN "
						+ "       (SELECT mc.cursoId FROM MatriculaCurso mc "
						+ "        JOIN Matricula x ON x.matriculaId = mc.matriculaId "
						+ "        JOIN Calificacion ca ON ca.matriculaId = mc.matriculaId AND ca.cursoId = mc.cursoId "
						+ "             AND ca.anioAcademicoId = x.anioAcademicoId "
						+ "        WHERE x.alumnoId = m.alumnoId AND x.gradoId = m.gradoId AND x.estado IN " + MATRICULA_VALIDA + " "
						+ "          AND x.anioAcademicoId <= ? AND mc.estado <> 'CANCELADA' "
						+ "          AND ca.nota >= " + MatriculacionModel.NOTA_APROBADO + ")) "
						+ "  < (SELECT COALESCE(SUM(c.creditos), 0) FROM GradoCurso gc JOIN Curso c ON c.cursoId = gc.cursoId "
						+ "     WHERE gc.gradoId = m.gradoId)",
				gradoId, anioMenos2, anioMenos1, anioId, anioId).get(0)[0];
		return porcentaje(abandonos.doubleValue(), matriculados.doubleValue());
	}

	/**
	 * parte / total * 100, redondeado a 1 decimal (HALF_UP); null si el total es 0.
	 */
	private Double porcentaje(double parte, double total) {
		if (total == 0) {
			return null;
		}
		return BigDecimal.valueOf(parte).multiply(BigDecimal.valueOf(100))
				.divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP).doubleValue();
	}

	/**
	 * Año académico por defecto de la vista: el actual (el de mayor fecha de inicio), aunque todavía no tenga datos.
	 *
	 * @return Identificador del año (p. ej. "2026-2027"), o null si no hay ningún año académico
	 */
	public String getAnioAcademicoPorDefecto() {
		List<Object[]> rows = db.executeQueryArray(
				"SELECT anioAcademicoId FROM AnioAcademico ORDER BY fechaInicio DESC LIMIT 1");
		return rows.isEmpty() ? null : (String) rows.get(0)[0];
	}
}
