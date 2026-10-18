package model.curso;

/**
 * Representa el estado de un curso / asignatura dentro de un grado (GradoCurso.estado).
 */
public enum EstadoCurso {
	ACTIVO,
	INACTIVO;

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
		return ACTIVO;
	}
}