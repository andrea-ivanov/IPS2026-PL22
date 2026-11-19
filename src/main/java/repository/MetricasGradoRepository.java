package repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.grado.FilaMetricasGrado;
import util.Database;

/**
 * Acceso a datos de las métricas de rendimiento de los grados para el revisor externo (HU 36411).
 * Solo SQL y mapeo de filas; las reglas (qué estados cuentan, tasas, redondeo) están en MetricasGradoModel.
 */
public class MetricasGradoRepository {
	private final Database db;

	public MetricasGradoRepository(Database db) {
		this.db = db;
	}

	/**
	 * Una fila por grado y año académico con matrículas en alguno de los estados válidos, ordenadas por nombre del
	 * grado, facultad y año de más reciente a más antiguo. Por cada asignatura no cancelada de la matrícula cuenta
	 * si se ha presentado (alguna nota ese año) y si la ha superado (alguna nota >= notaAprobado ese año).
	 *
	 * @param rama Parte de la rama en minúsculas (null: todas)
	 * @param nombreGrado Parte del nombre del grado en minúsculas (null: todos)
	 * @param anioAcademicoId Año exacto (null: todos)
	 */
	public List<FilaMetricasGrado> findFilasMetricas(String rama, String nombreGrado, String anioAcademicoId,
			List<String> estadosMatriculaValida, String estadoGradoExcluido, String estadoCursoCancelado,
			double notaAprobado) {
		List<Object> params = new ArrayList<>();
		params.add(notaAprobado);
		params.add(estadoCursoCancelado);
		params.addAll(estadosMatriculaValida);
		params.add(estadoGradoExcluido);
		String sql = "SELECT g.gradoId, g.nombre, g.facultad, g.ramaConocimiento, g.estado, m.anioAcademicoId, "
				+ "COUNT(DISTINCT m.alumnoId), COALESCE(SUM(a.superada), 0), "
				+ "COALESCE(SUM(a.presentada * a.creditos), 0), COALESCE(SUM(a.superada * a.creditos), 0) "
				+ "FROM Matricula m "
				+ "JOIN Grado g ON g.gradoId = m.gradoId "
				+ "LEFT JOIN (SELECT mc.matriculaId, c.creditos, "
				+ "                  MAX(CASE WHEN ca.nota IS NOT NULL THEN 1 ELSE 0 END) AS presentada, "
				+ "                  MAX(CASE WHEN ca.nota >= ? THEN 1 ELSE 0 END) AS superada "
				+ "           FROM MatriculaCurso mc "
				+ "           JOIN Matricula ma ON ma.matriculaId = mc.matriculaId "
				+ "           JOIN Curso c ON c.cursoId = mc.cursoId "
				+ "           LEFT JOIN Calificacion ca ON ca.matriculaId = mc.matriculaId AND ca.cursoId = mc.cursoId "
				+ "                AND ca.anioAcademicoId = ma.anioAcademicoId "
				+ "           WHERE mc.estado <> ? "
				+ "           GROUP BY mc.matriculaId, mc.cursoId) a ON a.matriculaId = m.matriculaId "
				+ "WHERE m.estado IN (" + marcas(estadosMatriculaValida) + ") AND g.estado <> ? ";
		if (rama != null) {
			sql += "AND LOWER(g.ramaConocimiento) LIKE ? ";
			params.add("%" + rama + "%");
		}
		if (nombreGrado != null) {
			sql += "AND LOWER(g.nombre) LIKE ? ";
			params.add("%" + nombreGrado + "%");
		}
		if (anioAcademicoId != null) {
			sql += "AND m.anioAcademicoId = ? ";
			params.add(anioAcademicoId);
		}
		sql += "GROUP BY g.gradoId, m.anioAcademicoId "
				+ "ORDER BY g.nombre, g.facultad, g.gradoId, m.anioAcademicoId DESC";

		List<FilaMetricasGrado> filas = new ArrayList<>();
		for (Object[] r : db.executeQueryArray(sql, params.toArray())) {
			filas.add(new FilaMetricasGrado((String) r[0], (String) r[1], (String) r[2], (String) r[3], (String) r[4],
					(String) r[5], ((Number) r[6]).intValue(), ((Number) r[7]).intValue(), ((Number) r[8]).doubleValue(),
					((Number) r[9]).doubleValue()));
		}
		return filas;
	}

	/**
	 * Grados en alguno de los estados indicados, con los mismos filtros de rama y nombre, ordenados por nombre,
	 * facultad e id. Se devuelven como filas sin métricas (sin año y con los contadores a 0)
	 *
	 * @param rama Parte de la rama en minúsculas (null: todas)
	 * @param nombreGrado Parte del nombre del grado en minúsculas (null: todos)
	 */
	public List<FilaMetricasGrado> findGradosEnEstados(String rama, String nombreGrado, List<String> estados) {
		List<Object> params = new ArrayList<>(estados);
		String sql = "SELECT gradoId, nombre, facultad, ramaConocimiento, estado FROM Grado "
				+ "WHERE estado IN (" + marcas(estados) + ") ";
		if (rama != null) {
			sql += "AND LOWER(ramaConocimiento) LIKE ? ";
			params.add("%" + rama + "%");
		}
		if (nombreGrado != null) {
			sql += "AND LOWER(nombre) LIKE ? ";
			params.add("%" + nombreGrado + "%");
		}
		sql += "ORDER BY nombre, facultad, gradoId";

		List<FilaMetricasGrado> grados = new ArrayList<>();
		for (Object[] r : db.executeQueryArray(sql, params.toArray())) {
			grados.add(new FilaMetricasGrado((String) r[0], (String) r[1], (String) r[2], (String) r[3], (String) r[4],
					null, 0, 0, 0, 0));
		}
		return grados;
	}

	/** Alumnos distintos con matrícula en alguno de los estados indicados en el grado y año */
	public int countMatriculados(String gradoId, String anioAcademicoId, List<String> estadosMatriculaValida) {
		List<Object> params = new ArrayList<>();
		params.add(gradoId);
		params.add(anioAcademicoId);
		params.addAll(estadosMatriculaValida);
		Number n = (Number) db.executeQueryArray("SELECT COUNT(DISTINCT alumnoId) FROM Matricula "
				+ "WHERE gradoId = ? AND anioAcademicoId = ? AND estado IN (" + marcas(estadosMatriculaValida) + ")",
				params.toArray()).get(0)[0];
		return n.intValue();
	}

	/**
	 * Alumnos con matrícula válida en el grado en anioMenos2 que:
	 * - no tienen matrícula válida ni en anioMenos1 ni en anio, en ese grado ni en otro que comparta alguna asignatura
	 * - no han superado todos los créditos del grado hasta anio (asignaturas no canceladas con nota >= notaAprobado)
	 */
	public int countAbandonos(String gradoId, String anioMenos2, String anioMenos1, String anio,
			List<String> estadosMatriculaValida, String estadoCursoCancelado, double notaAprobado) {
		String validas = marcas(estadosMatriculaValida);
		List<Object> params = new ArrayList<>();
		params.add(gradoId);
		params.add(anioMenos2);
		params.addAll(estadosMatriculaValida);
		params.add(anioMenos1);
		params.add(anio);
		params.addAll(estadosMatriculaValida);
		params.addAll(estadosMatriculaValida);
		params.add(anio);
		params.add(estadoCursoCancelado);
		params.add(notaAprobado);
		Number n = (Number) db.executeQueryArray(
				"SELECT COUNT(DISTINCT m.alumnoId) FROM Matricula m "
						+ "WHERE m.gradoId = ? AND m.anioAcademicoId = ? AND m.estado IN (" + validas + ") "
						+ "AND NOT EXISTS (SELECT 1 FROM Matricula n "
						+ "     WHERE n.alumnoId = m.alumnoId AND n.anioAcademicoId IN (?, ?) AND n.estado IN (" + validas + ") "
						+ "       AND (n.gradoId = m.gradoId "
						+ "            OR EXISTS (SELECT 1 FROM GradoCurso gm JOIN GradoCurso gn ON gn.cursoId = gm.cursoId "
						+ "                       WHERE gm.gradoId = m.gradoId AND gn.gradoId = n.gradoId))) "
						+ "AND (SELECT COALESCE(SUM(c.creditos), 0) FROM Curso c WHERE c.cursoId IN "
						+ "       (SELECT mc.cursoId FROM MatriculaCurso mc "
						+ "        JOIN Matricula x ON x.matriculaId = mc.matriculaId "
						+ "        JOIN Calificacion ca ON ca.matriculaId = mc.matriculaId AND ca.cursoId = mc.cursoId "
						+ "             AND ca.anioAcademicoId = x.anioAcademicoId "
						+ "        WHERE x.alumnoId = m.alumnoId AND x.gradoId = m.gradoId AND x.estado IN (" + validas + ") "
						+ "          AND x.anioAcademicoId <= ? AND mc.estado <> ? "
						+ "          AND ca.nota >= ?)) "
						+ "  < (SELECT COALESCE(SUM(c.creditos), 0) FROM GradoCurso gc JOIN Curso c ON c.cursoId = gc.cursoId "
						+ "     WHERE gc.gradoId = m.gradoId)",
				params.toArray()).get(0)[0];
		return n.intValue();
	}

	private static String marcas(List<String> valores) {
		return String.join(", ", Collections.nCopies(valores.size(), "?"));
	}
}
