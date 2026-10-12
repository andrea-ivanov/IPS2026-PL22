package model;

import lombok.*;

/**
 * Un acta de una asignatura en una convocatoria, con la fecha de fin de la convocatoria (HU 35239).
 * Los nombres de los atributos coinciden con los alias de la query para que DbUtils los mapee.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ActaDisplayDTO {
    private String cursoId;
    private String anioAcademicoId;
    private String tipoConvocatoria;
    private String fechaFin;
    private String estado;
}
