package repository;

import java.util.List;

import util.Database;

/**
 * Acceso a la base de datos para la entidad Alumno.
 */
public class AlumnoRepository {
	private final Database db;

	public AlumnoRepository(Database db) {
		this.db = db;
	}

    /**
     * Lista de alumnos para el combo del login con formato "UOxxxxx - Nombre Apellidos"
     * (con los apellidos se distinguen los alumnos que se llaman igual)
     */
    public List<Object[]> getListaAlumnos() {
        String sql = "SELECT CASE "
                + "  WHEN alumnoId LIKE 'AL%' THEN 'UO' || SUBSTR(alumnoId, 3) "
                + "  WHEN alumnoId LIKE 'UO%' THEN alumnoId "
                + "  ELSE 'UO' || alumnoId "
                + "END || ' - ' || nombre || ' ' || apellidos FROM Alumno ORDER BY alumnoId";
        return db.executeQueryArray(sql);
    }
}
