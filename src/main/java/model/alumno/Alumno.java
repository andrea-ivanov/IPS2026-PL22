package model.alumno;

import lombok.*;

/**
 * Entidad que representa a un alumno en el sistema.
 * Preparada para extensiones futuras mediante Lombok (constructores, builder y getters/setters).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Alumno {
	@EqualsAndHashCode.Include
	private Long id;
	private String nombre;
	private String email;
}

