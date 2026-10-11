package repository;

import model.MatriculaDisplayDTO;
import java.util.ArrayList;
import java.util.List;
// import giis.demo.util.Database; 

public class MatriculaRepository {
    
    // private Database db;
    
    // Lista en memoria para simular que la Base de Datos guarda los cambios
    private List<MatriculaDisplayDTO> mockData;

    public MatriculaRepository() {
        // Inicializamos los datos falsos UNA SOLA VEZ cuando arranca el programa
        mockData = new ArrayList<>();
        mockData.add(new MatriculaDisplayDTO("M001", "Alejandro El Harchi Argüelles", "71649234A", "Ing. Informática", "3º Curso", "A", "SOLICITADA"));
        mockData.add(new MatriculaDisplayDTO("M002", "Lucía Gómez", "53819200B", "Ing. Informática", "1º Curso", "B", "PAGADA"));
        mockData.add(new MatriculaDisplayDTO("M003", "Carlos Pérez", "12345678C", "Matemáticas", "2º Curso", "A", "ACTIVA"));
        mockData.add(new MatriculaDisplayDTO("M004", "Sofía Martín", "87654321D", "Física", "4º Curso", "C", "CANCELADA"));
    }

    public List<MatriculaDisplayDTO> getTodasLasMatriculas() {
        /* TODO SQL FUTURO: 
         * String sql = "SELECT id, estudiante, dni, grado, curso, grupo, estado FROM Matricula";
         * return db.executeQueryPojo(MatriculaDisplayDTO.class, sql);
         */
        return mockData; // Devolvemos la lista que está guardada en memoria
    }

    public void actualizarEstadoMatricula(String idMatricula, String nuevoEstado) {
        /* TODO SQL FUTURO: 
         * String sql = "UPDATE Matricula SET estado = ? WHERE id = ?";
         * db.executeUpdate(sql, nuevoEstado, idMatricula);
         */
         
        // Simulación: Buscamos la matrícula y le cambiamos el estado
        for (MatriculaDisplayDTO m : mockData) {
            if (m.getId().equals(idMatricula)) {
                m.setEstado(nuevoEstado);
                break;
            }
        }
        System.out.println("BD SIMULADA: Matrícula " + idMatricula + " actualizada al estado: " + nuevoEstado);
    }

    public void cancelarMatricula(String idMatricula, String motivo) {
        /* TODO SQL FUTURO: 
         * String sql = "UPDATE Matricula SET estado = 'CANCELADA', motivo_cancelacion = ? WHERE id = ?";
         * db.executeUpdate(sql, motivo, idMatricula);
         */
         
        // Simulación: Buscamos la matrícula y la ponemos como CANCELADA
        for (MatriculaDisplayDTO m : mockData) {
            if (m.getId().equals(idMatricula)) {
                m.setEstado("CANCELADA");
                break;
            }
        }
        System.out.println("BD SIMULADA: Matrícula " + idMatricula + " CANCELADA por motivo: " + motivo);
    }
}