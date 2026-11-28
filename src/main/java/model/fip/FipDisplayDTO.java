package model.fip;

import lombok.Getter;
import lombok.Setter;

/**
 * Una FIP en el listado y en el detalle del administrador (HU 37165).
 * responsable: nombre y apellidos de quien la imparte (profesor o formador externo); email: su correo.
 * Los nombres de los atributos coinciden con las columnas de la query para que DbUtils los mapee.
 */
@Getter @Setter
public class FipDisplayDTO {
	private String fipId;
	private String nombre;
	private String responsable;
	private String email;
	private String fechaInicio;
	private String fechaFin;

	public FipDisplayDTO() {
		super();
	}
}
