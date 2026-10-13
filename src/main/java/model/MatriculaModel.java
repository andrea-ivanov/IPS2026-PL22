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
     * Obtiene el listado de asignaturas matriculadas de un estudiante filtradas por el grado consultado.
     * Consulta de referencia HU 35232 de base-de-datos.md.
     */
    public List<MatriculaDisplayDTO> getMatriculasPorEstudianteYGrado(String uo, String gradoId) {
        String sql = "SELECT m.matriculaId AS id, "
                + "       m.anioAcademicoId, "
                + "       c.nombre AS curso, "
                + "       gc.anio, "
                + "       mc.estado, "
                + "       (SELECT COUNT(*) FROM MatriculaCurso mc2 "
                + "        JOIN Matricula m2 ON m2.matriculaId = mc2.matriculaId "
                + "        WHERE m2.alumnoId = m.alumnoId AND mc2.cursoId = mc.cursoId "
                + "          AND m2.anioAcademicoId <= m.anioAcademicoId) AS numMatricula "
                + "FROM MatriculaCurso mc "
                + "JOIN Matricula m ON m.matriculaId = mc.matriculaId "
                + "JOIN Curso c ON c.cursoId = mc.cursoId "
                + "JOIN GradoCurso gc ON gc.gradoId = mc.gradoId AND gc.cursoId = mc.cursoId "
                + "WHERE m.alumnoId = ? ";

        if (gradoId != null && !gradoId.trim().isEmpty()) {
            sql += "AND m.gradoId = ? ";
            sql += "ORDER BY gc.anio, numMatricula DESC";
            return db.executeQueryPojo(MatriculaDisplayDTO.class, sql, uo, gradoId);
        } else {
            sql += "ORDER BY gc.anio, numMatricula DESC";
            return db.executeQueryPojo(MatriculaDisplayDTO.class, sql, uo);
        }
    }
}
