package model.acta;

import lombok.*;

/**
 * La nota de un alumno en un acta, tal como la ve y edita el profesor (HU 35239).
 * nota es texto para la tabla: vacia si todavia no esta calificado y NP si es no presentado.
 * Los nombres de los atributos coinciden con los alias de la query para que DbUtils los mapee.
 */
@Getter @Setter
public class CalificacionActaDisplayDTO {
    private String matriculaId;
    private String alumnoId;
    private String alumno;
    private String dni;
    private String nota;

    public CalificacionActaDisplayDTO() {
        super();
    }

    public CalificacionActaDisplayDTO(String matriculaId, String alumnoId, String alumno, String dni, String nota) {
        super();
        this.matriculaId = matriculaId;
        this.alumnoId = alumnoId;
        this.alumno = alumno;
        this.dni = dni;
        this.nota = nota;
    }
}
