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
	/** Años académicos de vigencia de un grado desde su fechaInicioVigencia, pasados los cuales debe renovarse */
	public static final int ANIOS_VIGENCIA = 4;
	/** Motivo con el que se cancelan las matrículas del año de un grado que se extingue */
	private static final String MOTIVO_EXTINCION_GRADO = "EXTINCION_GRADO";

	private Database db;

	public GradoModel() {
		this.db = new Database();
	}

	public GradoModel(Database db) {
		this.db = db;
	}

	/**
	 * Comprueba que se puede crear un grado en el año académico indicado, sin modificar nada: solo antes de que
	 * empiece su periodo de matriculación.
	 *
	 * @param fechaHoy Fecha de referencia en formato ISO (AAAA-MM-DD)
	 * @throws ValidationException Si el año no existe, no tiene periodo de matriculación o ya ha empezado
	 */
	public void validarCreacionGrado(String anioAcademicoId, String fechaHoy) {
		validarObligatorio(anioAcademicoId, "No hay un año académico actual configurado");
		db.enTransaccion(conn -> {
			comprobarAnioAdmiteGrados(conn, anioAcademicoId.trim(), fechaHoy);
			return null;
		});
	}

	/**
	 * Crea un grado VIGENTE cuya vigencia empieza con el año académico indicado. Solo se puede crear antes de que
	 * empiece el periodo de matriculación de ese año.
	 *
	 * @param fechaHoy Fecha de referencia en formato ISO (AAAA-MM-DD)
	 * @return Identificador generado (G001, G002...)
	 * @throws ValidationException Si falta algún dato obligatorio, el año académico no existe o ya ha empezado
	 *         su periodo de matriculación
	 */
	public String crearGrado(String nombre, String facultad, String ramaConocimiento, String anioAcademicoId,
			String fechaHoy) {
		// TODO: validar aqui y en modificarGrado que la rama es una de las de ck_grado_rama (schema.sql) y lanzar
		// ValidationException con un mensaje claro; poner la lista en un enum RamaConocimiento (valor de BD sin tilde
		// y texto con tilde para la vista), reutilizable para ck_microcredencial_rama
		validarObligatorio(nombre, "El nombre del grado es obligatorio");
		validarObligatorio(facultad, "La facultad del grado es obligatoria");
		validarObligatorio(ramaConocimiento, "La rama de conocimiento del grado es obligatoria");
		validarObligatorio(anioAcademicoId, "No hay un año académico actual configurado");

		return db.enTransaccion(conn -> {
			String fechaInicio = comprobarAnioAdmiteGrados(conn, anioAcademicoId.trim(), fechaHoy);
			String gradoId = generarId(conn);
			Number existentes = new QueryRunner().query(conn, "SELECT COUNT(*) FROM Grado WHERE gradoId = ?",
					new ScalarHandler<Number>(), gradoId);
			if (existentes.intValue() > 0) {
				throw new ValidationException("Ya existe un grado con el código " + gradoId);
			}
			update(conn, "INSERT INTO Grado (gradoId, nombre, facultad, ramaConocimiento, estado, fechaInicioVigencia) "
					+ "VALUES (?, ?, ?, ?, ?, ?)",
					gradoId, nombre.trim(), facultad.trim(), ramaConocimiento.trim(), EstadoGrado.VIGENTE.name(),
					fechaInicio);
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
	 * @throws ValidationException Si el grado ya está en extinción o extinto, o alguno de sus cursos tiene matrículas
	 *         en el año
	 */
	public void validarEliminacion(String gradoId, String anioAcademicoId) {
		db.enTransaccion(conn -> {
			comprobarEliminable(conn, gradoId, anioAcademicoId);
			return null;
		});
	}

	/**
	 * Elimina lógicamente el grado: pasa a EXTINTO y todos sus cursos a INACTIVO.
	 *
	 * @throws ValidationException Si el grado no se puede eliminar (ver validarEliminacion)
	 */
	public void eliminarGrado(String gradoId, String anioAcademicoId) {
		db.enTransaccion(conn -> {
			comprobarEliminable(conn, gradoId, anioAcademicoId);
			update(conn, "UPDATE Grado SET estado = ? WHERE gradoId = ?", EstadoGrado.EXTINTO.name(), gradoId);
			update(conn, "UPDATE GradoCurso SET estado = ? WHERE gradoId = ?", EstadoCurso.INACTIVO.name(), gradoId);
			return null;
		});
	}

	/**
	 * Datos del grado para la pantalla de revisión (HU 36455). La decisión es el resultado de su dictamen para el año
	 * actual; si no lo tiene y está PENDIENTE_RENOVACION, DESFAVORABLE (lo que pasará si nadie decide); si no, nula.
	 * La fecha de la última revisión es la del dictamen de su año más reciente.
	 *
	 * @throws ResourceNotFoundException Si no existe el grado
	 */
	public GradoRevisionDisplayDTO getGradoRevision(String gradoId) {
		List<Object[]> actual = db.executeQueryArray(
				"SELECT anioAcademicoId FROM AnioAcademico ORDER BY fechaInicio DESC LIMIT 1");
		String anioActual = actual.isEmpty() ? null : (String) actual.get(0)[0];
		List<GradoRevisionDisplayDTO> filas = db.executeQueryPojo(GradoRevisionDisplayDTO.class,
				"SELECT g.gradoId AS idGrado, g.nombre, g.facultad, g.ramaConocimiento AS rama, g.estado, "
						+ "(SELECT r.resultado FROM RevisionGrado r WHERE r.gradoId = g.gradoId "
						+ "AND r.anioAcademicoId = ?) AS decision, "
						+ "(SELECT r.fechaRevision FROM RevisionGrado r WHERE r.gradoId = g.gradoId "
						+ "ORDER BY r.anioAcademicoId DESC LIMIT 1) AS fechaUltimaRevision "
						+ "FROM Grado g WHERE g.gradoId = ?",
				anioActual, gradoId);
		if (filas.isEmpty()) {
			throw new ResourceNotFoundException("Grado", gradoId);
		}
		GradoRevisionDisplayDTO grado = filas.get(0);
		if (grado.getDecision() == null && EstadoGrado.PENDIENTE_RENOVACION.name().equals(grado.getEstado())) {
			grado.setDecision(ResultadoRevision.DESFAVORABLE.name());
		}
		return grado;
	}

	/**
	 * Indica si hoy se puede emitir o cambiar el dictamen del grado: dentro de la ventana entre el fin del año anterior
	 * y el inicio del año actual, y con el grado PENDIENTE_RENOVACION, EN_EXTINCION o VIGENTE renovado en esta ventana.
	 * No modifica nada.
	 *
	 * @param fechaHoy Fecha de referencia en formato ISO (AAAA-MM-DD)
	 */
	public boolean admiteDictamen(String gradoId, String fechaHoy) {
		return db.enTransaccion(conn -> {
			String estado = findEstado(conn, gradoId);
			String anioNuevo = anioEnVentana(conn, fechaHoy);
			return estado != null && anioNuevo != null && estadoAdmiteDictamen(conn, gradoId, estado, anioNuevo);
		});
	}

	/**
	 * Emite el dictamen del revisor externo para el año que va a empezar, o lo sobrescribe si ya lo había emitido en
	 * esta ventana. FAVORABLE: el grado pasa a VIGENTE con la vigencia desde el inicio de ese año. DESFAVORABLE: pasa
	 * a EN_EXTINCION (se extingue al empezar el año).
	 *
	 * @param fechaHoy Fecha de referencia en formato ISO (AAAA-MM-DD); es la fecha que se registra
	 * @throws ValidationException Si el resultado no es válido, se está fuera de la ventana o el estado no lo admite
	 * @throws ResourceNotFoundException Si no existe el grado
	 */
	public void emitirDictamen(String gradoId, String resultado, String fechaHoy) {
		ResultadoRevision dictamen = parsearResultado(resultado);
		validarObligatorio(gradoId, "Debe indicarse el grado");
		validarObligatorio(fechaHoy, "Debe indicarse la fecha de hoy");

		db.enTransaccion(conn -> {
			String estado = findEstado(conn, gradoId);
			if (estado == null) {
				throw new ResourceNotFoundException("Grado", gradoId);
			}
			String anioNuevo = anioEnVentana(conn, fechaHoy);
			if (anioNuevo == null) {
				throw new ValidationException(
						"El dictamen solo se puede emitir entre el fin de un año académico y el inicio del siguiente");
			}
			if (!estadoAdmiteDictamen(conn, gradoId, estado, anioNuevo)) {
				throw new ValidationException("El grado " + gradoId
						+ " no está pendiente de renovación ni tiene un dictamen de este periodo que se pueda cambiar");
			}

			if (findResultadoRevision(conn, gradoId, anioNuevo) == null) {
				update(conn, "INSERT INTO RevisionGrado (gradoId, anioAcademicoId, resultado, fechaRevision) "
						+ "VALUES (?, ?, ?, ?)", gradoId, anioNuevo, dictamen.name(), fechaHoy.trim());
			} else {
				update(conn, "UPDATE RevisionGrado SET resultado = ?, fechaRevision = ? "
						+ "WHERE gradoId = ? AND anioAcademicoId = ?", dictamen.name(), fechaHoy.trim(), gradoId, anioNuevo);
			}
			if (dictamen == ResultadoRevision.FAVORABLE) {
				update(conn, "UPDATE Grado SET estado = ?, fechaInicioVigencia = ? WHERE gradoId = ?",
						EstadoGrado.VIGENTE.name(), findFechaInicioAnio(conn, anioNuevo), gradoId);
			} else {
				update(conn, "UPDATE Grado SET estado = ? WHERE gradoId = ?", EstadoGrado.EN_EXTINCION.name(), gradoId);
			}
			return null;
		});
	}

	/**
	 * Al iniciar el año académico actual (hoy igual o posterior a su fecha de inicio) pasan a EXTINTO, con sus cursos
	 * a INACTIVO:
	 * - los grados EN_EXTINCION (dictamen desfavorable) y PENDIENTE_RENOVACION (sin dictamen)
	 * - los grados VIGENTES que empezaron el año sin ninguna matrícula con alguna asignatura pagada (pago validado y
	 *   no cancelada antes de esa fecha)
	 * Las matrículas abiertas de ese año de los grados extinguidos se cancelan, con todas sus asignaturas abiertas,
	 * con motivo EXTINCION_GRADO y fecha de inicio del año. Se ejecuta en cada arranque: una segunda ejecución no
	 * encuentra nada que extinguir.
	 *
	 * @param fechaHoy Fecha de referencia en formato ISO (AAAA-MM-DD)
	 * @return Número de grados extinguidos
	 */
	public int extinguirGradosAlIniciarAnio(String fechaHoy) {
		validarObligatorio(fechaHoy, "Debe indicarse la fecha de hoy");
		return db.enTransaccion(conn -> {
			String anio = findAnioActualId(conn);
			if (anio == null) {
				return 0;
			}
			String inicio = findFechaInicioAnio(conn, anio);
			if (fechaHoy.trim().compareTo(inicio) < 0) {
				return 0;
			}
			List<String> grados = new QueryRunner().query(conn,
					"SELECT g.gradoId FROM Grado g WHERE g.estado IN (?, ?) "
							+ "OR (g.estado = ? AND NOT EXISTS (SELECT 1 FROM MatriculaCurso mc "
							+ "JOIN Matricula m ON m.matriculaId = mc.matriculaId "
							+ "WHERE m.gradoId = g.gradoId AND m.anioAcademicoId = ? AND mc.pagoValidado = 1 "
							+ "AND (mc.estado <> 'CANCELADA' OR mc.fechaCancelacion >= ?))) ORDER BY g.gradoId",
					new ColumnListHandler<String>(), EstadoGrado.EN_EXTINCION.name(),
					EstadoGrado.PENDIENTE_RENOVACION.name(), EstadoGrado.VIGENTE.name(), anio, inicio);

			for (String gradoId : grados) {
				update(conn, "UPDATE Grado SET estado = ? WHERE gradoId = ?", EstadoGrado.EXTINTO.name(), gradoId);
				update(conn, "UPDATE GradoCurso SET estado = ? WHERE gradoId = ?", EstadoCurso.INACTIVO.name(), gradoId);
				List<String> matriculas = new QueryRunner().query(conn,
						"SELECT matriculaId FROM Matricula WHERE gradoId = ? AND anioAcademicoId = ? "
								+ "AND estado NOT IN ('CANCELADA', 'FINALIZADA') ORDER BY matriculaId",
						new ColumnListHandler<String>(), gradoId, anio);
				for (String matriculaId : matriculas) {
					update(conn, "UPDATE MatriculaCurso SET estado = 'CANCELADA', motivoCancelacion = ?, "
							+ "fechaCancelacion = ? WHERE matriculaId = ? AND estado NOT IN ('CANCELADA', 'FINALIZADA')",
							MOTIVO_EXTINCION_GRADO, inicio, matriculaId);
					update(conn, "UPDATE Matricula SET estado = 'CANCELADA', motivoCancelacion = ?, fechaCancelacion = ? "
							+ "WHERE matriculaId = ?", MOTIVO_EXTINCION_GRADO, inicio, matriculaId);
				}
			}
			return grados.size();
		});
	}

	/**
	 * Año académico que va a empezar si hoy está dentro de la ventana de dictamen: antes del inicio del año actual y,
	 * si hay año anterior, después de su fin. Nulo si no hay años o hoy está fuera de la ventana.
	 */
	private String anioEnVentana(Connection conn, String fechaHoy) throws SQLException {
		String anioNuevo = findAnioActualId(conn);
		if (anioNuevo == null || fechaHoy == null) {
			return null;
		}
		String hoy = fechaHoy.trim();
		String inicio = findFechaInicioAnio(conn, anioNuevo);
		Object finAnterior = new QueryRunner().query(conn,
				"SELECT fechaFin FROM AnioAcademico WHERE fechaInicio < ? ORDER BY fechaInicio DESC LIMIT 1",
				new ScalarHandler<Object>(), inicio);
		boolean enVentana = hoy.compareTo(inicio) < 0
				&& (finAnterior == null || hoy.compareTo(finAnterior.toString()) > 0);
		return enVentana ? anioNuevo : null;
	}

	/** PENDIENTE_RENOVACION y EN_EXTINCION admiten dictamen; un VIGENTE, solo si ya tiene dictamen del año nuevo */
	private boolean estadoAdmiteDictamen(Connection conn, String gradoId, String estado, String anioNuevo)
			throws SQLException {
		EstadoGrado estadoGrado = EstadoGrado.fromString(estado);
		if (estadoGrado == EstadoGrado.PENDIENTE_RENOVACION || estadoGrado == EstadoGrado.EN_EXTINCION) {
			return true;
		}
		return estadoGrado == EstadoGrado.VIGENTE && findResultadoRevision(conn, gradoId, anioNuevo) != null;
	}

	private ResultadoRevision parsearResultado(String resultado) {
		validarObligatorio(resultado, "Debe indicarse si el dictamen es favorable o desfavorable");
		for (ResultadoRevision valor : ResultadoRevision.values()) {
			if (valor.name().equals(resultado.trim())) {
				return valor;
			}
		}
		throw new ValidationException("Resultado de dictamen no válido: " + resultado);
	}

	private void comprobarEliminable(Connection conn, String gradoId, String anioAcademicoId) throws SQLException {
		validarObligatorio(anioAcademicoId, "No hay un año académico actual configurado");
		validarObligatorio(gradoId, "Debe indicarse el grado a eliminar");

		String estado = findEstado(conn, gradoId);
		if (estado == null) {
			throw new ResourceNotFoundException("Grado", gradoId);
		}
		if (EstadoGrado.fromString(estado) == EstadoGrado.EN_EXTINCION) {
			throw new ValidationException("El grado " + gradoId + " ya está en extinción: se extinguirá al empezar el año");
		}
		if (EstadoGrado.fromString(estado) == EstadoGrado.EXTINTO) {
			throw new ValidationException("El grado " + gradoId + " ya está extinto");
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

	/**
	 * Comprueba que el año existe y que hoy es anterior al inicio de su periodo de matriculación.
	 *
	 * @return Fecha de inicio del año académico
	 */
	private String comprobarAnioAdmiteGrados(Connection conn, String anioAcademicoId, String fechaHoy)
			throws SQLException {
		validarObligatorio(fechaHoy, "Debe indicarse la fecha de hoy");
		String fechaInicio = findFechaInicioAnio(conn, anioAcademicoId);
		if (fechaInicio == null) {
			throw new ValidationException("El año académico " + anioAcademicoId + " no existe");
		}
		Object inicioMatriculacion = new QueryRunner().query(conn,
				"SELECT inicioMatriculacion FROM AnioAcademico WHERE anioAcademicoId = ?",
				new ScalarHandler<Object>(), anioAcademicoId);
		if (inicioMatriculacion == null) {
			throw new ValidationException("El año académico " + anioAcademicoId
					+ " no tiene periodo de matriculación configurado");
		}
		if (fechaHoy.trim().compareTo(inicioMatriculacion.toString()) >= 0) {
			throw new ValidationException("Solo se pueden crear grados antes de que empiece la matriculación del año académico "
					+ anioAcademicoId + " (" + inicioMatriculacion + ")");
		}
		return fechaInicio;
	}

	/** Estado del grado tal y como está en la base de datos. Nulo si no existe */
	private String findEstado(Connection conn, String gradoId) throws SQLException {
		return new QueryRunner().query(conn, "SELECT estado FROM Grado WHERE gradoId = ?",
				new ScalarHandler<String>(), gradoId);
	}

	/** Id del año académico actual (el de mayor fecha de inicio). Nulo si no hay ninguno */
	private String findAnioActualId(Connection conn) throws SQLException {
		return new QueryRunner().query(conn,
				"SELECT anioAcademicoId FROM AnioAcademico ORDER BY fechaInicio DESC LIMIT 1",
				new ScalarHandler<String>());
	}

	/** Fecha de inicio (ISO) del año académico. Nulo si no existe */
	private String findFechaInicioAnio(Connection conn, String anioAcademicoId) throws SQLException {
		Object inicio = new QueryRunner().query(conn,
				"SELECT fechaInicio FROM AnioAcademico WHERE anioAcademicoId = ?",
				new ScalarHandler<Object>(), anioAcademicoId);
		return inicio == null ? null : inicio.toString();
	}

	/** Resultado del dictamen del grado para el año académico; nulo si no tiene */
	private String findResultadoRevision(Connection conn, String gradoId, String anioAcademicoId) throws SQLException {
		return new QueryRunner().query(conn,
				"SELECT resultado FROM RevisionGrado WHERE gradoId = ? AND anioAcademicoId = ?",
				new ScalarHandler<String>(), gradoId, anioAcademicoId);
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
