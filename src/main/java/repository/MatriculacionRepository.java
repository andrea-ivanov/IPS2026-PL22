package repository;

import java.util.List;
import java.util.Map;

import model.matriculacion.GestionMatriculaModel;
import model.matriculacion.GrupoConsultaDTO;
import model.matriculacion.GrupoPlazasDTO;
import model.matriculacion.MatriculaSecretariaDTO;
import model.matriculacion.Matriculacion;
import model.matriculacion.MatriculacionDisplayDTO;
import model.matriculacion.MatriculacionModel;
import util.Database;

/**
 * Repositorio unificado para el acceso a datos de Matriculación.
 * Integra las operaciones de la vista de Administración, Secretaría y Estudiante.
 */
public class MatriculacionRepository {
	private MatriculacionModel matriculacionModel;
	private GestionMatriculaModel gestionModel;
	private Database db;

	public MatriculacionRepository() {
		try {
			this.db = new Database();
		} catch (Exception ignored) {
		}
		this.matriculacionModel = this.db != null ? new MatriculacionModel(this.db) : new MatriculacionModel();
		this.gestionModel = new GestionMatriculaModel(this.db);
	}

	public MatriculacionRepository(Database db) {
		this.db = db;
		this.matriculacionModel = new MatriculacionModel(db);
		this.gestionModel = new GestionMatriculaModel(db);
	}

	public Matriculacion findById(String id) {
		return matriculacionModel.buscarPorId(id);
	}

	public Matriculacion findById(Long id) {
		return matriculacionModel.buscarPorId(id);
	}

	// =========================================================================
	// Secretaría: consulta (HU 35226) y ciclo de vida (HU 35229)
	// =========================================================================

	/**
	 * Listado de cursos matriculados (una fila por MatriculaCurso) para la vista de Secretaría.
	 */
	public List<MatriculaSecretariaDTO> getMatriculasSecretaria() {
		return gestionModel.getMatriculas();
	}

	public MatriculaSecretariaDTO getMatriculaSecretaria(String matriculaId, String cursoId) {
		return gestionModel.getMatricula(matriculaId, cursoId);
	}

	public List<String> getAniosAcademicos() {
		return gestionModel.getAniosAcademicos();
	}

	public String getAnioAcademicoActual(String fechaHoy) {
		return gestionModel.getAnioAcademicoActual(fechaHoy);
	}

	public List<GrupoConsultaDTO> getGruposConsulta(String anioAcademicoId) {
		return gestionModel.getGruposConsulta(anioAcademicoId);
	}

	public List<MatriculaSecretariaDTO> getMatriculasGrupo(String cursoId, String grupoCodigo, String anioAcademicoId) {
		return gestionModel.getMatriculasGrupo(cursoId, grupoCodigo, anioAcademicoId);
	}

	/**
	 * Activa lo pagado de los años iniciados y finaliza lo activo de los años terminados.
	 */
	public int actualizarCicloVida(String fechaHoy) {
		return gestionModel.actualizarCicloVida(fechaHoy);
	}

	public List<GrupoPlazasDTO> getGruposMatricula(String matriculaId, String cursoId) {
		return gestionModel.getGrupos(matriculaId, cursoId);
	}

	public void asignarGrupos(String matriculaId, String cursoId, Map<String, String> grupoPorTipo) {
		gestionModel.asignarGrupos(matriculaId, cursoId, grupoPorTipo);
	}

	public String aceptarSolicitud(String matriculaId, String cursoId, String metodoPago, String fechaHoy) {
		return gestionModel.aceptarSolicitud(matriculaId, cursoId, metodoPago, fechaHoy);
	}

	public void modificarMatricula(String matriculaId, String cursoId, String metodoPago, String email,
			String fechaHoy) {
		gestionModel.modificarMatricula(matriculaId, cursoId, metodoPago, email, fechaHoy);
	}

	public void cancelarMatricula(String matriculaId, String cursoId, String motivo, String fechaHoy,
			boolean todaLaMatricula) {
		gestionModel.cancelar(matriculaId, cursoId, motivo, fechaHoy, todaLaMatricula);
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
		return matriculacionModel.getMatriculacionesPorCurso(idCurso);
	}

	/**
     * Obtiene las matrículas (alumnos) asociadas a un curso específico
     * filtradas por el año académico indicado.
     */
    public List<MatriculacionDisplayDTO> getMatriculacionesPorCursoYAnoActual(String idCurso, String anioAcademicoId) {
        return matriculacionModel.getMatriculacionesPorCursoYAnoActual(idCurso, anioAcademicoId);
    }

	/**
	 * Obtiene los grados en los que está o ha estado matriculado el estudiante.
	 */
	public List<Object[]> getGradosPorEstudiante(String uo) {
		return matriculacionModel.getGradosPorEstudiante(uo);
	}

	/**
	 * Obtiene las asignaturas matriculadas de un estudiante filtradas por el grado consultado (HU 35232).
	 */
	public List<MatriculacionDisplayDTO> getMatriculasPorEstudianteYGrado(String uo, String gradoId) {
		return matriculacionModel.getMatriculasPorEstudianteYGrado(uo, gradoId);
	}

	/**
	 * Obtiene el resumen de hitos de permanencia del estudiante (HU 35232).
	 */
	public model.matriculacion.HitosPermanenciaDTO getHitosPermanencia(String uo, String gradoId) {
		return matriculacionModel.getHitosPermanencia(uo, gradoId);
	}

	/**
	 * Obtiene el historial de calificaciones en convocatorias presentadas (HU 35232).
	 */
	public List<model.matriculacion.CalificacionDisplayDTO> getCalificacionesPorEstudianteYGrado(String uo, String gradoId) {
		return matriculacionModel.getCalificacionesPorEstudianteYGrado(uo, gradoId);
	}
}
