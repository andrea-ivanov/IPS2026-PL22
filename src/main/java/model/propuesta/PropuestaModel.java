package model.propuesta;

import static util.Util.parsearAnio;
import static util.Util.parsearCreditos;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import model.grado.GradoDisplayDTO;
import repository.PropuestaRepository;
import util.Database;
import util.exception.ValidationException;

/**
 * Lógica de negocio de las propuestas de asignaturas de un profesor (HU 35243).
 * - El profesor solo propone en los grados en los que imparte docencia, y solo si están VIGENTES
 * - Si elige varios grados se crea una propuesta por grado; si alguna no es válida no se guarda ninguna
 * - Una propuesta válida se guarda como PENDIENTE con la fecha actual
 * - El nombre no puede repetirse en el grado (ni como asignatura ni como propuesta no rechazada)
 */
public class PropuestaModel {
	public static final String GRADO_VIGENTE = "VIGENTE";
	public static final int MAX_NOMBRE = 150;
	public static final int MAX_DESCRIPCION = 1000;
	// PropuestaCurso.creditos es DECIMAL(4,1)
	private static final String PREFIJO_ID = "PR";

	private final Database db;
	private final PropuestaRepository propuestaRepository;

	public PropuestaModel(Database db) {
		this.db = db;
		this.propuestaRepository = new PropuestaRepository(db);
	}

	// =========================================================================
	// Consulta
	// =========================================================================

	/**
	 * Grados en los que el profesor imparte docencia (algún grupo de una asignatura del grado), con su estado.
	 * Salen también los no vigentes para poder avisar de por qué no se admite la propuesta.
	 */
	public List<GradoDisplayDTO> getGradosProfesor(String profesorId) {
		return propuestaRepository.findGradosProfesor(profesorId);
	}

	/**
	 * Propuestas del profesor ("Mis propuestas"), de la más reciente a la más antigua.
	 */
	public List<PropuestaDisplayDTO> getPropuestas(String profesorId) {
		return propuestaRepository.findPropuestas(profesorId);
	}

	// =========================================================================
	// Alta
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
		db.enTransaccion(conn -> {
			int siguiente = siguienteNumero(conn);
			int filas = 0;
			for (String gradoId : grados) {
				String propuestaId = String.format("%s%03d", PREFIJO_ID, siguiente++);
				filas += propuestaRepository.insertar(conn, propuestaId, profesorId, gradoId, nombreLimpio, valorAnio,
						valorCreditos, descripcionLimpia, fechaHoy, EstadoPropuesta.PENDIENTE.name());
				creadas.add(propuestaId);
			}
			return filas;
		});
		return creadas;
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
			if (propuestaRepository.existeNombreEnGrado(gradoId, nombreLimpio)) {
				throw new ValidationException("Ya existe una asignatura o propuesta llamada '" + nombreLimpio
						+ "' en el grado " + grado.getNombre());
			}
		}
	}

	// =========================================================================
	// Auxiliares
	// =========================================================================

	/** Número de la siguiente propuesta: PR031 si la última es PR030 */
	private int siguienteNumero(Connection conn) throws SQLException {
		Integer max = propuestaRepository.findMaxNumero(conn);
		return max == null ? 1 : max + 1;
	}

	private void validarObligatorio(String valor, String mensaje) {
		if (valor == null || valor.trim().isEmpty()) {
			throw new ValidationException(mensaje);
		}
	}

}
