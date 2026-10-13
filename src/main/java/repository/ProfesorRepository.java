package repository;

import java.util.List;

import model.asignaciondocente.AsignacionDocenteDisplayDTO;
import util.Database;

/**
 * Acceso a la base de datos para la parte del profesor.
 */
public class ProfesorRepository {
    private Database db = new Database();

    /** Lista de profesores para el combo, con formato "P001 - Nombre Apellidos" */
    public List<Object[]> getListaProfesores() {
        String sql = "SELECT profesorId || ' - ' || nombre || ' ' || apellidos FROM Profesor ORDER BY profesorId";
        return db.executeQueryArray(sql);
    }

    /**
     * Grupos que imparte el profesor. El rol no se guarda en la BD:
     * es COORDINADOR si es el coordinador de la asignatura y DOCENTE en otro caso
     */
    public List<AsignacionDocenteDisplayDTO> getAsignacionesDocentes(String profesorId) {
        String sql = "SELECT i.cursoId, c.nombre AS curso, i.grupoCodigo, g.tipo,"
                + " CASE WHEN c.coordinadorId = i.profesorId THEN 'COORDINADOR' ELSE 'DOCENTE' END AS rol"
                + " FROM Imparte i"
                + " JOIN Curso c ON c.cursoId = i.cursoId"
                + " JOIN Grupo g ON g.cursoId = i.cursoId AND g.grupoCodigo = i.grupoCodigo"
                + " WHERE i.profesorId = ?"
                + " ORDER BY i.cursoId, g.tipo, i.grupoCodigo";
        return db.executeQueryPojo(AsignacionDocenteDisplayDTO.class, sql, profesorId);
    }
}
