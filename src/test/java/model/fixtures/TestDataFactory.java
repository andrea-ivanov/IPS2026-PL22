package model.fixtures;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import model.alumno.Alumno;
import model.curso.Curso;
import model.grado.EstadoGrado;
import model.grado.Grado;
import model.matriculacion.EstadoMatricula;
import model.matriculacion.Matriculacion;
import util.Util;

/**
 * Patrón Object Mother / Factory de datos de prueba.
 * 
 * Permite desacoplar los tests de los constructores y atributos directos de las entidades.
 * Si en el futuro se agregan, modifican o eliminan campos de Grado, Curso o Matriculación,
 * solo es necesario ajustar los valores por defecto en esta fábrica, protegiendo todos
 * los tests existentes de romperse ante cambios de esquema o modelo.
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
				.creditos(6)
				.activo(true)
				.build();
	}

	public static Curso crearCursoBasesDeDatos(Long idGrado) {
		return Curso.builder()
				.id(2L)
				.nombre("Bases de Datos")
				.idGrado(idGrado)
				.idCoordinador(2L)
				.creditos(6)
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
}

