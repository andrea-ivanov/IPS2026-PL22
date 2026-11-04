package model.grado;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.curso.EstadoCurso;
import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Modelo de negocio y caso de uso para la gestión y búsqueda de Grados.
 * Maneja validaciones de entrada, consultas a base de datos y mapeo a entidades de dominio.
 */
public class GradoModel {
	private Database db;

	public GradoModel() {
		this.db = new Database();
	}

	public GradoModel(Database db) {
		this.db = db;
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
			Object fechaInicio = new QueryRunner().query(conn,
					"SELECT fechaInicio FROM AnioAcademico WHERE anioAcademicoId = ?",
					new ScalarHandler<Object>(), anioAcademicoId.trim());
			if (fechaInicio == null) {
				throw new ValidationException("El año académico " + anioAcademicoId + " no existe");
			}
			String gradoId = generarId(conn);
			Number existentes = new QueryRunner().query(conn, "SELECT COUNT(*) FROM Grado WHERE gradoId = ?",
					new ScalarHandler<Number>(), gradoId);
			if (existentes.intValue() > 0) {
				throw new ValidationException("Ya existe un grado con el código " + gradoId);
			}
			update(conn, "INSERT INTO Grado (gradoId, nombre, facultad, ramaConocimiento, estado, fechaInicioVigencia) "
					+ "VALUES (?, ?, ?, ?, ?, ?)",
					gradoId, nombre.trim(), facultad.trim(), ramaConocimiento.trim(), EstadoGrado.VIGENTE.name(),
					fechaInicio.toString());
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
			int filas = update(conn, "UPDATE Grado SET nombre = ?, facultad = ?, ramaConocimiento = ? WHERE gradoId = ?",
					nombre.trim(), facultad.trim(), ramaConocimiento.trim(), gradoId);
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
			update(conn, "UPDATE Grado SET estado = ? WHERE gradoId = ?", EstadoGrado.EN_EXTINCION.name(), gradoId);
			update(conn, "UPDATE GradoCurso SET estado = ? WHERE gradoId = ?", EstadoCurso.INACTIVO.name(), gradoId);
			return null;
		});
	}

	private void comprobarEliminable(Connection conn, String gradoId, String anioAcademicoId) throws SQLException {
		validarObligatorio(anioAcademicoId, "No hay un año académico actual configurado");
		validarObligatorio(gradoId, "Debe indicarse el grado a eliminar");

		String estado = new QueryRunner().query(conn, "SELECT estado FROM Grado WHERE gradoId = ?",
				new ScalarHandler<String>(), gradoId);
		if (estado == null) {
			throw new ResourceNotFoundException("Grado", gradoId);
		}
		if (EstadoGrado.fromString(estado) == EstadoGrado.EN_EXTINCION) {
			throw new ValidationException("El grado " + gradoId + " ya está en extinción");
		}

		Number matriculas = new QueryRunner().query(conn,
				"SELECT COUNT(*) FROM MatriculaCurso mc JOIN Matricula m ON m.matriculaId = mc.matriculaId "
						+ "WHERE mc.gradoId = ? AND m.anioAcademicoId = ? AND mc.estado <> 'CANCELADA'",
				new ScalarHandler<Number>(), gradoId, anioAcademicoId);
		if (matriculas.intValue() > 0) {
			throw new ValidationException("No se puede eliminar el grado " + gradoId
					+ ": tiene cursos con matrículas en el año académico " + anioAcademicoId);
		}
	}

	private String generarId(Connection conn) throws SQLException {
		List<String> ids = new QueryRunner().query(conn, "SELECT gradoId FROM Grado WHERE gradoId GLOB 'G[0-9]*'",
				new ColumnListHandler<String>());
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

