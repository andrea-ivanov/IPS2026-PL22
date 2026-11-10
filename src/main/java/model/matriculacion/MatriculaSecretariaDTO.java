package model.matriculacion;

/**
 * DTO para la vista de Secretaría (HU 35226 consulta y HU 35229 ciclo de vida).
 * Cada fila representa un curso matriculado (MatriculaCurso) con los datos
 * básicos del estudiante, del grado, de los grupos asignados y del pago/cancelación.
 *
 * Sin Lombok a propósito: el compilador de VS Code/Eclipse generaba la clase corrupta
 * (ClassFormatError) y Apache Commons DbUtils necesita getters/setters reales.
 */
public class MatriculaSecretariaDTO {
	private String matriculaId;
	private String cursoId;
	private String anioAcademicoId;
	private String alumnoId;
	private String dni;
	private String estudiante;
	private String email;
	private String gradoId;
	private String grado;
	private String curso;
	private String grupo; // Grupos asignados concatenados (p. ej. "TE1, PL2") o "Sin asignar"
	private String estado; // Estado del curso matriculado (MatriculaCurso)
	private String estadoMatricula; // Estado de la matrícula completa (Matricula)
	private Double importe;
	private String metodoPago;
	private String fechaPago;
	private int pagoValidado;
	private String motivoCancelacion;
	private String fechaCancelacion;
	private String fechaInicioAnio; // Inicio del año académico de la matrícula
	private int gruposPendientes; // Tipos de grupo del curso que aún no tiene asignados

	public MatriculaSecretariaDTO() {
		super();
	}

	// =========================================================================
	// Situaciones que revisa el secretario (consultas de docs/DATABASE.md, HU 35229)
	// =========================================================================

	/** Pagada por transferencia o carta de pago y pendiente de que el secretario la valide. */
	public boolean isPagoPendienteValidar() {
		return "SOLICITADA".equals(estado) && metodoPago != null && pagoValidado == 0;
	}

	/** Solicitada sin ningún pago cuando su año académico ya ha empezado. */
	public boolean isSinPagarAnioIniciado(String fechaHoy) {
		return "SOLICITADA".equals(estado) && metodoPago == null
				&& fechaInicioAnio != null && fechaInicioAnio.compareTo(fechaHoy) <= 0;
	}

	/** Pagada o activa a la que le falta algún grupo por asignar. */
	public boolean isFaltanGrupos() {
		return ("PAGADA".equals(estado) || "ACTIVA".equals(estado)) && gruposPendientes > 0;
	}

	public String getMatriculaId() { return matriculaId; }
	public void setMatriculaId(String matriculaId) { this.matriculaId = matriculaId; }

	public String getCursoId() { return cursoId; }
	public void setCursoId(String cursoId) { this.cursoId = cursoId; }

	public String getAnioAcademicoId() { return anioAcademicoId; }
	public void setAnioAcademicoId(String anioAcademicoId) { this.anioAcademicoId = anioAcademicoId; }

	public String getAlumnoId() { return alumnoId; }
	public void setAlumnoId(String alumnoId) { this.alumnoId = alumnoId; }

	public String getDni() { return dni; }
	public void setDni(String dni) { this.dni = dni; }

	public String getEstudiante() { return estudiante; }
	public void setEstudiante(String estudiante) { this.estudiante = estudiante; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getGradoId() { return gradoId; }
	public void setGradoId(String gradoId) { this.gradoId = gradoId; }

	public String getGrado() { return grado; }
	public void setGrado(String grado) { this.grado = grado; }

	public String getCurso() { return curso; }
	public void setCurso(String curso) { this.curso = curso; }

	public String getGrupo() { return grupo; }
	public void setGrupo(String grupo) { this.grupo = grupo; }

	public String getEstado() { return estado; }
	public void setEstado(String estado) { this.estado = estado; }

	public String getEstadoMatricula() { return estadoMatricula; }
	public void setEstadoMatricula(String estadoMatricula) { this.estadoMatricula = estadoMatricula; }

	public Double getImporte() { return importe; }
	public void setImporte(Double importe) { this.importe = importe; }

	public String getMetodoPago() { return metodoPago; }
	public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

	public String getFechaPago() { return fechaPago; }
	public void setFechaPago(String fechaPago) { this.fechaPago = fechaPago; }

	public int getPagoValidado() { return pagoValidado; }
	public void setPagoValidado(int pagoValidado) { this.pagoValidado = pagoValidado; }

	public String getMotivoCancelacion() { return motivoCancelacion; }
	public void setMotivoCancelacion(String motivoCancelacion) { this.motivoCancelacion = motivoCancelacion; }

	public String getFechaCancelacion() { return fechaCancelacion; }
	public void setFechaCancelacion(String fechaCancelacion) { this.fechaCancelacion = fechaCancelacion; }

	public String getFechaInicioAnio() { return fechaInicioAnio; }
	public void setFechaInicioAnio(String fechaInicioAnio) { this.fechaInicioAnio = fechaInicioAnio; }

	public int getGruposPendientes() { return gruposPendientes; }
	public void setGruposPendientes(int gruposPendientes) { this.gruposPendientes = gruposPendientes; }

	@Override
	public String toString() {
		return "MatriculaSecretariaDTO [matriculaId=" + matriculaId + ", cursoId=" + cursoId + ", estudiante="
				+ estudiante + ", estado=" + estado + "]";
	}
}
