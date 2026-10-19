package model.matriculacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Pruebas de la Secretaría: HU 35226 (consulta de matriculaciones) y HU 35229 (ciclo de vida de las matrículas).
 * Cada prueba parte de sus propios datos (loadCleanDatabase), independientes de data.sql.
 * Fecha de referencia de los datos: octubre de 2026 (el año 2026-2027 empezó el 2026-09-07).
 */
class GestionMatriculaModelTest {
	private static final String ANTES_INICIO = "2026-08-01";
	private static final String CURSO_INICIADO = "2026-10-17";
	private static final String ANIO_TERMINADO = "2027-08-01";

	private static Database db = new Database();
	private GestionMatriculaModel model;

	@BeforeEach
	void setUp() {
		db.createDatabase(true);
		loadCleanDatabase(db);
		model = new GestionMatriculaModel(db);
	}

	public static void loadCleanDatabase(Database db) {
		db.executeBatch(new String[] {
				"DELETE FROM Calificacion",
				"DELETE FROM MatriculaGrupo",
				"DELETE FROM MatriculaCurso",
				"DELETE FROM Matricula",
				"DELETE FROM Acta",
				"DELETE FROM Convocatoria",
				"DELETE FROM Imparte",
				"DELETE FROM Grupo",
				"DELETE FROM PropuestaCurso",
				"DELETE FROM GradoCurso",
				"DELETE FROM Curso",
				"DELETE FROM Grado",
				"DELETE FROM AnioAcademico",
				"DELETE FROM Profesor",
				"DELETE FROM Alumno",
				"INSERT INTO AnioAcademico VALUES ('2025-2026','2025-09-08','2026-07-31','2025-07-01','2025-09-30')",
				"INSERT INTO AnioAcademico VALUES ('2026-2027','2026-09-07','2027-07-30','2026-07-01','2026-09-30')",
				// Un grado de cada estado y un doble grado
				"INSERT INTO Grado VALUES ('G001','Ingenieria Informatica del Software','Escuela de Ingenieria Informatica','Ingenieria y Arquitectura','VIGENTE','2024-09-01')",
				"INSERT INTO Grado VALUES ('G002','Ingenieria Informatica en Tecnologias de la Informacion','Escuela Politecnica de Ingenieria de Gijon','Ingenieria y Arquitectura','VIGENTE','2025-09-01')",
				"INSERT INTO Grado VALUES ('G003','Matematicas','Facultad de Ciencias','Ciencias','EN_EXTINCION','2021-09-01')",
				"INSERT INTO Grado VALUES ('G004','Ciencia e Ingenieria de Datos','Escuela de Ingenieria Informatica','Ingenieria y Arquitectura','PENDIENTE_RENOVACION','2022-09-01')",
				"INSERT INTO Grado VALUES ('G005','Quimica','Facultad de Quimica','Ciencias','EXTINTO','2018-09-01')",
				"INSERT INTO Grado VALUES ('G006','Doble Grado en Ingenieria Informatica del Software y Matematicas','Escuela de Ingenieria Informatica','Ingenieria y Arquitectura','VIGENTE','2024-09-01')",
				"INSERT INTO Profesor VALUES ('P001','10000001A','Ana','Garcia Lopez','agarcia@uni.es')",
				"INSERT INTO Profesor VALUES ('P002','10000002B','Luis','Fernandez Diaz','lfernandez@uni.es')",
				"INSERT INTO Profesor VALUES ('P003','10000003C','Marta','Suarez Alvarez','msuarez@uni.es')",
				"INSERT INTO Profesor VALUES ('P004','10000004D','Pablo','Menendez Rodriguez','pmenendez@uni.es')",
				"INSERT INTO Profesor VALUES ('P005','10000005E','Elena','Gonzalez Perez','egonzalez@uni.es')",
				"INSERT INTO Profesor VALUES ('P006','10000006F','Jorge','Alonso Iglesias','jalonso@uni.es')",
				"INSERT INTO Profesor VALUES ('P007','10000007G','Carmen','Martinez Fernandez','cmartinez@uni.es')",
				"INSERT INTO Profesor VALUES ('P008','10000008H','David','Lopez Sanchez','dlopez@uni.es')",
				"INSERT INTO Curso VALUES ('C001','P001','Ingenieria del Proceso Software',6)",
				"INSERT INTO Curso VALUES ('C002','P002','Sistemas de Informacion',6)",
				"INSERT INTO Curso VALUES ('C003','P003','Calidad, Validacion y Verificacion',6)",
				"INSERT INTO Curso VALUES ('C004','P001','Arquitectura del Software',6)",
				"INSERT INTO Curso VALUES ('C005','P004','Fundamentos de Informatica',6)",
				"INSERT INTO Curso VALUES ('C006','P005','Calculo',6)",
				"INSERT INTO Curso VALUES ('C007','P006','Redes de Computadores',6)",
				"INSERT INTO Curso VALUES ('C008','P007','Administracion de Sistemas',6)",
				"INSERT INTO Curso VALUES ('C009','P008','Algebra Lineal',6)",
				"INSERT INTO Curso VALUES ('C010','P008','Topologia',6)",
				"INSERT INTO Curso VALUES ('C011','P002','Aprendizaje Automatico',6)",
				// Sin coordinador ni grado todavia
				"INSERT INTO Curso VALUES ('C012',NULL,'Desarrollo de Videojuegos',4.5)",
				"INSERT INTO Curso VALUES ('C013','P003','Estructuras de Datos',6)",
				// G001 (Software)
				"INSERT INTO GradoCurso VALUES ('G001','C005',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G001','C006',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G001','C013',2,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G001','C001',3,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G001','C002',3,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G001','C003',3,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G001','C004',4,'Ingenieria del Software','ACTIVO')",
				// G002 (Tecnologias de la Informacion): Fundamentos de Informatica compartida con G001
				"INSERT INTO GradoCurso VALUES ('G002','C005',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G002','C007',2,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G002','C008',3,'Sistemas','ACTIVO')",
				// G003 (en extincion): todo inactivo, aunque Calculo y Algebra siguen activas en otros grados
				"INSERT INTO GradoCurso VALUES ('G003','C006',1,NULL,'INACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G003','C009',1,NULL,'INACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G003','C010',3,NULL,'INACTIVO')",
				// G004 (pendiente de renovacion)
				"INSERT INTO GradoCurso VALUES ('G004','C011',3,NULL,'ACTIVO')",
				// G006 (doble grado): asignaturas compartidas con G001 y G003
				"INSERT INTO GradoCurso VALUES ('G006','C005',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G006','C006',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G006','C009',1,NULL,'ACTIVO')",
				"INSERT INTO PropuestaCurso VALUES ('PR001','P001','G001','Ingenieria de Requisitos Avanzada',4,6,'Tecnicas avanzadas de elicitacion y especificacion','2026-05-10','PENDIENTE')",
				"INSERT INTO PropuestaCurso VALUES ('PR002','P006','G002','Ciberseguridad en Redes',4,6,'Seguridad perimetral y analisis de trafico','2026-03-15','ACEPTADA')",
				"INSERT INTO PropuestaCurso VALUES ('PR003','P004','G001','Programacion Competitiva',2,4.5,'Algoritmia orientada a concursos','2026-02-01','RECHAZADA')",
				// Capacidades: TE 80, PA 40, PL 20
				"INSERT INTO Grupo VALUES ('C001','TE1','TE',80)",
				"INSERT INTO Grupo VALUES ('C001','PA1','PA',40)",
				"INSERT INTO Grupo VALUES ('C001','PL1','PL',20)",
				"INSERT INTO Grupo VALUES ('C001','PL2','PL',20)",
				"INSERT INTO Grupo VALUES ('C002','TE1','TE',80)",
				"INSERT INTO Grupo VALUES ('C002','PA1','PA',40)",
				"INSERT INTO Grupo VALUES ('C002','PL1','PL',20)",
				"INSERT INTO Grupo VALUES ('C003','TE1','TE',80)",
				"INSERT INTO Grupo VALUES ('C003','PL1','PL',20)",
				"INSERT INTO Grupo VALUES ('C004','TE1','TE',80)",
				"INSERT INTO Grupo VALUES ('C004','PL1','PL',20)",
				"INSERT INTO Grupo VALUES ('C005','TE1','TE',80)",
				"INSERT INTO Grupo VALUES ('C005','PA1','PA',40)",
				"INSERT INTO Grupo VALUES ('C005','PL1','PL',20)",
				"INSERT INTO Grupo VALUES ('C005','PL2','PL',20)",
				"INSERT INTO Grupo VALUES ('C006','TE1','TE',80)",
				"INSERT INTO Grupo VALUES ('C006','PA1','PA',40)",
				"INSERT INTO Grupo VALUES ('C007','TE1','TE',80)",
				"INSERT INTO Grupo VALUES ('C007','PL1','PL',20)",
				"INSERT INTO Grupo VALUES ('C008','TE1','TE',80)",
				"INSERT INTO Grupo VALUES ('C009','TE1','TE',80)",
				"INSERT INTO Grupo VALUES ('C011','TE1','TE',80)",
				"INSERT INTO Grupo VALUES ('C011','PL1','PL',20)",
				"INSERT INTO Grupo VALUES ('C013','TE1','TE',80)",
				"INSERT INTO Grupo VALUES ('C013','PL1','PL',20)",
				// El coordinador de cada curso tambien imparte alguno de sus grupos
				"INSERT INTO Imparte VALUES ('P001','C001','TE1')",
				"INSERT INTO Imparte VALUES ('P001','C001','PL1')",
				"INSERT INTO Imparte VALUES ('P003','C001','PA1')",
				"INSERT INTO Imparte VALUES ('P003','C001','PL2')",
				"INSERT INTO Imparte VALUES ('P002','C002','TE1')",
				"INSERT INTO Imparte VALUES ('P002','C002','PA1')",
				"INSERT INTO Imparte VALUES ('P004','C002','PL1')",
				"INSERT INTO Imparte VALUES ('P003','C003','TE1')",
				"INSERT INTO Imparte VALUES ('P003','C003','PL1')",
				"INSERT INTO Imparte VALUES ('P001','C004','TE1')",
				"INSERT INTO Imparte VALUES ('P002','C004','PL1')",
				"INSERT INTO Imparte VALUES ('P004','C005','TE1')",
				"INSERT INTO Imparte VALUES ('P004','C005','PL1')",
				"INSERT INTO Imparte VALUES ('P006','C005','PA1')",
				"INSERT INTO Imparte VALUES ('P006','C005','PL2')",
				"INSERT INTO Imparte VALUES ('P005','C006','TE1')",
				"INSERT INTO Imparte VALUES ('P005','C006','PA1')",
				"INSERT INTO Imparte VALUES ('P006','C007','TE1')",
				"INSERT INTO Imparte VALUES ('P007','C007','PL1')",
				"INSERT INTO Imparte VALUES ('P007','C008','TE1')",
				"INSERT INTO Imparte VALUES ('P008','C009','TE1')",
				"INSERT INTO Imparte VALUES ('P002','C011','TE1')",
				"INSERT INTO Imparte VALUES ('P008','C011','PL1')",
				"INSERT INTO Imparte VALUES ('P003','C013','TE1')",
				"INSERT INTO Imparte VALUES ('P004','C013','PL1')",
				"INSERT INTO Convocatoria VALUES ('2025-2026','ORDINARIA_ENERO','2026-01-08','2026-01-31')",
				"INSERT INTO Convocatoria VALUES ('2025-2026','ORDINARIA_JUNIO','2026-05-25','2026-06-15')",
				"INSERT INTO Convocatoria VALUES ('2025-2026','EXTRAORDINARIA_JULIO','2026-06-29','2026-07-17')",
				"INSERT INTO Convocatoria VALUES ('2026-2027','ORDINARIA_ENERO','2027-01-07','2027-01-29')",
				"INSERT INTO Convocatoria VALUES ('2026-2027','ORDINARIA_JUNIO','2027-05-24','2027-06-14')",
				"INSERT INTO Convocatoria VALUES ('2026-2027','EXTRAORDINARIA_JULIO','2027-06-28','2027-07-16')",
				"INSERT INTO Alumno VALUES ('AL001','20000001A','Lucia','Rodriguez Garcia','lrodriguez@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL002','20000002B','Hugo','Fernandez Martinez','hfernandez@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL003','20000003C','Sara','Gonzalez Alvarez','sgonzalez@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL004','20000004D','Mario','Diaz Suarez','mdiaz@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL005','20000005E','Paula','Lopez Menendez','plopez@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL006','20000006F','Daniel','Perez Iglesias','dperez@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL007','20000007G','Laura','Sanchez Fernandez','lsanchez@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL008','20000008H','Alvaro','Martin Garcia','amartin@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL009','20000009J','Irene','Alonso Diaz','ialonso@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL010','20000010K','Pablo','Gutierrez Lopez','pgutierrez@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL011','20000011L','Noelia','Castro Rodriguez','ncastro@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL012','20000012M','Ivan','Blanco Perez','iblanco@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL013','20000013N','Marina','Vega Ruiz','mvega@alumno.es')",
				// =========================
				// MATRICULAS 2025-2026 (anio cerrado)
				// =========================
				"INSERT INTO Matricula VALUES ('M001','AL001','G001','2025-2026','2025-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M002','AL002','G001','2025-2026','2025-07-15','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M003','AL003','G001','2025-2026','2025-07-03','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M004','AL004','G002','2025-2026','2025-07-20','FINALIZADA',NULL,NULL)",
				// AL005 nunca pago: cancelada por impago (el anio cuenta, pero no se le exigen los 12 creditos)
				"INSERT INTO Matricula VALUES ('M005','AL005','G001','2025-2026','2025-09-15','CANCELADA','IMPAGO','2025-10-15')",
				"INSERT INTO MatriculaCurso VALUES ('M001','G001','C005','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M001','G001','C006','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M002','G001','C005','FINALIZADA',NULL,NULL,120,'TRANSFERENCIA','2025-07-16',1)",
				"INSERT INTO MatriculaCurso VALUES ('M002','G001','C006','FINALIZADA',NULL,NULL,120,'TRANSFERENCIA','2025-07-16',1)",
				"INSERT INTO MatriculaCurso VALUES ('M003','G001','C001','FINALIZADA',NULL,NULL,120,'CARTA_PAGO','2025-07-08',1)",
				"INSERT INTO MatriculaCurso VALUES ('M003','G001','C002','FINALIZADA',NULL,NULL,120,'CARTA_PAGO','2025-07-08',1)",
				"INSERT INTO MatriculaCurso VALUES ('M003','G001','C003','FINALIZADA',NULL,NULL,120,'CARTA_PAGO','2025-07-08',1)",
				"INSERT INTO MatriculaCurso VALUES ('M004','G002','C005','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-20',1)",
				"INSERT INTO MatriculaCurso VALUES ('M004','G002','C007','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-20',1)",
				"INSERT INTO MatriculaCurso VALUES ('M005','G001','C005','CANCELADA','IMPAGO','2025-10-15',120,NULL,NULL,0)",
				"INSERT INTO MatriculaGrupo VALUES ('M001','C005','TE','TE1')",
				"INSERT INTO MatriculaGrupo VALUES ('M001','C005','PA','PA1')",
				"INSERT INTO MatriculaGrupo VALUES ('M001','C005','PL','PL1')",
				"INSERT INTO MatriculaGrupo VALUES ('M001','C006','TE','TE1')",
				"INSERT INTO MatriculaGrupo VALUES ('M001','C006','PA','PA1')",
				"INSERT INTO MatriculaGrupo VALUES ('M002','C005','TE','TE1')",
				"INSERT INTO MatriculaGrupo VALUES ('M002','C005','PA','PA1')",
				"INSERT INTO MatriculaGrupo VALUES ('M002','C005','PL','PL2')",
				"INSERT INTO MatriculaGrupo VALUES ('M002','C006','TE','TE1')",
				"INSERT INTO MatriculaGrupo VALUES ('M002','C006','PA','PA1')",
				"INSERT INTO MatriculaGrupo VALUES ('M003','C001','TE','TE1')",
				"INSERT INTO MatriculaGrupo VALUES ('M003','C001','PA','PA1')",
				"INSERT INTO MatriculaGrupo VALUES ('M003','C001','PL','PL1')",
				"INSERT INTO MatriculaGrupo VALUES ('M003','C002','TE','TE1')",
				"INSERT INTO MatriculaGrupo VALUES ('M003','C002','PL','PL1')",
				"INSERT INTO MatriculaGrupo VALUES ('M003','C003','TE','TE1')",
				"INSERT INTO MatriculaGrupo VALUES ('M003','C003','PL','PL1')",
				// AL004 (G002) comparte grupos de Fundamentos con los alumnos de G001
				"INSERT INTO MatriculaGrupo VALUES ('M004','C005','TE','TE1')",
				"INSERT INTO MatriculaGrupo VALUES ('M004','C005','PL','PL2')",
				"INSERT INTO MatriculaGrupo VALUES ('M004','C007','TE','TE1')",
				"INSERT INTO MatriculaGrupo VALUES ('M004','C007','PL','PL1')",
				// Actas 2025-2026: todas cerradas, una por el sistema
				"INSERT INTO Acta VALUES ('C005','2025-2026','ORDINARIA_ENERO','CERRADA','2026-02-05',0)",
				"INSERT INTO Acta VALUES ('C005','2025-2026','EXTRAORDINARIA_JULIO','CERRADA','2026-07-17',0)",
				"INSERT INTO Acta VALUES ('C006','2025-2026','ORDINARIA_JUNIO','CERRADA','2026-06-20',0)",
				"INSERT INTO Acta VALUES ('C006','2025-2026','EXTRAORDINARIA_JULIO','CERRADA','2026-07-17',1)",
				"INSERT INTO Acta VALUES ('C001','2025-2026','ORDINARIA_JUNIO','CERRADA','2026-06-18',0)",
				"INSERT INTO Acta VALUES ('C002','2025-2026','ORDINARIA_ENERO','CERRADA','2026-02-03',0)",
				"INSERT INTO Acta VALUES ('C003','2025-2026','ORDINARIA_JUNIO','CERRADA','2026-06-19',0)",
				"INSERT INTO Acta VALUES ('C003','2025-2026','EXTRAORDINARIA_JULIO','CERRADA','2026-07-15',0)",
				"INSERT INTO Acta VALUES ('C007','2025-2026','ORDINARIA_ENERO','CERRADA','2026-02-04',0)",
				// AL001 supera 12 creditos (Calculo en extraordinaria): cumple permanencia
				"INSERT INTO Calificacion VALUES ('M001','C005','2025-2026','ORDINARIA_ENERO',7.5)",
				"INSERT INTO Calificacion VALUES ('M001','C006','2025-2026','ORDINARIA_JUNIO',4.0)",
				"INSERT INTO Calificacion VALUES ('M001','C006','2025-2026','EXTRAORDINARIA_JULIO',6.0)",
				// AL002 supera 0 creditos: no puede volver a matricularse en G001 (se cambia a G002)
				// Fundamentos: 2 convocatorias consumidas. Calculo: 1 (la de junio fue no presentado)
				"INSERT INTO Calificacion VALUES ('M002','C005','2025-2026','ORDINARIA_ENERO',3.5)",
				"INSERT INTO Calificacion VALUES ('M002','C005','2025-2026','EXTRAORDINARIA_JULIO',4.0)",
				"INSERT INTO Calificacion VALUES ('M002','C006','2025-2026','ORDINARIA_JUNIO',NULL)",
				"INSERT INTO Calificacion VALUES ('M002','C006','2025-2026','EXTRAORDINARIA_JULIO',4.5)",
				// AL003 supera 18 creditos
				"INSERT INTO Calificacion VALUES ('M003','C001','2025-2026','ORDINARIA_JUNIO',8.5)",
				"INSERT INTO Calificacion VALUES ('M003','C002','2025-2026','ORDINARIA_ENERO',6.0)",
				"INSERT INTO Calificacion VALUES ('M003','C003','2025-2026','ORDINARIA_JUNIO',4.0)",
				"INSERT INTO Calificacion VALUES ('M003','C003','2025-2026','EXTRAORDINARIA_JULIO',5.5)",
				// AL004 supera 12 creditos
				"INSERT INTO Calificacion VALUES ('M004','C005','2025-2026','ORDINARIA_ENERO',6.0)",
				"INSERT INTO Calificacion VALUES ('M004','C007','2025-2026','ORDINARIA_ENERO',9.0)",
				// =========================
				// MATRICULAS 2026-2027 (anio en curso)
				// =========================
				"INSERT INTO Matricula VALUES ('M006','AL001','G001','2026-2027','2026-07-05','ACTIVA',NULL,NULL)",
				// AL002 cambia de grado; en Fundamentos arrastra 2 convocatorias de G001
				"INSERT INTO Matricula VALUES ('M007','AL002','G002','2026-2027','2026-07-08','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M008','AL003','G001','2026-2027','2026-07-02','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M009','AL004','G002','2026-2027','2026-07-21','ACTIVA',NULL,NULL)",
				// Transferencias pendientes de validar por el secretario
				"INSERT INTO Matricula VALUES ('M010','AL006','G001','2026-2027','2026-09-25','SOLICITADA',NULL,NULL)",
				// Una asignatura activa y otra sin pagar, pendiente de revision del secretario
				"INSERT INTO Matricula VALUES ('M011','AL007','G001','2026-2027','2026-07-28','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M012','AL008','G004','2026-2027','2026-07-20','ACTIVA',NULL,NULL)",
				// Cancelada entera por fuerza mayor (pagada, sin devolucion)
				"INSERT INTO Matricula VALUES ('M013','AL009','G001','2026-2027','2026-07-12','CANCELADA','FUERZA_MAYOR','2026-10-01')",
				// Cancelada al cancelar su unica asignatura
				"INSERT INTO Matricula VALUES ('M014','AL010','G002','2026-2027','2026-08-01','CANCELADA','INCOMPATIBILIDAD','2026-09-15')",
				// Pagada despues de iniciar el anio: pendiente de activar
				"INSERT INTO Matricula VALUES ('M015','AL011','G001','2026-2027','2026-09-29','SOLICITADA',NULL,NULL)",
				// Alumna de doble grado
				"INSERT INTO Matricula VALUES ('M016','AL013','G006','2026-2027','2026-07-15','ACTIVA',NULL,NULL)",
				// AL012 no tiene matriculas (perfil vacio)
				// AL001: una activa y otra cancelada a mitad (desmatricula parcial, queda registrada)
				"INSERT INTO MatriculaCurso VALUES ('M006','G001','C013','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-05',1)",
				"INSERT INTO MatriculaCurso VALUES ('M006','G001','C001','CANCELADA','INCOMPATIBILIDAD','2026-09-20',120,'TARJETA','2026-07-05',1)",
				"INSERT INTO MatriculaCurso VALUES ('M007','G002','C005','ACTIVA',NULL,NULL,120,'TRANSFERENCIA','2026-07-09',1)",
				"INSERT INTO MatriculaCurso VALUES ('M007','G002','C007','ACTIVA',NULL,NULL,120,'TRANSFERENCIA','2026-07-09',1)",
				"INSERT INTO MatriculaCurso VALUES ('M008','G001','C004','ACTIVA',NULL,NULL,120,'CARTA_PAGO','2026-07-06',1)",
				"INSERT INTO MatriculaCurso VALUES ('M009','G002','C008','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-21',1)",
				"INSERT INTO MatriculaCurso VALUES ('M010','G001','C005','SOLICITADA',NULL,NULL,120,'TRANSFERENCIA','2026-09-26',0)",
				"INSERT INTO MatriculaCurso VALUES ('M010','G001','C006','SOLICITADA',NULL,NULL,120,'TRANSFERENCIA','2026-09-26',0)",
				"INSERT INTO MatriculaCurso VALUES ('M011','G001','C005','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-28',1)",
				"INSERT INTO MatriculaCurso VALUES ('M011','G001','C006','SOLICITADA',NULL,NULL,120,NULL,NULL,0)",
				"INSERT INTO MatriculaCurso VALUES ('M012','G004','C011','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-20',1)",
				"INSERT INTO MatriculaCurso VALUES ('M013','G001','C005','CANCELADA','FUERZA_MAYOR','2026-10-01',120,'TARJETA','2026-07-12',1)",
				"INSERT INTO MatriculaCurso VALUES ('M013','G001','C006','CANCELADA','FUERZA_MAYOR','2026-10-01',120,'TARJETA','2026-07-12',1)",
				"INSERT INTO MatriculaCurso VALUES ('M014','G002','C007','CANCELADA','INCOMPATIBILIDAD','2026-09-15',120,'TARJETA','2026-08-01',1)",
				"INSERT INTO MatriculaCurso VALUES ('M015','G001','C005','PAGADA',NULL,NULL,120,'TARJETA','2026-09-29',1)",
				"INSERT INTO MatriculaCurso VALUES ('M016','G006','C005','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-15',1)",
				"INSERT INTO MatriculaCurso VALUES ('M016','G006','C009','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-15',1)",
				"INSERT INTO MatriculaGrupo VALUES ('M006','C013','TE','TE1')",
				"INSERT INTO MatriculaGrupo VALUES ('M006','C013','PL','PL1')",
				"INSERT INTO MatriculaGrupo VALUES ('M007','C005','TE','TE1')",
				"INSERT INTO MatriculaGrupo VALUES ('M007','C005','PA','PA1')",
				"INSERT INTO MatriculaGrupo VALUES ('M007','C005','PL','PL1')",
				"INSERT INTO MatriculaGrupo VALUES ('M007','C007','TE','TE1')",
				"INSERT INTO MatriculaGrupo VALUES ('M007','C007','PL','PL1')",
				"INSERT INTO MatriculaGrupo VALUES ('M008','C004','TE','TE1')",
				"INSERT INTO MatriculaGrupo VALUES ('M008','C004','PL','PL1')",
				"INSERT INTO MatriculaGrupo VALUES ('M009','C008','TE','TE1')",
				// AL007 solo tiene TE en Fundamentos: le faltan PA y PL (pendiente del secretario)
				"INSERT INTO MatriculaGrupo VALUES ('M011','C005','TE','TE1')",
				"INSERT INTO MatriculaGrupo VALUES ('M012','C011','TE','TE1')",
				"INSERT INTO MatriculaGrupo VALUES ('M012','C011','PL','PL1')",
				// AL013 (doble grado) comparte grupos con alumnos de G001 y G002
				"INSERT INTO MatriculaGrupo VALUES ('M016','C005','TE','TE1')",
				"INSERT INTO MatriculaGrupo VALUES ('M016','C005','PA','PA1')",
				"INSERT INTO MatriculaGrupo VALUES ('M016','C005','PL','PL2')",
				"INSERT INTO MatriculaGrupo VALUES ('M016','C009','TE','TE1')",
				// M015 (pagada, sin activar) todavia no tiene grupos
				// Actas 2026-2027: abiertas, todavia sin notas
				"INSERT INTO Acta VALUES ('C005','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0)",
				"INSERT INTO Acta VALUES ('C006','2026-2027','ORDINARIA_JUNIO','ABIERTA',NULL,0)",
				"INSERT INTO Acta VALUES ('C004','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0)",
				"INSERT INTO Acta VALUES ('C007','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0)",
				"INSERT INTO Acta VALUES ('C008','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0)",
				"INSERT INTO Acta VALUES ('C009','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0)",
				"INSERT INTO Acta VALUES ('C011','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0)",
				"INSERT INTO Acta VALUES ('C013','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0)"
		});
	}

	private String estadoMatricula(String matriculaId) {
		return db.executeQueryArray("SELECT estado FROM Matricula WHERE matriculaId = ?", matriculaId).get(0)[0].toString();
	}

	// =========================================================================
	// HU 35226: consulta
	// =========================================================================

	@Test
	void listadoIncluyeTodosLosCursosMatriculados() {
		List<MatriculaSecretariaDTO> lista = model.getMatriculas();
		Number total = (Number) db.executeQueryArray("SELECT count(*) FROM MatriculaCurso").get(0)[0];
		assertEquals(total.intValue(), lista.size());
		// Ordenado del año más reciente al más antiguo
		assertEquals("2026-2027", lista.get(0).getAnioAcademicoId());
		assertEquals("2025-2026", lista.get(lista.size() - 1).getAnioAcademicoId());
	}

	@Test
	void detalleMuestraEstudianteGradoCursoGruposYEstado() {
		MatriculaSecretariaDTO m = model.getMatricula("M007", "C005");
		assertEquals("20000002B", m.getDni());
		assertEquals("Ingenieria Informatica en Tecnologias de la Informacion", m.getGrado());
		assertEquals("Fundamentos de Informatica", m.getCurso());
		assertEquals("ACTIVA", m.getEstado());
		assertTrue(m.getGrupo().contains("TE1"));
		assertTrue(m.getGrupo().contains("PA1"));
		assertTrue(m.getGrupo().contains("PL1"));
		assertEquals(120.0, m.getImporte(), 0.001);
	}

	@Test
	void cursoSinGruposSeMuestraSinAsignar() {
		assertEquals("Sin asignar", model.getMatricula("M015", "C005").getGrupo());
	}

	@Test
	void detalleDeMatriculaInexistenteFalla() {
		assertThrows(ResourceNotFoundException.class, () -> model.getMatricula("M999", "C005"));
	}

	// =========================================================================
	// HU 35226: consulta de grupos
	// =========================================================================

	@Test
	void aniosAcademicosOrdenadosYAnioActual() {
		assertEquals(List.of("2026-2027", "2025-2026"), model.getAniosAcademicos());
		assertEquals("2026-2027", model.getAnioAcademicoActual(CURSO_INICIADO));
		assertEquals("2025-2026", model.getAnioAcademicoActual(ANTES_INICIO));
	}

	@Test
	void gruposMuestranGradosProfesoresYOcupacionDelAnio() {
		List<GrupoConsultaDTO> grupos = model.getGruposConsulta("2026-2027");
		assertEquals(((Number) db.executeQueryArray("SELECT count(*) FROM Grupo").get(0)[0]).intValue(), grupos.size());
		GrupoConsultaDTO te1 = grupos.stream()
				.filter(g -> "C005".equals(g.getCursoId()) && "TE1".equals(g.getGrupoCodigo())).findFirst().get();
		assertEquals("Fundamentos de Informatica", te1.getCurso());
		assertEquals("TE", te1.getTipo());
		// Fundamentos está en Software, Tecnologías de la Información y el doble grado
		assertEquals(3, te1.getGrados().split("; ").length);
		assertTrue(te1.getProfesores().contains("Pablo"));
		// En 2026-2027 ocupan plaza M007, M011 y M016
		assertEquals(3, te1.getOcupadas());
		assertEquals(te1.getCapacidad() - 3, te1.getPlazasLibres());
		// En 2025-2026 la ocupación se cuenta aparte (M001, M002 y M004 ya están FINALIZADAS)
		GrupoConsultaDTO te1Anterior = model.getGruposConsulta("2025-2026").stream()
				.filter(g -> "C005".equals(g.getCursoId()) && "TE1".equals(g.getGrupoCodigo())).findFirst().get();
		assertEquals(0, te1Anterior.getOcupadas());
	}

	@Test
	void alumnosDeUnGrupoEnUnAnio() {
		List<MatriculaSecretariaDTO> alumnos = model.getMatriculasGrupo("C005", "TE1", "2026-2027");
		assertEquals(3, alumnos.size());
		assertTrue(alumnos.stream().anyMatch(m -> "M007".equals(m.getMatriculaId())));
		assertTrue(alumnos.stream().anyMatch(m -> "M011".equals(m.getMatriculaId())));
		assertTrue(alumnos.stream().anyMatch(m -> "M016".equals(m.getMatriculaId())));
		assertEquals(3, model.getMatriculasGrupo("C005", "TE1", "2025-2026").size());
		assertTrue(model.getMatriculasGrupo("C005", "PL2", "2025-2026").stream()
				.allMatch(m -> "2025-2026".equals(m.getAnioAcademicoId())));
	}

	// =========================================================================
	// HU 35229: activación al iniciar el curso
	// =========================================================================

	@Test
	void pagadaPasaAActivaAlIniciarElCurso() {
		assertEquals(1, model.activarMatriculasIniciadas(CURSO_INICIADO));
		assertEquals("ACTIVA", model.getMatricula("M015", "C005").getEstado());
		assertEquals("ACTIVA", estadoMatricula("M015"));
		// Las solicitadas sin pagar no cambian
		assertEquals("SOLICITADA", model.getMatricula("M010", "C005").getEstado());
		assertEquals("SOLICITADA", estadoMatricula("M010"));
	}

	@Test
	void pagadaNoSeActivaAntesDeIniciarElCurso() {
		assertEquals(0, model.activarMatriculasIniciadas(ANTES_INICIO));
		assertEquals("PAGADA", model.getMatricula("M015", "C005").getEstado());
		assertEquals("SOLICITADA", estadoMatricula("M015"));
	}

	// =========================================================================
	// HU 35229: aceptar solicitudes
	// =========================================================================

	@Test
	void aceptarAntesDeIniciarElCursoQuedaPagada() {
		assertEquals("PAGADA", model.aceptarSolicitud("M010", "C005", null, ANTES_INICIO));
		MatriculaSecretariaDTO m = model.getMatricula("M010", "C005");
		assertEquals(1, m.getPagoValidado());
		assertEquals("TRANSFERENCIA", m.getMetodoPago());
		assertEquals("SOLICITADA", estadoMatricula("M010"));
	}

	@Test
	void aceptarConElCursoIniciadoQuedaActiva() {
		assertEquals("ACTIVA", model.aceptarSolicitud("M010", "C006", null, CURSO_INICIADO));
		assertEquals("ACTIVA", estadoMatricula("M010"));
		// Solo cambia el curso aceptado
		assertEquals("SOLICITADA", model.getMatricula("M010", "C005").getEstado());
	}

	@Test
	void aceptarSinMetodoDePagoExigeIndicarlo() {
		assertThrows(ValidationException.class, () -> model.aceptarSolicitud("M011", "C006", null, CURSO_INICIADO));
		assertEquals("ACTIVA", model.aceptarSolicitud("M011", "C006", "TARJETA", CURSO_INICIADO));
		MatriculaSecretariaDTO m = model.getMatricula("M011", "C006");
		assertEquals("TARJETA", m.getMetodoPago());
		assertEquals(CURSO_INICIADO, m.getFechaPago());
		// El otro curso de la matrícula, que ya estaba activo, no cambia
		assertEquals("ACTIVA", model.getMatricula("M011", "C005").getEstado());
	}

	@Test
	void soloSeAceptanSolicitudes() {
		assertThrows(ValidationException.class, () -> model.aceptarSolicitud("M007", "C005", "TARJETA", CURSO_INICIADO));
		assertThrows(ValidationException.class, () -> model.aceptarSolicitud("M011", "C006", "BIZUM", CURSO_INICIADO));
	}

	// =========================================================================
	// HU 35229: modificar
	// =========================================================================

	@Test
	void modificarActualizaMetodoDePagoYEmail() {
		String fechaPagoAntes = model.getMatricula("M010", "C005").getFechaPago();
		model.modificarMatricula("M010", "C005", "CARTA_PAGO", "nuevo@alumno.es", CURSO_INICIADO);
		MatriculaSecretariaDTO m = model.getMatricula("M010", "C005");
		assertEquals("CARTA_PAGO", m.getMetodoPago());
		assertEquals("nuevo@alumno.es", m.getEmail());
		// Ya tenía fecha de pago: se conserva
		assertEquals(fechaPagoAntes, m.getFechaPago());
	}

	@Test
	void modificarSinMetodoMantieneElActual() {
		model.modificarMatricula("M007", "C005", null, "otro@alumno.es", CURSO_INICIADO);
		assertEquals("TRANSFERENCIA", model.getMatricula("M007", "C005").getMetodoPago());
	}

	@Test
	void modificarRegistraElPagoPendienteDeValidar() {
		// AL007 no había pagado C006: al registrar el método queda con fecha y pendiente de validar
		model.modificarMatricula("M011", "C006", "TRANSFERENCIA", "lsanchez2@alumno.es", CURSO_INICIADO);
		MatriculaSecretariaDTO m = model.getMatricula("M011", "C006");
		assertEquals("SOLICITADA", m.getEstado());
		assertEquals("TRANSFERENCIA", m.getMetodoPago());
		assertEquals(CURSO_INICIADO, m.getFechaPago());
		assertTrue(m.isPagoPendienteValidar());
	}

	@Test
	void modificarNoCambiaElMetodoDeUnPagoValidado() {
		// M007 pagó por transferencia y el pago está validado
		assertThrows(ValidationException.class,
				() -> model.modificarMatricula("M007", "C005", "TARJETA", "otro@alumno.es", CURSO_INICIADO));
		// Indicar el mismo método sí está permitido
		model.modificarMatricula("M007", "C005", "TRANSFERENCIA", "otro@alumno.es", CURSO_INICIADO);
		assertEquals("TRANSFERENCIA", model.getMatricula("M007", "C005").getMetodoPago());
	}

	@Test
	void modificarValidaLosDatos() {
		assertThrows(ValidationException.class,
				() -> model.modificarMatricula("M010", "C005", null, "sin-arroba", CURSO_INICIADO));
		// El email ya pertenece a otro estudiante (AL001)
		String emailOtro = db.executeQueryArray("SELECT email FROM Alumno WHERE alumnoId = 'AL001'").get(0)[0].toString();
		assertThrows(ValidationException.class,
				() -> model.modificarMatricula("M010", "C005", null, emailOtro, CURSO_INICIADO));
		// Matrícula cancelada
		assertThrows(ValidationException.class,
				() -> model.modificarMatricula("M013", "C005", null, "x@alumno.es", CURSO_INICIADO));
	}

	// =========================================================================
	// HU 35229: cancelar
	// =========================================================================

	@Test
	void cancelarUnCursoNoCancelaElResto() {
		model.cancelar("M010", "C005", "IMPAGO", CURSO_INICIADO, false);
		MatriculaSecretariaDTO m = model.getMatricula("M010", "C005");
		assertEquals("CANCELADA", m.getEstado());
		assertEquals("IMPAGO", m.getMotivoCancelacion());
		assertEquals(CURSO_INICIADO, m.getFechaCancelacion());
		assertEquals("SOLICITADA", model.getMatricula("M010", "C006").getEstado());
		assertEquals("SOLICITADA", estadoMatricula("M010"));
	}

	@Test
	void cancelarElUltimoCursoCancelaLaMatricula() {
		model.cancelar("M006", "C013", "FUERZA_MAYOR", CURSO_INICIADO, false);
		assertEquals("CANCELADA", estadoMatricula("M006"));
	}

	@Test
	void cancelarTodaLaMatriculaCancelaSusCursosYNoLaBorra() {
		model.cancelar("M016", "C005", "INCOMPATIBILIDAD", CURSO_INICIADO, true);
		assertEquals("CANCELADA", model.getMatricula("M016", "C005").getEstado());
		assertEquals("CANCELADA", model.getMatricula("M016", "C009").getEstado());
		Object[] fila = db.executeQueryArray(
				"SELECT estado, motivoCancelacion, fechaCancelacion FROM Matricula WHERE matriculaId = 'M016'").get(0);
		assertEquals("CANCELADA", fila[0]);
		assertEquals("INCOMPATIBILIDAD", fila[1]);
		assertEquals(CURSO_INICIADO, fila[2]);
	}

	// =========================================================================
	// HU 35229: finalización al terminar el año
	// =========================================================================

	@Test
	void activasPasanAFinalizadaAlTerminarElAnio() {
		// El 2026-2027 termina el 2027-07-30
		assertEquals(0, model.finalizarMatriculasTerminadas(CURSO_INICIADO));
		assertTrue(model.finalizarMatriculasTerminadas("2027-08-01") > 0);
		assertEquals("FINALIZADA", model.getMatricula("M007", "C005").getEstado());
		assertEquals("FINALIZADA", estadoMatricula("M007"));
		// Las canceladas y las que no llegaron a activarse no cambian
		assertEquals("CANCELADA", model.getMatricula("M006", "C001").getEstado());
		assertEquals("CANCELADA", estadoMatricula("M013"));
		assertEquals("SOLICITADA", model.getMatricula("M010", "C005").getEstado());
	}

	@Test
	void pagadaDeUnAnioYaTerminadoPasaAFinalizada() {
		// M015 pagó C005 pero la aplicación no se abrió hasta acabar el año: no puede quedarse PAGADA
		model.actualizarCicloVida(ANIO_TERMINADO);
		assertEquals("FINALIZADA", model.getMatricula("M015", "C005").getEstado());
		assertEquals("FINALIZADA", estadoMatricula("M015"));
		assertEquals("FINALIZADA", model.getMatricula("M007", "C005").getEstado());
	}

	@Test
	void actualizarCicloVidaActivaLasPagadasDelAnioEnCurso() {
		assertEquals(1, model.actualizarCicloVida(CURSO_INICIADO));
		assertEquals("ACTIVA", model.getMatricula("M015", "C005").getEstado());
		assertEquals("ACTIVA", estadoMatricula("M015"));
		// Ejecutarlo otra vez no cambia nada
		assertEquals(0, model.actualizarCicloVida(CURSO_INICIADO));
	}

	@Test
	void aceptarConElAnioTerminadoQuedaFinalizada() {
		assertEquals("FINALIZADA", model.aceptarSolicitud("M010", "C005", null, ANIO_TERMINADO));
		assertEquals(1, model.getMatricula("M010", "C005").getPagoValidado());
	}

	// =========================================================================
	// HU 35229: situaciones a revisar (consultas de base-de-datos.md)
	// =========================================================================

	@Test
	void detectaLasSituacionesQueRevisaElSecretario() {
		// AL006 pagó por transferencia: pendiente de validar
		assertTrue(model.getMatricula("M010", "C005").isPagoPendienteValidar());
		// AL007 tiene un curso sin pagar con el año iniciado
		assertTrue(model.getMatricula("M011", "C006").isSinPagarAnioIniciado(CURSO_INICIADO));
		assertTrue(!model.getMatricula("M011", "C006").isSinPagarAnioIniciado(ANTES_INICIO));
		// AL007 solo tiene TE en Fundamentos (le faltan PA y PL)
		MatriculaSecretariaDTO m011 = model.getMatricula("M011", "C005");
		assertTrue(m011.isFaltanGrupos());
		assertEquals(2, m011.getGruposPendientes());
		// M007 tiene todos sus grupos
		assertTrue(!model.getMatricula("M007", "C005").isFaltanGrupos());
	}

	// =========================================================================
	// HU 35229: asignación de grupos
	// =========================================================================

	private static Map<String, String> grupos(String... tipoYCodigo) {
		Map<String, String> map = new LinkedHashMap<>();
		for (int i = 0; i < tipoYCodigo.length; i += 2) {
			map.put(tipoYCodigo[i], tipoYCodigo[i + 1]);
		}
		return map;
	}

	@Test
	void gruposMuestranPlazasOcupadasDelAnio() {
		List<GrupoPlazasDTO> lista = model.getGrupos("M015", "C005");
		assertEquals(4, lista.size());
		GrupoPlazasDTO te1 = lista.stream().filter(g -> "TE1".equals(g.getGrupoCodigo())).findFirst().get();
		// En 2026-2027 están en TE1 de Fundamentos: M007, M011 y M016
		assertEquals(3, te1.getOcupadas());
		assertEquals(77, te1.getPlazasLibres());
		assertTrue(!te1.estaAsignado());
		// M007 sí está en TE1
		assertTrue(model.getGrupos("M007", "C005").stream()
				.anyMatch(g -> "TE1".equals(g.getGrupoCodigo()) && g.estaAsignado()));
	}

	@Test
	void asignarGruposCompletaLosQueFaltan() {
		model.asignarGrupos("M011", "C005", grupos("TE", "TE1", "PA", "PA1", "PL", "PL2"));
		MatriculaSecretariaDTO m = model.getMatricula("M011", "C005");
		assertEquals(0, m.getGruposPendientes());
		assertTrue(m.getGrupo().contains("PL2"));
	}

	@Test
	void asignarGruposPermiteCambiarDeGrupo() {
		model.asignarGrupos("M007", "C005", grupos("TE", "TE1", "PA", "PA1", "PL", "PL2"));
		String g = model.getMatricula("M007", "C005").getGrupo();
		assertTrue(g.contains("PL2"));
		assertTrue(!g.contains("PL1"));
	}

	@Test
	void noSePuedeSuperarLaCapacidadDelGrupo() {
		// PL1 de Fundamentos ya tiene a M007: con capacidad 1 está completo
		db.executeUpdate("UPDATE Grupo SET capacidad = 1 WHERE cursoId = 'C005' AND grupoCodigo = 'PL1'");
		model.activarMatriculasIniciadas(CURSO_INICIADO);
		assertThrows(ValidationException.class,
				() -> model.asignarGrupos("M015", "C005", grupos("TE", "TE1", "PA", "PA1", "PL", "PL1")));
		// Quien ya está en el grupo puede conservarlo
		model.asignarGrupos("M007", "C005", grupos("TE", "TE1", "PA", "PA1", "PL", "PL1"));
	}

	@Test
	void asignarGruposValidaEstadoTipoYQueEstenTodos() {
		// Solo a cursos PAGADOS o ACTIVOS
		assertThrows(ValidationException.class,
				() -> model.asignarGrupos("M010", "C005", grupos("TE", "TE1", "PA", "PA1", "PL", "PL1")));
		// Un grupo de cada tipo
		assertThrows(ValidationException.class, () -> model.asignarGrupos("M015", "C005", grupos("TE", "TE1")));
		// El grupo tiene que ser del tipo indicado
		assertThrows(ValidationException.class,
				() -> model.asignarGrupos("M015", "C005", grupos("TE", "PL1", "PA", "PA1", "PL", "PL2")));
		// El grupo tiene que existir en el curso
		assertThrows(ValidationException.class,
				() -> model.asignarGrupos("M015", "C005", grupos("TE", "TE9", "PA", "PA1", "PL", "PL2")));
	}

	@Test
	void cancelarValidaMotivoYEstado() {
		assertThrows(ValidationException.class, () -> model.cancelar("M010", "C005", "OTRO", CURSO_INICIADO, false));
		assertThrows(ValidationException.class, () -> model.cancelar("M013", "C005", "IMPAGO", CURSO_INICIADO, false));
		assertThrows(ValidationException.class, () -> model.cancelar("M001", "C005", "IMPAGO", CURSO_INICIADO, false));
		assertNull(model.getMatricula("M010", "C005").getMotivoCancelacion());
	}
}
