package repository;

import java.util.List;
import java.util.Map;

import model.ActaDisplayDTO;
import model.CalificacionDisplayDTO;
import util.ApplicationException;
import util.Database;

/**
 * Acceso a la base de datos para las actas y las notas (HU 35239).
 */
public class ActaRepository {
    private Database db = new Database();

    /** Actas de la asignatura, de la mas reciente a la mas antigua */
    public List<ActaDisplayDTO> getActasCurso(String cursoId) {
        String sql = "SELECT a.cursoId, a.anioAcademicoId, a.tipoConvocatoria, cv.fechaFin, a.estado"
                + " FROM Acta a"
                + " JOIN Convocatoria cv ON cv.anioAcademicoId = a.anioAcademicoId AND cv.tipoConvocatoria = a.tipoConvocatoria"
                + " WHERE a.cursoId = ?"
                + " ORDER BY cv.fechaFin DESC";
        return db.executeQueryPojo(ActaDisplayDTO.class, sql, cursoId);
    }

    /**
     * Alumnos del acta con su nota. En un acta abierta salen todos los matriculados en la asignatura ese anio
     * (PAGADA, ACTIVA o FINALIZADA); en una cerrada solo los que tienen calificacion.
     * La nota sale vacia si no tiene calificacion y NP si la calificacion no tiene nota
     */
    public List<CalificacionDisplayDTO> getCalificaciones(String cursoId, String anioAcademicoId, String tipoConvocatoria) {
        String sql = "SELECT mc.matriculaId, al.alumnoId, al.nombre || ' ' || al.apellidos AS alumno, al.dni,"
                + " CASE WHEN ca.matriculaId IS NULL THEN '' WHEN ca.nota IS NULL THEN 'NP' ELSE CAST(ca.nota AS TEXT) END AS nota"
                + " FROM MatriculaCurso mc"
                + " JOIN Matricula m ON m.matriculaId = mc.matriculaId"
                + " JOIN Alumno al ON al.alumnoId = m.alumnoId"
                + " JOIN Acta a ON a.cursoId = mc.cursoId AND a.anioAcademicoId = m.anioAcademicoId"
                + " LEFT JOIN Calificacion ca ON ca.matriculaId = mc.matriculaId AND ca.cursoId = a.cursoId"
                + "   AND ca.anioAcademicoId = a.anioAcademicoId AND ca.tipoConvocatoria = a.tipoConvocatoria"
                + " WHERE mc.cursoId = ? AND a.anioAcademicoId = ? AND a.tipoConvocatoria = ?"
                + "   AND mc.estado IN ('PAGADA', 'ACTIVA', 'FINALIZADA')"
                + "   AND (a.estado = 'ABIERTA' OR ca.matriculaId IS NOT NULL)"
                + " ORDER BY al.apellidos, al.nombre";
        return db.executeQueryPojo(CalificacionDisplayDTO.class, sql, cursoId, anioAcademicoId, tipoConvocatoria);
    }

    /**
     * Guarda las notas de un acta (clave: matriculaId, valor: nota escrita en la tabla).
     * Solo el coordinador de la asignatura y solo en actas abiertas.
     * Se validan todas las notas antes de guardar: si alguna no es valida no se guarda ninguna.
     * Nota vacia: sin calificar (se borra la calificacion); NP: no presentado (calificacion sin nota)
     */
    public void guardarNotas(String profesorId, String cursoId, String anioAcademicoId, String tipoConvocatoria,
            Map<String, String> notas) {
        comprobarCoordinador(profesorId, cursoId);
        comprobarActaAbierta(cursoId, anioAcademicoId, tipoConvocatoria);
        for (Map.Entry<String, String> n : notas.entrySet())
            parseNota(n.getKey(), n.getValue());

        for (Map.Entry<String, String> n : notas.entrySet()) {
            String texto = n.getValue() == null ? "" : n.getValue().trim();
            if (texto.isEmpty()) {
                db.executeUpdate("DELETE FROM Calificacion"
                        + " WHERE matriculaId = ? AND cursoId = ? AND anioAcademicoId = ? AND tipoConvocatoria = ?",
                        n.getKey(), cursoId, anioAcademicoId, tipoConvocatoria);
            } else {
                db.executeUpdate("INSERT OR REPLACE INTO Calificacion"
                        + " (matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) VALUES (?, ?, ?, ?, ?)",
                        n.getKey(), cursoId, anioAcademicoId, tipoConvocatoria, parseNota(n.getKey(), texto));
            }
        }
    }

    /** Convierte el texto de la tabla en nota: null si es vacio o NP; admite coma o punto decimal */
    private Double parseNota(String matriculaId, String texto) {
        texto = texto == null ? "" : texto.trim();
        if (texto.isEmpty() || "NP".equalsIgnoreCase(texto))
            return null;
        try {
            double nota = Double.parseDouble(texto.replace(',', '.'));
            if (nota >= 0 && nota <= 10)
                return nota;
        } catch (NumberFormatException e) {
            // se trata igual que una nota fuera de rango
        }
        throw new ApplicationException("Nota no valida en la matricula " + matriculaId + ": '" + texto
                + "'. La nota debe ser un numero entre 0 y 10 o NP");
    }

    private void comprobarCoordinador(String profesorId, String cursoId) {
        List<Object[]> rows = db.executeQueryArray("SELECT 1 FROM Curso WHERE cursoId = ? AND coordinadorId = ?",
                cursoId, profesorId);
        if (rows.isEmpty())
            throw new ApplicationException("Solo el coordinador de la asignatura puede modificar las notas");
    }

    private void comprobarActaAbierta(String cursoId, String anioAcademicoId, String tipoConvocatoria) {
        List<Object[]> rows = db.executeQueryArray("SELECT 1 FROM Acta WHERE cursoId = ? AND anioAcademicoId = ?"
                + " AND tipoConvocatoria = ? AND estado = 'ABIERTA'", cursoId, anioAcademicoId, tipoConvocatoria);
        if (rows.isEmpty())
            throw new ApplicationException("El acta esta cerrada: las notas son de solo lectura");
    }
}
