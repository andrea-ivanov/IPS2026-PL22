package model.fip;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import model.profesor.ProfesorDTO;
import repository.FormadorExternoRepository;
import repository.ProfesorRepository;
import util.Database;
import util.exception.ValidationException;

/**
 * Modelo de los formadores externos (HU 37166): el administrador los da de alta al configurar una FIP y quedan
 * disponibles para otras FIP y para entrar por el login de profesor.
 */
public class FormadorExternoModel {
	private static final String PREFIJO_ID = "FE";
	/** El mismo formato que ck_formadorExterno_email de schema.sql: algo@algo.algo, sin espacios */
	private static final Pattern FORMATO_EMAIL = Pattern.compile("^[^ ]+@[^ ]+\\.[^ ]+$");

	private Database db;
	private FormadorExternoRepository repository;
	private ProfesorRepository profesorRepository;

	public FormadorExternoModel(Database db) {
		this.db = db;
		this.repository = new FormadorExternoRepository(db);
		this.profesorRepository = new ProfesorRepository(db);
	}

	/** Profesores de plantilla y después formadores externos, para los combos de "Crear FIP" y del login */
	public List<ProfesorDTO> getDocentesFip() {
		List<ProfesorDTO> docentes = new ArrayList<>(profesorRepository.getProfesores());
		docentes.addAll(repository.findAll());
		return docentes;
	}

	/**
	 * Da de alta un formador externo, sin DNI.
	 *
	 * @return el identificador del formador creado (FE001, FE002...)
	 * @throws ValidationException si falta algún dato, el correo no tiene formato válido o ya lo tiene otro formador
	 *         externo o un profesor (sin distinguir mayúsculas); en ese caso no se guarda nada
	 */
	public String crearFormadorExterno(String nombre, String apellidos, String email) {
		validarObligatorio(nombre, "El nombre es obligatorio");
		validarObligatorio(apellidos, "Los apellidos son obligatorios");
		validarObligatorio(email, "El correo electrónico es obligatorio");
		String emailLimpio = email.trim();
		if (!FORMATO_EMAIL.matcher(emailLimpio).matches()) {
			throw new ValidationException("El correo electrónico no tiene un formato válido: '" + emailLimpio + "'");
		}

		String nombreLimpio = nombre.trim();
		String apellidosLimpios = apellidos.trim();
		return db.enTransaccion(conn -> {
			if (repository.existeEmail(conn, emailLimpio)) {
				throw new ValidationException("Ya hay un usuario registrado con el correo " + emailLimpio);
			}
			String formadorExternoId = String.format("%s%03d", PREFIJO_ID, repository.siguienteNumero(conn));
			repository.insertar(conn, formadorExternoId, nombreLimpio, apellidosLimpios, emailLimpio);
			return formadorExternoId;
		});
	}

	private void validarObligatorio(String valor, String mensaje) {
		if (valor == null || valor.trim().isEmpty()) {
			throw new ValidationException(mensaje);
		}
	}
}
