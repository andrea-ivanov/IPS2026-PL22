package model.curso;

import static util.Util.parsearAnio;
import static util.Util.parsearCreditos;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import model.grado.EstadoGrado;
import repository.CursoRepository;
import repository.GradoRepository;
import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Modelo de negocio y caso de uso para la gestión y búsqueda de Cursos / Asignaturas.
 */
public class CursoModel {
	private final Database db;
	private final CursoRepository cursoRepository;
	private final GradoRepository gradoRepository;

	public CursoModel(Database db) {
		this.db = db;
		this.cursoRepository = new CursoRepository(db);
		this.gradoRepository = new GradoRepository(db);
	}

	/**
	 * Obtiene los cursos de un grado para la tabla de AdminOfertaView.
	 */
	public List<CursoDisplayDTO> getCursosPorGrado(String gradoId) {
		return cursoRepository.findByGrado(gradoId);
	}

	/**
	 * Crea un curso nuevo y lo asocia ACTIVO al grado indicado, que debe estar VIGENTE.
	 *
	 * @return Identificador generado (C001, C002...)
	 * @throws ValidationException Si falta algún dato, los créditos o el año no son válidos o el grado no está VIGENTE
	 */
	public String crearCurso(String gradoId, String nombre, String creditos, String anio, String coordinadorId,
			String itinerario) {
		validarObligatorio(gradoId, "Debe indicarse el grado del curso");
		validarObligatorio(nombre, "El nombre del curso es obligatorio");
		validarObligatorio(creditos, "Los créditos del curso son obligatorios");
		validarObligatorio(anio, "El año del curso es obligatorio");
		validarObligatorio(coordinadorId, "El coordinador del curso es obligatorio");
		validarObligatorio(itinerario, "El itinerario del curso es obligatorio");
		double valorCreditos = parsearCreditos(creditos);
		int valorAnio = parsearAnio(anio);

		return db.enTransaccion(conn -> {
			String estadoGrado = gradoRepository.findEstado(conn, gradoId);
			if (estadoGrado == null) {
				throw new ResourceNotFoundException("Grado", gradoId);
			}
			if (!EstadoGrado.VIGENTE.name().equals(estadoGrado)) {
				throw new ValidationException("Solo se pueden añadir cursos a grados vigentes");
			}
			String cursoId = generarId(conn);
			if (cursoRepository.existe(conn, cursoId)) {
				throw new ValidationException("Ya existe un curso con el código " + cursoId);
			}
			cursoRepository.insertarCurso(conn, cursoId, coordinadorId.trim(), nombre.trim(), valorCreditos);
			cursoRepository.insertarEnGrado(conn, gradoId, cursoId, valorAnio, itinerario.trim(),
					EstadoCurso.ACTIVO.name());
			return cursoId;
		});
	}

	/**
	 * Modifica los datos editables del curso en el grado, en cualquier estado. El año y el itinerario
	 * cambian solo en ese grado; los créditos son de la asignatura y cambian en todos sus grados.
	 *
	 * @throws ValidationException Si falta algún dato o los créditos o el año no son válidos
	 * @throws ResourceNotFoundException Si el curso no pertenece a ese grado
	 */
	public void modificarCurso(String gradoId, String cursoId, String anio, String itinerario, String creditos) {
		validarObligatorio(gradoId, "Debe indicarse el grado del curso");
		validarObligatorio(cursoId, "Debe indicarse el curso a modificar");
		validarObligatorio(anio, "El año del curso es obligatorio");
		validarObligatorio(itinerario, "El itinerario del curso es obligatorio");
		validarObligatorio(creditos, "Los créditos del curso son obligatorios");
		double valorCreditos = parsearCreditos(creditos);
		int valorAnio = parsearAnio(anio);

		db.enTransaccion(conn -> {
			int filas = cursoRepository.actualizarEnGrado(conn, gradoId, cursoId, valorAnio, itinerario.trim());
			if (filas == 0) {
				throw new ResourceNotFoundException("Curso", cursoId);
			}
			cursoRepository.actualizarCreditos(conn, cursoId, valorCreditos);
			return null;
		});
	}

	/**
	 * Obtiene los datos actuales del curso en el grado, sin transformar, para precargar su modificación.
	 *
	 * @throws ResourceNotFoundException Si el curso no pertenece a ese grado
	 */
	public CursoEdicionDTO buscarDatosEdicion(String gradoId, String cursoId) {
		CursoEdicionDTO datos = cursoRepository.findDatosEdicion(gradoId, cursoId);
		if (datos == null) {
			throw new ResourceNotFoundException("Curso", cursoId);
		}
		return datos;
	}

	/**
	 * Cuenta en cuántos grados está el curso.
	 */
	public int contarGradosDelCurso(String cursoId) {
		return cursoRepository.contarGrados(cursoId);
	}

	/**
	 * Comprueba que el curso se puede eliminar del grado en el año académico indicado.
	 *
	 * @throws ValidationException Si el curso ya está INACTIVO o tiene matrículas en el año en ese grado
	 */
	public void validarEliminacion(String gradoId, String cursoId, String anioAcademicoId) {
		db.enTransaccion(conn -> {
			comprobarEliminable(conn, gradoId, cursoId, anioAcademicoId);
			return null;
		});
	}

	/**
	 * Elimina lógicamente el curso del grado: pasa a INACTIVO solo en ese grado.
	 *
	 * @throws ValidationException Si el curso no se puede eliminar (ver validarEliminacion)
	 */
	public void eliminarCurso(String gradoId, String cursoId, String anioAcademicoId) {
		db.enTransaccion(conn -> {
			comprobarEliminable(conn, gradoId, cursoId, anioAcademicoId);
			cursoRepository.inactivarEnGrado(conn, gradoId, cursoId);
			return null;
		});
	}

	private void comprobarEliminable(Connection conn, String gradoId, String cursoId, String anioAcademicoId)
			throws SQLException {
		validarObligatorio(anioAcademicoId, "No hay un año académico actual configurado");
		validarObligatorio(gradoId, "Debe indicarse el grado del curso");
		validarObligatorio(cursoId, "Debe indicarse el curso a eliminar");

		String estado = cursoRepository.findEstadoEnGrado(conn, gradoId, cursoId);
		if (estado == null) {
			throw new ResourceNotFoundException("Curso", cursoId);
		}
		if (EstadoCurso.fromString(estado) == EstadoCurso.INACTIVO) {
			throw new ValidationException("El curso " + cursoId + " ya está inactivo");
		}

		if (cursoRepository.contarMatriculasActivas(conn, gradoId, cursoId, anioAcademicoId) > 0) {
			throw new ValidationException("No se puede eliminar el curso " + cursoId
					+ ": tiene matrículas en el año académico " + anioAcademicoId);
		}
	}

	private String generarId(Connection conn) throws SQLException {
		List<String> ids = cursoRepository.findIdsNumerados(conn);
		int max = 0;
		for (String id : ids) {
			if (id.matches("^C\\d+$")) {
				max = Math.max(max, Integer.parseInt(id.substring(1)));
			}
		}
		return String.format("C%03d", max + 1);
	}

	private void validarObligatorio(String valor, String mensaje) {
		if (valor == null || valor.trim().isEmpty()) {
			throw new ValidationException(mensaje);
		}
	}

}

