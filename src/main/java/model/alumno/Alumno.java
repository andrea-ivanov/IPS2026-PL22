package model.alumno;

import lombok.*;

/**
 * Entidad de dominio que representa a un Alumno.
 * Preparada para extensiones futuras (nuevos campos como DNI, teléfono, dirección, etc.)
 * utilizando Lombok con constructores flexibles y patrón Builder.
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

