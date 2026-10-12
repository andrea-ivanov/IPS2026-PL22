package model.curso;

import lombok.*;

/**
 * Entidad de dominio que representa a un Curso o Asignatura.
 * Diseñada para soportar cambios y adiciones de atributos en el futuro sin romper compatibilidad existente.
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
	private String ano = "2026";

	@Builder.Default
	private String itinerario = "General";

	@Builder.Default
	private String profesor = "Dr. López";

	@Builder.Default
	private Integer creditos = 6;

	@Builder.Default
	private EstadoCurso estado = EstadoCurso.ACTIVO;

	@Builder.Default
	private boolean activo = true;

	public CursoDisplayDTO toDisplayDTO() {
		return CursoDisplayDTO.builder()
				.id(id != null ? id.toString() : "")
				.nombre(nombre != null ? nombre : "")
				.ano(ano != null ? ano : "2026")
				.itinerario(itinerario != null ? itinerario : "General")
				.profesor(profesor != null ? profesor : "Profesor Coordinador")
				.estado(estado != null ? estado.name() : "ABIERTO")
				.idGrado(idGrado != null ? idGrado.toString() : "")
				.build();
	}
}
