package repository;

import model.MatriculaDisplayDTO;
import java.util.ArrayList;
import java.util.List;

public class MatriculaRepository {
    
    private List<MatriculaDisplayDTO> mockData;

    public MatriculaRepository() {
        mockData = new ArrayList<>();
        mockData.add(new MatriculaDisplayDTO("M001", "Alejandro El Harchi Argüelles", "71649234A", "UO200001@uniovi.es", "Ing. Informática", "3º Curso", "A", "SOLICITADA"));
        mockData.add(new MatriculaDisplayDTO("M002", "Lucía Gómez", "53819200B", "UO200002@uniovi.es", "Ing. Informática", "1º Curso", "B", "PAGADA"));
        mockData.add(new MatriculaDisplayDTO("M003", "Carlos Pérez", "12345678C", "UO200003@uniovi.es", "Matemáticas", "2º Curso", "A", "ACTIVA"));
        mockData.add(new MatriculaDisplayDTO("M004", "Sofía Martín", "87654321D", "UO200004@uniovi.es", "Física", "4º Curso", "C", "CANCELADA"));
    }

    public List<MatriculaDisplayDTO> getTodasLasMatriculas() {
        return mockData; 
    }

    public void actualizarEstadoMatricula(String idMatricula, String nuevoEstado) {
        for (MatriculaDisplayDTO m : mockData) {
            if (m.getId().equals(idMatricula)) {
                m.setEstado(nuevoEstado);
                break;
            }
        }
    }

    public void cancelarMatricula(String idMatricula, String motivo) {
        for (MatriculaDisplayDTO m : mockData) {
            if (m.getId().equals(idMatricula)) {
                m.setEstado("CANCELADA");
                break;
            }
        }
    }
}