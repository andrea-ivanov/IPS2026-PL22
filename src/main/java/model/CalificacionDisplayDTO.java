package model;

import lombok.*;

/**
 * La nota de un alumno en un acta (HU 35239).
 * nota es texto para la tabla: vacia si todavia no esta calificado y NP si es no presentado.
 * Los nombres de los atributos coinciden con los alias de la query para que DbUtils los mapee.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CalificacionDisplayDTO {
    private String matriculaId;
    private String alumnoId;
    private String alumno;
    private String dni;
    private String nota;
}
