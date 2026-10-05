package model.curso;

import lombok.*;

/**
 * Entidad que representa un curso / asignatura.
 * Diseñada para soportar cambios y adiciones de atributos en el futuro sin romper compatibilidad.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Curso {
	@EqualsAndHashCode.Include
	private Long id;
	private String nombre;
	private Long idGrado;
	private Long idCoordinador;
	
	@Builder.Default
	private Integer creditos = 6;
	
	@Builder.Default
	private boolean activo = true;
}

