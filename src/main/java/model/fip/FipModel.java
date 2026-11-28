package model.fip;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import repository.FipRepository;
import repository.FormadorExternoRepository;
import repository.ProfesorRepository;
import util.Database;
import util.exception.ValidationException;

/**
 * Modelo de las Formaciones Internas del Personal (FIP): el administrador las crea directamente y les asigna un
 * profesor de plantilla (HU 37165) o un formador externo (HU 37166). La matriculación queda abierta desde la
 * creación hasta la fecha de inicio.
 */
public class FipModel {
	/** La fecha de inicio debe ser posterior a hoy + estos días (el día límite no vale) */
	public static final int DIAS_MINIMOS_ANTES_INICIO_FIP = 7;
	private static final String PREFIJO_ID = "FIP";
	private static final DateTimeFormatter FORMATO_MENSAJE = DateTimeFormatter.ofPattern("dd/MM/yy");

	private Database db;
	private FipRepository fipRepository;
	private ProfesorRepository profesorRepository;
	private FormadorExternoRepository formadorExternoRepository;

	public FipModel(Database db) {
		this.db = db;
		this.fipRepository = new FipRepository(db);
		this.profesorRepository = new ProfesorRepository(db);
		this.formadorExternoRepository = new FormadorExternoRepository(db);
	}

	/** Todas las FIP para el listado del administrador, de la que empieza más tarde a la que empieza antes */
	public List<FipDisplayDTO> getFips() {
		return fipRepository.findAll();
	}

	/**
	 * Crea una FIP impartida por el profesor indicado, con fecha de creación fechaHoy.
	 *
	 * @param fechaInicio Fecha ISO (AAAA-MM-DD), posterior a fechaHoy + 7 días
	 * @param fechaFin Fecha ISO (AAAA-MM-DD), no anterior a fechaInicio
	 * @param fechaHoy Fecha de referencia en formato ISO (AAAA-MM-DD)
	 * @return el identificador de la FIP creada (FIP001, FIP002...)
	 * @throws ValidationException con el primer dato no válido; en ese caso no se guarda nada
	 */
	public String crearFip(String nombre, String profesorId, String fechaInicio, String fechaFin, String fechaHoy) {
		validarObligatorio(nombre, "El nombre de la FIP es obligatorio");
		validarObligatorio(profesorId, "El profesor de la FIP es obligatorio");
		validarFechas(fechaInicio, fechaFin, fechaHoy);

		String nombreLimpio = nombre.trim();
		String profesor = profesorId.trim();
		return db.enTransaccion(conn -> {
			if (!profesorRepository.existe(conn, profesor)) {
				throw new ValidationException("El profesor " + profesor + " no existe");
			}
			String fipId = String.format("%s%03d", PREFIJO_ID, fipRepository.siguienteNumero(conn));
			fipRepository.insertar(conn, fipId, nombreLimpio, profesor, fechaHoy.trim(), fechaInicio.trim(),
					fechaFin.trim());
			return fipId;
		});
	}

	/**
	 * Crea una FIP impartida por el formador externo indicado (HU 37166), con las mismas reglas que crearFip.
	 *
	 * @param fechaInicio Fecha ISO (AAAA-MM-DD), posterior a fechaHoy + 7 días
	 * @param fechaFin Fecha ISO (AAAA-MM-DD), no anterior a fechaInicio
	 * @param fechaHoy Fecha de referencia en formato ISO (AAAA-MM-DD)
	 * @return el identificador de la FIP creada (FIP001, FIP002...)
	 * @throws ValidationException con el primer dato no válido; en ese caso no se guarda nada
	 */
	public String crearFipFormadorExterno(String nombre, String formadorExternoId, String fechaInicio,
			String fechaFin, String fechaHoy) {
		validarObligatorio(nombre, "El nombre de la FIP es obligatorio");
		validarObligatorio(formadorExternoId, "El formador externo de la FIP es obligatorio");
		validarFechas(fechaInicio, fechaFin, fechaHoy);

		String nombreLimpio = nombre.trim();
		String formador = formadorExternoId.trim();
		return db.enTransaccion(conn -> {
			if (!formadorExternoRepository.existe(conn, formador)) {
				throw new ValidationException("El formador externo " + formador + " no existe");
			}
			String fipId = String.format("%s%03d", PREFIJO_ID, fipRepository.siguienteNumero(conn));
			fipRepository.insertarConFormadorExterno(conn, fipId, nombreLimpio, formador, fechaHoy.trim(),
					fechaInicio.trim(), fechaFin.trim());
			return fipId;
		});
	}

	/** Las dos fechas obligatorias, el inicio posterior a hoy + 7 días y el fin no anterior al inicio */
	private void validarFechas(String fechaInicio, String fechaFin, String fechaHoy) {
		validarObligatorio(fechaInicio, "La fecha de inicio es obligatoria");
		validarObligatorio(fechaFin, "La fecha de fin es obligatoria");

		LocalDate inicio = LocalDate.parse(fechaInicio.trim());
		LocalDate fin = LocalDate.parse(fechaFin.trim());
		LocalDate limite = LocalDate.parse(fechaHoy.trim()).plusDays(DIAS_MINIMOS_ANTES_INICIO_FIP);
		if (!inicio.isAfter(limite)) {
			throw new ValidationException(
					"La fecha de inicio debe ser posterior al " + limite.format(FORMATO_MENSAJE));
		}
		if (fin.isBefore(inicio)) {
			throw new ValidationException("La fecha de fin no puede ser anterior a la de inicio");
		}
	}

	private void validarObligatorio(String valor, String mensaje) {
		if (valor == null || valor.trim().isEmpty()) {
			throw new ValidationException(mensaje);
		}
	}
}
