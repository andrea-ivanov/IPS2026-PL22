package model.propuesta;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import model.grado.GradoDisplayDTO;
import util.Database;
import util.UnexpectedException;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Lógica de negocio de las propuestas de asignaturas de un profesor (HU 35243)
 * y de la revisión de propuestas por parte de Secretaría.
 * - El profesor solo propone en los grados en los que imparte docencia, y solo si están VIGENTES
 * - Si elige varios grados se crea una propuesta por grado; si alguna no es válida no se guarda ninguna
 * - Una propuesta válida se guarda como PENDIENTE con la fecha actual
 * - El nombre no puede repetirse en el grado (ni como asignatura ni como propuesta no rechazada)
 * - Secretaría puede revisar las propuestas pendientes para aceptarlas (registrándolas en Curso y GradoCurso)
 *   o rechazarlas (guardando obligatoriamente el motivo).
 */
public class PropuestaModel {
	public static final String GRADO_VIGENTE = "VIGENTE";
	public static final int MAX_NOMBRE = 150;
	public static final int MAX_DESCRIPCION = 1000;
	// PropuestaCurso.creditos es DECIMAL(4,1)
	private static final BigDecimal MAX_CREDITOS = new BigDecimal("999.9");
	private static final String PREFIJO_ID = "PR";

	private Database db;

	public PropuestaModel(Database db) {
		this.db = db;
	}

	// =========================================================================
	// Consulta
	// =========================================================================

	/**
	 * Grados en los que el profesor imparte docencia (algún grupo de una asignatura del grado), con su estado.
	 * Salen también los no vigentes para poder avisar de por qué no se admite la propuesta.
	 */
	public List<GradoDisplayDTO> getGradosProfesor(String profesorId) {
		return db.executeQueryPojo(GradoDisplayDTO.class,
				"SELECT DISTINCT g.gradoId AS id, g.nombre, g.facultad, g.ramaConocimiento AS rama, g.estado "
						+ "FROM Imparte i "
						+ "JOIN GradoCurso gc ON gc.cursoId = i.cursoId "
						+ "JOIN Grado g ON g.gradoId = gc.gradoId "
						+ "WHERE i.profesorId = ? "
						+ "ORDER BY g.nombre",
				profesorId);
	}

	/**
	 * Propuestas del profesor ("Mis propuestas"), de la más reciente a la más antigua.
	 */
	public List<PropuestaDisplayDTO> getPropuestas(String profesorId) {
		return db.executeQueryPojo(PropuestaDisplayDTO.class,
				"SELECT p.propuestaId, p.gradoId, g.nombre AS grado, p.nombre, p.anio, CAST(p.creditos AS REAL) AS creditos, "
						+ "p.descripcion, p.fechaPropuesta, p.estado "
						+ "FROM PropuestaCurso p "
						+ "JOIN Grado g ON g.gradoId = p.gradoId "
						+ "WHERE p.profesorId = ? "
						+ "ORDER BY p.fechaPropuesta DESC, p.propuestaId DESC",
				profesorId);
	}

	/**
	 * Propuestas pendientes de revisión para Secretaría, visualizando los datos clave
	 * (nombre del curso, profesor proponente, créditos y periodo de impartición / año).
	 */
	public List<PropuestaDisplayDTO> getPropuestasPendientes() {
		return db.executeQueryPojo(PropuestaDisplayDTO.class,
				"SELECT p.propuestaId, p.gradoId, g.nombre AS grado, p.nombre, p.profesorId, "
						+ "(pr.nombre || ' ' || pr.apellidos) AS profesorNombre, "
						+ "p.anio, CAST(p.creditos AS REAL) AS creditos, p.descripcion, p.fechaPropuesta, p.estado "
						+ "FROM PropuestaCurso p "
						+ "JOIN Profesor pr ON pr.profesorId = p.profesorId "
						+ "JOIN Grado g ON g.gradoId = p.gradoId "
						+ "WHERE p.estado = ? "
						+ "ORDER BY p.fechaPropuesta ASC, p.propuestaId ASC",
				EstadoPropuesta.PENDIENTE.name());
	}

	// =========================================================================
	// Alta y Gestión de Propuestas
	// =========================================================================

	/**
	 * Crea una propuesta PENDIENTE por cada grado elegido, con la fecha fechaHoy.
	 * Se valida todo antes de guardar y se guarda en una única transacción: o todas o ninguna.
	 * @return los identificadores de las propuestas creadas, en el orden de los grados
	 * @throws ValidationException si algún dato o algún grado no es válido
	 */
	public List<String> crearPropuestas(String profesorId, List<String> gradoIds, String nombre, String anio,
			String creditos, String descripcion, String fechaHoy) {
		validarPropuestas(profesorId, gradoIds, nombre, anio, creditos, descripcion);
		String nombreLimpio = nombre.trim();
		String descripcionLimpia = descripcion == null || descripcion.trim().isEmpty() ? null : descripcion.trim();
		int valorAnio = parsearAnio(anio);
		double valorCreditos = parsearCreditos(creditos);
		// el mismo grado elegido dos veces cuenta una sola vez
		Set<String> grados = new LinkedHashSet<>(gradoIds);

		List<String> creadas = new ArrayList<>();
		enTransaccion(conn -> {
			int siguiente = siguienteNumero(conn);
			int filas = 0;
			for (String gradoId : grados) {
				String propuestaId = String.format("%s%03d", PREFIJO_ID, siguiente++);
				filas += update(conn, "INSERT INTO PropuestaCurso (propuestaId, profesorId, gradoId, nombre, anio, creditos, "
						+ "descripcion, fechaPropuesta, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
						propuestaId, profesorId, gradoId, nombreLimpio, valorAnio, valorCreditos, descripcionLimpia, fechaHoy,
						EstadoPropuesta.PENDIENTE.name());
				creadas.add(propuestaId);
			}
			return filas;
		});
		return creadas;
	}

	/**
	 * Acepta una propuesta pendiente:
	 * - Cambia su estado internamente a ACEPTADA.
	 * - Registra oficialmente el curso en Curso y lo asocia en GradoCurso con estado ACTIVO.
	 * Todo se realiza en una única transacción atómica.
	 *
	 * @param propuestaId Identificador de la propuesta
	 * @return Identificador del curso creado (e.g. C001)
	 */
	public String aceptarPropuesta(String propuestaId) {
		validarObligatorio(propuestaId, "El identificador de la propuesta es obligatorio");
		return enTransaccion(conn -> {
			Map<String, Object> propuesta = new QueryRunner().query(conn,
					"SELECT propuestaId, profesorId, gradoId, nombre, anio, CAST(creditos AS REAL) AS creditos, estado "
							+ "FROM PropuestaCurso WHERE propuestaId = ?",
					new MapHandler(), propuestaId.trim());
			if (propuesta == null || propuesta.isEmpty()) {
				throw new ResourceNotFoundException("Propuesta", propuestaId);
			}

			String estado = (String) propuesta.get("estado");
			if (!EstadoPropuesta.PENDIENTE.name().equalsIgnoreCase(estado)) {
				throw new ValidationException("Solo se pueden aceptar propuestas en estado PENDIENTE (estado actual: " + estado + ")");
			}

			String nombre = (String) propuesta.get("nombre");
			String gradoId = (String) propuesta.get("gradoId");
			String profesorId = (String) propuesta.get("profesorId");
			Number anioNum = (Number) propuesta.get("anio");
			int anio = anioNum != null ? anioNum.intValue() : 1;
			Number creditosNum = (Number) propuesta.get("creditos");
			double creditos = creditosNum != null ? creditosNum.doubleValue() : 6.0;

			// Generar nuevo código de curso
			String nuevoCursoId = siguienteCursoId(conn);

			// 1. Insertar el curso oficial coordinado por el profesor proponente
			update(conn, "INSERT INTO Curso (cursoId, coordinadorId, nombre, creditos) VALUES (?, ?, ?, ?)",
					nuevoCursoId, profesorId, nombre, creditos);

			// 2. Asociar a GradoCurso en estado ACTIVO
			update(conn, "INSERT INTO GradoCurso (gradoId, cursoId, anio, itinerario, estado) VALUES (?, ?, ?, NULL, 'ACTIVO')",
					gradoId, nuevoCursoId, anio);

			// 3. Actualizar estado de la propuesta a ACEPTADA
			update(conn, "UPDATE PropuestaCurso SET estado = ? WHERE propuestaId = ?",
					EstadoPropuesta.ACEPTADA.name(), propuestaId.trim());

			return nuevoCursoId;
		});
	}

	/**
	 * Rechaza una propuesta pendiente:
	 * - Exige un motivo de rechazo obligatorio.
	 * - Cambia su estado internamente a RECHAZADA.
	 * - Guarda el motivo de rechazo asociado en la descripción.
	 *
	 * @param propuestaId Identificador de la propuesta
	 * @param motivoRechazo Motivo obligatorio del rechazo
	 */
	public void rechazarPropuesta(String propuestaId, String motivoRechazo) {
		validarObligatorio(propuestaId, "El identificador de la propuesta es obligatorio");
		validarObligatorio(motivoRechazo, "El motivo del rechazo es obligatorio");
		String motivoLimpio = motivoRechazo.trim();
		if (motivoLimpio.isEmpty()) {
			throw new ValidationException("El motivo del rechazo es obligatorio");
		}
		if (motivoLimpio.length() > MAX_DESCRIPCION) {
			throw new ValidationException("El motivo del rechazo no puede superar los " + MAX_DESCRIPCION + " caracteres");
		}

		enTransaccion(conn -> {
			Map<String, Object> propuesta = new QueryRunner().query(conn,
					"SELECT propuestaId, estado, descripcion FROM PropuestaCurso WHERE propuestaId = ?",
					new MapHandler(), propuestaId.trim());
			if (propuesta == null || propuesta.isEmpty()) {
				throw new ResourceNotFoundException("Propuesta", propuestaId);
			}

			String estado = (String) propuesta.get("estado");
			if (!EstadoPropuesta.PENDIENTE.name().equalsIgnoreCase(estado)) {
				throw new ValidationException("Solo se pueden rechazar propuestas en estado PENDIENTE (estado actual: " + estado + ")");
			}

			String descActual = (String) propuesta.get("descripcion");
			String nuevaDesc = (descActual != null && !descActual.trim().isEmpty())
					? descActual.trim() + " | Motivo rechazo: " + motivoLimpio
					: "Motivo rechazo: " + motivoLimpio;
			if (nuevaDesc.length() > MAX_DESCRIPCION) {
				nuevaDesc = nuevaDesc.substring(0, MAX_DESCRIPCION);
			}

			update(conn, "UPDATE PropuestaCurso SET estado = ?, descripcion = ? WHERE propuestaId = ?",
					EstadoPropuesta.RECHAZADA.name(), nuevaDesc, propuestaId.trim());

			return 1;
		});
	}

	/**
	 * Comprueba los datos de una propuesta sin guardar nada (el controlador la usa antes de pedir confirmación):
	 * campos obligatorios, año entre 1 y 6, créditos mayores que 0, y que cada grado sea del profesor,
	 * esté vigente y no tenga ya una asignatura o propuesta con ese nombre.
	 * @throws ValidationException con el primer problema encontrado
	 */
	public void validarPropuestas(String profesorId, List<String> gradoIds, String nombre, String anio,
			String creditos, String descripcion) {
		validarObligatorio(nombre, "El nombre de la asignatura es obligatorio");
		validarObligatorio(anio, "El año es obligatorio");
		validarObligatorio(creditos, "Los créditos son obligatorios");
		if (gradoIds == null || gradoIds.isEmpty()) {
			throw new ValidationException("Selecciona al menos un grado");
		}
		String nombreLimpio = nombre.trim();
		if (nombreLimpio.length() > MAX_NOMBRE) {
			throw new ValidationException("El nombre no puede superar los " + MAX_NOMBRE + " caracteres");
		}
		String descripcionLimpia = descripcion == null || descripcion.trim().isEmpty() ? null : descripcion.trim();
		if (descripcionLimpia != null && descripcionLimpia.length() > MAX_DESCRIPCION) {
			throw new ValidationException("La descripción no puede superar los " + MAX_DESCRIPCION + " caracteres");
		}
		parsearAnio(anio);
		parsearCreditos(creditos);

		Map<String, GradoDisplayDTO> gradosProfesor = getGradosProfesor(profesorId).stream()
				.collect(Collectors.toMap(GradoDisplayDTO::getId, Function.identity()));
		for (String gradoId : new LinkedHashSet<>(gradoIds)) {
			GradoDisplayDTO grado = gradosProfesor.get(gradoId);
			if (grado == null) {
				throw new ValidationException("No impartes docencia en el grado " + gradoId);
			}
			if (!GRADO_VIGENTE.equals(grado.getEstado())) {
				throw new ValidationException("El grado " + grado.getNombre() + " no está vigente (" + grado.getEstado()
						+ "): no admite nuevas propuestas");
			}
			if (existeNombreEnGrado(gradoId, nombreLimpio)) {
				throw new ValidationException("Ya existe una asignatura o propuesta llamada '" + nombreLimpio
						+ "' en el grado " + grado.getNombre());
			}
		}
	}

	// =========================================================================
	// Auxiliares
	// =========================================================================

	/** Genera el siguiente ID de curso: C001, C002... */
	private String siguienteCursoId(Connection conn) throws SQLException {
		List<String> ids = new QueryRunner().query(conn, "SELECT cursoId FROM Curso WHERE cursoId GLOB 'C[0-9]*'",
				new ColumnListHandler<String>());
		int max = 0;
		for (String id : ids) {
			if (id != null && id.matches("^C\\d+$")) {
				max = Math.max(max, Integer.parseInt(id.substring(1)));
			}
		}
		return String.format("C%03d", max + 1);
	}

	/** Nombre repetido en el grado, sin distinguir mayúsculas: como asignatura del grado o como propuesta no rechazada */
	private boolean existeNombreEnGrado(String gradoId, String nombre) {
		return !db.executeQueryArray("SELECT 1 FROM GradoCurso gc JOIN Curso c ON c.cursoId = gc.cursoId "
				+ "WHERE gc.gradoId = ? AND LOWER(TRIM(c.nombre)) = LOWER(?) "
				+ "UNION ALL "
				+ "SELECT 1 FROM PropuestaCurso p WHERE p.gradoId = ? AND LOWER(TRIM(p.nombre)) = LOWER(?) AND p.estado <> ?",
				gradoId, nombre, gradoId, nombre, EstadoPropuesta.RECHAZADA.name()).isEmpty();
	}

	/** Número de la siguiente propuesta: PR031 si la última es PR030 */
	private int siguienteNumero(Connection conn) throws SQLException {
		Number max = new QueryRunner().query(conn, "SELECT MAX(CAST(SUBSTR(propuestaId, 3) AS INTEGER)) "
				+ "FROM PropuestaCurso WHERE propuestaId LIKE 'PR%'", new ScalarHandler<Number>());
		return max == null ? 1 : max.intValue() + 1;
	}

	private int parsearAnio(String anio) {
		int valor;
		try {
			valor = Integer.parseInt(anio.trim());
		} catch (NumberFormatException e) {
			throw new ValidationException("El año debe ser un número entero: '" + anio.trim() + "'");
		}
		if (valor < 1 || valor > 6) {
			throw new ValidationException("El año debe estar entre 1 y 6");
		}
		return valor;
	}

	/** Admite coma o punto decimal y como mucho un decimal (la columna es DECIMAL(4,1)) */
	private double parsearCreditos(String creditos) {
		BigDecimal valor;
		try {
			valor = new BigDecimal(creditos.trim().replace(',', '.'));
		} catch (NumberFormatException e) {
			throw new ValidationException("Los créditos deben ser un número: '" + creditos.trim() + "'");
		}
		if (valor.signum() <= 0) {
			throw new ValidationException("Los créditos deben ser mayores que 0");
		}
		if (valor.compareTo(MAX_CREDITOS) > 0 || valor.stripTrailingZeros().scale() > 1) {
			throw new ValidationException("Los créditos deben ser menores que 1000 y tener como mucho un decimal");
		}
		return valor.doubleValue();
	}

	private void validarObligatorio(String valor, String mensaje) {
		if (valor == null || valor.trim().isEmpty()) {
			throw new ValidationException(mensaje);
		}
	}

	private int update(Connection conn, String sql, Object... params) throws SQLException {
		return new QueryRunner().update(conn, sql, params);
	}

	@FunctionalInterface
	private interface OperacionBD<T> {
		T ejecutar(Connection conn) throws SQLException;
	}

	/** Ejecuta la operación en una única transacción: o se aplican todos los cambios o ninguno. */
	private <T> T enTransaccion(OperacionBD<T> operacion) {
		Connection conn = null;
		try {
			conn = db.getConnection();
			conn.setAutoCommit(false);
			T result = operacion.ejecutar(conn);
			conn.commit();
			return result;
		} catch (SQLException e) {
			DbUtils.rollbackQuietly(conn);
			throw new UnexpectedException(e);
		} finally {
			DbUtils.closeQuietly(conn);
		}
	}
}
