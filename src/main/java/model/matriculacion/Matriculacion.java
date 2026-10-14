package model.matriculacion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.*;
import model.alumno.Alumno;
import model.curso.Curso;

/**
 * Entidad de dominio que representa una Matriculación.
 * Pertenece a un Alumno, contiene una lista de cursos matriculados,
 * y almacena el estado, fecha y datos de pago.
 * Preparada para extensiones futuras (modalidad, descuentos aplicados, observaciones, etc.)
 * sin romper contratos de compatibilidad gracias al uso de Lombok y Builder.
 */
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Matriculacion {
	@EqualsAndHashCode.Include
	private Long id;
	private String matriculaId;
	private Alumno alumno;
	private Long idGrado;
	private Long idAnoAcademico;

	@Builder.Default
	private List<Curso> cursos = new ArrayList<>();

	private Date fecha;

	@Builder.Default
	private EstadoMatricula estado = EstadoMatricula.ACTIVA;

	@Builder.Default
	private boolean pagada = true;

	public Matriculacion() {
		super();
		this.cursos = new ArrayList<>();
		this.estado = EstadoMatricula.ACTIVA;
		this.pagada = true;
	}

	public Matriculacion(Long id, String matriculaId, Alumno alumno, Long idGrado, Long idAnoAcademico,
			List<Curso> cursos, Date fecha, EstadoMatricula estado, boolean pagada) {
		super();
		this.id = id;
		this.matriculaId = matriculaId;
		this.alumno = alumno;
		this.idGrado = idGrado;
		this.idAnoAcademico = idAnoAcademico;
		this.cursos = cursos != null ? cursos : new ArrayList<>();
		this.fecha = fecha;
		this.estado = estado != null ? estado : EstadoMatricula.ACTIVA;
		this.pagada = pagada;
	}

	public void addCurso(Curso curso) {
		if (this.cursos == null) {
			this.cursos = new ArrayList<>();
		}
		this.cursos.add(curso);
	}
}

