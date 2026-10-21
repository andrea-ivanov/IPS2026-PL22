package model.curso;

import lombok.*;

/**
 * DTO para la visualización de Cursos en la tabla de la vista de administración.
 * Se adapta al formato de columnas de AdminOfertaView:
 * {"Código", "Nombre", "Año", "Itinerario", "Profesor", "Estado", "Acciones"}
 */
@Getter
@Setter
@Builder
@ToString
public class CursoDisplayDTO {
	private String id;         // Código
	private String nombre;     // Nombre del curso
	private String ano;        // Año académico
	private String itinerario; // Itinerario formativo
	private String profesor;   // Profesor coordinador / responsable
	private String estado;     // Estado (ACTIVO, INACTIVO)
	private String idGrado;    // Referencia al grado contenedor

	public CursoDisplayDTO() {
		super();
	}

	public CursoDisplayDTO(String id, String nombre, String ano, String itinerario, String profesor, String estado, String idGrado) {
		super();
		this.id = id;
		this.nombre = nombre;
		this.ano = ano;
		this.itinerario = itinerario;
		this.profesor = profesor;
		this.estado = estado;
		this.idGrado = idGrado;
	}
}

