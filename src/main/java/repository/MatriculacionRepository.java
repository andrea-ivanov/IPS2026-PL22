package repository;

import java.util.List;
import java.util.Map;

import model.matriculacion.FiltroMatriculasSecretaria;
import model.matriculacion.GestionMatriculaModel;
import model.matriculacion.GrupoConsultaDTO;
import model.matriculacion.GrupoPlazasDTO;
import model.matriculacion.MatriculaSecretariaDTO;
import util.Database;

/**
 * Repositorio unificado para el acceso a datos de Matriculación.
 * Operaciones de la vista de Secretaría (las del estudiante están en MatriculacionModel).
 */
public class MatriculacionRepository {
	private GestionMatriculaModel gestionModel;

	public MatriculacionRepository(Database db) {
		this.gestionModel = new GestionMatriculaModel(db);
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

	/** Número de cursos matriculados que cumplen los filtros (para paginar el listado). */
	public int contarMatriculasSecretaria(FiltroMatriculasSecretaria filtro) {
		return gestionModel.contarMatriculas(filtro);
	}

	/** Una página del listado de cursos matriculados que cumplen los filtros. */
	public List<MatriculaSecretariaDTO> getMatriculasSecretaria(FiltroMatriculasSecretaria filtro, int offset,
			int limite) {
		return gestionModel.getMatriculas(filtro, offset, limite);
	}

	public List<String> getAniosConMatriculas() {
		return gestionModel.getAniosConMatriculas();
	}

	public List<String> getGradosConMatriculas() {
		return gestionModel.getGradosConMatriculas();
	}

	public List<String> getCursosConMatriculas() {
		return gestionModel.getCursosConMatriculas();
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

	/** Número de grupos del grado y curso indicados (para paginar la consulta de grupos). */
	public int contarGruposConsulta(String grado, String curso) {
		return gestionModel.contarGruposConsulta(grado, curso);
	}

	/** Una página de los grupos del grado y curso indicados con su ocupación en el año académico. */
	public List<GrupoConsultaDTO> getGruposConsulta(String anioAcademicoId, String grado, String curso, int offset,
			int limite) {
		return gestionModel.getGruposConsulta(anioAcademicoId, grado, curso, offset, limite);
	}

	public List<String> getGradosConGrupos() {
		return gestionModel.getGradosConGrupos();
	}

	public List<String> getCursosConGrupos() {
		return gestionModel.getCursosConGrupos();
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
}
