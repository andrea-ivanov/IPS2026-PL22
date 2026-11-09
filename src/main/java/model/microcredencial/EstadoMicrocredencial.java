package model.microcredencial;

/**
 * Estado de una microcredencial (Microcredencial.estado). El profesor solo crea PROPUESTAS (HU 35988);
 * el resto de transiciones son de otras historias (ver docs/DATABASE.md, 5.6).
 */
public enum EstadoMicrocredencial {
	PROPUESTA,
	ACEPTADA,
	RECHAZADA,
	CANCELADA_BAJA_DEMANDA,
	FINALIZADA;

	public static EstadoMicrocredencial fromString(String value) {
		for (EstadoMicrocredencial estado : values()) {
			if (estado.name().equalsIgnoreCase(value == null ? "" : value.trim())) {
				return estado;
			}
		}
		throw new IllegalArgumentException("Estado de microcredencial desconocido: " + value);
	}
}
