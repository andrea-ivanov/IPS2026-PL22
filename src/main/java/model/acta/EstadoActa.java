package model.acta;

/**
 * Estado de un acta (Acta.estado): en una ABIERTA el coordinador pone notas; una CERRADA es de solo lectura.
 */
public enum EstadoActa {
	ABIERTA,
	CERRADA;

	public static EstadoActa fromString(String value) {
		for (EstadoActa estado : values()) {
			if (estado.name().equalsIgnoreCase(value == null ? "" : value.trim())) {
				return estado;
			}
		}
		throw new IllegalArgumentException("Estado de acta desconocido: " + value);
	}
}
