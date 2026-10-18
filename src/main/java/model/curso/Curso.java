package model.curso;

import lombok.*;

/**
 * Entidad de dominio que representa a un Curso o Asignatura.
 * Diseñada para soportar cambios y adiciones de atributos en el futuro sin romper compatibilidad existente.
 */
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Curso {
	@EqualsAndHashCode.Include
	private Long id;
	private String cursoId;
	private String nombre;
	private Long idGrado;
	private Long idCoordinador;

	@Builder.Default
	private String ano = "1";

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

	public Curso() {
		super();
		this.ano = "2026";
		this.itinerario = "General";
		this.profesor = "Dr. López";
		this.creditos = 6;
		this.estado = EstadoCurso.ACTIVO;
		this.activo = true;
	}

	public Curso(Long id, String cursoId, String nombre, Long idGrado, Long idCoordinador, String ano,
			String itinerario, String profesor, Integer creditos, EstadoCurso estado, boolean activo) {
		super();
		this.id = id;
		this.cursoId = cursoId;
		this.nombre = nombre;
		this.idGrado = idGrado;
		this.idCoordinador = idCoordinador;
		this.ano = ano != null ? ano : "2026";
		this.itinerario = itinerario != null ? itinerario : "General";
		this.profesor = profesor != null ? profesor : "Dr. López";
		this.creditos = creditos != null ? creditos : 6;
		this.estado = estado != null ? estado : EstadoCurso.ACTIVO;
		this.activo = activo;
	}

	public String getCursoId() {
		if (cursoId != null && !cursoId.isEmpty()) {
			return cursoId;
		}
		return id != null ? id.toString() : "";
	}

	public CursoDisplayDTO toDisplayDTO() {
		String displayId = (cursoId != null && !cursoId.isEmpty()) ? cursoId : (id != null ? id.toString() : "");
		return CursoDisplayDTO.builder()
				.id(displayId)
				.nombre(nombre != null ? nombre : "")
				.ano(ano != null ? ano : "2026")
				.itinerario(itinerario != null ? itinerario : "General")
				.profesor(profesor != null ? profesor : "Profesor Coordinador")
				.estado(estado != null ? estado.name() : "ACTIVO")
				.idGrado(idGrado != null ? idGrado.toString() : "")
				.build();
	}
}
