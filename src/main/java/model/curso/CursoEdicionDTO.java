package model.curso;

import lombok.*;

/**
 * DTO con los datos de un curso dentro de un grado, sin transformar, para precargar el formulario de modificación.
 */
@Getter
@Setter
@ToString
public class CursoEdicionDTO {
	private String anio;          // Año dentro del grado (1-6)
	private String itinerario;    // Itinerario dentro del grado
	private String creditos;      // Créditos de la asignatura
	private String nombre;        // Nombre (no editable)
	private String coordinadorId; // Coordinador (no editable)
}
