package model.matriculacion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.*;
import model.alumno.Alumno;
import model.curso.Curso;

/**
 * Entidad que representa una Matriculación.
 * Pertenece a un alumno, contiene una lista de cursos matriculados,
 * y almacena el estado, fecha y datos de pago.
 * Preparada para extensiones futuras sin romper contratos de compatibilidad.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Matriculacion {
	@EqualsAndHashCode.Include
	private Long id;
	private Alumno alumno;
	private Long idGrado;
	
	@Builder.Default
	private List<Curso> cursos = new ArrayList<>();
	
	private Date fecha;
	
	@Builder.Default
	private EstadoMatricula estado = EstadoMatricula.ACTIVA;
	
	@Builder.Default
	private boolean pagada = true;

	public void addCurso(Curso curso) {
		if (this.cursos == null) {
			this.cursos = new ArrayList<>();
		}
		this.cursos.add(curso);
	}
}

