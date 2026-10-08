package model;

import java.util.List;
import util.Database;

public class MatriculaModel {
    private Database db = new Database();

    /**
     * Obtiene la lista de matrículas de un estudiante dado su UO.
     * De momento hace un JOIN básico con Grado, porque es la tabla que tenemos conectada a Matricula.
     */
    public List<MatriculaDisplayDTO> getMatriculasPorEstudiante(String uo) {
        String sql = "SELECT m.id_matricula AS idMatricula, "
                   + "       g.nombre AS nombreGrado, "
                   + "       m.estado, "
                   + "       m.fecha "
                   + "FROM Matricula m "
                   + "JOIN Alumno a ON m.id_alumno = a.id_alumno "
                   + "JOIN Grado g ON m.id_grado = g.id_grado "
                   + "WHERE a.uo = ?";
        
        // Ejecutamos la consulta y mapeamos el resultado automáticamente a nuestro DTO
        return db.executeQueryPojo(MatriculaDisplayDTO.class, sql, uo);
    }
}

