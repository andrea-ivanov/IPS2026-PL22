package model.curso;

import static util.DbUtil.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.grado.EstadoGrado;
import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Modelo de negocio y caso de uso para la gestión y búsqueda de Cursos / Asignaturas.
 */
public class CursoModel {
	private Database db;

	public CursoModel() {
		this.db = new Database();
	}

	public CursoModel(Database db) {
		this.db = db;
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
			String estadoGrado = new QueryRunner().query(conn, "SELECT estado FROM Grado WHERE gradoId = ?",
					new ScalarHandler<String>(), gradoId);
			if (estadoGrado == null) {
				throw new ResourceNotFoundException("Grado", gradoId);
			}
			if (!EstadoGrado.VIGENTE.name().equals(estadoGrado)) {
				throw new ValidationException("Solo se pueden añadir cursos a grados vigentes");
			}
			String cursoId = generarId(conn);
			Number existentes = new QueryRunner().query(conn, "SELECT COUNT(*) FROM Curso WHERE cursoId = ?",
					new ScalarHandler<Number>(), cursoId);
			if (existentes.intValue() > 0) {
				throw new ValidationException("Ya existe un curso con el código " + cursoId);
			}
			update(conn, "INSERT INTO Curso (cursoId, coordinadorId, nombre, creditos) VALUES (?, ?, ?, ?)",
					cursoId, coordinadorId.trim(), nombre.trim(), valorCreditos);
			update(conn, "INSERT INTO GradoCurso (gradoId, cursoId, anio, itinerario, estado) VALUES (?, ?, ?, ?, ?)",
					gradoId, cursoId, valorAnio, itinerario.trim(), EstadoCurso.ACTIVO.name());
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
			int filas = update(conn, "UPDATE GradoCurso SET anio = ?, itinerario = ? WHERE gradoId = ? AND cursoId = ?",
					valorAnio, itinerario.trim(), gradoId, cursoId);
			if (filas == 0) {
				throw new ResourceNotFoundException("Curso", cursoId);
			}
			update(conn, "UPDATE Curso SET creditos = ? WHERE cursoId = ?", valorCreditos, cursoId);
			return null;
		});
	}

	/**
	 * Obtiene los datos actuales del curso en el grado, sin transformar, para precargar su modificación.
	 *
	 * @throws ResourceNotFoundException Si el curso no pertenece a ese grado
	 */
	public CursoEdicionDTO buscarDatosEdicion(String gradoId, String cursoId) {
		List<CursoEdicionDTO> datos = db.executeQueryPojo(CursoEdicionDTO.class,
				"SELECT gc.anio, gc.itinerario, c.creditos, c.nombre, c.coordinadorId "
						+ "FROM GradoCurso gc JOIN Curso c ON c.cursoId = gc.cursoId WHERE gc.gradoId = ? AND gc.cursoId = ?",
				gradoId, cursoId);
		if (datos.isEmpty()) {
			throw new ResourceNotFoundException("Curso", cursoId);
		}
		return datos.get(0);
	}

	/**
	 * Cuenta en cuántos grados está el curso.
	 */
	public int contarGradosDelCurso(String cursoId) {
		return ((Number) db.executeQueryArray("SELECT COUNT(*) FROM GradoCurso WHERE cursoId = ?", cursoId).get(0)[0])
				.intValue();
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
			update(conn, "UPDATE GradoCurso SET estado = ? WHERE gradoId = ? AND cursoId = ?",
					EstadoCurso.INACTIVO.name(), gradoId, cursoId);
			return null;
		});
	}

	private void comprobarEliminable(Connection conn, String gradoId, String cursoId, String anioAcademicoId)
			throws SQLException {
		validarObligatorio(anioAcademicoId, "No hay un año académico actual configurado");
		validarObligatorio(gradoId, "Debe indicarse el grado del curso");
		validarObligatorio(cursoId, "Debe indicarse el curso a eliminar");

		String estado = new QueryRunner().query(conn,
				"SELECT estado FROM GradoCurso WHERE gradoId = ? AND cursoId = ?",
				new ScalarHandler<String>(), gradoId, cursoId);
		if (estado == null) {
			throw new ResourceNotFoundException("Curso", cursoId);
		}
		if (EstadoCurso.fromString(estado) == EstadoCurso.INACTIVO) {
			throw new ValidationException("El curso " + cursoId + " ya está inactivo");
		}

		Number matriculas = new QueryRunner().query(conn,
				"SELECT COUNT(*) FROM MatriculaCurso mc JOIN Matricula m ON m.matriculaId = mc.matriculaId "
						+ "WHERE mc.gradoId = ? AND mc.cursoId = ? AND m.anioAcademicoId = ? AND mc.estado <> 'CANCELADA'",
				new ScalarHandler<Number>(), gradoId, cursoId, anioAcademicoId);
		if (matriculas.intValue() > 0) {
			throw new ValidationException("No se puede eliminar el curso " + cursoId
					+ ": tiene matrículas en el año académico " + anioAcademicoId);
		}
	}

	private double parsearCreditos(String creditos) {
		double valor;
		try {
			valor = Double.parseDouble(creditos.trim());
		} catch (NumberFormatException e) {
			throw new ValidationException("Los créditos deben ser un número: '" + creditos + "'");
		}
		if (valor <= 0) {
			throw new ValidationException("Los créditos deben ser mayores que 0");
		}
		return valor;
	}

	private int parsearAnio(String anio) {
		int valor;
		try {
			valor = Integer.parseInt(anio.trim());
		} catch (NumberFormatException e) {
			throw new ValidationException("El año debe ser un número entero: '" + anio + "'");
		}
		if (valor < 1 || valor > 6) {
			throw new ValidationException("El año debe estar entre 1 y 6");
		}
		return valor;
	}

	private String generarId(Connection conn) throws SQLException {
		List<String> ids = new QueryRunner().query(conn, "SELECT cursoId FROM Curso WHERE cursoId GLOB 'C[0-9]*'",
				new ColumnListHandler<String>());
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

