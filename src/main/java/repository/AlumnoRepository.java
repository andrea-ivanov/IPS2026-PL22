package repository;

import java.util.List;
import util.Database;

/**
 * Acceso a la base de datos para la entidad Alumno.
 */
public class AlumnoRepository {
    private Database db = new Database();

    /**
     * Lista de alumnos para el combo del login con formato "AL001 - Nombre Apellidos"
     */
    public List<Object[]> getListaAlumnos() {
        String sql = "SELECT alumnoId || ' - ' || nombre || ' ' || apellidos FROM Alumno ORDER BY alumnoId";
        return db.executeQueryArray(sql);
    }
}
