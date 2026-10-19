package model.asignaciondocente;

/**
 * Rol de un profesor en una asignatura. No se guarda en la BD: es COORDINADOR si es el coordinador
 * de la asignatura (Curso.coordinadorId) y DOCENTE si solo imparte alguno de sus grupos.
 */
public enum RolDocente {
	COORDINADOR,
	DOCENTE;

	public static RolDocente fromString(String value) {
		for (RolDocente rol : values()) {
			if (rol.name().equalsIgnoreCase(value == null ? "" : value.trim())) {
				return rol;
			}
		}
		throw new IllegalArgumentException("Rol docente desconocido: " + value);
	}
}
