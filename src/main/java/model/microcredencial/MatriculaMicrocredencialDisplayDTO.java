package model.microcredencial;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO para mostrar las inscripciones en microcredenciales de un estudiante en "Mis Matrículas"
 * (HU 35232, HU 36410 y HU 36459).
 */
@Getter @Setter
public class MatriculaMicrocredencialDisplayDTO {
	private String microcredencialId;
	private String anioAcademicoId;
	private String nombre;
	private String ramaConocimiento;
	private Double creditos;
	private String inicioImparticion;
	private String finImparticion;
	private Double importe;
	private String estado;
	private String motivoCancelacion;
	private String fechaBaja;
	private String nota;

	public MatriculaMicrocredencialDisplayDTO() {
		super();
	}

	/**
	 * Texto de estado para la columna Estado en la vista del estudiante.
	 * Si fue cancelada por baja demanda, muestra "Cancelada por baja demanda" (HU 36410).
	 */
	public String getEstadoLegible() {
		if ("CANCELADA".equals(estado)) {
			if ("BAJA_DEMANDA".equals(motivoCancelacion)) {
				return "Cancelada por baja demanda";
			}
			if ("IMPAGO".equals(motivoCancelacion)) {
				return "Cancelada por impago";
			}
		}
		return estado != null ? estado : "-";
	}

	/**
	 * Detalle de baja o cancelación para mostrar al estudiante.
	 */
	public String getDetalleTexto() {
		if ("CANCELADA".equals(estado)) {
			if ("BAJA_DEMANDA".equals(motivoCancelacion)) {
				return "Microcredencial cancelada por no cubrir el 10% de plazas"
						+ (fechaBaja != null ? " (" + fechaBaja + ")" : "");
			}
			if ("IMPAGO".equals(motivoCancelacion)) {
				return "Inscripción cancelada por falta de pago"
						+ (fechaBaja != null ? " (" + fechaBaja + ")" : "");
			}
		}
		if ("ABANDONADA".equals(estado) && fechaBaja != null) {
			return "Abandonada el " + fechaBaja;
		}
		return "-";
	}

	/**
	 * Calificación formateada (solo visible cuando el acta está cerrada y el estado es FINALIZADA, HU 36459).
	 */
	public String getNotaTexto() {
		return (nota == null || nota.trim().isEmpty()) ? "Pendiente" : nota;
	}
}
