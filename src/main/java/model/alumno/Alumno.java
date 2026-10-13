package model.alumno;

import lombok.*;
import model.matriculacion.MatriculacionDisplayDTO;

/**
 * Entidad de dominio que representa a un Alumno.
 * Preparada para extensiones futuras utilizando Lombok con constructores flexibles y patrón Builder.
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
	private String alumnoId;
	private String nombre;
	private String email;

	public String getCodigo() {
		if (alumnoId != null && !alumnoId.isEmpty()) {
			return alumnoId;
		}
		return id != null ? "UO" + id : "";
	}

	public MatriculacionDisplayDTO toDisplayDTO() {
		return MatriculacionDisplayDTO.builder()
				.id(getCodigo())
				.nombreAlumno(nombre != null ? nombre : "")
				.email(email != null ? email : "")
				.build();
	}
}
