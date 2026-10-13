package repository;

import model.MatriculaDisplayDTO;
import giis.demo.util.Database; // El util de los profesores
import java.util.List;

public class MatriculaRepository {
    
    private Database db;

    public MatriculaRepository(Database db) {
        this.db = db;
    }

    public List<MatriculaDisplayDTO> getTodasLasMatriculas() {
        // Consulta SQL real cruzando las tablas para sacar los datos de consulta
        String sql = "SELECT mc.matriculaId AS id, a.email AS email, a.dni AS dni, " +
                     "a.nombre || ' ' || a.apellidos AS estudiante, g.nombre AS grado, " +
                     "c.nombre AS curso, COALESCE(GROUP_CONCAT(mg.grupoCodigo, ', '), 'Sin asignar') AS grupo, " +
                     "mc.estado AS estado " +
                     "FROM MatriculaCurso mc " +
                     "JOIN Matricula m ON mc.matriculaId = m.matriculaId " +
                     "JOIN Alumno a ON m.alumnoId = a.alumnoId " +
                     "JOIN Grado g ON mc.gradoId = g.gradoId " +
                     "JOIN Curso c ON mc.cursoId = c.cursoId " +
                     "LEFT JOIN MatriculaGrupo mg ON mc.matriculaId = mg.matriculaId AND mc.cursoId = mg.cursoId " +
                     "GROUP BY mc.matriculaId, mc.cursoId";

        return db.executeQueryPojo(MatriculaDisplayDTO.class, sql);
    }

    public MatriculaDisplayDTO getMatriculaById(String idMatricula) {
        String sql = "SELECT mc.matriculaId AS id, a.email AS email, a.dni AS dni, " +
                     "a.nombre || ' ' || a.apellidos AS estudiante, g.nombre AS grado, " +
                     "c.nombre AS curso, COALESCE(GROUP_CONCAT(mg.grupoCodigo, ', '), 'Sin asignar') AS grupo, " +
                     "mc.estado AS estado " +
                     "FROM MatriculaCurso mc " +
                     "JOIN Matricula m ON mc.matriculaId = m.matriculaId " +
                     "JOIN Alumno a ON m.alumnoId = a.alumnoId " +
                     "JOIN Grado g ON mc.gradoId = g.gradoId " +
                     "JOIN Curso c ON mc.cursoId = c.cursoId " +
                     "LEFT JOIN MatriculaGrupo mg ON mc.matriculaId = mg.matriculaId AND mc.cursoId = mg.cursoId " +
                     "WHERE mc.matriculaId = ? " +
                     "GROUP BY mc.matriculaId, mc.cursoId LIMIT 1";
                     
        List<MatriculaDisplayDTO> result = db.executeQueryPojo(MatriculaDisplayDTO.class, sql, idMatricula);
        return result.isEmpty() ? null : result.get(0);
    }

    public void modificarEmailMatricula(String idMatricula, String nuevoEmail) {
        // Actualizamos el email del alumno basándonos en el ID de su matrícula
        String sql = "UPDATE Alumno SET email = ? WHERE alumnoId = (SELECT alumnoId FROM Matricula WHERE matriculaId = ?)";
        db.executeUpdate(sql, nuevoEmail, idMatricula);
    }

    public void actualizarEstadoMatricula(String idMatricula, String nuevoEstado) {
        String sql = "UPDATE MatriculaCurso SET estado = ?, pagoValidado = 1 WHERE matriculaId = ?";
        db.executeUpdate(sql, nuevoEstado, idMatricula);
    }

    public void cancelarMatricula(String idMatricula, String motivo) {
        // 1. Cancelar la matrícula general
        String sqlMatricula = "UPDATE Matricula SET estado = 'CANCELADA', motivoCancelacion = ?, fechaCancelacion = date('now') WHERE matriculaId = ?";
        db.executeUpdate(sqlMatricula, motivo, idMatricula);

        // 2. Cancelar en cascada las asignaturas (cursos) asociados
        String sqlCursos = "UPDATE MatriculaCurso SET estado = 'CANCELADA', motivoCancelacion = ?, fechaCancelacion = date('now') WHERE matriculaId = ? AND estado != 'FINALIZADA'";
        db.executeUpdate(sqlCursos, motivo, idMatricula);
    }
}