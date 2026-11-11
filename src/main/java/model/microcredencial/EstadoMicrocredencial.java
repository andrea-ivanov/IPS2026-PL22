package model.microcredencial;

/**
 * Estado de una microcredencial (Microcredencial.estado). El profesor solo crea PROPUESTAS (HU 35988);
 * el resto de transiciones son de otras historias (ver docs/DATABASE.md, 5.6).
 */
public enum EstadoMicrocredencial {
	PROPUESTA("PROPUESTA"),
	ACEPTADA("ACEPTADA"),
	RECHAZADA("RECHAZADA"),
	CANCELADA_BAJA_DEMANDA("Cancelada por baja demanda"),
	FINALIZADA("FINALIZADA");

	private final String etiqueta;

	EstadoMicrocredencial(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	public String getEtiqueta() {
		return etiqueta;
	}

	public static String toEtiqueta(String estado) {
		if (estado == null) {
			return "";
		}
		try {
			return fromString(estado).getEtiqueta();
		} catch (IllegalArgumentException e) {
			return estado;
		}
	}

	public static EstadoMicrocredencial fromString(String value) {
		for (EstadoMicrocredencial estado : values()) {
			if (estado.name().equalsIgnoreCase(value == null ? "" : value.trim())) {
				return estado;
			}
		}
		throw new IllegalArgumentException("Estado de microcredencial desconocido: " + value);
	}
}
