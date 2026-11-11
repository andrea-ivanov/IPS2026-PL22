package model.microcredencial;

import lombok.*;

/**
 * Una microcredencial de un profesor, para el listado "Mis microcredenciales" (HU 35988).
 * Los nombres de los atributos coinciden con los alias de la query para que DbUtils los mapee.
 */
@Getter @Setter
public class MicrocredencialDisplayDTO {
	private String microcredencialId;
	private String anioAcademicoId;
	private String nombre;
	private String ramaConocimiento;
	private Double creditos;
	private Integer plazas;
	private String inicioMatriculacion;
	private String finMatriculacion;
	private String inicioImparticion;
	private String finImparticion;
	private String fechaPropuesta;
	private String fechaResolucion;
	private String motivoRechazo;
	private String fechaCancelacion;
	/** Fecha de cierre del acta (HU 36459); nula mientras está abierta */
	private String fechaCierreActa;
	/** 1 si cerró el acta el sistema, 0 si la cerró el profesor o sigue abierta */
	private Integer cierreAutomatico;
	private String estado;

	public MicrocredencialDisplayDTO() {
		super();
	}

	/**
	 * Texto de estado para mostrar en la vista del profesor (HU 36410: "Cancelada por baja demanda").
	 */
	public String getEstadoLegible() {
		return EstadoMicrocredencial.toEtiqueta(estado);
	}

	/**
	 * Detalle adicional del estado (fecha de cancelación por baja demanda, motivo de rechazo, etc.).
	 */
	public String getDetalleEstado() {
		if (EstadoMicrocredencial.CANCELADA_BAJA_DEMANDA.name().equals(estado)) {
			return "Baja demanda (< 10% plazas)"
					+ (fechaCancelacion != null ? " · " + fechaCancelacion : "");
		}
		if (EstadoMicrocredencial.FINALIZADA.name().equals(estado) && fechaCierreActa != null) {
			return (Integer.valueOf(1).equals(cierreAutomatico) ? "Acta cerrada por el sistema el " : "Acta cerrada el ")
					+ fechaCierreActa;
		}
		if (EstadoMicrocredencial.RECHAZADA.name().equals(estado) && motivoRechazo != null) {
			return motivoRechazo;
		}
		if (fechaResolucion != null) {
			return "Resuelta el " + fechaResolucion;
		}
		return "-";
	}
}
