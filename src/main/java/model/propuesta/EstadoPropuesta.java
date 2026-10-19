package model.propuesta;

/**
 * Estado de una propuesta de asignatura (PropuestaCurso.estado). De momento el profesor solo crea PENDIENTES;
 * aceptarlas o rechazarlas queda para un sprint futuro.
 */
public enum EstadoPropuesta {
	PENDIENTE,
	ACEPTADA,
	RECHAZADA;

	public static EstadoPropuesta fromString(String value) {
		for (EstadoPropuesta estado : values()) {
			if (estado.name().equalsIgnoreCase(value == null ? "" : value.trim())) {
				return estado;
			}
		}
		throw new IllegalArgumentException("Estado de propuesta desconocido: " + value);
	}
}
