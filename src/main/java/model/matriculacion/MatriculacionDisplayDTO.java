package model.matriculacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO para la visualización de Matrículas / Alumnos tanto en la vista de
 * administración,
 * secretaría y estudiante (Mis Matrículas).
 * 
 * Unifica los atributos de todas las vistas:
 * - AdminOfertaView: {"Código", "NombreAlumno"} (además de idCurso, email,
 * estado, idAnoAcademico)
 * - SecretarioView: {"Nº Ref.", "DNI", "Estudiante", "Grado", "Curso", "Grupo",
 * "Estado"}
 * - MisMatriculasView: {"ID Matrícula", "Grado", "Estado", "Fecha"}
 * 
 * Proporciona métodos getter/setter con alias y compatibilidad con Apache
 * Commons DbUtils (POJO):
 * - getNombreAlumno() / getEstudiante()
 * - getIdCurso() / getCurso()
 * - getId() / getIdMatricula()
 */
@Getter
@Setter
@Builder
@ToString
public class MatriculacionDisplayDTO {
	private String id; // Código o Nº de referencia (p. ej. "U00123", "M001")
	private String nombreAlumno; // Nombre completo del alumno / estudiante
	private String idCurso; // Id o denominación del curso
	private String email; // Email de contacto
	private String estado; // Estado de la matrícula (ACTIVA, PAGADA, SOLICITADA, CANCELADA, etc.)
	private String dni; // DNI / NIE del estudiante
	private String grado; // Grado universitario
	private String curso; // Asignatura (p. ej. "3º Curso", "Bases de Datos")
	private String grupo; // Grupo asignado (p. ej. "A", "B", "Sin asignar")
	private String fecha; // Fecha de la solicitud o matrícula
	private Long idAnoAcademico; // Id del año académico asociado (numérico)
	private String anioAcademicoId; // Código del año académico (p. ej. "2026-2027")
	private int anio; // Anio del grado dentro del plan de estudios (1º, 2º, 3º, 4º)
	private int numMatricula; // Número de matrícula (1ª, 2ª, etc.)

	/**
	 * Constructor por defecto sin argumentos para Apache Commons DbUtils.
	 */
	public MatriculacionDisplayDTO() {
		super();
	}

	/**
	 * Constructor con todos los campos para el patrón Builder y mapeos completos.
	 */
	public MatriculacionDisplayDTO(String id, String nombreAlumno, String idCurso, String email, String estado,
			String dni, String grado, String curso, String grupo, String fecha, Long idAnoAcademico,
			String anioAcademicoId, int anio, int numMatricula) {
		super();
		this.id = id;
		this.nombreAlumno = nombreAlumno;
		this.idCurso = idCurso;
		this.email = email;
		this.estado = estado;
		this.dni = dni;
		this.grado = grado;
		this.curso = curso;
		this.grupo = grupo;
		this.fecha = fecha;
		this.idAnoAcademico = idAnoAcademico;
		this.anioAcademicoId = anioAcademicoId;
		this.anio = anio;
		this.numMatricula = numMatricula;
	}

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
	 * Constructor de 7 parámetros utilizado en la vista de Secretaría
	 * (SecretarioView / MatriculacionRepository).
	 */
	public MatriculacionDisplayDTO(String id, String estudiante, String dni, String grado, String curso, String grupo,
			String estado) {
		this.id = id;
		this.nombreAlumno = estudiante;
		this.dni = dni;
		this.grado = grado;
		this.curso = curso;
		this.idCurso = curso;
		this.grupo = grupo;
		this.estado = estado;
	}

	/**
	 * Constructor de 8 parámetros utilizado para el cruce completo con email.
	 */
	public MatriculacionDisplayDTO(String id, String estudiante, String dni, String email, String grado, String curso,
			String grupo, String estado) {
		this.id = id;
		this.nombreAlumno = estudiante;
		this.dni = dni;
		this.email = email;
		this.grado = grado;
		this.curso = curso;
		this.idCurso = curso;
		this.grupo = grupo;
		this.estado = estado;
	}

	/**
	 * Constructor de 11 parámetros para compatibilidad con el modelo de
	 * matriculación.
	 */
	public MatriculacionDisplayDTO(String id, String nombreAlumno, String idCurso, String email, String estado,
			String dni, String grado, String curso, String grupo, String fecha, Long idAnoAcademico) {
		this.id = id;
		this.nombreAlumno = nombreAlumno;
		this.idCurso = idCurso;
		this.email = email;
		this.estado = estado;
		this.dni = dni;
		this.grado = grado;
		this.curso = curso;
		this.grupo = grupo;
		this.fecha = fecha;
		this.idAnoAcademico = idAnoAcademico;
	}

	// =========================================================================
	// Métodos de compatibilidad y alias (ID / ID Matrícula)
	// =========================================================================

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Object getIdMatricula() {
		return this.id;
	}

	public void setIdMatricula(Object idMatricula) {
		this.id = idMatricula != null ? idMatricula.toString() : null;
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

	// =========================================================================
	// Métodos de acceso explícito para DbUtils y reflection
	// =========================================================================

	public String getDni() {
		return this.dni;
	}

	public void setDni(String dni) {
		this.dni = dni;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getGrado() {
		return this.grado;
	}

	public void setGrado(String grado) {
		this.grado = grado;
	}

	public String getGrupo() {
		return this.grupo;
	}

	public void setGrupo(String grupo) {
		this.grupo = grupo;
	}

	public String getEstado() {
		return this.estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public String getFecha() {
		return this.fecha;
	}

	public void setFecha(String fecha) {
		this.fecha = fecha;
	}

	public Long getIdAnoAcademico() {
		return this.idAnoAcademico;
	}

	public void setIdAnoAcademico(Long idAnoAcademico) {
		this.idAnoAcademico = idAnoAcademico;
	}

	public String getAnioAcademicoId() {
		return this.anioAcademicoId;
	}

	public void setAnioAcademicoId(String anioAcademicoId) {
		this.anioAcademicoId = anioAcademicoId;
	}

	public int getAnio() {
		return this.anio;
	}

	public void setAnio(int anio) {
		this.anio = anio;
	}

	public int getNumMatricula() {
		return this.numMatricula;
	}

	public void setNumMatricula(int numMatricula) {
		this.numMatricula = numMatricula;
	}
}
