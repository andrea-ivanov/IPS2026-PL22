package model.matriculacion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import model.alumno.Alumno;
import model.curso.Curso;
import util.Database;
import util.Util;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Modelo de negocio y caso de uso para la gestión y búsqueda de Matriculaciones.
 * Integra la resolución de Alumno, asignaturas/cursos matriculados y el mapeo tipado
 * de fechas, enums (EstadoMatricula) y booleanos.
 */
public class MatriculacionModel {
	private Database db;

	public MatriculacionModel() {
		this.db = new Database();
	}

	public MatriculacionModel(Database db) {
		this.db = db;
	}

	/**
	 * Busca una Matriculación por su ID en formato String tras validar formato y nulidad.
	 *
	 * @param id Identificador en formato String
	 * @return Objeto Matriculacion completamente poblado
	 * @throws ValidationException Si el id es nulo, vacío o con formato inválido
	 * @throws ResourceNotFoundException Si no existe en la base de datos
	 */
	public Matriculacion buscarPorId(String id) {
		validarFormatoId(id);
		long idNumerico = Long.parseLong(id.trim());
		return buscarPorId(idNumerico);
	}

	/**
	 * Busca una Matriculación por su ID numérico.
	 *
	 * @param id Identificador numérico Long
	 * @return Objeto Matriculacion completamente poblado
	 * @throws ValidationException Si el id es nulo
	 * @throws ResourceNotFoundException Si no existe en la base de datos
	 */
	public Matriculacion buscarPorId(Long id) {
		if (id == null) {
			throw new ValidationException("El ID de matrícula no puede ser nulo");
		}

		String sqlMatricula = "SELECT id_matricula, id_alumno, id_grado, estado, fecha, id_ano_academico FROM Matricula WHERE id_matricula = ?";
		List<Object[]> rowsMatricula = db.executeQueryArray(sqlMatricula, id);
		if (rowsMatricula == null || rowsMatricula.isEmpty()) {
			throw new ResourceNotFoundException("Matriculacion", id);
		}

		Object[] rowMatricula = rowsMatricula.get(0);
		Long idMatricula = ((Number) rowMatricula[0]).longValue();
		Long idAlumno = rowMatricula[1] != null ? ((Number) rowMatricula[1]).longValue() : null;
		Long idGrado = rowMatricula[2] != null ? ((Number) rowMatricula[2]).longValue() : null;
		String estadoStr = (String) rowMatricula[3];
		Object fechaObj = rowMatricula[4];
		Long idAnoAcademico = rowMatricula.length > 5 && rowMatricula[5] != null ? ((Number) rowMatricula[5]).longValue() : null;

		// 1. Mapeo de datos: Enum
		EstadoMatricula estado = EstadoMatricula.fromString(estadoStr);

		// 2. Mapeo de datos: Fecha
		Date fecha = mapearFecha(fechaObj);

		// 3. Mapeo de datos: Booleano
		boolean pagada = EstadoMatricula.ACTIVA.equals(estado);

		// 4. Mapeo relacional: Alumno
		Alumno alumno = null;
		if (idAlumno != null) {
			String sqlAlumno = "SELECT id_alumno, nombre, email FROM Alumno WHERE id_alumno = ?";
			List<Object[]> rowsAlumno = db.executeQueryArray(sqlAlumno, idAlumno);
			if (rowsAlumno != null && !rowsAlumno.isEmpty()) {
				Object[] rA = rowsAlumno.get(0);
				alumno = Alumno.builder()
						.id(((Number) rA[0]).longValue())
						.nombre((String) rA[1])
						.email((String) rA[2])
						.build();
			}
		}

		// 5. Mapeo relacional: Cursos asociados al grado matriculado
		List<Curso> cursos = new ArrayList<>();
		if (idGrado != null) {
			String sqlCursos = "SELECT id_curso, nombre, id_grado, id_coordinador FROM Curso WHERE id_grado = ? ORDER BY id_curso";
			List<Object[]> rowsCursos = db.executeQueryArray(sqlCursos, idGrado);
			if (rowsCursos != null) {
				for (Object[] rC : rowsCursos) {
					cursos.add(Curso.builder()
							.id(((Number) rC[0]).longValue())
							.nombre((String) rC[1])
							.idGrado(rC[2] != null ? ((Number) rC[2]).longValue() : null)
							.idCoordinador(rC[3] != null ? ((Number) rC[3]).longValue() : null)
							.creditos(6)
							.activo(true)
							.build());
				}
			}
		}

		return Matriculacion.builder()
				.id(idMatricula)
				.alumno(alumno)
				.idGrado(idGrado)
				.idAnoAcademico(idAnoAcademico)
				.cursos(cursos)
				.fecha(fecha)
				.estado(estado)
				.pagada(pagada)
				.build();
	}

	private Date mapearFecha(Object fechaObj) {
		if (fechaObj == null) {
			return null;
		}
		if (fechaObj instanceof Date) {
			return (Date) fechaObj;
		}
		if (fechaObj instanceof java.sql.Date) {
			return new Date(((java.sql.Date) fechaObj).getTime());
		}
		if (fechaObj instanceof String) {
			return Util.isoStringToDate((String) fechaObj);
		}
		throw new ValidationException("Tipo de fecha incompatible recibido desde base de datos: " + fechaObj.getClass());
	}

	private void validarFormatoId(String id) {
		if (id == null) {
			throw new ValidationException("El ID de matrícula no puede ser nulo");
		}
		String trimmed = id.trim();
		if (trimmed.isEmpty()) {
			throw new ValidationException("El ID de matrícula no puede estar vacío");
		}
		if (!trimmed.matches("^\\d+$")) {
			throw new ValidationException("El formato del ID de matrícula es inválido: '" + id + "'. Debe ser un número entero positivo.");
		}
	}
}

