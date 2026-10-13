package model.curso;

import lombok.*;

/**
 * DTO para la visualización de Cursos en la tabla de la vista de administración.
 * Se adapta al formato de columnas de AdminOfertaView:
 * {"Código", "NombreCurso", "Año", "Itinerario", "Profesor", "Estado", "Acciones"}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CursoDisplayDTO {
	private String id;         // Código
	private String nombre;     // Nombre del curso
	private String ano;        // Año académico
	private String itinerario; // Itinerario formativo
	private String profesor;   // Profesor coordinador / responsable
	private String estado;     // Estado (ABIERTO, CERRADO, etc.)
	private String idGrado;    // Referencia al grado contenedor
}

