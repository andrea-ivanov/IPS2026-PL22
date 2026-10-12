package model.matriculacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO para la visualización de Matrículas / Alumnos tanto en la vista de administración
 * como en la vista de secretaría.
 * 
 * Unifica los atributos de ambas vistas:
 * - AdminOfertaView: {"Código", "NombreAlumno"} (además de idCurso, email, estado)
 * - SecretarioView: {"Nº Ref.", "DNI", "Estudiante", "Grado", "Curso", "Grupo", "Estado"}
 * 
 * Proporciona métodos getter/setter con alias para mantener compatibilidad total:
 * - getNombreAlumno() / getEstudiante()
 * - getIdCurso() / getCurso()
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MatriculacionDisplayDTO {
	private String id;           // Código o Nº de referencia (p. ej. "U00123", "M001")
	private String nombreAlumno; // Nombre completo del alumno / estudiante
	private String idCurso;      // Id o denominación del curso
	private String email;        // Email de contacto
	private String estado;       // Estado de la matrícula (ACTIVA, PAGADA, SOLICITADA, CANCELADA, etc.)
	private String dni;          // DNI / NIE del estudiante
	private String grado;        // Grado universitario
	private String curso;        // Curso académico (p. ej. "3º Curso", "Bases de Datos")
	private String grupo;        // Grupo asignado (p. ej. "A", "B")
	private Long idAnoAcademico; // Id del año académico asociado

	/**
	 * Constructor de 5 parámetros utilizado en AdminOfertaView y TestDataFactory.
	 */
	public MatriculacionDisplayDTO(String id, String nombreAlumno, String idCurso, String email, String estado) {
		this.id = id;
		this.nombreAlumno = nombreAlumno;
		this.idCurso = idCurso;
		this.curso = idCurso;
		this.email = email;
		this.estado = estado;
	}

	/**
	 * Constructor de 7 parámetros utilizado en la vista de Secretaría (SecretarioView / MatriculaRepository).
	 */
	public MatriculacionDisplayDTO(String id, String estudiante, String dni, String grado, String curso, String grupo, String estado) {
		this.id = id;
		this.nombreAlumno = estudiante;
		this.dni = dni;
		this.grado = grado;
		this.curso = curso;
		this.idCurso = curso;
		this.grupo = grupo;
		this.estado = estado;
	}

	// =========================================================================
	// Métodos de compatibilidad y alias (Estudiante <-> NombreAlumno)
	// =========================================================================

	public String getEstudiante() {
		return this.nombreAlumno;
	}

	public void setEstudiante(String estudiante) {
		this.nombreAlumno = estudiante;
	}

	public String getNombreAlumno() {
		return this.nombreAlumno;
	}

	public void setNombreAlumno(String nombreAlumno) {
		this.nombreAlumno = nombreAlumno;
	}

	// =========================================================================
	// Métodos de compatibilidad y alias (Curso <-> IdCurso)
	// =========================================================================

	public String getCurso() {
		return this.curso != null ? this.curso : this.idCurso;
	}

	public void setCurso(String curso) {
		this.curso = curso;
		if (this.idCurso == null) {
			this.idCurso = curso;
		}
	}

	public String getIdCurso() {
		return this.idCurso != null ? this.idCurso : this.curso;
	}

	public void setIdCurso(String idCurso) {
		this.idCurso = idCurso;
		if (this.curso == null) {
			this.curso = idCurso;
		}
	}
}
