package model.matriculacion;

import model.grado.GradoDisplayDTO;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import model.alumno.AlumnoModel;
import repository.AnoAcademicoRepository;
import repository.MatriculaRepository;
import util.Database;
import util.UnexpectedException;
import util.exception.ValidationException;

/**
 * Modelo de negocio y caso de uso para la gestión y búsqueda de
 * Matriculaciones.
 * Integra la resolución de Alumno, asignaturas/cursos matriculados y el mapeo
 * tipado
 * de fechas, enums (EstadoMatricula) y booleanos.
 */
public class MatriculacionModel {
	// Constantes de negocio según docs/DATABASE.md (Sección 6.1)
	public static final double PRECIO_CREDITO = 20.0;
	public static final int CREDITOS_MINIMOS_ANIO = 12;
	public static final int CREDITOS_MINIMOS_TERCER_ANIO = 90;
	public static final int MAX_CONVOCATORIAS = 6;
	public static final double NOTA_APROBADO = 5.0;

	private Database db;
	private AlumnoModel alumnoModel;
	private AnoAcademicoRepository anoAcademicoRepository;
	private MatriculaRepository matriculaRepository;

	public MatriculacionModel(Database db) {
		this.db = db;
		this.alumnoModel = new AlumnoModel(db);
		this.anoAcademicoRepository = new AnoAcademicoRepository(db);
		this.matriculaRepository = new MatriculaRepository(db);
	}

	/**
	 * Obtiene la lista de matrículas de un estudiante dado su UO o alumnoId.
	 * Cruza la tabla Matricula con Grado para poblar MatriculacionDisplayDTO.
	 *
	 * @param uo Identificador del alumno (p. ej. "AL001", "1")
	 * @return Lista de MatriculacionDisplayDTO correspondientes al alumno
	 */
	public List<MatriculacionDisplayDTO> getMatriculasPorEstudiante(String uo) {
		if (uo == null || uo.trim().isEmpty()) {
			return new ArrayList<>();
		}
		return matriculaRepository.findMatriculasAlumno(resolverAlumnoId(uo));
	}

	/**
	 * Resuelve el identificador oficial de un alumno en la base de datos (p. ej. "AL001")
	 * tanto si se le pasa con prefijo UO ("UO001"), AL ("AL001") o numérico ("1").
	 * Garantiza el acceso coherente a los datos de la base de datos.
	 *
	 * @param uoOrId Código UO o identificador del alumno
	 * @return Identificador exacto en la tabla Alumno
	 */
	public String resolverAlumnoId(String uoOrId) {
		return alumnoModel.resolverAlumnoId(uoOrId);
	}

	/**
	 * Obtiene el nombre de pila del alumno a partir de su UO o alumnoId o cadena "UO - Nombre".
	 *
	 * @param uoOrId Código UO, ID del alumno o etiqueta "UOxxxxx - Nombre"
	 * @return Nombre del estudiante, o cadena vacía si no se encuentra
	 */
	public String obtenerNombreAlumno(String uoOrId) {
		return alumnoModel.obtenerNombreAlumno(uoOrId);
	}

	/**
	 * Obtiene los grados en los que está o ha estado matriculado el estudiante.
	 * Formato para el combo: "G001 - Ingenieria Informatica del Software".
	 *
	 * @param uo Identificador del alumno (p. ej. "AL001")
	 * @return Lista de Object[] para poblar el combo de selección de grado
	 */
	public List<Object[]> getGradosPorEstudiante(String uo) {
		if (uo == null || uo.trim().isEmpty()) {
			return new ArrayList<>();
		}
		return matriculaRepository.findGradosAlumno(resolverAlumnoId(uo));
	}

	/**
	 * Obtiene las asignaturas matriculadas de un estudiante filtradas por el grado
	 * consultado.
	 * Implementa la consulta de referencia de la HU 35232 de docs/DATABASE.md.
	 *
	 * @param uo      Identificador del alumno (p. ej. "AL001")
	 * @param gradoId Identificador del grado (p. ej. "G001") o null para todos
	 * @return Lista de MatriculacionDisplayDTO con anioAcademicoId, anio, curso,
	 *         numMatricula y estado
	 */
	public List<MatriculacionDisplayDTO> getMatriculasPorEstudianteYGrado(String uo, String gradoId) {
		if (uo == null || uo.trim().isEmpty()) {
			return new ArrayList<>();
		}
		return matriculaRepository.findCursosMatriculados(resolverAlumnoId(uo), limpiar(gradoId));
	}

	/** Valor sin espacios alrededor; vacío si es nulo (los filtros vacíos no se aplican) */
	private static String limpiar(String valor) {
		return valor != null ? valor.trim() : "";
	}

	/**
	 * Obtiene el resumen de hitos del régimen de permanencia del estudiante (HU 35232 - Paso 3).
	 * Compara:
	 * 1. Créditos superados en el último año evaluado frente a 12 créditos.
	 * 2. Créditos superados acumulados en el grado frente a 90 créditos (al tercer año).
	 *
	 * @param uo Identificador del alumno (p. ej. "AL001")
	 * @param gradoId Identificador del grado (p. ej. "G001")
	 * @return Objeto HitosPermanenciaDTO con el cálculo realizado
	 */
	public HitosPermanenciaDTO getHitosPermanencia(String uo, String gradoId) {
		if (uo == null || uo.trim().isEmpty()) {
			return new HitosPermanenciaDTO("", "", null, 0, 0, 0);
		}
		String cleanUo = resolverAlumnoId(uo);
		String cleanGrado = limpiar(gradoId);

		String ultimoAnoEvaluadoId = null;
		int creditosAnuales = 0;
		int creditosTotales = 0;
		int aniosEnGrado = 0;

		// 1. Créditos superados en cada año matriculado
		List<Object[]> rowsAnual = matriculaRepository.findCreditosSuperadosPorMatricula(cleanUo, cleanGrado,
				NOTA_APROBADO);

		if (rowsAnual != null && !rowsAnual.isEmpty()) {
			Object[] selectedRow = rowsAnual.get(0);
			if (rowsAnual.size() > 1) {
				int creds0 = selectedRow[3] != null ? ((Number) selectedRow[3]).intValue() : 0;
				String estado0 = selectedRow[2] != null ? selectedRow[2].toString() : "";
				if (creds0 == 0 && ("ACTIVA".equalsIgnoreCase(estado0) || "SOLICITADA".equalsIgnoreCase(estado0))) {
					selectedRow = rowsAnual.get(1); // Usamos el último año cerrado / con evaluaciones
				}
			}
			ultimoAnoEvaluadoId = selectedRow[1] != null ? selectedRow[1].toString() : null;
			creditosAnuales = selectedRow[3] != null ? ((Number) selectedRow[3]).intValue() : 0;
		}

		// 2. Años en el grado y créditos totales acumulados (frente a 90)
		List<Object[]> rowsTotal = matriculaRepository.findAniosYCreditosTotales(cleanUo, cleanGrado, NOTA_APROBADO);

		if (rowsTotal != null && !rowsTotal.isEmpty()) {
			Object[] r = rowsTotal.get(0);
			aniosEnGrado = r[0] != null ? ((Number) r[0]).intValue() : 0;
			creditosTotales = r[1] != null ? ((Number) r[1]).intValue() : 0;
		}

		return new HitosPermanenciaDTO(cleanUo, cleanGrado, ultimoAnoEvaluadoId, creditosAnuales, creditosTotales, aniosEnGrado);
	}

	/**
	 * Obtiene el historial de calificaciones obtenidas por un estudiante en las convocatorias presentadas (HU 35232 - Paso 4).
	 *
	 * @param uo Identificador del alumno
	 * @param gradoId Identificador del grado (opcional)
	 * @return Lista de CalificacionDisplayDTO
	 */
	public List<CalificacionDisplayDTO> getCalificacionesPorEstudianteYGrado(String uo, String gradoId) {
		if (uo == null || uo.trim().isEmpty()) {
			return new ArrayList<>();
		}
		return matriculaRepository.findCalificaciones(resolverAlumnoId(uo), limpiar(gradoId));
	}

	/**
	 * Comprueba si el estudiante ya cuenta con una matrícula registrada (no cancelada)
	 * para el grado y año académico indicados (impide solicitudes duplicadas - HU 35235).
	 *
	 * @param alumnoId Identificador del alumno (p. ej. "AL001")
	 * @param gradoId Identificador del grado (p. ej. "G001")
	 * @param anioAcademicoId Identificador del año académico (p. ej. "2026-2027")
	 * @return true si ya existe una matrícula previa no cancelada
	 */
	public boolean comprobarMatriculaExistente(String alumnoId, String gradoId, String anioAcademicoId) {
		if (alumnoId == null || gradoId == null || anioAcademicoId == null) {
			return false;
		}
		return matriculaRepository.existeMatriculaNoCancelada(resolverAlumnoId(alumnoId), gradoId.trim(),
				anioAcademicoId.trim());
	}

	/**
	 * Devuelve el número de convocatorias con calificación numérica consumidas por el estudiante
	 * en una asignatura a lo largo de todos sus años y grados (HU 35235 / Regla 6.3).
	 * Las notas nulas (no presentado) no computan.
	 *
	 * @param alumnoId Identificador del alumno
	 * @param cursoId Identificador de la asignatura (p. ej. "C005")
	 * @return Cantidad de convocatorias consumidas (0 a 6)
	 */
	public int getConvocatoriasConsumidas(String alumnoId, String cursoId) {
		if (alumnoId == null || cursoId == null) {
			return 0;
		}
		return matriculaRepository.contarConvocatoriasConsumidas(resolverAlumnoId(alumnoId), cursoId.trim());
	}

	/**
	 * Comprueba si el alumno se encuentra al corriente de pago (HU 35235 / Regla 6.4).
	 * No debe tener asignaturas SOLICITADAS sin pagar en años académicos ya iniciados,
	 * ni cancelaciones previas por motivo de IMPAGO.
	 *
	 * @param alumnoId Identificador del alumno
	 * @return true si está al corriente de pago, false si tiene deudas o impagos pendientes
	 */
	public boolean comprobarAlCorrienteDePago(String alumnoId) {
		if (alumnoId == null || alumnoId.trim().isEmpty()) {
			return true;
		}
		String cleanId = resolverAlumnoId(alumnoId);
		// 1. Asignaturas sin pagar con el año ya iniciado
		if (matriculaRepository.contarCursosSinPagarIniciados(cleanId) > 0) {
			return false;
		}
		// 2. Cancelaciones por IMPAGO
		return matriculaRepository.contarCancelacionesPorImpago(cleanId) == 0;
	}

	/**
	 * Verifica si el estudiante cumple el régimen de permanencia en el grado (HU 35235 / Regla 6.3):
	 * 1. Mínimo anual: si la matrícula anterior en este grado no fue cancelada, debió superar >= 12 créditos.
	 * 2. Mínimo al tercer año: si ya acumula >= 3 años en el grado, debe sumar >= 90 créditos totales superados.
	 *
	 * @param alumnoId Identificador del alumno
	 * @param gradoId Identificador del grado
	 * @return true si cumple el régimen de permanencia
	 */
	public boolean comprobarCumplimientoPermanencia(String alumnoId, String gradoId) {
		HitosPermanenciaDTO hitos = getHitosPermanencia(alumnoId, gradoId);
		if (hitos == null) {
			return true;
		}

		// Si tuvo año evaluado previo, debe superar el mínimo anual de 12 créditos
		if (hitos.getUltimoAnoEvaluadoId() != null && !hitos.getUltimoAnoEvaluadoId().isEmpty()) {
			if (!hitos.isCumpleHitoAnual()) {
				return false;
			}
		}

		// Si lleva 3 o más años, debe sumar al menos 90 créditos
		if (!hitos.isCumpleHitoTercerAnio()) {
			return false;
		}

		return true;
	}

	/**
	 * Valida integralmente una solicitud de matrícula frente a todas las reglas de negocio (HU 35235).
	 * Lanza ValidationException con un mensaje descriptivo si alguna regla es vulnerada.
	 *
	 * @param alumnoId Identificador del alumno
	 * @param gradoId Identificador del grado solicitado
	 * @param anioAcademicoId Año académico actual
	 * @param cursoIds Lista de asignaturas seleccionadas
	 * @throws ValidationException si alguna regla no se cumple
	 */
	public void validarSolicitudMatricula(String alumnoId, String gradoId, String anioAcademicoId, List<String> cursoIds) {
		if (alumnoId == null || alumnoId.trim().isEmpty()) {
			throw new ValidationException("Debe especificarse el identificador del alumno.");
		}
		if (gradoId == null || gradoId.trim().isEmpty()) {
			throw new ValidationException("Debe seleccionar un grado para la matrícula.");
		}
		if (anioAcademicoId == null || anioAcademicoId.trim().isEmpty()) {
			throw new ValidationException("Debe especificarse el año académico actual.");
		}
		if (cursoIds == null || cursoIds.isEmpty()) {
			throw new ValidationException("Debe seleccionar al menos una asignatura para matricularse.");
		}

		// 1. Validar al corriente de pago
		if (!comprobarAlCorrienteDePago(alumnoId)) {
			throw new ValidationException("No es posible solicitar la matrícula: el estudiante tiene pagos pendientes o deudas previas.");
		}

		// 2. Validar solicitud duplicada
		if (comprobarMatriculaExistente(alumnoId, gradoId, anioAcademicoId)) {
			throw new ValidationException("Ya existe una matrícula activa o en trámite para este grado en el año académico " + anioAcademicoId + ".");
		}

		// 3. Validar régimen de permanencia
		if (!comprobarCumplimientoPermanencia(alumnoId, gradoId)) {
			throw new ValidationException("La matrícula no cumple con los requisitos del régimen de permanencia en este grado.");
		}

		// 4. Validar límite de convocatorias por cada asignatura solicitada
		for (String cId : cursoIds) {
			int consumidas = getConvocatoriasConsumidas(alumnoId, cId);
			if (consumidas >= MAX_CONVOCATORIAS) {
				throw new ValidationException("No es posible matricularse en la asignatura '" + cId + "': se ha alcanzado el límite de " + MAX_CONVOCATORIAS + " convocatorias.");
			}
		}
	}

	/**
	 * Consulta la oferta de grados disponibles para nueva matrícula (HU 35235 / Fase 2).
	 * Solo incluye grados en estado 'VIGENTE' o 'PENDIENTE_RENOVACION',
	 * excluyendo los grados 'EN_EXTINCION' y 'EXTINTO'.
	 *
	 * @return Lista de GradoDisplayDTO disponibles
	 */
	public List<GradoDisplayDTO> getOfertaGradosDisponibles() {
		return matriculaRepository.findGradosOfertados();
	}

	/**
	 * Consulta las asignaturas activas ofertadas para un grado seleccionado (HU 35235 / Fase 2).
	 * Incluye el cálculo del precio (20.0 €/crédito) y, si se facilita el identificador del alumno,
	 * el número de convocatorias consumidas con calificación en cada asignatura.
	 *
	 * @param gradoId Identificador del grado (p. ej. "G001")
	 * @param alumnoId Identificador del alumno (p. ej. "AL001", opcional para cálculo de convocatorias)
	 * @return Lista de OfertaCursoDTO activos en dicho grado
	 */
	public List<OfertaCursoDTO> getOfertaCursosPorGrado(String gradoId, String alumnoId) {
		if (gradoId == null || gradoId.trim().isEmpty()) {
			return new ArrayList<>();
		}
		List<OfertaCursoDTO> list = matriculaRepository.findCursosOfertados(gradoId.trim());
		for (OfertaCursoDTO curso : list) {
			if (curso.getPrecio() <= 0 && curso.getCreditos() > 0) {
				curso.setPrecio(curso.getCreditos() * PRECIO_CREDITO);
			}
			if (alumnoId != null && !alumnoId.trim().isEmpty()) {
				curso.setConvocatoriasConsumidas(getConvocatoriasConsumidas(alumnoId, curso.getCursoId()));
			}
		}
		return list;
	}

	/**
	 * Sobrecarga de conveniencia para consultar las asignaturas ofertadas sin contextualizar alumno.
	 *
	 * @param gradoId Identificador del grado
	 * @return Lista de OfertaCursoDTO activos en dicho grado
	 */
	public List<OfertaCursoDTO> getOfertaCursosPorGrado(String gradoId) {
		return getOfertaCursosPorGrado(gradoId, null);
	}

	/**
	 * Obtiene el identificador del año académico en curso o más reciente (HU 35235 / Fase 2).
	 * Comprueba primero si la fecha actual está dentro del rango oficial (fechaInicio - fechaFin).
	 *
	 * @return Identificador del año académico (p. ej. "2026-2027")
	 */
	public String getAnioAcademicoActual() {
		String vigente = anoAcademicoRepository.findVigenteId();
		if (vigente != null) {
			return vigente;
		}
		String reciente = anoAcademicoRepository.findActualId();
		if (reciente != null) {
			return reciente;
		}
		return "2026-2027";
	}

	/**
	 * Alias explícito para comprobar las convocatorias consumidas con calificación en una asignatura
	 * (solicitado según la nomenclatura de la Fase 2 del plan de trabajo).
	 *
	 * @param alumnoId Identificador del alumno
	 * @param cursoId Identificador de la asignatura
	 * @return Número de convocatorias consumidas (con nota != null)
	 */
	public int comprobarConvocatorias(String alumnoId, String cursoId) {
		return getConvocatoriasConsumidas(alumnoId, cursoId);
	}

	/**
	 * Alias explícito para comprobar el cumplimiento y los hitos del régimen de permanencia
	 * (solicitado según la nomenclatura de la Fase 2 del plan de trabajo).
	 *
	 * @param alumnoId Identificador del alumno
	 * @param gradoId Identificador del grado
	 * @return Objeto HitosPermanenciaDTO con el desglose de permanencia
	 */
	public HitosPermanenciaDTO comprobarPermanencia(String alumnoId, String gradoId) {
		return getHitosPermanencia(alumnoId, gradoId);
	}

	/**
	 * Genera de forma segura el siguiente identificador correlativo para una matrícula (p. ej. "M02101").
	 * Utiliza la conexión proporcionada para operar dentro de una transacción.
	 *
	 * @param conn Conexión JDBC activa
	 * @return Nuevo identificador formateado como "M%05d", el mismo que data.sql
	 * @throws SQLException si ocurre un error SQL
	 */
	public String generarSiguienteMatriculaId(Connection conn) throws SQLException {
		return String.format("M%05d", matriculaRepository.siguienteNumeroMatricula(conn));
	}

	/**
	 * Registra de forma transaccional una nueva solicitud de matrícula en la base de datos (HU 35235 / Fase 2).
	 * <p>
	 * Reglas de persistencia:
	 * 1. Valida previamente permanencia, convocatorias, al corriente de pago y ausencia de duplicidad.
	 * 2. Inserta el registro en {@code Matricula} con estado {@code 'SOLICITADA'}.
	 * 3. Inserta cada asignatura en {@code MatriculaCurso}:
	 *    - Si el pago es con {@code TARJETA}: estado {@code 'PAGADA'}, pagoValidado {@code = 1}.
	 *    - Si es {@code TRANSFERENCIA} o {@code CARTA_PAGO}: estado {@code 'SOLICITADA'}, pagoValidado {@code = 0} (requiere aprobación manual de secretaría).
	 * </p>
	 *
	 * @param solicitud DTO consolidado con toda la información de la solicitud y método de pago
	 * @return El identificador de la nueva matrícula registrada (p. ej. "M02101")
	 * @throws ValidationException si alguna validación de negocio falla
	 * @throws UnexpectedException si ocurre algún fallo transaccional en base de datos
	 */
	public String registrarSolicitudMatricula(ResumenSolicitudDTO solicitud) {
		if (solicitud == null) {
			throw new ValidationException("La información de la solicitud de matrícula no puede ser nula.");
		}

		String alumnoId = resolverAlumnoId(solicitud.getAlumnoId());
		String gradoId = solicitud.getGradoId();
		String anioAcademicoId = solicitud.getAnioAcademicoId();
		if (anioAcademicoId == null || anioAcademicoId.trim().isEmpty()) {
			anioAcademicoId = getAnioAcademicoActual();
			solicitud.setAnioAcademicoId(anioAcademicoId);
		}

		List<OfertaCursoDTO> asignaturas = solicitud.getAsignaturas();
		if (asignaturas == null || asignaturas.isEmpty()) {
			throw new ValidationException("Debe seleccionar al menos una asignatura para matricularse.");
		}

		List<String> cursoIds = new ArrayList<>();
		for (OfertaCursoDTO c : asignaturas) {
			if (c != null && c.getCursoId() != null) {
				cursoIds.add(c.getCursoId().trim());
			}
		}

		// Validar reglas de negocio completas (permanencia, convocatorias, duplicados, al corriente de pago)
		validarSolicitudMatricula(alumnoId, gradoId, anioAcademicoId, cursoIds);

		// Validar método de pago admitido
		String metodoPago = solicitud.getMetodoPago();
		if (metodoPago == null || metodoPago.trim().isEmpty()) {
			throw new ValidationException("Debe seleccionar un método de pago (TARJETA, TRANSFERENCIA o CARTA_PAGO).");
		}
		metodoPago = metodoPago.trim().toUpperCase();
		if (!"TARJETA".equals(metodoPago) && !"TRANSFERENCIA".equals(metodoPago) && !"CARTA_PAGO".equals(metodoPago)) {
			throw new ValidationException("Método de pago '" + metodoPago + "' no reconocido. Opciones válidas: TARJETA, TRANSFERENCIA o CARTA_PAGO.");
		}

		// Fecha de la solicitud (formato ISO YYYY-MM-DD)
		String fecha = solicitud.getFechaSolicitud();
		if (fecha == null || fecha.trim().isEmpty()) {
			fecha = LocalDate.now().toString();
			solicitud.setFechaSolicitud(fecha);
		} else {
			fecha = fecha.trim();
		}

		// Estado y pago según método elegido
		boolean esTarjeta = "TARJETA".equals(metodoPago);
		String estadoCurso = esTarjeta ? "PAGADA" : "SOLICITADA";
		int pagoValidado = esTarjeta ? 1 : 0;
		String metodo = metodoPago;
		String fechaSolicitud = fecha;
		String anio = anioAcademicoId.trim();

		// Matricula y sus MatriculaCurso se insertan en la misma transacción: o todo o nada
		return db.enTransaccion(conn -> {
			try {
				// 1. Obtener nuevo ID correlativo de matrícula
				String matriculaId = generarSiguienteMatriculaId(conn);

				// 2. Insertar cabecera en Matricula (siempre SOLICITADA)
				matriculaRepository.insertarMatricula(conn, matriculaId, alumnoId.trim(), gradoId.trim(), anio,
						fechaSolicitud);

				// 3. Insertar cada asignatura en MatriculaCurso
				for (OfertaCursoDTO c : asignaturas) {
					double importe = c.getPrecio() > 0 ? c.getPrecio() : c.getCreditos() * PRECIO_CREDITO;
					matriculaRepository.insertarCurso(conn, matriculaId, gradoId.trim(), c.getCursoId().trim(),
							estadoCurso, importe, metodo, fechaSolicitud, pagoValidado);
				}
				return matriculaId;
			} catch (SQLException ex) {
				if (ex.getMessage() != null && ex.getMessage().contains("UNIQUE constraint failed")) {
					throw new ValidationException("Ya existe una matrícula registrada para este estudiante en el grado y año indicados.");
				}
				throw ex; // enTransaccion la convierte en UnexpectedException
			}
		});
	}

	/**
	 * Sobrecarga de conveniencia para registrar solicitud a partir de los datos atómicos.
	 *
	 * @param alumnoId Identificador del alumno
	 * @param gradoId Identificador del grado
	 * @param anioAcademicoId Año académico
	 * @param asignaturas Asignaturas seleccionadas
	 * @param metodoPago Método de pago ("TARJETA", "TRANSFERENCIA", "CARTA_PAGO")
	 * @return El ID de la matrícula creada
	 */
	public String registrarSolicitudMatricula(String alumnoId, String gradoId, String anioAcademicoId,
			List<OfertaCursoDTO> asignaturas, String metodoPago) {
		ResumenSolicitudDTO dto = new ResumenSolicitudDTO();
		dto.setAlumnoId(alumnoId);
		dto.setGradoId(gradoId);
		dto.setAnioAcademicoId(anioAcademicoId);
		dto.setAsignaturas(asignaturas);
		dto.setMetodoPago(metodoPago);
		return registrarSolicitudMatricula(dto);
	}

	/**
	 * Carga los datos informativos del alumno y del grado seleccionado para precargar
	 * el objeto ResumenSolicitudDTO que se presentará en la vista resumen (HU 35235).
	 *
	 * @param alumnoId Identificador del alumno
	 * @param gradoId Identificador del grado (opcional)
	 * @return DTO resumen precargado con datos del alumno, año académico y grado
	 */
	public ResumenSolicitudDTO obtenerDatosEstudianteParaSolicitud(String alumnoId, String gradoId) {
		ResumenSolicitudDTO resumen = new ResumenSolicitudDTO();
		String cleanAlumnoId = resolverAlumnoId(alumnoId);
		resumen.setAlumnoId(cleanAlumnoId);
		resumen.setGradoId(gradoId);
		resumen.setAnioAcademicoId(getAnioAcademicoActual());
		resumen.setFechaSolicitud(LocalDate.now().toString());

		if (cleanAlumnoId != null && !cleanAlumnoId.trim().isEmpty()) {
			Object[] r = matriculaRepository.findDatosAlumno(cleanAlumnoId.trim());
			if (r != null) {
				resumen.setDni(r[0] != null ? r[0].toString() : "");
				String nom = r[1] != null ? r[1].toString() : "";
				String ape = r[2] != null ? r[2].toString() : "";
				resumen.setNombreCompleto((nom + " " + ape).trim());
				resumen.setEmail(r[3] != null ? r[3].toString() : "");
			}
		}

		if (gradoId != null && !gradoId.trim().isEmpty()) {
			String nombreGrado = matriculaRepository.findNombreGrado(gradoId.trim());
			if (nombreGrado != null) {
				resumen.setNombreGrado(nombreGrado);
			}
		}

		return resumen;
	}

	/**
	 * Obtiene las matrículas (alumnos) asociadas a un curso específico para la tabla de AdminOfertaView.
	 */
	public List<MatriculacionDisplayDTO> getMatriculacionesPorCurso(String idCurso) {
		return getMatriculacionesPorCursoYAnoActual(idCurso, null);
	}

	/**
     * Obtiene las matrículas (alumnos) asociadas a un curso específico
     * filtradas por el año académico indicado.
     */
    public List<MatriculacionDisplayDTO> getMatriculacionesPorCursoYAnoActual(String idCurso, String anioAcademicoId) {
        if (idCurso == null) {
            return new ArrayList<>();
        }
        return matriculaRepository.findAlumnosCurso(idCurso, anioAcademicoId);
    }
}
