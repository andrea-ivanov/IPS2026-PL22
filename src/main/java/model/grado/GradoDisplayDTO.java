package model.grado;

import lombok.*;

/**
 * DTO para la visualización de Grados en la tabla de la vista de administración.
 * Se adapta al formato de columnas de AdminOfertaView:
 * {"Código", "Nombre", "Facultad", "Rama", "Estado", "Acciones"}
 */
@Getter
@Setter
@Builder
@ToString
public class GradoDisplayDTO {
	private String id;       // Código
	private String nombre;   // Nombre
	private String facultad; // Facultad
	private String rama;     // Rama de conocimiento
	private String estado;   // Estado (VIGENTE, EN_EXTINCION, etc.)

	public GradoDisplayDTO() {
		super();
	}

	public GradoDisplayDTO(String id, String nombre, String facultad, String rama, String estado) {
		super();
		this.id = id;
		this.nombre = nombre;
		this.facultad = facultad;
		this.rama = rama;
		this.estado = estado;
	}

	/**
	 * Constructor de 4 argumentos para compatibilidad hacia atrás.
	 */
	public GradoDisplayDTO(String id, String nombre, String facultad, String estado) {
		this(id, nombre, facultad, "Ingeniería y Arquitectura", estado);
	}
}