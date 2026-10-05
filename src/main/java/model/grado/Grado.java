package model.grado;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.*;
import model.curso.Curso;

/**
 * Entidad que representa un Grado académico.
 * Contiene un id, un nombre y una lista de cursos asociados.
 * Preparada para soportar atributos adicionales en el futuro mediante el patrón Builder y Lombok.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Grado {
	@EqualsAndHashCode.Include
	private Long id;
	private String nombre;
	
	@Builder.Default
	private List<Curso> cursos = new ArrayList<>();
	
	@Builder.Default
	private EstadoGrado estado = EstadoGrado.ACTIVO;
	
	private Date fechaCreacion;
	
	@Builder.Default
	private boolean activo = true;

	public void addCurso(Curso curso) {
		if (this.cursos == null) {
			this.cursos = new ArrayList<>();
		}
		this.cursos.add(curso);
	}
}

