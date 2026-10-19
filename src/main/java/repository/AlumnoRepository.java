package repository;

import java.util.List;

import model.alumno.Alumno;
import model.alumno.AlumnoModel;
import util.Database;

/**
 * Acceso a la base de datos para la entidad Alumno.
 */
public class AlumnoRepository {
    private Database db = new Database();

	private final AlumnoModel alumnoModel;

	public AlumnoRepository() {
		this.alumnoModel = new AlumnoModel();
	}

	public AlumnoRepository(Database db) {
		this.alumnoModel = new AlumnoModel(db);
	}

	public Alumno findById(String id) {
		return alumnoModel.buscarPorId(id);
	}

	public Alumno findById(Long id) {
		return alumnoModel.buscarPorId(id);
	}

    /**
     * Lista de alumnos para el combo del login con formato "UOxxxxx - Nombre Apellidos"
     * (con los apellidos se distinguen los alumnos que se llaman igual)
     */
    public List<Object[]> getListaAlumnos() {
        try {
            String sql = "SELECT CASE "
                    + "  WHEN alumnoId LIKE 'AL%' THEN 'UO' || SUBSTR(alumnoId, 3) "
                    + "  WHEN alumnoId LIKE 'UO%' THEN alumnoId "
                    + "  ELSE 'UO' || alumnoId "
                    + "END || ' - ' || nombre || ' ' || apellidos FROM Alumno ORDER BY alumnoId";
            List<Object[]> rows = db.executeQueryArray(sql);
            if (rows != null && !rows.isEmpty()) {
                return rows;
            }
        } catch (Exception ignored) {
        }
        List<Object[]> mock = new java.util.ArrayList<>();
        mock.add(new Object[] { "UO001 - Lucia" });
        mock.add(new Object[] { "UO002 - Hugo" });
        mock.add(new Object[] { "UO003 - Sara" });
        return mock;
    }
}

