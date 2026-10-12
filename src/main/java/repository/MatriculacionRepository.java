package repository;

import java.util.ArrayList;
import java.util.List;

import model.matriculacion.Matriculacion;
import model.matriculacion.MatriculacionDisplayDTO;
import model.matriculacion.MatriculacionModel;
import util.Database;

/**
 * Repositorio unificado para el acceso a datos de Matriculación.
 * Permite integrarse fácilmente tanto con la arquitectura basada en repositorios
 * y modelos de GIIS (búsqueda por ID) como con las operaciones de gestión
 * de la vista de Secretaría y de Administración.
 */
public class MatriculacionRepository {
	private final MatriculacionModel matriculacionModel;
	private Database db;

	// Lista en memoria para simular datos y soportar vistas interactivas / mocks
	private List<MatriculacionDisplayDTO> mockData;

	public MatriculacionRepository() {
		this.matriculacionModel = new MatriculacionModel();
		try {
			this.db = new Database();
		} catch (Exception ignored) {
		}
		initMockData();
	}

	public MatriculacionRepository(Database db) {
		this.db = db;
		this.matriculacionModel = new MatriculacionModel(db);
		initMockData();
	}

	private void initMockData() {
		mockData = new ArrayList<>();
		mockData.add(new MatriculacionDisplayDTO("M001", "Alejandro El Harchi Argüelles", "71649234A", "Ing. Informática", "3º Curso", "A", "SOLICITADA"));
		mockData.add(new MatriculacionDisplayDTO("M002", "Lucía Gómez", "53819200B", "Ing. Informática", "1º Curso", "B", "PAGADA"));
		mockData.add(new MatriculacionDisplayDTO("M003", "Carlos Pérez", "12345678C", "Matemáticas", "2º Curso", "A", "ACTIVA"));
		mockData.add(new MatriculacionDisplayDTO("M004", "Sofía Martín", "87654321D", "Física", "4º Curso", "C", "CANCELADA"));
		for (MatriculacionDisplayDTO m : mockData) {
			m.setIdAnoAcademico(1L);
		}
	}

	public Matriculacion findById(String id) {
		return matriculacionModel.buscarPorId(id);
	}

	public Matriculacion findById(Long id) {
		return matriculacionModel.buscarPorId(id);
	}

	/**
	 * Obtiene todas las matrículas para el listado de la vista de Secretaría.
	 */
	public List<MatriculacionDisplayDTO> getTodasLasMatriculas() {
		return mockData;
	}

	/**
	 * Actualiza el estado de una matrícula (p. ej. "PAGADA", "ACTIVA").
	 */
	public void actualizarEstadoMatricula(String idMatricula, String nuevoEstado) {
		for (MatriculacionDisplayDTO m : mockData) {
			if (m.getId() != null && m.getId().equals(idMatricula)) {
				m.setEstado(nuevoEstado);
				break;
			}
		}
		if (db != null) {
			try {
				String sql = "UPDATE Matricula SET estado = ? WHERE id_matricula = ?";
				db.executeUpdate(sql, nuevoEstado, idMatricula);
			} catch (Exception ignored) {
			}
		}
		System.out.println("BD SIMULADA: Matrícula " + idMatricula + " actualizada al estado: " + nuevoEstado);
	}

	/**
	 * Cancela una matrícula registrando el motivo de cancelación.
	 */
	public void cancelarMatricula(String idMatricula, String motivo) {
		for (MatriculacionDisplayDTO m : mockData) {
			if (m.getId() != null && m.getId().equals(idMatricula)) {
				m.setEstado("CANCELADA");
				break;
			}
		}
		if (db != null) {
			try {
				String sql = "UPDATE Matricula SET estado = 'CANCELADA' WHERE id_matricula = ?";
				db.executeUpdate(sql, idMatricula);
			} catch (Exception ignored) {
			}
		}
		System.out.println("BD SIMULADA: Matrícula " + idMatricula + " CANCELADA por motivo: " + motivo);
	}
}
