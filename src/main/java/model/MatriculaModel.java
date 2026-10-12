package model;

import java.util.List;
import util.Database;

public class MatriculaModel {
    private Database db = new Database();

    /**
     * Obtiene la lista de matrículas de un estudiante dado su UO.
     * De momento hace un JOIN básico con Grado, porque es la tabla que tenemos
     * conectada a Matricula.
     */
    public List<MatriculaDisplayDTO> getMatriculasPorEstudiante(String uo) {
        String sql = "SELECT m.matriculaId AS id, "
                + "       g.nombre AS grado, "
                + "       m.estado AS estado, "
                + "       m.fechaSolicitud AS fecha "
                + "FROM Matricula m "
                + "JOIN Grado g ON m.gradoId = g.gradoId "
                + "WHERE m.alumnoId = ?";

        // Ejecutamos la consulta y mapeamos el resultado automáticamente a nuestro DTO
        return db.executeQueryPojo(MatriculaDisplayDTO.class, sql, uo);
    }
}
