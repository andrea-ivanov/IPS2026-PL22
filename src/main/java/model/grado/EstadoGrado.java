package model.grado;

/**
 * Representa el estado administrativo o académico de un grado.
 */
public enum EstadoGrado {
	ACTIVO,
	VIGENTE,
	EN_EXTINCION;

	/**
	 * Mapea una cadena de texto al enum correspondiente de forma segura.
	 */
	public static EstadoGrado fromString(String value) {
		if (value == null || value.trim().isEmpty()) {
			return ACTIVO;
		}
		for (EstadoGrado estado : values()) {
			if (estado.name().equalsIgnoreCase(value.trim())) {
				return estado;
			}
		}
		return ACTIVO;
	}
}

