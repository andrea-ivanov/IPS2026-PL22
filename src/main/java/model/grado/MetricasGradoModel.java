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
import repository.AnoAcademicoRepository;
import repository.MetricasGradoRepository;
import util.Database;

/**
 * Métricas de rendimiento académico de los grados para el revisor externo (HU 36411).
 * No se guardan: se calculan al consultar a partir de las matrículas y las calificaciones.
 * - Hay una fila por cada grado y año académico con alumnos matriculados (matrícula ACTIVA o FINALIZADA)
 * - Los grados EXTINTO no aparecen; los EN_EXTINCION sí
 * - Los PENDIENTE_RENOVACION y EN_EXTINCION aparecen siempre, aunque no tengan métricas (HU 36455)
 */
public class MetricasGradoModel {
	/** Solo cuentan las matrículas que han superado la solicitud y no se han cancelado */
	static final List<String> ESTADOS_MATRICULA_VALIDA = List.of("ACTIVA", "FINALIZADA");
	/** Una asignatura cancelada de la matrícula no cuenta ni como presentada ni como superada */
	private static final String ESTADO_CURSO_CANCELADO = "CANCELADA";
	/** Grados que requieren acción del revisor: aparecen siempre, aunque no tengan métricas (HU 36455) */
	private static final List<String> ESTADOS_REQUIEREN_ACCION = List.of(EstadoGrado.PENDIENTE_RENOVACION.name(),
			EstadoGrado.EN_EXTINCION.name());

	private final MetricasGradoRepository repository;
	private final AnoAcademicoRepository anoAcademicoRepository;

	public MetricasGradoModel(Database db) {
		this.repository = new MetricasGradoRepository(db);
		this.anoAcademicoRepository = new AnoAcademicoRepository(db);
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
		List<FilaMetricasGrado> filas = repository.findFilasMetricas(normalizarFiltro(rama),
				normalizarFiltro(nombreGrado), anioAcademicoId, ESTADOS_MATRICULA_VALIDA, EstadoGrado.EXTINTO.name(),
				ESTADO_CURSO_CANCELADO, MatriculacionModel.NOTA_APROBADO);
		List<String[]> anios = anoAcademicoRepository.findIdsYFinMatriculacion();
		List<GradoAnioRevisorDisplayDTO> metricas = new ArrayList<>();
		Set<String> gradosConFilas = new HashSet<>();
		for (FilaMetricasGrado f : filas) {
			gradosConFilas.add(f.gradoId());
			metricas.add(new GradoAnioRevisorDisplayDTO(f.gradoId(), f.nombre(), f.facultad(), f.ramaConocimiento(),
					f.estado(), f.anioAcademicoId(), porcentaje(f.creditosSuperados(), f.creditosPresentados()),
					tasaAbandono(f.gradoId(), f.anioAcademicoId(), anios, fechaHoy),
					f.aprobados(), f.matriculados()));
		}
		for (FilaMetricasGrado g : repository.findGradosEnEstados(normalizarFiltro(rama), normalizarFiltro(nombreGrado),
				ESTADOS_REQUIEREN_ACCION)) {
			if (!gradosConFilas.contains(g.gradoId())) {
				metricas.add(new GradoAnioRevisorDisplayDTO(g.gradoId(), g.nombre(), g.facultad(), g.ramaConocimiento(),
						g.estado(), anioAcademicoId, null, null, 0, 0));
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
	 * @param anios Años académicos ordenados del más antiguo al más reciente {anioAcademicoId, finMatriculacion}
	 */
	private Double tasaAbandono(String gradoId, String anioId, List<String[]> anios, String fechaHoy) {
		int z = 0;
		while (!anios.get(z)[0].equals(anioId)) {
			z++;
		}
		String finMatriculacion = anios.get(z)[1];
		if (z < 2 || finMatriculacion == null || finMatriculacion.compareTo(fechaHoy) >= 0) {
			return null;
		}
		String anioMenos2 = anios.get(z - 2)[0];
		String anioMenos1 = anios.get(z - 1)[0];

		int matriculados = repository.countMatriculados(gradoId, anioMenos2, ESTADOS_MATRICULA_VALIDA);
		int abandonos = repository.countAbandonos(gradoId, anioMenos2, anioMenos1, anioId, ESTADOS_MATRICULA_VALIDA,
				ESTADO_CURSO_CANCELADO, MatriculacionModel.NOTA_APROBADO);
		return porcentaje(abandonos, matriculados);
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

	/** Filtro de texto en minúsculas y sin espacios alrededor; null si no filtra */
	private static String normalizarFiltro(String filtro) {
		return filtro == null || filtro.trim().isEmpty() ? null : filtro.trim().toLowerCase();
	}

	/**
	 * Año académico por defecto de la vista: el actual (el de mayor fecha de inicio), aunque todavía no tenga datos.
	 *
	 * @return Identificador del año (p. ej. "2026-2027"), o null si no hay ningún año académico
	 */
	public String getAnioAcademicoPorDefecto() {
		return anoAcademicoRepository.findActualId();
	}

	/** Años académicos para el combo de la vista, del más reciente al más antiguo */
	public List<String> getAniosAcademicos() {
		return anoAcademicoRepository.findAllIds();
	}
}
