package model.matriculacion;

import lombok.*;

/**
 * DTO para la visualización de Matrículas / Alumnos en la tabla de la vista de administración.
 * Se adapta al formato de columnas de AdminOfertaView:
 * {"Código", "NombreAlumno"}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MatriculacionDisplayDTO {
	private String id;           // Código (p. ej. "U00123" o ID de alumno/matrícula)
	private String nombreAlumno; // Nombre completo del alumno
	private String idCurso;      // Id del curso asociado
	private String email;        // Email de contacto
	private String estado;       // Estado de la matrícula
}

