package view.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import model.alumno.Alumno;
import model.curso.Curso;
import model.curso.CursoDisplayDTO;
import model.curso.EstadoCurso;
import model.grado.EstadoGrado;
import model.grado.Grado;
import model.grado.GradoDisplayDTO;
import model.matriculacion.EstadoMatricula;
import model.matriculacion.Matriculacion;
import model.matriculacion.MatriculacionDisplayDTO;
import util.Util;

/**
 * Patrón Object Mother / Factory de datos de prueba.
 * 
 * Permite desacoplar los tests y vistas de los constructores y atributos directos de las entidades.
 * Si en el futuro se agregan, modifican o eliminan campos de Grado, Curso, Matriculación o Alumno,
 * solo es necesario ajustar los valores por defecto en esta fábrica, protegiendo todos
 * los tests y componentes existentes de romperse ante cambios de esquema o modelo.
 */
public class TestDataFactory {

	public static Alumno crearAlumnoValido() {
		return Alumno.builder()
				.id(1L)
				.nombre("Juan Perez")
				.email("juan@uni.es")
				.build();
	}

	public static Curso crearCursoProgramacion(Long idGrado) {
		return Curso.builder()
				.id(1L)
				.nombre("Programación")
				.idGrado(idGrado)
				.idCoordinador(1L)
				.ano("2026")
				.itinerario("Desarrollo de Software")
				.profesor("Profesor Martinez")
				.creditos(6)
				.estado(EstadoCurso.ACTIVO)
				.activo(true)
				.build();
	}

	public static Curso crearCursoBasesDeDatos(Long idGrado) {
		return Curso.builder()
				.id(2L)
				.nombre("Bases de Datos")
				.idGrado(idGrado)
				.idCoordinador(2L)
				.ano("2026")
				.itinerario("Gestión de Datos")
				.profesor("Profesora Lopez")
				.creditos(6)
				.estado(EstadoCurso.ACTIVO)
				.activo(true)
				.build();
	}

	public static Grado crearGradoConCursos() {
		Long idGrado = 1L;
		List<Curso> cursos = new ArrayList<>();
		cursos.add(crearCursoProgramacion(idGrado));
		cursos.add(crearCursoBasesDeDatos(idGrado));

		return Grado.builder()
				.id(idGrado)
				.nombre("Ingeniería Informática")
				.facultad("Escuela de Ingeniería Informática")
				.rama("Ingeniería y Arquitectura")
				.cursos(cursos)
				.estado(EstadoGrado.ACTIVO)
				.fechaCreacion(new Date())
				.activo(true)
				.build();
	}

	public static Matriculacion crearMatriculacionActiva() {
		Long idGrado = 1L;
		List<Curso> cursos = new ArrayList<>();
		cursos.add(crearCursoProgramacion(idGrado));
		cursos.add(crearCursoBasesDeDatos(idGrado));

		return Matriculacion.builder()
				.id(1L)
				.alumno(crearAlumnoValido())
				.idGrado(idGrado)
				.cursos(cursos)
				.fecha(Util.isoStringToDate("2026-09-01"))
				.estado(EstadoMatricula.ACTIVA)
				.pagada(true)
				.build();
	}

	// =========================================================================
	// Generadores de Mocks para Vistas y DTOs
	// =========================================================================

	public static List<GradoDisplayDTO> crearMockGradosDisplay() {
		List<GradoDisplayDTO> list = new ArrayList<>();
		list.add(new GradoDisplayDTO("1", "Ingeniería Informática", "Escuela de Ingeniería Informática", "Ingeniería y Arquitectura", "VIGENTE"));
		list.add(new GradoDisplayDTO("2", "Ingeniería de Telecomunicaciones", "Escuela Politécnica de Ingeniería", "Ingeniería y Arquitectura", "VIGENTE"));
		list.add(new GradoDisplayDTO("3", "Matemáticas", "Facultad de Ciencias", "Ciencias", "EN_EXTINCION"));
		return list;
	}

	public static List<CursoDisplayDTO> crearMockCursosDisplay(String idGrado) {
		List<CursoDisplayDTO> list = new ArrayList<>();
		if ("2".equals(idGrado)) {
			list.add(new CursoDisplayDTO("201", "Sistemas de Comunicación", "2026", "Telecomunicación", "Dr. Fernández", "ABIERTO", "2"));
			list.add(new CursoDisplayDTO("202", "Teoría de Señales", "2026", "Telemática", "Dra. García", "CERRADO", "2"));
		} else if ("3".equals(idGrado)) {
			list.add(new CursoDisplayDTO("301", "Álgebra Lineal", "2026", "Básica", "Dr. Suárez", "ABIERTO", "3"));
			list.add(new CursoDisplayDTO("302", "Cálculo Infinitesimal", "2026", "Básica", "Dra. Álvarez", "CERRADO", "3"));
		} else {
			// Por defecto o Grado 1 (Informática)
			list.add(new CursoDisplayDTO("1", "Programación", "2026", "Desarrollo de Software", "Profesor Martinez", "ABIERTO", "1"));
			list.add(new CursoDisplayDTO("2", "Bases de Datos", "2026", "Gestión de Datos", "Profesora Lopez", "ABIERTO", "1"));
			list.add(new CursoDisplayDTO("101", "Programación Orientada a Objetos", "2026", "Desarrollo", "Dr. López", "ABIERTO", "1"));
		}
		return list;
	}

	public static List<MatriculacionDisplayDTO> crearMockMatriculacionesDisplay(String idCurso) {
		List<MatriculacionDisplayDTO> list = new ArrayList<>();
		list.add(new MatriculacionDisplayDTO("U00123", "Juan Pérez", idCurso, "juan@uni.es", "ACTIVA"));
		list.add(new MatriculacionDisplayDTO("U00124", "María Gómez", idCurso, "maria@uni.es", "ACTIVA"));
		list.add(new MatriculacionDisplayDTO("U00125", "Carlos Rodríguez", idCurso, "carlos@uni.es", "CANCELADA"));
		return list;
	}
}
