package model.grado;

/**
 * Representa el estado administrativo o académico de un grado.
 * EN_EXTINCION es el paso previo a EXTINTO: el grado tiene un dictamen desfavorable y se extingue al empezar el año.
 */
public enum EstadoGrado {
	VIGENTE,
	PENDIENTE_RENOVACION,
	EN_EXTINCION,
	EXTINTO;

	/**
	 * Mapea una cadena de texto al enum correspondiente de forma segura.
	 */
	public static EstadoGrado fromString(String value) {
		if (value == null || value.trim().isEmpty()) {
			return VIGENTE;
		}
		for (EstadoGrado estado : values()) {
			if (estado.name().equalsIgnoreCase(value.trim())) {
				return estado;
			}
		}
		return VIGENTE;
	}
}

