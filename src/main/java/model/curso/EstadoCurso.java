package model.curso;

/**
 * Representa el estado administrativo o académico de un curso / asignatura.
 * Admite tanto estados generales (ACTIVO/INACTIVO) como estados de oferta/matrícula (ABIERTO/CERRADO).
 */
public enum EstadoCurso {
	ACTIVO,
	INACTIVO,
	ABIERTO,
	CERRADO;

	/**
	 * Mapea una cadena de texto al enum correspondiente de forma segura.
	 */
	public static EstadoCurso fromString(String value) {
		if (value == null || value.trim().isEmpty()) {
			return ACTIVO;
		}
		for (EstadoCurso estado : values()) {
			if (estado.name().equalsIgnoreCase(value.trim())) {
				return estado;
			}
		}
		if ("ABIERTO".equalsIgnoreCase(value.trim())) {
			return ABIERTO;
		}
		if ("CERRADO".equalsIgnoreCase(value.trim())) {
			return CERRADO;
		}
		return ACTIVO;
	}
}