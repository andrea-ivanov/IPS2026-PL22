package repository;

import java.util.ArrayList;
import java.util.List;

import model.matriculacion.Matriculacion;
import model.matriculacion.MatriculacionDisplayDTO;
import model.matriculacion.MatriculacionModel;
import util.Database;

/**
 * Repositorio unificado para el acceso a datos de Matriculación.
 * Integra las operaciones de la vista de Administración, Secretaría y Estudiante.
 * Soporta consultas en base de datos real y fallback a datos simulados para vistas y pruebas.
 */
public class MatriculacionRepository {
	private MatriculacionModel matriculacionModel;
	private Database db;

	// Lista en memoria para simular datos y soportar vistas interactivas / mocks
	private List<MatriculacionDisplayDTO> mockData;

	public MatriculacionRepository() {
		try {
			this.db = new Database();
		} catch (Exception ignored) {
		}
		this.matriculacionModel = this.db != null ? new MatriculacionModel(this.db) : new MatriculacionModel();
		initMockData();
	}

	public MatriculacionRepository(Database db) {
		this.db = db;
		this.matriculacionModel = new MatriculacionModel(db);
		initMockData();
	}

	public MatriculacionRepository(giis.demo.util.Database db) {
		try {
			this.db = new Database();
		} catch (Exception ignored) {
		}
		this.matriculacionModel = this.db != null ? new MatriculacionModel(this.db) : new MatriculacionModel();
		initMockData();
	}

	private void initMockData() {
		mockData = new ArrayList<>();
		mockData.add(new MatriculacionDisplayDTO("M001", "Alejandro El Harchi Argüelles", "71649234A", "aharchi@uni.es", "Ing. Informática", "3º Curso", "A", "SOLICITADA"));
		mockData.add(new MatriculacionDisplayDTO("M002", "Lucía Gómez", "53819200B", "lgomez@uni.es", "Ing. Informática", "1º Curso", "B", "PAGADA"));
		mockData.add(new MatriculacionDisplayDTO("M003", "Carlos Pérez", "12345678C", "cperez@uni.es", "Matemáticas", "2º Curso", "A", "ACTIVA"));
		mockData.add(new MatriculacionDisplayDTO("M004", "Sofía Martín", "87654321D", "smartin@uni.es", "Física", "4º Curso", "Sin asignar", "CANCELADA"));
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
	 * Consulta la base de datos real cruzando las tablas MatriculaCurso, Matricula, Alumno, Grado, Curso y Grupo,
	 * con fallback transparente a los datos simulados si la BD no está disponible.
	 */
	public List<MatriculacionDisplayDTO> getTodasLasMatriculas() {
		if (db != null) {
			try {
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
				List<MatriculacionDisplayDTO> list = db.executeQueryPojo(MatriculacionDisplayDTO.class, sql);
				if (list != null && !list.isEmpty()) {
					return list;
				}
			} catch (Exception ex) {
				System.err.println("Aviso al consultar BD en getTodasLasMatriculas: " + ex.getMessage());
			}
		}
		return mockData;
	}

	/**
	 * Obtiene los detalles de una matrícula específica por su ID.
	 */
	public MatriculacionDisplayDTO getMatriculaById(String idMatricula) {
		if (db != null && idMatricula != null) {
			try {
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
				List<MatriculacionDisplayDTO> result = db.executeQueryPojo(MatriculacionDisplayDTO.class, sql, idMatricula);
				if (result != null && !result.isEmpty()) {
					return result.get(0);
				}
			} catch (Exception ex) {
				System.err.println("Aviso al consultar BD en getMatriculaById: " + ex.getMessage());
			}
		}
		if (idMatricula != null) {
			for (MatriculacionDisplayDTO m : mockData) {
				if (idMatricula.equalsIgnoreCase(m.getId())) {
					return m;
				}
			}
		}
		return null;
	}

	/**
	 * Modifica el email de contacto del alumno asociado a una matrícula.
	 */
	public void modificarEmailMatricula(String idMatricula, String nuevoEmail) {
		if (db != null && idMatricula != null) {
			try {
				String sql = "UPDATE Alumno SET email = ? WHERE alumnoId = (SELECT alumnoId FROM Matricula WHERE matriculaId = ?)";
				db.executeUpdate(sql, nuevoEmail, idMatricula);
			} catch (Exception ex) {
				System.err.println("Aviso al modificar email en BD: " + ex.getMessage());
			}
		}
		if (idMatricula != null) {
			for (MatriculacionDisplayDTO m : mockData) {
				if (idMatricula.equalsIgnoreCase(m.getId())) {
					m.setEmail(nuevoEmail);
				}
			}
		}
	}

	/**
	 * Actualiza el estado de una matrícula (p. ej. "PAGADA", "ACTIVA").
	 */
	public void actualizarEstadoMatricula(String idMatricula, String nuevoEstado) {
		if (idMatricula != null) {
			for (MatriculacionDisplayDTO m : mockData) {
				if (idMatricula.equalsIgnoreCase(m.getId())) {
					m.setEstado(nuevoEstado);
					break;
				}
			}
		}
		if (db != null && idMatricula != null) {
			try {
				String sqlCurso = "UPDATE MatriculaCurso SET estado = ?, pagoValidado = 1 WHERE matriculaId = ?";
				db.executeUpdate(sqlCurso, nuevoEstado, idMatricula);
			} catch (Exception ignored) {
			}
			try {
				String sqlMat = "UPDATE Matricula SET estado = ? WHERE matriculaId = ?";
				db.executeUpdate(sqlMat, nuevoEstado, idMatricula);
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * Cancela una matrícula registrando el motivo de cancelación.
	 */
	public void cancelarMatricula(String idMatricula, String motivo) {
		if (idMatricula != null) {
			for (MatriculacionDisplayDTO m : mockData) {
				if (idMatricula.equalsIgnoreCase(m.getId())) {
					m.setEstado("CANCELADA");
					break;
				}
			}
		}
		if (db != null && idMatricula != null) {
			try {
				String sqlMatricula = "UPDATE Matricula SET estado = 'CANCELADA', motivoCancelacion = ?, fechaCancelacion = date('now') WHERE matriculaId = ?";
				db.executeUpdate(sqlMatricula, motivo, idMatricula);
			} catch (Exception ignored) {
			}
			try {
				String sqlCursos = "UPDATE MatriculaCurso SET estado = 'CANCELADA', motivoCancelacion = ?, fechaCancelacion = date('now') WHERE matriculaId = ? AND estado != 'FINALIZADA'";
				db.executeUpdate(sqlCursos, motivo, idMatricula);
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * Obtiene las matrículas de un estudiante por su UO o alumnoId.
	 */
	public List<MatriculacionDisplayDTO> getMatriculasPorEstudiante(String uo) {
		return matriculacionModel.getMatriculasPorEstudiante(uo);
	}

	/**
	 * Obtiene las matrículas (alumnos) asociadas a un curso específico para la tabla de AdminOfertaView.
	 */
	public List<MatriculacionDisplayDTO> getMatriculacionesPorCurso(String idCurso) {
		if (db != null && idCurso != null) {
			try {
				String sql = "SELECT a.alumnoId AS id, " +
							 "       a.nombre || ' ' || a.apellidos AS nombreAlumno, " +
							 "       mc.cursoId AS idCurso, " +
							 "       a.email AS email, " +
							 "       mc.estado AS estado " +
							 "FROM MatriculaCurso mc " +
							 "JOIN Matricula m ON mc.matriculaId = m.matriculaId " +
							 "JOIN Alumno a ON m.alumnoId = a.alumnoId " +
							 "WHERE mc.cursoId = ? " +
							 "ORDER BY a.alumnoId";
				List<MatriculacionDisplayDTO> list = db.executeQueryPojo(MatriculacionDisplayDTO.class, sql, idCurso);
				if (list != null && !list.isEmpty()) {
					return list;
				}
			} catch (Exception ex) {
				System.err.println("Aviso al consultar BD en getMatriculacionesPorCurso: " + ex.getMessage());
			}
		}
		return view.admin.TestDataFactory.crearMockMatriculacionesDisplay(idCurso);
	}
}
