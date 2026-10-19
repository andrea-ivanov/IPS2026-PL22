package model.grado;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.*;
import model.curso.Curso;

/**
 * Entidad de dominio que representa a un Grado académico.
 * Contiene id, nombre, facultad, rama y una lista de cursos asociados.
 * Preparada para soportar atributos adicionales en el futuro mediante el patrón Builder y Lombok.
 */
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Grado {
	@EqualsAndHashCode.Include
	private Long id;
	private String gradoId;
	private String nombre;

	@Builder.Default
	private String facultad = "Escuela de Ingeniería Informática";

	@Builder.Default
	private String rama = "Ingeniería y Arquitectura";

	@Builder.Default
	private List<Curso> cursos = new ArrayList<>();

	@Builder.Default
	private EstadoGrado estado = EstadoGrado.VIGENTE;

	private Date fechaCreacion;

	@Builder.Default
	private boolean activo = true;

	public Grado() {
		super();
		this.facultad = "Escuela de Ingeniería Informática";
		this.rama = "Ingeniería y Arquitectura";
		this.cursos = new ArrayList<>();
		this.estado = EstadoGrado.VIGENTE;
		this.activo = true;
	}

	public Grado(Long id, String gradoId, String nombre, String facultad, String rama, List<Curso> cursos,
			EstadoGrado estado, Date fechaCreacion, boolean activo) {
		super();
		this.id = id;
		this.gradoId = gradoId;
		this.nombre = nombre;
		this.facultad = facultad != null ? facultad : "Escuela de Ingeniería Informática";
		this.rama = rama != null ? rama : "Ingeniería y Arquitectura";
		this.cursos = cursos != null ? cursos : new ArrayList<>();
		this.estado = estado != null ? estado : EstadoGrado.VIGENTE;
		this.fechaCreacion = fechaCreacion;
		this.activo = activo;
	}

	public void addCurso(Curso curso) {
		if (this.cursos == null) {
			this.cursos = new ArrayList<>();
		}
		this.cursos.add(curso);
	}

	public String getGradoId() {
		if (gradoId != null && !gradoId.isEmpty()) {
			return gradoId;
		}
		return id != null ? id.toString() : "";
	}

	public GradoDisplayDTO toDisplayDTO() {
		String displayId = (gradoId != null && !gradoId.isEmpty()) ? gradoId : (id != null ? id.toString() : "");
		return GradoDisplayDTO.builder()
				.id(displayId)
				.nombre(nombre != null ? nombre : "")
				.facultad(facultad != null ? facultad : "Ciencias")
				.rama(rama != null ? rama : "Ingeniería y Arquitectura")
				.estado(estado != null ? estado.name() : "VIGENTE")
				.build();
	}
}
