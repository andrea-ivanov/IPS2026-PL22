package model.grado;

import lombok.*;

/**
 * DTO para la visualización de Grados en la tabla de la vista de administración.
 * Se adapta al formato de columnas de AdminOfertaView:
 * {"Código", "Nombre", "Facultad", "Rama", "Estado", "Acciones"}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class GradoDisplayDTO {
	private String id;       // Código
	private String nombre;   // Nombre
	private String facultad; // Facultad
	private String rama;     // Rama de conocimiento
	private String estado;   // Estado (VIGENTE, EN_EXTINCION, etc.)

	/**
	 * Constructor de 4 argumentos para compatibilidad hacia atrás.
	 */
	public GradoDisplayDTO(String id, String nombre, String facultad, String estado) {
		this(id, nombre, facultad, "Ingeniería y Arquitectura", estado);
	}
}