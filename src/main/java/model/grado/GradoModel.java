package model.grado;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import model.curso.EstadoCurso;
import repository.AnoAcademicoRepository;
import repository.GradoRepository;
import repository.MatriculaRepository;
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
	/** Motivo con el que se cancelan las matrículas del año de un grado que se extingue */
	private static final String MOTIVO_EXTINCION_GRADO = "EXTINCION_GRADO";

	private final Database db;
	private final GradoRepository gradoRepository;
	private final AnoAcademicoRepository anoAcademicoRepository;
	private final MatriculaRepository matriculaRepository;

	public GradoModel(Database db) {
		this.db = db;
		this.gradoRepository = new GradoRepository(db);
		this.anoAcademicoRepository = new AnoAcademicoRepository(db);
		this.matriculaRepository = new MatriculaRepository(db);
	}

	/**
	 * Obtiene todos los grados para la tabla de AdminOfertaView.
	 */
	public List<GradoDisplayDTO> getTodosLosGrados() {
		return gradoRepository.findAll();
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
	 * Elimina lógicamente el grado: pasa a EXTINTO y todos sus cursos a INACTIVO.
	 *
	 * @throws ValidationException Si el grado no se puede eliminar (ver validarEliminacion)
	 */
	public void eliminarGrado(String gradoId, String anioAcademicoId) {
		db.enTransaccion(conn -> {
			comprobarEliminable(conn, gradoId, anioAcademicoId);
			gradoRepository.actualizarEstado(conn, gradoId, EstadoGrado.EXTINTO.name());
			gradoRepository.actualizarEstadoCursos(conn, gradoId, EstadoCurso.INACTIVO.name());
			return null;
		});
	}

	/**
	 * Datos del grado para la pantalla de revisión (HU 36455). La decisión es el resultado de su dictamen para el año
	 * actual; si no lo tiene y está PENDIENTE_RENOVACION, DESFAVORABLE (lo que pasará si nadie decide); si no, nula.
	 *
	 * @throws ResourceNotFoundException Si no existe el grado
	 */
	public GradoRevisionDisplayDTO getGradoRevision(String gradoId) {
		GradoRevisionDisplayDTO grado = gradoRepository.findRevision(gradoId, anoAcademicoRepository.findActualId());
		if (grado == null) {
			throw new ResourceNotFoundException("Grado", gradoId);
		}
		if (grado.getDecision() == null && EstadoGrado.PENDIENTE_RENOVACION.name().equals(grado.getEstado())) {
			grado.setDecision(ResultadoRevision.DESFAVORABLE.name());
		}
		return grado;
	}

	/**
	 * Indica si hoy se puede emitir o cambiar el dictamen del grado: dentro de la ventana entre el fin del año anterior
	 * y el inicio del año actual, y con el grado PENDIENTE_RENOVACION (con o sin dictamen favorable) o EN_EXTINCION.
	 * No modifica nada.
	 *
	 * @param fechaHoy Fecha de referencia en formato ISO (AAAA-MM-DD)
	 */
	public boolean admiteDictamen(String gradoId, String fechaHoy) {
		return db.enTransaccion(conn -> {
			String estado = gradoRepository.findEstado(conn, gradoId);
			String anioNuevo = anioEnVentana(conn, fechaHoy);
			return estado != null && anioNuevo != null && estadoAdmiteDictamen(estado);
		});
	}

	/**
	 * Emite el dictamen del revisor externo para el año que va a empezar, o lo sobrescribe si ya lo había emitido en
	 * esta ventana. FAVORABLE: el grado queda PENDIENTE_RENOVACION hasta que empiece el año, y entonces pasa a VIGENTE
	 * (extinguirGradosAlIniciarAnio). DESFAVORABLE: pasa a EN_EXTINCION (se extingue al empezar el año).
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
			String estado = gradoRepository.findEstado(conn, gradoId);
			if (estado == null) {
				throw new ResourceNotFoundException("Grado", gradoId);
			}
			String anioNuevo = anioEnVentana(conn, fechaHoy);
			if (anioNuevo == null) {
				throw new ValidationException(
						"El dictamen solo se puede emitir entre el fin de un año académico y el inicio del siguiente");
			}
			if (!estadoAdmiteDictamen(estado)) {
				throw new ValidationException("El grado " + gradoId + " no está pendiente de renovación ni en extinción");
			}

			if (gradoRepository.findResultadoRevision(conn, gradoId, anioNuevo) == null) {
				gradoRepository.insertarRevision(conn, gradoId, anioNuevo, dictamen.name(), fechaHoy.trim());
			} else {
				gradoRepository.actualizarRevision(conn, gradoId, anioNuevo, dictamen.name(), fechaHoy.trim());
			}
			gradoRepository.actualizarEstado(conn, gradoId, dictamen == ResultadoRevision.FAVORABLE
					? EstadoGrado.PENDIENTE_RENOVACION.name() : EstadoGrado.EN_EXTINCION.name());
			return null;
		});
	}

	/**
	 * Al iniciar el año académico actual (hoy igual o posterior a su fecha de inicio), primero los grados
	 * PENDIENTE_RENOVACION con dictamen FAVORABLE para ese año pasan a VIGENTE, con la vigencia desde su inicio. Después
	 * pasan a EXTINTO, con sus cursos a INACTIVO:
	 * - los grados EN_EXTINCION (dictamen desfavorable) y PENDIENTE_RENOVACION (sin dictamen favorable)
	 * - los grados VIGENTES (también los recién renovados) que empezaron el año sin ninguna matrícula con alguna
	 *   asignatura pagada
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
			String anio = anoAcademicoRepository.findActualId(conn);
			if (anio == null) {
				return 0;
			}
			String inicio = gradoRepository.findFechaInicioAnio(conn, anio);
			if (fechaHoy.trim().compareTo(inicio) < 0) {
				return 0;
			}
			gradoRepository.renovarPendientesConDictamen(conn, anio, ResultadoRevision.FAVORABLE.name(),
					EstadoGrado.PENDIENTE_RENOVACION.name(), EstadoGrado.VIGENTE.name(), inicio);
			List<String> grados = new ArrayList<>(gradoRepository.findIdsPorEstados(conn,
					Arrays.asList(EstadoGrado.EN_EXTINCION.name(), EstadoGrado.PENDIENTE_RENOVACION.name())));
			grados.addAll(gradoRepository.findIdsVigentesSinMatriculaPagada(conn, anio, inicio));

			for (String gradoId : grados) {
				gradoRepository.actualizarEstado(conn, gradoId, EstadoGrado.EXTINTO.name());
				gradoRepository.actualizarEstadoCursos(conn, gradoId, EstadoCurso.INACTIVO.name());
				for (String matriculaId : matriculaRepository.findIdsAbiertasDeGrado(conn, gradoId, anio)) {
					matriculaRepository.cancelarCursosAbiertos(conn, matriculaId, MOTIVO_EXTINCION_GRADO, inicio);
					matriculaRepository.cancelarMatricula(conn, matriculaId, MOTIVO_EXTINCION_GRADO, inicio);
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
		String anioNuevo = anoAcademicoRepository.findActualId(conn);
		if (anioNuevo == null || fechaHoy == null) {
			return null;
		}
		String hoy = fechaHoy.trim();
		String inicio = gradoRepository.findFechaInicioAnio(conn, anioNuevo);
		String finAnterior = anoAcademicoRepository.findFechaFinAnterior(conn, inicio);
		boolean enVentana = hoy.compareTo(inicio) < 0 && (finAnterior == null || hoy.compareTo(finAnterior) > 0);
		return enVentana ? anioNuevo : null;
	}

	/** Solo PENDIENTE_RENOVACION (con o sin dictamen favorable) y EN_EXTINCION (desfavorable) admiten dictamen */
	private boolean estadoAdmiteDictamen(String estado) {
		EstadoGrado estadoGrado = EstadoGrado.fromString(estado);
		return estadoGrado == EstadoGrado.PENDIENTE_RENOVACION || estadoGrado == EstadoGrado.EN_EXTINCION;
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

		String estado = gradoRepository.findEstado(conn, gradoId);
		if (estado == null) {
			throw new ResourceNotFoundException("Grado", gradoId);
		}
		if (EstadoGrado.fromString(estado) == EstadoGrado.EN_EXTINCION) {
			throw new ValidationException("El grado " + gradoId + " ya está en extinción: se extinguirá al empezar el año");
		}
		if (EstadoGrado.fromString(estado) == EstadoGrado.EXTINTO) {
			throw new ValidationException("El grado " + gradoId + " ya está extinto");
		}

		if (gradoRepository.contarMatriculasActivas(conn, gradoId, anioAcademicoId) > 0) {
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
		String fechaInicio = gradoRepository.findFechaInicioAnio(conn, anioAcademicoId);
		if (fechaInicio == null) {
			throw new ValidationException("El año académico " + anioAcademicoId + " no existe");
		}
		String inicioMatriculacion = gradoRepository.findInicioMatriculacionAnio(conn, anioAcademicoId);
		if (inicioMatriculacion == null) {
			throw new ValidationException("El año académico " + anioAcademicoId
					+ " no tiene periodo de matriculación configurado");
		}
		if (fechaHoy.trim().compareTo(inicioMatriculacion) >= 0) {
			throw new ValidationException("Solo se pueden crear grados antes de que empiece la matriculación del año académico "
					+ anioAcademicoId + " (" + inicioMatriculacion + ")");
		}
		return fechaInicio;
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

