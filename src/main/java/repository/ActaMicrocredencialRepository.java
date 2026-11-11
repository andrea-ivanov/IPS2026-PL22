package repository;

import java.util.Collections;
import java.util.List;

import model.microcredencial.CalificacionMicrocredencialDTO;
import model.microcredencial.MicrocredencialDisplayDTO;
import util.Database;

/**
 * Acceso a datos del acta de una microcredencial (HU 36459): alumnos del acta, sus notas y el cierre.
 * Solo SQL y mapeo de filas a DTO; las reglas están en ActaMicrocredencialModel.
 */
public class ActaMicrocredencialRepository {
	private Database db;

	public ActaMicrocredencialRepository(Database db) {
		this.db = db;
	}

	/** La microcredencial si existe y la imparte ese profesor; null si no */
	public MicrocredencialDisplayDTO findByIdYProfesor(String microcredencialId, String profesorId) {
		List<MicrocredencialDisplayDTO> rows = db.executeQueryPojo(MicrocredencialDisplayDTO.class,
				"SELECT microcredencialId, anioAcademicoId, nombre, ramaConocimiento, CAST(creditos AS REAL) AS creditos, "
						+ "plazas, inicioMatriculacion, finMatriculacion, inicioImparticion, finImparticion, "
						+ "fechaPropuesta, fechaResolucion, motivoRechazo, fechaCancelacion, fechaCierreActa, "
						+ "cierreAutomatico, estado "
						+ "FROM Microcredencial WHERE microcredencialId = ? AND profesorId = ?",
				microcredencialId, profesorId);
		return rows.isEmpty() ? null : rows.get(0);
	}

	/** Alumnos del acta: inscripciones en alguno de los estados indicados, por apellidos y nombre */
	public List<CalificacionMicrocredencialDTO> findCalificaciones(String microcredencialId, List<String> estados) {
		String marcas = String.join(", ", Collections.nCopies(estados.size(), "?"));
		Object[] params = new Object[estados.size() + 1];
		params[0] = microcredencialId;
		for (int i = 0; i < estados.size(); i++) {
			params[i + 1] = estados.get(i);
		}
		return db.executeQueryPojo(CalificacionMicrocredencialDTO.class,
				"SELECT mm.matriculaId, a.alumnoId, a.nombre || ' ' || a.apellidos AS alumno, a.dni, mm.estado, "
						+ "CAST(mm.nota AS REAL) AS nota "
						+ "FROM MatriculaMicrocredencial mm "
						+ "JOIN Matricula m ON m.matriculaId = mm.matriculaId "
						+ "JOIN Alumno a ON a.alumnoId = m.alumnoId "
						+ "WHERE mm.microcredencialId = ? AND mm.estado IN (" + marcas + ") "
						+ "ORDER BY a.apellidos, a.nombre",
				params);
	}
}
