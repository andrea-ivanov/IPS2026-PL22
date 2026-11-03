package repository;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
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

    /** Si existe un alumno con exactamente ese alumnoId */
    public boolean existeAlumnoId(String alumnoId) {
        List<Object[]> rows = db.executeQueryArray("SELECT alumnoId FROM Alumno WHERE alumnoId = ?", alumnoId);
        return rows != null && !rows.isEmpty() && rows.get(0)[0] != null;
    }

    /** Nombre de pila del alumno, o nulo si no existe */
    public String findNombre(String alumnoId) {
        List<Object[]> rows = db.executeQueryArray("SELECT nombre FROM Alumno WHERE alumnoId = ?", alumnoId);
        return rows != null && !rows.isEmpty() && rows.get(0)[0] != null ? rows.get(0)[0].toString() : null;
    }

    /** Si el email pertenece a un alumno distinto del indicado */
    public boolean existeEmailDeOtroAlumno(String email, String alumnoId) {
        List<Object[]> otros = db.executeQueryArray(
                "SELECT alumnoId FROM Alumno WHERE email = ? AND alumnoId <> ?", email, alumnoId);
        return !otros.isEmpty();
    }

    /** Cambia el email del alumno (dentro de la transacción del modelo) */
    public void actualizarEmail(Connection conn, String alumnoId, String email) throws SQLException {
        update(conn, "UPDATE Alumno SET email = ? WHERE alumnoId = ?", email, alumnoId);
    }
}
