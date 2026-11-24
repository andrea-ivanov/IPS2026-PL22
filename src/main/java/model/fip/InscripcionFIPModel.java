package model.fip;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;

import model.contrato.ContratoAlumnoModel;
import repository.AlumnoRepository;
import repository.InscripcionFipRepository;
import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Matrícula presencial de profesores y alumnos en una FIP por el secretario (HU 37046).
 * Reglas de negocio:
 * - Los usuarios no se automatriculan: la inscripción la hace solo el secretario buscando al usuario.
 * - Un profesor de la plantilla puede matricularse directamente.
 * - Los formadores externos no pueden matricularse: solo imparten FIP.
 * - El profesor responsable de una FIP no puede matricularse como asistente en ella (no se puede ser docente y alumno en el mismo curso).
 * - Un alumno solo puede matricularse si tiene un contrato registrado vigente en la fecha de inicio de la FIP.
 * - Solo se puede matricular hasta el día anterior al inicio de la FIP; desde el día de inicio no se admiten nuevas altas.
 * - La matrícula se abre el día en que se crea la FIP (Fip.fechaCreacion).
 * - No se puede matricular dos veces al mismo usuario en la misma FIP.
 * - La matrícula en una FIP es siempre gratuita: no hay pago.
 * - Al matricular, la inscripción queda con resultado PENDIENTE hasta que la califique el profesor responsable.
 * - Las matrículas no se pueden anular: la plaza asignada queda ocupada.
 * - Si la FIP tiene un máximo de plazas (Fip.plazas), se bloquea la matrícula al llenarse.
 */
public class InscripcionFIPModel {
	public static final double PRECIO_MATRICULA = 0.0;
	private static final String PREFIJO_ID = "IF";

	private final ContratoAlumnoModel contratoModel;
	private final Database db;
	private final InscripcionFipRepository repository;
	private final AlumnoRepository alumnoRepository;

	public InscripcionFIPModel(Database db) {
		this.contratoModel = new ContratoAlumnoModel(db);
		this.db = db;
		this.repository = new InscripcionFipRepository(db);
		this.alumnoRepository = new AlumnoRepository(db);
	}

	/** Solo para comprobar las reglas sin base de datos (no se puede guardar ni consultar) */
	public InscripcionFIPModel(ContratoAlumnoModel contratoModel) {
		this.contratoModel = contratoModel;
		this.db = null;
		this.repository = null;
		this.alumnoRepository = null;
	}

	// =========================================================================
	// Consulta y guardado (tablas Fip e InscripcionFip)
	// =========================================================================

	/** Todas las FIP con sus plazas ocupadas, de la que empieza más tarde a la que empezó antes */
	public List<FIPDTO> getFips() {
		return repositorio().findFips();
	}

	/** Inscritos en la FIP, con su nombre, tipo y resultado */
	public List<InscritoFipDTO> getInscritos(String fipId) {
		validarObligatorio(fipId, "Seleccione una FIP");
		return repositorio().findInscritos(fipId.trim());
	}

	/**
	 * Matricula al alumno o profesor en la FIP con fecha de inscripción fechaHoy y resultado PENDIENTE,
	 * comprobando todas las reglas de validarMatricula. Mientras la tabla Fip no tenga la columna plazas no hay máximo
	 * (plazasMaximas = 0).
	 * @return identificador de la inscripción (IF00001, IF00002...)
	 * @throws ResourceNotFoundException si no existe la FIP o el usuario
	 * @throws ValidationException si se incumple alguna regla
	 */
	public String matricular(String fipId, String usuarioId, TipoUsuarioFIP tipoUsuario, String fechaHoy) {
		validarObligatorio(fipId, "Seleccione una FIP");
		validarObligatorio(usuarioId, "Seleccione el usuario que se va a matricular");
		comprobarNoEsFormadorExterno(tipoUsuario);
		String fip = fipId.trim();
		String usuario = usuarioId.trim();
		comprobarUsuarioExiste(usuario, tipoUsuario);
		String alumnoId = tipoUsuario == TipoUsuarioFIP.ALUMNO ? usuario : null;
		String profesorId = tipoUsuario == TipoUsuarioFIP.PROFESOR ? usuario : null;
		return db.enTransaccion(conn -> {
			FIPDTO datos = repository.findFip(conn, fip);
			if (datos == null) {
				throw new ResourceNotFoundException("FIP", fip);
			}
			comprobarMatriculaAbierta(datos.getFechaCreacion(), fechaHoy);
			validarMatricula(fip, datos.getFechaInicio(), fechaHoy, usuario, tipoUsuario,
					datos.getProfesorResponsableId(), datos.getPlazasOcupadas(), datos.getPlazasMaximas(),
					repository.existeInscripcion(conn, fip, alumnoId, profesorId));
			String inscripcionId = String.format("%s%05d", PREFIJO_ID, repository.siguienteNumero(conn));
			repository.insertar(conn, inscripcionId, fip, alumnoId, profesorId, parsearFecha(fechaHoy, "hoy").toString(),
					ResultadoInscripcionFIP.PENDIENTE.name());
			return inscripcionId;
		});
	}

	private void comprobarUsuarioExiste(String usuarioId, TipoUsuarioFIP tipoUsuario) {
		boolean existe = tipoUsuario == TipoUsuarioFIP.ALUMNO ? alumnoRepositorio().existeAlumnoId(usuarioId)
				: repositorio().existeProfesor(usuarioId);
		if (!existe) {
			throw new ResourceNotFoundException(tipoUsuario == TipoUsuarioFIP.ALUMNO ? "Alumno" : "Profesor", usuarioId);
		}
	}

	private InscripcionFipRepository repositorio() {
		if (repository == null) {
			throw new IllegalStateException("Este InscripcionFIPModel se creó sin base de datos");
		}
		return repository;
	}

	private AlumnoRepository alumnoRepositorio() {
		repositorio();
		return alumnoRepository;
	}

	// =========================================================================
	// Reglas de la matrícula (no usan la base de datos, salvo el contrato del alumno)
	// =========================================================================

	/**
	 * La matrícula se abre el día en que el administrador crea la FIP.
	 * @throws ValidationException si hoy es anterior a la creación de la FIP o las fechas no son válidas
	 */
	public void comprobarMatriculaAbierta(String fechaCreacionFip, String fechaHoy) {
		LocalDate creacion = parsearFecha(fechaCreacionFip, "creación de la FIP");
		if (parsearFecha(fechaHoy, "hoy").isBefore(creacion)) {
			throw new ValidationException("La matrícula de la FIP no se abre hasta el " + creacion);
		}
	}

	/**
	 * Comprueba que todavía se puede matricular: hoy tiene que ser anterior al inicio de la FIP
	 * (desde el día de inicio no se admiten nuevas altas).
	 * @throws ValidationException si el plazo ha terminado o las fechas no son válidas
	 */
	public void comprobarPlazo(String fechaInicioFip, String fechaHoy) {
		LocalDate inicio = parsearFecha(fechaInicioFip, "inicio de la FIP");
		LocalDate hoy = parsearFecha(fechaHoy, "hoy");
		if (!hoy.isBefore(inicio)) {
			throw new ValidationException("El plazo de matrícula terminó el día anterior al inicio de la FIP ("
					+ inicio.minusDays(1) + ")");
		}
	}

	/**
	 * Comprueba que el usuario no sea un formador externo (los formadores externos no pueden
	 * matricularse: solo imparten FIP).
	 * @throws ValidationException si es formador externo o nulo
	 */
	public void comprobarNoEsFormadorExterno(TipoUsuarioFIP tipoUsuario) {
		if (tipoUsuario == null) {
			throw new ValidationException("Debe especificarse el tipo de usuario");
		}
		if (tipoUsuario == TipoUsuarioFIP.FORMADOR_EXTERNO) {
			throw new ValidationException("Los formadores externos no pueden matricularse: solo imparten FIP");
		}
	}

	/**
	 * El profesor responsable de una FIP no puede matricularse como asistente en ella
	 * (no se puede ser docente y alumno en el mismo curso).
	 * @throws ValidationException si el profesor a matricular es el responsable de la FIP
	 */
	public void comprobarProfesorNoEsResponsable(String profesorId, String responsableFipId) {
		if (profesorId == null || profesorId.trim().isEmpty()) {
			throw new ValidationException("Debe indicarse el identificador del profesor");
		}
		if (responsableFipId != null && profesorId.trim().equalsIgnoreCase(responsableFipId.trim())) {
			throw new ValidationException(
					"El profesor responsable de una FIP no puede matricularse como asistente en ella (no se puede ser docente y alumno en el mismo curso)");
		}
	}

	/**
	 * Comprueba que la FIP disponga de plazas libres. Se bloquea la matrícula al llenarse.
	 * @throws ValidationException si no quedan plazas
	 */
	public void comprobarPlazasDisponibles(int plazasOcupadas, int plazasMaximas) {
		if (plazasMaximas > 0 && plazasOcupadas >= plazasMaximas) {
			throw new ValidationException("No quedan plazas disponibles en la FIP (máximo " + plazasMaximas + " plazas cubiertas)");
		}
	}

	/**
	 * Comprueba que el usuario no esté matriculado previamente en la misma FIP.
	 * @throws ValidationException si ya está matriculado
	 */
	public void comprobarNoDuplicada(String usuarioId, Collection<String> usuariosYaInscritos) {
		if (usuarioId == null || usuarioId.trim().isEmpty()) {
			throw new ValidationException("Debe indicarse el usuario");
		}
		if (usuariosYaInscritos != null && usuariosYaInscritos.contains(usuarioId.trim())) {
			throw new ValidationException("No se puede matricular dos veces al mismo usuario en la misma FIP ("
					+ usuarioId.trim() + ")");
		}
	}

	/**
	 * Comprueba que el alumno tiene un contrato vigente en la fecha de inicio de la FIP.
	 * @throws ValidationException si no lo tiene
	 */
	public void comprobarContratoAlumno(String alumnoId, String fechaInicioFip) {
		if (alumnoId == null || alumnoId.trim().isEmpty()) {
			throw new ValidationException("Debe indicarse el alumno");
		}
		if (contratoModel == null) {
			throw new ValidationException("No se ha configurado el modelo de contratos para comprobar los requisitos del alumno");
		}
		String inicio = parsearFecha(fechaInicioFip, "inicio de la FIP").toString();
		if (!contratoModel.tieneContratoVigente(alumnoId, inicio)) {
			throw new ValidationException("El alumno " + alumnoId.trim() + " no tiene ningún contrato vigente el "
					+ inicio + " (fecha de inicio de la FIP)");
		}
	}

	/**
	 * Valida de forma completa los requisitos para matricular a un usuario en una FIP (HU 37046).
	 * @param fipId identificador de la FIP
	 * @param fechaInicioFip fecha de inicio de la FIP (AAAA-MM-DD)
	 * @param fechaHoy fecha actual de la operación (AAAA-MM-DD)
	 * @param usuarioId identificador del usuario (alumno o profesor)
	 * @param tipoUsuario rol del usuario (ALUMNO, PROFESOR o FORMADOR_EXTERNO)
	 * @param responsableFipId identificador del profesor responsable de la FIP
	 * @param plazasOcupadas plazas cubiertas actualmente
	 * @param plazasMaximas plazas máximas autorizadas
	 * @param yaInscrito si el usuario ya cuenta con inscripción activa en la FIP
	 * @throws ValidationException si se incumple alguna regla de negocio
	 */
	public void validarMatricula(String fipId, String fechaInicioFip, String fechaHoy, String usuarioId,
			TipoUsuarioFIP tipoUsuario, String responsableFipId, int plazasOcupadas, int plazasMaximas,
			boolean yaInscrito) {
		validarObligatorio(fipId, "Debe indicarse la FIP");
		validarObligatorio(usuarioId, "Debe indicarse el usuario a matricular");
		comprobarPlazo(fechaInicioFip, fechaHoy);
		comprobarNoEsFormadorExterno(tipoUsuario);

		if (yaInscrito) {
			throw new ValidationException("No se puede matricular dos veces al mismo usuario en la misma FIP ("
					+ usuarioId.trim() + ")");
		}

		comprobarPlazasDisponibles(plazasOcupadas, plazasMaximas);

		if (tipoUsuario == TipoUsuarioFIP.PROFESOR) {
			comprobarProfesorNoEsResponsable(usuarioId, responsableFipId);
		} else if (tipoUsuario == TipoUsuarioFIP.ALUMNO) {
			comprobarContratoAlumno(usuarioId, fechaInicioFip);
		}
	}

	/**
	 * Crea el DTO de inscripción presencial con resultado PENDIENTE y coste 0.0 (gratuita).
	 */
	public InscripcionFIPDTO crearInscripcion(String inscripcionId, String fipId, String usuarioId,
			TipoUsuarioFIP tipoUsuario, String fechaInscripcion) {
		validarObligatorio(inscripcionId, "Falta el identificador de inscripción");
		validarObligatorio(fipId, "Falta el identificador de la FIP");
		validarObligatorio(usuarioId, "Falta el identificador del usuario");
		comprobarNoEsFormadorExterno(tipoUsuario);
		parsearFecha(fechaInscripcion, "inscripción");

		InscripcionFIPDTO dto = new InscripcionFIPDTO();
		dto.setInscripcionId(inscripcionId.trim());
		dto.setFipId(fipId.trim());
		dto.setUsuarioId(usuarioId.trim());
		dto.setTipoUsuario(tipoUsuario);
		dto.setFechaInscripcion(fechaInscripcion.trim());
		dto.setResultado(ResultadoInscripcionFIP.PENDIENTE);
		dto.setPrecio(PRECIO_MATRICULA);
		return dto;
	}

	private LocalDate parsearFecha(String fecha, String cual) {
		validarObligatorio(fecha, "Falta la fecha de " + cual);
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
