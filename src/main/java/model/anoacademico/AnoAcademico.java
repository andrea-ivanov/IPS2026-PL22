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
 * Fiel a la tabla AnioAcademico de la base de datos (anioAcademicoId, fechaInicio, fechaFin, inicioMatriculacion, finMatriculacion).
 * Mantiene compatibilidad con identificadores numéricos y alias para la vista y las pruebas.
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

	private String nombre; // Ej. "2026-2027" o "2026/2027"
	private String anioAcademicoId; // PK en base de datos: "2026-2027"

	private Date inicioAnoAcademico;
	private Date finAnoAcademico;

	private Date inicioPeriodoMatriculacion;
	private Date finPeriodoMatriculacion;

	@Builder.Default
	private boolean actual = false;

	public String getAnioAcademicoId() {
		if (anioAcademicoId != null && !anioAcademicoId.isEmpty()) {
			return anioAcademicoId;
		}
		if (nombre != null && !nombre.isEmpty()) {
			return nombre;
		}
		return id != null ? String.valueOf(id) : "2026-2027";
	}

	public void setAnioAcademicoId(String anioAcademicoId) {
		this.anioAcademicoId = anioAcademicoId;
		if (this.nombre == null) {
			this.nombre = anioAcademicoId;
		}
	}

	public String getNombre() {
		if (nombre != null && !nombre.isEmpty()) {
			return nombre;
		}
		if (anioAcademicoId != null && !anioAcademicoId.isEmpty()) {
			return anioAcademicoId;
		}
		return id != null ? String.valueOf(id) : "2026-2027";
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
		if (this.anioAcademicoId == null) {
			this.anioAcademicoId = nombre;
		}
	}

	// Alias para mapeo directo de la tabla AnioAcademico
	public Date getFechaInicio() {
		return inicioAnoAcademico;
	}

	public void setFechaInicio(Date fechaInicio) {
		this.inicioAnoAcademico = fechaInicio;
	}

	public Date getFechaFin() {
		return finAnoAcademico;
	}

	public void setFechaFin(Date fechaFin) {
		this.finAnoAcademico = fechaFin;
	}

	public Date getInicioMatriculacion() {
		return inicioPeriodoMatriculacion;
	}

	public void setInicioMatriculacion(Date inicioMatriculacion) {
		this.inicioPeriodoMatriculacion = inicioMatriculacion;
	}

	public Date getFinMatriculacion() {
		return finPeriodoMatriculacion;
	}

	public void setFinMatriculacion(Date finMatriculacion) {
		this.finPeriodoMatriculacion = finMatriculacion;
	}

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
