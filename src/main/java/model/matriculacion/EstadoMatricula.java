package model.matriculacion;

/**
 * Representa los estados del ciclo de vida de una matrícula.
 */
public enum EstadoMatricula {
	ACTIVA,
	PENDIENTE,
	CANCELADA;

	/**
	 * Mapea una cadena de texto proveniente de la base de datos o entrada al enum correspondiente.
	 * Tolera mayúsculas/minúsculas y espacios.
	 */
	public static EstadoMatricula fromString(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		for (EstadoMatricula estado : values()) {
			if (estado.name().equalsIgnoreCase(value.trim())) {
				return estado;
			}
		}
		throw new IllegalArgumentException("Estado de matrícula desconocido: " + value);
	}
}

