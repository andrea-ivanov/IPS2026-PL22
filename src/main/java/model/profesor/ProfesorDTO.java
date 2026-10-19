package model.profesor;

import lombok.*;

/**
 * Un profesor tal y como se muestra en los combos: el combo guarda el objeto y muestra su toString,
 * así el id se lee del objeto y no hay que sacarlo del texto.
 * Los nombres de los atributos coinciden con las columnas de la query para que DbUtils los mapee.
 */
@Getter @Setter
public class ProfesorDTO {
    private String profesorId;
    private String nombre;
    private String apellidos;

    public ProfesorDTO() {
        super();
    }

    public ProfesorDTO(String profesorId, String nombre, String apellidos) {
        super();
        this.profesorId = profesorId;
        this.nombre = nombre;
        this.apellidos = apellidos;
    }

    @Override
    public String toString() {
        return profesorId + " - " + nombre + " " + apellidos;
    }
}
