package repository;

import java.util.List;

import model.asignaciondocente.AsignacionDocenteDisplayDTO;
import model.asignaciondocente.RolDocente;
import model.profesor.ProfesorDTO;
import util.Database;

/**
 * Acceso a la base de datos para la parte del profesor.
 */
public class ProfesorRepository {
    private Database db = new Database();

    /** Profesores para los combos, ordenados por id (el combo muestra "P001 - Nombre Apellidos") */
    public List<ProfesorDTO> getProfesores() {
        String sql = "SELECT profesorId, nombre, apellidos FROM Profesor ORDER BY profesorId";
        return db.executeQueryPojo(ProfesorDTO.class, sql);
    }

    /**
     * Grupos que imparte el profesor. El rol no se guarda en la BD:
     * es COORDINADOR si es el coordinador de la asignatura y DOCENTE en otro caso.
     * Una asignatura puede estar en varios grados: se muestran todos separados por comas
     */
    public List<AsignacionDocenteDisplayDTO> getAsignacionesDocentes(String profesorId) {
        String sql = "SELECT i.cursoId, c.nombre AS curso, GROUP_CONCAT(gr.nombre, ', ') AS grado, i.grupoCodigo, g.tipo,"
                + " CASE WHEN c.coordinadorId = i.profesorId THEN ? ELSE ? END AS rol"
                + " FROM Imparte i"
                + " JOIN Curso c ON c.cursoId = i.cursoId"
                + " JOIN Grupo g ON g.cursoId = i.cursoId AND g.grupoCodigo = i.grupoCodigo"
                + " LEFT JOIN GradoCurso gc ON gc.cursoId = i.cursoId"
                + " LEFT JOIN Grado gr ON gr.gradoId = gc.gradoId"
                + " WHERE i.profesorId = ?"
                + " GROUP BY i.cursoId, i.grupoCodigo"
                + " ORDER BY i.cursoId, g.tipo, i.grupoCodigo";
        return db.executeQueryPojo(AsignacionDocenteDisplayDTO.class, sql,
                RolDocente.COORDINADOR.name(), RolDocente.DOCENTE.name(), profesorId);
    }
}
