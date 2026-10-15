package model.asignaciondocente;

import lombok.*;

/**
 * Un grupo que imparte un profesor, con su rol en la asignatura (HU 35239).
 * Los nombres de los atributos coinciden con los alias de la query para que DbUtils los mapee.
 */
@Getter @Setter
public class AsignacionDocenteDisplayDTO {
    private String cursoId;
    private String curso;
    private String grupoCodigo;
    private String tipo;
    private String rol;

    public AsignacionDocenteDisplayDTO() {
        super();
    }

    public AsignacionDocenteDisplayDTO(String cursoId, String curso, String grupoCodigo, String tipo, String rol) {
        super();
        this.cursoId = cursoId;
        this.curso = curso;
        this.grupoCodigo = grupoCodigo;
        this.tipo = tipo;
        this.rol = rol;
    }
}
