package model.matriculacion;

import lombok.*;

/**
 * Una fila de matrícula para las vistas de administración (alumnos de una
 * asignatura) y de estudiante (Mis Matrículas).
 * Los nombres de los atributos coinciden con los alias de las queries del
 * repositorio para que DbUtils los mapee.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatriculacionDisplayDTO {
	private String id; // Código de matrícula o del alumno
	private String nombreAlumno; // Nombre completo del alumno
	private String idCurso; // Id de la asignatura
	private String email; // Email de contacto
	private String estado; // Estado de la matrícula
	private String grado; // Nombre del grado
	private String curso; // Nombre de la asignatura
	private String fecha; // Fecha de la solicitud
	private String anioAcademicoId; // Código del año académico (p. ej. "2026-2027")
	private int anio; // Año del grado dentro del plan de estudios (1º, 2º, 3º, 4º)
	private int numMatricula; // Número de matrícula (1ª, 2ª, etc.)
}
