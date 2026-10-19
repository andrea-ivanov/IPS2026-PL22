package model.acta;

import lombok.*;

/**
 * Un acta de una asignatura en una convocatoria, con la fecha de fin de la convocatoria (HU 35239).
 * Los nombres de los atributos coinciden con los alias de la query para que DbUtils los mapee.
 */
@Getter @Setter
public class ActaDisplayDTO {
    private String cursoId;
    private String anioAcademicoId;
    private String tipoConvocatoria;
    private String fechaFin;
    private String estado;

    public ActaDisplayDTO() {
        super();
    }

    public ActaDisplayDTO(String cursoId, String anioAcademicoId, String tipoConvocatoria, String fechaFin, String estado) {
        super();
        this.cursoId = cursoId;
        this.anioAcademicoId = anioAcademicoId;
        this.tipoConvocatoria = tipoConvocatoria;
        this.fechaFin = fechaFin;
        this.estado = estado;
    }

    public EstadoActa getEstadoActa() {
        return EstadoActa.fromString(estado);
    }
}
