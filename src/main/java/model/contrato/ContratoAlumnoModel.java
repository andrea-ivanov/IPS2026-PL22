package model.contrato;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import repository.AlumnoRepository;
import repository.ContratoAlumnoRepository;
import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Contratos de los alumnos que registra el secretario (HU 37045).
 * - Un alumno puede tener varios contratos, aunque se solapen, y no hace falta que esté matriculado en ningún grado
 * - Son obligatorias la fecha de inicio, la de fin y la descripción; la fecha de fin no puede ser anterior a la de inicio
 * - Se admiten contratos pasados (retroactivos) y futuros
 * - Solo se dan de alta y se consultan: no se modifican ni se borran
 * El SQL está en ContratoAlumnoRepository.
 */
public class ContratoAlumnoModel {
	// ContratoAlumno.descripcion es VARCHAR(200)
	public static final int MAX_DESCRIPCION = 200;
	private static final String PREFIJO_ID = "CT";

	private final Database db;
	private final ContratoAlumnoRepository repository;
	private final AlumnoRepository alumnoRepository;

	public ContratoAlumnoModel(Database db) {
		this.db = db;
		this.repository = new ContratoAlumnoRepository(db);
		this.alumnoRepository = new AlumnoRepository(db);
	}

	/**
	 * Historial de contratos del alumno, del que empieza más tarde al que empieza antes.
	 * @throws ResourceNotFoundException si el alumno no existe
	 */
	public List<ContratoAlumnoDTO> getContratos(String alumnoId) {
		String id = comprobarAlumno(alumnoId);
		return repository.findByAlumno(id);
	}

	/**
	 * Registra un contrato del alumno.
	 * @param fechaInicio fecha ISO (AAAA-MM-DD)
	 * @param fechaFin fecha ISO (AAAA-MM-DD), no anterior a fechaInicio
	 * @return identificador del contrato creado (CT0001, CT0002...)
	 * @throws ResourceNotFoundException si el alumno no existe
	 * @throws ValidationException si falta algún dato o las fechas no son válidas
	 */
	public String registrarContrato(String alumnoId, String descripcion, String fechaInicio, String fechaFin) {
		String id = comprobarAlumno(alumnoId);
		validarObligatorio(descripcion, "La descripción del contrato es obligatoria");
		String descripcionLimpia = descripcion.trim();
		if (descripcionLimpia.length() > MAX_DESCRIPCION) {
			throw new ValidationException("La descripción no puede superar los " + MAX_DESCRIPCION + " caracteres");
		}
		LocalDate inicio = parsearFecha(fechaInicio, "inicio");
		LocalDate fin = parsearFecha(fechaFin, "fin");
		if (fin.isBefore(inicio)) {
			throw new ValidationException("La fecha de fin no puede ser anterior a la de inicio");
		}
		return db.enTransaccion(conn -> {
			String contratoId = String.format("%s%04d", PREFIJO_ID, repository.siguienteNumero(conn));
			repository.insertar(conn, contratoId, id, descripcionLimpia, inicio.toString(), fin.toString());
			return contratoId;
		});
	}

	/** Si el alumno tiene algún contrato vigente en la fecha (requisito para matricularse en una FIP, HU 37046) */
	public boolean tieneContratoVigente(String alumnoId, String fecha) {
		validarObligatorio(alumnoId, "Debe indicarse el alumno");
		return repository.existeContratoVigente(alumnoId.trim(), parsearFecha(fecha, "referencia").toString());
	}

	private String comprobarAlumno(String alumnoId) {
		validarObligatorio(alumnoId, "Selecciona el alumno");
		String id = alumnoId.trim();
		if (!alumnoRepository.existeAlumnoId(id)) {
			throw new ResourceNotFoundException("Alumno", id);
		}
		return id;
	}

	private LocalDate parsearFecha(String fecha, String cual) {
		validarObligatorio(fecha, "La fecha de " + cual + " es obligatoria");
		try {
			return LocalDate.parse(fecha.trim());
		} catch (DateTimeParseException e) {
			throw new ValidationException("La fecha de " + cual + " no es válida (formato AAAA-MM-DD): '" + fecha.trim() + "'");
		}
	}

	private void validarObligatorio(String valor, String mensaje) {
		if (valor == null || valor.trim().isEmpty()) {
			throw new ValidationException(mensaje);
		}
	}
}
