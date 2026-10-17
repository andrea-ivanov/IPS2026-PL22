package model.grado;

/**
 * Representa el estado administrativo o académico de un grado.
 * El estado EXTINTO de la base de datos se trata como EN_EXTINCION.
 */
public enum EstadoGrado {
	VIGENTE,
	PENDIENTE_RENOVACION,
	EN_EXTINCION;

	/**
	 * Mapea una cadena de texto al enum correspondiente de forma segura.
	 */
	public static EstadoGrado fromString(String value) {
		if (value == null || value.trim().isEmpty()) {
			return VIGENTE;
		}
		if ("EXTINTO".equalsIgnoreCase(value.trim())) {
			return EN_EXTINCION;
		}
		for (EstadoGrado estado : values()) {
			if (estado.name().equalsIgnoreCase(value.trim())) {
				return estado;
			}
		}
		return VIGENTE;
	}
}

