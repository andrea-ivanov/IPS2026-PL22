package model;

import java.util.List;
import util.Database;

public class MatriculaModel {
    private Database db = new Database();

    /**
     * Obtiene la lista de grados en los que está matriculado un estudiante.
     * Formato de cada elemento: "G001 - Nombre del Grado"
     */
    public List<Object[]> getGradosPorEstudiante(String uo) {
        String sql = "SELECT DISTINCT g.gradoId || ' - ' || g.nombre "
                + "FROM Matricula m "
                + "JOIN Grado g ON m.gradoId = g.gradoId "
                + "WHERE m.alumnoId = ? "
                + "ORDER BY g.nombre";
        return db.executeQueryArray(sql, uo);
    }

    /**
     * Obtiene la lista de matrículas de un estudiante dado su UO.
     */
    public List<MatriculaDisplayDTO> getMatriculasPorEstudiante(String uo) {
        return getMatriculasPorEstudianteYGrado(uo, null);
    }

    /**
     * Obtiene la lista de matrículas de un estudiante filtradas por el grado consultado.
     */
    public List<MatriculaDisplayDTO> getMatriculasPorEstudianteYGrado(String uo, String gradoId) {
        if (gradoId == null || gradoId.trim().isEmpty()) {
            String sql = "SELECT m.matriculaId AS id, "
                    + "       g.nombre AS grado, "
                    + "       m.estado AS estado, "
                    + "       m.fechaSolicitud AS fecha "
                    + "FROM Matricula m "
                    + "JOIN Grado g ON m.gradoId = g.gradoId "
                    + "WHERE m.alumnoId = ?";
            return db.executeQueryPojo(MatriculaDisplayDTO.class, sql, uo);
        }

        String sql = "SELECT m.matriculaId AS id, "
                + "       g.nombre AS grado, "
                + "       m.estado AS estado, "
                + "       m.fechaSolicitud AS fecha "
                + "FROM Matricula m "
                + "JOIN Grado g ON m.gradoId = g.gradoId "
                + "WHERE m.alumnoId = ? AND m.gradoId = ?";
        return db.executeQueryPojo(MatriculaDisplayDTO.class, sql, uo, gradoId);
    }
}
