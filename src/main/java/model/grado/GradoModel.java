package model.grado;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import model.curso.EstadoCurso;
import repository.GradoRepository;
import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Modelo de negocio y caso de uso para la gestión y búsqueda de Grados.
 * Maneja validaciones de entrada y orquesta las transacciones; el SQL está en GradoRepository.
 */
public class GradoModel {
	/** Años de vigencia de un grado desde su fechaInicioVigencia, pasados los cuales debe renovarse */
	public static final int ANIOS_VIGENCIA = 4;

	private final Database db;
	private final GradoRepository gradoRepository;

	public GradoModel(Database db) {
		this.db = db;
		this.gradoRepository = new GradoRepository(db);
	}

	/**
	 * Obtiene todos los grados para la tabla de AdminOfertaView.
	 */
	public List<GradoDisplayDTO> getTodosLosGrados() {
		return gradoRepository.findAll();
	}

	/**
	 * Crea un grado VIGENTE cuya vigencia empieza con el año académico indicado.
	 *
	 * @return Identificador generado (G001, G002...)
	 * @throws ValidationException Si falta algún dato obligatorio o el año académico no existe
	 */
	public String crearGrado(String nombre, String facultad, String ramaConocimiento, String anioAcademicoId) {
		// TODO: validar aqui y en modificarGrado que la rama es una de las de ck_grado_rama (schema.sql) y lanzar
		// ValidationException con un mensaje claro; poner la lista en un enum RamaConocimiento (valor de BD sin tilde
		// y texto con tilde para la vista), reutilizable para ck_microcredencial_rama
		validarObligatorio(nombre, "El nombre del grado es obligatorio");
		validarObligatorio(facultad, "La facultad del grado es obligatoria");
		validarObligatorio(ramaConocimiento, "La rama de conocimiento del grado es obligatoria");
		validarObligatorio(anioAcademicoId, "No hay un año académico actual configurado");

		return db.enTransaccion(conn -> {
			String fechaInicio = gradoRepository.findFechaInicioAnio(conn, anioAcademicoId.trim());
			if (fechaInicio == null) {
				throw new ValidationException("El año académico " + anioAcademicoId + " no existe");
			}
			String gradoId = generarId(conn);
			if (gradoRepository.existe(conn, gradoId)) {
				throw new ValidationException("Ya existe un grado con el código " + gradoId);
			}
			gradoRepository.insertar(conn, gradoId, nombre.trim(), facultad.trim(), ramaConocimiento.trim(),
					EstadoGrado.VIGENTE.name(), fechaInicio);
			return gradoId;
		});
	}

	/**
	 * Modifica los datos editables del grado (nombre, facultad y rama), en cualquier estado.
	 *
	 * @throws ValidationException Si falta algún dato obligatorio
	 * @throws ResourceNotFoundException Si no existe el grado
	 */
	public void modificarGrado(String gradoId, String nombre, String facultad, String ramaConocimiento) {
		validarObligatorio(gradoId, "Debe indicarse el grado a modificar");
		validarObligatorio(nombre, "El nombre del grado es obligatorio");
		validarObligatorio(facultad, "La facultad del grado es obligatoria");
		validarObligatorio(ramaConocimiento, "La rama de conocimiento del grado es obligatoria");

		db.enTransaccion(conn -> {
			int filas = gradoRepository.actualizar(conn, gradoId, nombre.trim(), facultad.trim(),
					ramaConocimiento.trim());
			if (filas == 0) {
				throw new ResourceNotFoundException("Grado", gradoId);
			}
			return null;
		});
	}

	/**
	 * Comprueba que el grado se puede eliminar en el año académico indicado.
	 *
	 * @throws ValidationException Si el grado ya está en extinción o alguno de sus cursos tiene matrículas en el año
	 */
	public void validarEliminacion(String gradoId, String anioAcademicoId) {
		db.enTransaccion(conn -> {
			comprobarEliminable(conn, gradoId, anioAcademicoId);
			return null;
		});
	}

	/**
	 * Elimina lógicamente el grado: pasa a EN_EXTINCION y todos sus cursos a INACTIVO.
	 *
	 * @throws ValidationException Si el grado no se puede eliminar (ver validarEliminacion)
	 */
	public void eliminarGrado(String gradoId, String anioAcademicoId) {
		db.enTransaccion(conn -> {
			comprobarEliminable(conn, gradoId, anioAcademicoId);
			gradoRepository.actualizarEstado(conn, gradoId, EstadoGrado.EN_EXTINCION.name());
			gradoRepository.actualizarEstadoCursos(conn, gradoId, EstadoCurso.INACTIVO.name());
			return null;
		});
	}

	private void comprobarEliminable(Connection conn, String gradoId, String anioAcademicoId) throws SQLException {
		validarObligatorio(anioAcademicoId, "No hay un año académico actual configurado");
		validarObligatorio(gradoId, "Debe indicarse el grado a eliminar");

		String estado = gradoRepository.findEstado(conn, gradoId);
		if (estado == null) {
			throw new ResourceNotFoundException("Grado", gradoId);
		}
		if (EstadoGrado.fromString(estado) == EstadoGrado.EN_EXTINCION) {
			throw new ValidationException("El grado " + gradoId + " ya está en extinción");
		}

		if (gradoRepository.contarMatriculasActivas(conn, gradoId, anioAcademicoId) > 0) {
			throw new ValidationException("No se puede eliminar el grado " + gradoId
					+ ": tiene cursos con matrículas en el año académico " + anioAcademicoId);
		}
	}

	private String generarId(Connection conn) throws SQLException {
		List<String> ids = gradoRepository.findIdsNumerados(conn);
		int max = 0;
		for (String id : ids) {
			if (id.matches("^G\\d+$")) {
				max = Math.max(max, Integer.parseInt(id.substring(1)));
			}
		}
		return String.format("G%03d", max + 1);
	}

	private void validarObligatorio(String valor, String mensaje) {
		if (valor == null || valor.trim().isEmpty()) {
			throw new ValidationException(mensaje);
		}
	}

}

