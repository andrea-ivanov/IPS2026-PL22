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
     * Lista de alumnos para el combo del login con formato "AL001 - Nombre Apellidos"
     */
    public List<Object[]> getListaAlumnos() {
        String sql = "SELECT alumnoId || ' - ' || nombre || ' ' || apellidos FROM Alumno ORDER BY alumnoId";
        return db.executeQueryArray(sql);
    }
}

