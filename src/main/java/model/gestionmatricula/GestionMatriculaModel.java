package model.gestionmatricula;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import repository.AlumnoRepository;
import repository.AnoAcademicoRepository;
import repository.GrupoRepository;
import repository.MatriculaRepository;
import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Lógica de negocio de la Secretaría sobre las matrículas.
 * HU 35226: consulta de matriculaciones de cursos, grados y grupos
 * HU 35229: ciclo de vida (aceptar solicitudes, modificar, asignar grupos, cancelar, activar al inicio
 * del año y finalizar al terminarlo)
 * Las operaciones que cambian el estado actualizan Matricula y MatriculaCurso en la misma transacción.
 * El SQL está en MatriculaRepository, GrupoRepository, AlumnoRepository y AnoAcademicoRepository.
 */
public class GestionMatriculaModel {
	public static final List<String> MOTIVOS_CANCELACION = Arrays.asList("IMPAGO", "FUERZA_MAYOR", "INCOMPATIBILIDAD");
	public static final List<String> METODOS_PAGO = Arrays.asList("TARJETA", "TRANSFERENCIA", "CARTA_PAGO");

	private Database db;
	private AnoAcademicoRepository anoAcademicoRepository;
	private MatriculaRepository matriculaRepository;
	private GrupoRepository grupoRepository;
	private AlumnoRepository alumnoRepository;

	public GestionMatriculaModel(Database db) {
		this.db = db;
		this.anoAcademicoRepository = new AnoAcademicoRepository(db);
		this.matriculaRepository = new MatriculaRepository(db);
		this.grupoRepository = new GrupoRepository(db);
		this.alumnoRepository = new AlumnoRepository(db);
	}

	// =========================================================================
	// HU 35226: consulta
	// =========================================================================

	/**
	 * Listado de todos los cursos matriculados, del año académico más reciente al más antiguo.
	 */
	public List<MatriculaSecretariaDTO> getMatriculas() {
		return matriculaRepository.findMatriculasSecretaria();
	}

	/**
	 * Número de cursos matriculados que cumplen los filtros.
	 */
	public int contarMatriculas(FiltroMatriculasSecretaria filtro) {
		return matriculaRepository.contarMatriculasSecretaria(filtro);
	}

	/**
	 * Una página del listado de cursos matriculados que cumplen los filtros, en el mismo orden que getMatriculas.
	 * @param offset filas que se saltan (desde 0)
	 * @param limite número máximo de filas de la página
	 */
	public List<MatriculaSecretariaDTO> getMatriculas(FiltroMatriculasSecretaria filtro, int offset, int limite) {
		return matriculaRepository.findMatriculasSecretaria(filtro, offset, limite);
	}

	/** Años académicos que tienen matrículas, del más reciente al más antiguo. */
	public List<String> getAniosConMatriculas() {
		return matriculaRepository.findAniosConMatriculas();
	}

	/** Nombres de los grados que tienen cursos matriculados. */
	public List<String> getGradosConMatriculas() {
		return matriculaRepository.findGradosConMatriculas();
	}

	/** Nombres de los cursos que tienen alguna matrícula. */
	public List<String> getCursosConMatriculas() {
		return matriculaRepository.findCursosConMatriculas();
	}

	/**
	 * Detalle de un curso matriculado.
	 * @throws ResourceNotFoundException si no existe
	 */
	public MatriculaSecretariaDTO getMatricula(String matriculaId, String cursoId) {
		MatriculaSecretariaDTO result = matriculaRepository.findMatriculaSecretaria(matriculaId, cursoId);
		if (result == null) {
			throw new ResourceNotFoundException("No existe la matrícula " + matriculaId + " en el curso " + cursoId);
		}
		return result;
	}

	/**
	 * Años académicos, del más reciente al más antiguo.
	 */
	public List<String> getAniosAcademicos() {
		return anoAcademicoRepository.findAllIds();
	}

	/**
	 * Año académico en curso: el último que ya ha empezado en la fecha indicada,
	 * o el más antiguo si todavía no ha empezado ninguno. Nulo si no hay años.
	 */
	public String getAnioAcademicoActual(String fechaHoy) {
		return anoAcademicoRepository.findEnCursoId(fechaHoy);
	}

	// =========================================================================
	// HU 35226: consulta de grupos
	// =========================================================================

	/**
	 * Todos los grupos con los grados de su curso, sus profesores y las plazas ocupadas en el año indicado.
	 * Solo ocupan plaza los cursos PAGADOS o ACTIVOS.
	 */
	public List<GrupoConsultaDTO> getGruposConsulta(String anioAcademicoId) {
		return grupoRepository.findGruposConsulta(anioAcademicoId);
	}

	/**
	 * Número de grupos del grado y curso indicados (nulo o vacío para no filtrar).
	 */
	public int contarGruposConsulta(String grado, String curso) {
		return grupoRepository.contarGruposConsulta(grado, curso);
	}

	/**
	 * Una página de los grupos del grado y curso indicados, en el mismo orden que getGruposConsulta.
	 * @param offset filas que se saltan (desde 0)
	 * @param limite número máximo de filas de la página
	 */
	public List<GrupoConsultaDTO> getGruposConsulta(String anioAcademicoId, String grado, String curso, int offset,
			int limite) {
		return grupoRepository.findGruposConsulta(anioAcademicoId, grado, curso, offset, limite);
	}

	/** Nombres de los grados que tienen algún curso con grupos. */
	public List<String> getGradosConGrupos() {
		return grupoRepository.findGradosConGrupos();
	}

	/** Nombres de los cursos que tienen grupos. */
	public List<String> getCursosConGrupos() {
		return grupoRepository.findCursosConGrupos();
	}

	/**
	 * Cursos matriculados asignados a un grupo en un año académico (en cualquier estado).
	 */
	public List<MatriculaSecretariaDTO> getMatriculasGrupo(String cursoId, String grupoCodigo, String anioAcademicoId) {
		return matriculaRepository.findMatriculasGrupo(cursoId, grupoCodigo, anioAcademicoId);
	}

	// =========================================================================
	// HU 35229: ciclo de vida
	// =========================================================================

	/**
	 * Mantiene al día el estado de todas las matrículas según la fecha: activa lo pagado de los años
	 * ya iniciados y finaliza lo activo de los años terminados. Se ejecuta al arrancar la aplicación.
	 * @param fechaHoy fecha de referencia en formato yyyy-MM-dd
	 * @return número de cursos que han cambiado de estado
	 */
	public int actualizarCicloVida(String fechaHoy) {
		return db.enTransaccion(conn -> activarMatriculasIniciadas(conn, fechaHoy) + finalizarMatriculasTerminadas(conn, fechaHoy));
	}

	/**
	 * Al iniciar el año académico los cursos PAGADOS pasan a ACTIVA, y la matrícula
	 * pasa a ACTIVA si tiene algún curso activo. Los SOLICITADOS (sin pagar) no cambian.
	 * @param fechaHoy fecha de referencia en formato yyyy-MM-dd
	 * @return número de cursos activados
	 */
	public int activarMatriculasIniciadas(String fechaHoy) {
		return db.enTransaccion(conn -> activarMatriculasIniciadas(conn, fechaHoy));
	}

	/**
	 * Al terminar el año académico los cursos ACTIVOS y sus matrículas activas pasan a FINALIZADA.
	 * @param fechaHoy fecha de referencia en formato yyyy-MM-dd
	 * @return número de cursos finalizados
	 */
	public int finalizarMatriculasTerminadas(String fechaHoy) {
		return db.enTransaccion(conn -> finalizarMatriculasTerminadas(conn, fechaHoy));
	}

	// =========================================================================
	// HU 35229: grupos
	// =========================================================================

	/**
	 * Grupos del curso con las plazas ocupadas en el año académico de la matrícula.
	 * Solo ocupan plaza los cursos PAGADOS o ACTIVOS.
	 */
	public List<GrupoPlazasDTO> getGrupos(String matriculaId, String cursoId) {
		return grupoRepository.findGruposMatricula(matriculaId, cursoId);
	}

	/**
	 * Asigna (o cambia) los grupos de un curso matriculado: uno de cada tipo que tenga el curso,
	 * solo a cursos PAGADOS o ACTIVOS y sin superar la capacidad del grupo.
	 * @param grupoPorTipo tipo de grupo (TE, PA, PL) → código del grupo elegido
	 */
	public void asignarGrupos(String matriculaId, String cursoId, Map<String, String> grupoPorTipo) {
		MatriculaSecretariaDTO actual = getMatricula(matriculaId, cursoId);
		if (!"PAGADA".equals(actual.getEstado()) && !"ACTIVA".equals(actual.getEstado())) {
			throw new ValidationException("Solo se asignan grupos a cursos PAGADOS o ACTIVOS (estado actual: "
					+ actual.getEstado() + ")");
		}
		List<GrupoPlazasDTO> grupos = getGrupos(matriculaId, cursoId);
		for (GrupoPlazasDTO g : grupos) {
			if (!grupoPorTipo.containsKey(g.getTipo())) {
				throw new ValidationException("Debe asignarse un grupo de cada tipo; falta el tipo " + g.getTipo());
			}
		}
		for (Map.Entry<String, String> e : grupoPorTipo.entrySet()) {
			GrupoPlazasDTO elegido = grupos.stream().filter(g -> g.getGrupoCodigo().equals(e.getValue()))
					.findFirst().orElseThrow(() -> new ValidationException(
							"El grupo " + e.getValue() + " no existe en el curso " + actual.getCurso()));
			if (!elegido.getTipo().equals(e.getKey())) {
				throw new ValidationException("El grupo " + elegido.getGrupoCodigo() + " es de tipo "
						+ elegido.getTipo() + ", no " + e.getKey());
			}
			if (!elegido.estaAsignado() && elegido.getPlazasLibres() <= 0) {
				throw new ValidationException("El grupo " + elegido.getGrupoCodigo() + " está completo ("
						+ elegido.getCapacidad() + " plazas)");
			}
		}

		db.enTransaccion(conn -> {
			for (Map.Entry<String, String> e : grupoPorTipo.entrySet()) {
				grupoRepository.quitarGrupo(conn, matriculaId, cursoId, e.getKey());
				grupoRepository.asignarGrupo(conn, matriculaId, cursoId, e.getKey(), e.getValue());
			}
			return 0;
		});
	}

	/**
	 * Acepta una solicitud pendiente de pago: valida el pago y el curso pasa a PAGADA
	 * (o directamente a ACTIVA si el año académico ya ha comenzado, o a FINALIZADA si ya ha terminado).
	 * @param metodoPago método de pago a registrar; puede ser nulo si la solicitud ya lo tenía
	 * @return el estado final del curso
	 */
	public String aceptarSolicitud(String matriculaId, String cursoId, String metodoPago, String fechaHoy) {
		MatriculaSecretariaDTO actual = getMatricula(matriculaId, cursoId);
		if (!"SOLICITADA".equals(actual.getEstado())) {
			throw new ValidationException("Solo se pueden aceptar solicitudes en estado SOLICITADA (estado actual: "
					+ actual.getEstado() + ")");
		}
		String metodo = metodoPago != null ? metodoPago : actual.getMetodoPago();
		validarMetodoPago(metodo);
		String fechaPago = actual.getFechaPago() != null ? actual.getFechaPago() : fechaHoy;

		db.enTransaccion(conn -> {
			matriculaRepository.validarPagoCurso(conn, matriculaId, cursoId, metodo, fechaPago);
			return activarMatriculasIniciadas(conn, fechaHoy) + finalizarMatriculasTerminadas(conn, fechaHoy);
		});
		return getMatricula(matriculaId, cursoId).getEstado();
	}

	/**
	 * Modifica la información asociada a un curso matriculado: método de pago y email de contacto.
	 * El método de pago solo se puede cambiar mientras el pago no esté validado; al registrarlo en una
	 * solicitud sin pago queda con fecha de pago y pendiente de que el secretario lo valide.
	 * @param metodoPago nuevo método de pago, o nulo para mantener el actual
	 */
	public void modificarMatricula(String matriculaId, String cursoId, String metodoPago, String email, String fechaHoy) {
		MatriculaSecretariaDTO actual = getMatricula(matriculaId, cursoId);
		if ("CANCELADA".equals(actual.getEstado()) || "FINALIZADA".equals(actual.getEstado())) {
			throw new ValidationException("No se puede modificar una matrícula " + actual.getEstado());
		}
		if (metodoPago != null) {
			validarMetodoPago(metodoPago);
			if (actual.getPagoValidado() == 1 && !metodoPago.equals(actual.getMetodoPago())) {
				throw new ValidationException("El pago por " + actual.getMetodoPago()
						+ " ya está validado: no se puede cambiar su método de pago");
			}
		}
		String fechaPago = metodoPago != null && actual.getFechaPago() == null ? fechaHoy : actual.getFechaPago();
		if (email == null || !email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
			throw new ValidationException("El email introducido no es válido");
		}
		String emailLimpio = email.trim();
		if (alumnoRepository.existeEmailDeOtroAlumno(emailLimpio, actual.getAlumnoId())) {
			throw new ValidationException("El email " + emailLimpio + " ya pertenece a otro estudiante");
		}

		db.enTransaccion(conn -> {
			matriculaRepository.actualizarPagoCurso(conn, matriculaId, cursoId, metodoPago, fechaPago);
			alumnoRepository.actualizarEmail(conn, actual.getAlumnoId(), emailLimpio);
			return 0;
		});
	}

	/**
	 * Cancela un curso o la matrícula completa. La matrícula no se borra: queda registrada como CANCELADA.
	 * Cancelar la matrícula cancela todos sus cursos no finalizados, y cancelar el último curso
	 * no cancelado cancela la matrícula.
	 * @param todaLaMatricula true para cancelar la matrícula completa, false solo el curso indicado
	 */
	public void cancelar(String matriculaId, String cursoId, String motivo, String fechaHoy, boolean todaLaMatricula) {
		if (motivo == null || !MOTIVOS_CANCELACION.contains(motivo)) {
			throw new ValidationException("Motivo de cancelación no válido: " + motivo);
		}
		MatriculaSecretariaDTO actual = getMatricula(matriculaId, cursoId);
		if ("CANCELADA".equals(actual.getEstado()) || "FINALIZADA".equals(actual.getEstado())) {
			throw new ValidationException("No se puede cancelar una matrícula " + actual.getEstado());
		}

		db.enTransaccion(conn -> {
			if (todaLaMatricula) {
				matriculaRepository.cancelarCursosAbiertos(conn, matriculaId, motivo, fechaHoy);
			} else {
				matriculaRepository.cancelarCurso(conn, matriculaId, cursoId, motivo, fechaHoy);
			}
			int restantes = matriculaRepository.contarCursosNoCancelados(conn, matriculaId);
			if (todaLaMatricula || restantes == 0) {
				matriculaRepository.cancelarMatricula(conn, matriculaId, motivo, fechaHoy);
			}
			return 0;
		});
	}

	// =========================================================================
	// Auxiliares
	// =========================================================================

	/**
	 * Activa lo pagado de cualquier año ya iniciado, aunque ya haya terminado: así un curso pagado
	 * nunca se queda PAGADO, y a continuación finalizarMatriculasTerminadas lo pasa a FINALIZADA.
	 */
	private int activarMatriculasIniciadas(Connection conn, String fechaHoy) throws SQLException {
		int activadas = matriculaRepository.activarCursosPagadosIniciados(conn, fechaHoy);
		matriculaRepository.activarMatriculasConCursoActivo(conn);
		return activadas;
	}

	private int finalizarMatriculasTerminadas(Connection conn, String fechaHoy) throws SQLException {
		int finalizadas = matriculaRepository.finalizarCursosActivosTerminados(conn, fechaHoy);
		matriculaRepository.finalizarMatriculasActivasTerminadas(conn, fechaHoy);
		return finalizadas;
	}

	private void validarMetodoPago(String metodoPago) {
		if (metodoPago == null || !METODOS_PAGO.contains(metodoPago)) {
			throw new ValidationException("Debe indicarse un método de pago válido " + METODOS_PAGO);
		}
	}

}
