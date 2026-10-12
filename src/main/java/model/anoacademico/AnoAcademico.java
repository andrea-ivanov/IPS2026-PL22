package model.anoacademico;

import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entidad de dominio que representa un Año Académico.
 * Contiene el periodo de inicio y fin del año académico, el periodo de inicio y fin
 * de matriculación, y el indicador de si es el año académico actual.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AnoAcademico {
	@EqualsAndHashCode.Include
	private Long id;

	private String nombre; // Ej. "2026/2027"

	private Date inicioAnoAcademico;
	private Date finAnoAcademico;

	private Date inicioPeriodoMatriculacion;
	private Date finPeriodoMatriculacion;

	@Builder.Default
	private boolean actual = false;

	/**
	 * Formatea un rango de fechas al formato dd/MM/yy - dd/MM/yy para la vista.
	 */
	public static String formatearRangoFechas(Date desde, Date hasta) {
		if (desde == null && hasta == null) {
			return "--/--/-- - --/--/--";
		}
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
		String d = desde != null ? sdf.format(desde) : "--/--/--";
		String h = hasta != null ? sdf.format(hasta) : "--/--/--";
		return d + " - " + h;
	}

	public String getAnoAcademicoDisplay() {
		return formatearRangoFechas(inicioAnoAcademico, finAnoAcademico);
	}

	public String getPeriodoMatriculacionDisplay() {
		return formatearRangoFechas(inicioPeriodoMatriculacion, finPeriodoMatriculacion);
	}
}

