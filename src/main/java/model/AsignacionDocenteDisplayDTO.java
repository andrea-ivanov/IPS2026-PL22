package model;

import lombok.*;

/**
 * Un grupo que imparte un profesor, con su rol en la asignatura (HU 35239).
 * Los nombres de los atributos coinciden con los alias de la query para que DbUtils los mapee.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AsignacionDocenteDisplayDTO {
    private String cursoId;
    private String curso;
    private String grado;
    private String grupoCodigo;
    private String tipo;
    private String rol;
}
