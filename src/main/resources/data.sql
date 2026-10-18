-- Datos de prueba IPS (SQLite). Ejecutar despues de schema.sql
-- Primero se vacian las tablas (de detalle a maestro) para poder recargar los datos sin recrear el esquema

DELETE FROM Calificacion;
DELETE FROM MatriculaGrupo;
DELETE FROM MatriculaCurso;
DELETE FROM Matricula;
DELETE FROM Acta;
DELETE FROM Convocatoria;
DELETE FROM Imparte;
DELETE FROM Grupo;
DELETE FROM PropuestaCurso;
DELETE FROM GradoCurso;
DELETE FROM Curso;
DELETE FROM Grado;
DELETE FROM AnioAcademico;
DELETE FROM Profesor;
DELETE FROM Alumno;



-- =========================
-- DATOS DE PRUEBA
-- Fecha de referencia: octubre de 2026 (anio 2026-2027 ya iniciado)
-- Precio: 20 euros/credito (6 creditos = 120 euros)
-- =========================

INSERT INTO AnioAcademico VALUES ('2025-2026','2025-09-08','2026-07-31','2025-07-01','2025-09-30');

INSERT INTO AnioAcademico VALUES ('2026-2027','2026-09-07','2027-07-30','2026-07-01','2026-09-30');



-- Un grado de cada estado y un doble grado
INSERT INTO Grado VALUES ('G001','Ingenieria Informatica del Software','Escuela de Ingenieria Informatica','Ingenieria y Arquitectura','VIGENTE','2024-09-01');

INSERT INTO Grado VALUES ('G002','Ingenieria Informatica en Tecnologias de la Informacion','Escuela Politecnica de Ingenieria de Gijon','Ingenieria y Arquitectura','VIGENTE','2025-09-01');

INSERT INTO Grado VALUES ('G003','Matematicas','Facultad de Ciencias','Ciencias','EN_EXTINCION','2021-09-01');

INSERT INTO Grado VALUES ('G004','Ciencia e Ingenieria de Datos','Escuela de Ingenieria Informatica','Ingenieria y Arquitectura','PENDIENTE_RENOVACION','2022-09-01');

INSERT INTO Grado VALUES ('G005','Quimica','Facultad de Quimica','Ciencias','EXTINTO','2018-09-01');

INSERT INTO Grado VALUES ('G006','Doble Grado en Ingenieria Informatica del Software y Matematicas','Escuela de Ingenieria Informatica','Ingenieria y Arquitectura','VIGENTE','2024-09-01');



INSERT INTO Profesor VALUES ('P001','10000001A','Ana','Garcia Lopez','agarcia@uni.es');

INSERT INTO Profesor VALUES ('P002','10000002B','Luis','Fernandez Diaz','lfernandez@uni.es');

INSERT INTO Profesor VALUES ('P003','10000003C','Marta','Suarez Alvarez','msuarez@uni.es');

INSERT INTO Profesor VALUES ('P004','10000004D','Pablo','Menendez Rodriguez','pmenendez@uni.es');

INSERT INTO Profesor VALUES ('P005','10000005E','Elena','Gonzalez Perez','egonzalez@uni.es');

INSERT INTO Profesor VALUES ('P006','10000006F','Jorge','Alonso Iglesias','jalonso@uni.es');

INSERT INTO Profesor VALUES ('P007','10000007G','Carmen','Martinez Fernandez','cmartinez@uni.es');

INSERT INTO Profesor VALUES ('P008','10000008H','David','Lopez Sanchez','dlopez@uni.es');



INSERT INTO Curso VALUES ('C001','P001','Ingenieria del Proceso Software',6);

INSERT INTO Curso VALUES ('C002','P002','Sistemas de Informacion',6);

INSERT INTO Curso VALUES ('C003','P003','Calidad, Validacion y Verificacion',6);

INSERT INTO Curso VALUES ('C004','P001','Arquitectura del Software',6);

INSERT INTO Curso VALUES ('C005','P004','Fundamentos de Informatica',6);

INSERT INTO Curso VALUES ('C006','P005','Calculo',6);

INSERT INTO Curso VALUES ('C007','P006','Redes de Computadores',6);

INSERT INTO Curso VALUES ('C008','P007','Administracion de Sistemas',6);

INSERT INTO Curso VALUES ('C009','P008','Algebra Lineal',6);

INSERT INTO Curso VALUES ('C010','P008','Topologia',6);

INSERT INTO Curso VALUES ('C011','P002','Aprendizaje Automatico',6);

-- Sin coordinador ni grado todavia
INSERT INTO Curso VALUES ('C012',NULL,'Desarrollo de Videojuegos',4.5);

INSERT INTO Curso VALUES ('C013','P003','Estructuras de Datos',6);



-- G001 (Software)
INSERT INTO GradoCurso VALUES ('G001','C005',1,NULL,'ACTIVO');

INSERT INTO GradoCurso VALUES ('G001','C006',1,NULL,'ACTIVO');

INSERT INTO GradoCurso VALUES ('G001','C013',2,NULL,'ACTIVO');

INSERT INTO GradoCurso VALUES ('G001','C001',3,NULL,'ACTIVO');

INSERT INTO GradoCurso VALUES ('G001','C002',3,NULL,'ACTIVO');

INSERT INTO GradoCurso VALUES ('G001','C003',3,NULL,'ACTIVO');

INSERT INTO GradoCurso VALUES ('G001','C004',4,'Ingenieria del Software','ACTIVO');

-- G002 (Tecnologias de la Informacion): Fundamentos de Informatica compartida con G001
INSERT INTO GradoCurso VALUES ('G002','C005',1,NULL,'ACTIVO');

INSERT INTO GradoCurso VALUES ('G002','C007',2,NULL,'ACTIVO');

INSERT INTO GradoCurso VALUES ('G002','C008',3,'Sistemas','ACTIVO');

-- G003 (en extincion): todo inactivo, aunque Calculo y Algebra siguen activas en otros grados
INSERT INTO GradoCurso VALUES ('G003','C006',1,NULL,'INACTIVO');

INSERT INTO GradoCurso VALUES ('G003','C009',1,NULL,'INACTIVO');

INSERT INTO GradoCurso VALUES ('G003','C010',3,NULL,'INACTIVO');

-- G004 (pendiente de renovacion)
INSERT INTO GradoCurso VALUES ('G004','C011',3,NULL,'ACTIVO');

-- G006 (doble grado): asignaturas compartidas con G001 y G003
INSERT INTO GradoCurso VALUES ('G006','C005',1,NULL,'ACTIVO');

INSERT INTO GradoCurso VALUES ('G006','C006',1,NULL,'ACTIVO');

INSERT INTO GradoCurso VALUES ('G006','C009',1,NULL,'ACTIVO');



INSERT INTO PropuestaCurso VALUES ('PR001','P001','G001','Ingenieria de Requisitos Avanzada',4,6,'Tecnicas avanzadas de elicitacion y especificacion','2026-05-10','PENDIENTE');

INSERT INTO PropuestaCurso VALUES ('PR002','P006','G002','Ciberseguridad en Redes',4,6,'Seguridad perimetral y analisis de trafico','2026-03-15','ACEPTADA');

INSERT INTO PropuestaCurso VALUES ('PR003','P004','G001','Programacion Competitiva',2,4.5,'Algoritmia orientada a concursos','2026-02-01','RECHAZADA');



-- Capacidades: TE 80, PA 40, PL 20
INSERT INTO Grupo VALUES ('C001','TE1','TE',80);

INSERT INTO Grupo VALUES ('C001','PA1','PA',40);

INSERT INTO Grupo VALUES ('C001','PL1','PL',20);

INSERT INTO Grupo VALUES ('C001','PL2','PL',20);

INSERT INTO Grupo VALUES ('C002','TE1','TE',80);

INSERT INTO Grupo VALUES ('C002','PA1','PA',40);

INSERT INTO Grupo VALUES ('C002','PL1','PL',20);

INSERT INTO Grupo VALUES ('C003','TE1','TE',80);

INSERT INTO Grupo VALUES ('C003','PL1','PL',20);

INSERT INTO Grupo VALUES ('C004','TE1','TE',80);

INSERT INTO Grupo VALUES ('C004','PL1','PL',20);

INSERT INTO Grupo VALUES ('C005','TE1','TE',80);

INSERT INTO Grupo VALUES ('C005','PA1','PA',40);

INSERT INTO Grupo VALUES ('C005','PL1','PL',20);

INSERT INTO Grupo VALUES ('C005','PL2','PL',20);

INSERT INTO Grupo VALUES ('C006','TE1','TE',80);

INSERT INTO Grupo VALUES ('C006','PA1','PA',40);

INSERT INTO Grupo VALUES ('C007','TE1','TE',80);

INSERT INTO Grupo VALUES ('C007','PL1','PL',20);

INSERT INTO Grupo VALUES ('C008','TE1','TE',80);

INSERT INTO Grupo VALUES ('C009','TE1','TE',80);

INSERT INTO Grupo VALUES ('C011','TE1','TE',80);

INSERT INTO Grupo VALUES ('C011','PL1','PL',20);

INSERT INTO Grupo VALUES ('C013','TE1','TE',80);

INSERT INTO Grupo VALUES ('C013','PL1','PL',20);



-- El coordinador de cada curso tambien imparte alguno de sus grupos
INSERT INTO Imparte VALUES ('P001','C001','TE1');

INSERT INTO Imparte VALUES ('P001','C001','PL1');

INSERT INTO Imparte VALUES ('P003','C001','PA1');

INSERT INTO Imparte VALUES ('P003','C001','PL2');

INSERT INTO Imparte VALUES ('P002','C002','TE1');

INSERT INTO Imparte VALUES ('P002','C002','PA1');

INSERT INTO Imparte VALUES ('P004','C002','PL1');

INSERT INTO Imparte VALUES ('P003','C003','TE1');

INSERT INTO Imparte VALUES ('P003','C003','PL1');

INSERT INTO Imparte VALUES ('P001','C004','TE1');

INSERT INTO Imparte VALUES ('P002','C004','PL1');

INSERT INTO Imparte VALUES ('P004','C005','TE1');

INSERT INTO Imparte VALUES ('P004','C005','PL1');

INSERT INTO Imparte VALUES ('P006','C005','PA1');

INSERT INTO Imparte VALUES ('P006','C005','PL2');

INSERT INTO Imparte VALUES ('P005','C006','TE1');

INSERT INTO Imparte VALUES ('P005','C006','PA1');

INSERT INTO Imparte VALUES ('P006','C007','TE1');

INSERT INTO Imparte VALUES ('P007','C007','PL1');

INSERT INTO Imparte VALUES ('P007','C008','TE1');

INSERT INTO Imparte VALUES ('P008','C009','TE1');

INSERT INTO Imparte VALUES ('P002','C011','TE1');

INSERT INTO Imparte VALUES ('P008','C011','PL1');

INSERT INTO Imparte VALUES ('P003','C013','TE1');

INSERT INTO Imparte VALUES ('P004','C013','PL1');



INSERT INTO Convocatoria VALUES ('2025-2026','ORDINARIA_ENERO','2026-01-08','2026-01-31');

INSERT INTO Convocatoria VALUES ('2025-2026','ORDINARIA_JUNIO','2026-05-25','2026-06-15');

INSERT INTO Convocatoria VALUES ('2025-2026','EXTRAORDINARIA_JULIO','2026-06-29','2026-07-17');

INSERT INTO Convocatoria VALUES ('2026-2027','ORDINARIA_ENERO','2027-01-07','2027-01-29');

INSERT INTO Convocatoria VALUES ('2026-2027','ORDINARIA_JUNIO','2027-05-24','2027-06-14');

INSERT INTO Convocatoria VALUES ('2026-2027','EXTRAORDINARIA_JULIO','2027-06-28','2027-07-16');



INSERT INTO Alumno VALUES ('AL001','20000001A','Lucia','Rodriguez Garcia','lrodriguez@alumno.es');

INSERT INTO Alumno VALUES ('AL002','20000002B','Hugo','Fernandez Martinez','hfernandez@alumno.es');

INSERT INTO Alumno VALUES ('AL003','20000003C','Sara','Gonzalez Alvarez','sgonzalez@alumno.es');

INSERT INTO Alumno VALUES ('AL004','20000004D','Mario','Diaz Suarez','mdiaz@alumno.es');

INSERT INTO Alumno VALUES ('AL005','20000005E','Paula','Lopez Menendez','plopez@alumno.es');

INSERT INTO Alumno VALUES ('AL006','20000006F','Daniel','Perez Iglesias','dperez@alumno.es');

INSERT INTO Alumno VALUES ('AL007','20000007G','Laura','Sanchez Fernandez','lsanchez@alumno.es');

INSERT INTO Alumno VALUES ('AL008','20000008H','Alvaro','Martin Garcia','amartin@alumno.es');

INSERT INTO Alumno VALUES ('AL009','20000009J','Irene','Alonso Diaz','ialonso@alumno.es');

INSERT INTO Alumno VALUES ('AL010','20000010K','Pablo','Gutierrez Lopez','pgutierrez@alumno.es');

INSERT INTO Alumno VALUES ('AL011','20000011L','Noelia','Castro Rodriguez','ncastro@alumno.es');

INSERT INTO Alumno VALUES ('AL012','20000012M','Ivan','Blanco Perez','iblanco@alumno.es');

INSERT INTO Alumno VALUES ('AL013','20000013N','Marina','Vega Ruiz','mvega@alumno.es');



-- =========================
-- MATRICULAS 2025-2026 (anio cerrado)
-- =========================
INSERT INTO Matricula VALUES ('M001','AL001','G001','2025-2026','2025-07-10','FINALIZADA',NULL,NULL);

INSERT INTO Matricula VALUES ('M002','AL002','G001','2025-2026','2025-07-15','FINALIZADA',NULL,NULL);

INSERT INTO Matricula VALUES ('M003','AL003','G001','2025-2026','2025-07-03','FINALIZADA',NULL,NULL);

INSERT INTO Matricula VALUES ('M004','AL004','G002','2025-2026','2025-07-20','FINALIZADA',NULL,NULL);

-- AL005 nunca pago: cancelada por impago (el anio cuenta, pero no se le exigen los 12 creditos)
INSERT INTO Matricula VALUES ('M005','AL005','G001','2025-2026','2025-09-15','CANCELADA','IMPAGO','2025-10-15');



INSERT INTO MatriculaCurso VALUES ('M001','G001','C005','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-10',1);

INSERT INTO MatriculaCurso VALUES ('M001','G001','C006','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-10',1);

INSERT INTO MatriculaCurso VALUES ('M002','G001','C005','FINALIZADA',NULL,NULL,120,'TRANSFERENCIA','2025-07-16',1);

INSERT INTO MatriculaCurso VALUES ('M002','G001','C006','FINALIZADA',NULL,NULL,120,'TRANSFERENCIA','2025-07-16',1);

INSERT INTO MatriculaCurso VALUES ('M003','G001','C001','FINALIZADA',NULL,NULL,120,'CARTA_PAGO','2025-07-08',1);

INSERT INTO MatriculaCurso VALUES ('M003','G001','C002','FINALIZADA',NULL,NULL,120,'CARTA_PAGO','2025-07-08',1);

INSERT INTO MatriculaCurso VALUES ('M003','G001','C003','FINALIZADA',NULL,NULL,120,'CARTA_PAGO','2025-07-08',1);

INSERT INTO MatriculaCurso VALUES ('M004','G002','C005','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-20',1);

INSERT INTO MatriculaCurso VALUES ('M004','G002','C007','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-20',1);

INSERT INTO MatriculaCurso VALUES ('M005','G001','C005','CANCELADA','IMPAGO','2025-10-15',120,NULL,NULL,0);



INSERT INTO MatriculaGrupo VALUES ('M001','C005','TE','TE1');

INSERT INTO MatriculaGrupo VALUES ('M001','C005','PA','PA1');

INSERT INTO MatriculaGrupo VALUES ('M001','C005','PL','PL1');

INSERT INTO MatriculaGrupo VALUES ('M001','C006','TE','TE1');

INSERT INTO MatriculaGrupo VALUES ('M001','C006','PA','PA1');

INSERT INTO MatriculaGrupo VALUES ('M002','C005','TE','TE1');

INSERT INTO MatriculaGrupo VALUES ('M002','C005','PA','PA1');

INSERT INTO MatriculaGrupo VALUES ('M002','C005','PL','PL2');

INSERT INTO MatriculaGrupo VALUES ('M002','C006','TE','TE1');

INSERT INTO MatriculaGrupo VALUES ('M002','C006','PA','PA1');

INSERT INTO MatriculaGrupo VALUES ('M003','C001','TE','TE1');

INSERT INTO MatriculaGrupo VALUES ('M003','C001','PA','PA1');

INSERT INTO MatriculaGrupo VALUES ('M003','C001','PL','PL1');

INSERT INTO MatriculaGrupo VALUES ('M003','C002','TE','TE1');

INSERT INTO MatriculaGrupo VALUES ('M003','C002','PL','PL1');

INSERT INTO MatriculaGrupo VALUES ('M003','C003','TE','TE1');

INSERT INTO MatriculaGrupo VALUES ('M003','C003','PL','PL1');

-- AL004 (G002) comparte grupos de Fundamentos con los alumnos de G001
INSERT INTO MatriculaGrupo VALUES ('M004','C005','TE','TE1');

INSERT INTO MatriculaGrupo VALUES ('M004','C005','PL','PL2');

INSERT INTO MatriculaGrupo VALUES ('M004','C007','TE','TE1');

INSERT INTO MatriculaGrupo VALUES ('M004','C007','PL','PL1');



-- Actas 2025-2026: todas cerradas, una por el sistema
INSERT INTO Acta VALUES ('C005','2025-2026','ORDINARIA_ENERO','CERRADA','2026-02-05',0);

INSERT INTO Acta VALUES ('C005','2025-2026','EXTRAORDINARIA_JULIO','CERRADA','2026-07-17',0);

INSERT INTO Acta VALUES ('C006','2025-2026','ORDINARIA_JUNIO','CERRADA','2026-06-20',0);

INSERT INTO Acta VALUES ('C006','2025-2026','EXTRAORDINARIA_JULIO','CERRADA','2026-07-17',1);

INSERT INTO Acta VALUES ('C001','2025-2026','ORDINARIA_JUNIO','CERRADA','2026-06-18',0);

INSERT INTO Acta VALUES ('C002','2025-2026','ORDINARIA_ENERO','CERRADA','2026-02-03',0);

INSERT INTO Acta VALUES ('C003','2025-2026','ORDINARIA_JUNIO','CERRADA','2026-06-19',0);

INSERT INTO Acta VALUES ('C003','2025-2026','EXTRAORDINARIA_JULIO','CERRADA','2026-07-15',0);

INSERT INTO Acta VALUES ('C007','2025-2026','ORDINARIA_ENERO','CERRADA','2026-02-04',0);

-- Acta que el coordinador (P001) no cerro: la convocatoria ya termino, asi que el sistema la cierra
-- al arrancar la aplicacion (MainController), y secretaria lo ve como aviso (HU 35239)
INSERT INTO Acta VALUES ('C001','2025-2026','EXTRAORDINARIA_JULIO','ABIERTA',NULL,0);



-- AL001 supera 12 creditos (Calculo en extraordinaria): cumple permanencia
INSERT INTO Calificacion VALUES ('M001','C005','2025-2026','ORDINARIA_ENERO',7.5);

INSERT INTO Calificacion VALUES ('M001','C006','2025-2026','ORDINARIA_JUNIO',4.0);

INSERT INTO Calificacion VALUES ('M001','C006','2025-2026','EXTRAORDINARIA_JULIO',6.0);

-- AL002 supera 0 creditos: no puede volver a matricularse en G001 (se cambia a G002)
-- Fundamentos: 2 convocatorias consumidas. Calculo: 1 (la de junio fue no presentado)
INSERT INTO Calificacion VALUES ('M002','C005','2025-2026','ORDINARIA_ENERO',3.5);

INSERT INTO Calificacion VALUES ('M002','C005','2025-2026','EXTRAORDINARIA_JULIO',4.0);

INSERT INTO Calificacion VALUES ('M002','C006','2025-2026','ORDINARIA_JUNIO',NULL);

INSERT INTO Calificacion VALUES ('M002','C006','2025-2026','EXTRAORDINARIA_JULIO',4.5);

-- AL003 supera 18 creditos
INSERT INTO Calificacion VALUES ('M003','C001','2025-2026','ORDINARIA_JUNIO',8.5);

INSERT INTO Calificacion VALUES ('M003','C002','2025-2026','ORDINARIA_ENERO',6.0);

INSERT INTO Calificacion VALUES ('M003','C003','2025-2026','ORDINARIA_JUNIO',4.0);

INSERT INTO Calificacion VALUES ('M003','C003','2025-2026','EXTRAORDINARIA_JULIO',5.5);

-- AL004 supera 12 creditos
INSERT INTO Calificacion VALUES ('M004','C005','2025-2026','ORDINARIA_ENERO',6.0);

INSERT INTO Calificacion VALUES ('M004','C007','2025-2026','ORDINARIA_ENERO',9.0);



-- =========================
-- MATRICULAS 2026-2027 (anio en curso)
-- =========================
INSERT INTO Matricula VALUES ('M006','AL001','G001','2026-2027','2026-07-05','ACTIVA',NULL,NULL);

-- AL002 cambia de grado; en Fundamentos arrastra 2 convocatorias de G001
INSERT INTO Matricula VALUES ('M007','AL002','G002','2026-2027','2026-07-08','ACTIVA',NULL,NULL);

INSERT INTO Matricula VALUES ('M008','AL003','G001','2026-2027','2026-07-02','ACTIVA',NULL,NULL);

INSERT INTO Matricula VALUES ('M009','AL004','G002','2026-2027','2026-07-21','ACTIVA',NULL,NULL);

-- Transferencias pendientes de validar por el secretario
INSERT INTO Matricula VALUES ('M010','AL006','G001','2026-2027','2026-09-25','SOLICITADA',NULL,NULL);

-- Una asignatura activa y otra sin pagar, pendiente de revision del secretario
INSERT INTO Matricula VALUES ('M011','AL007','G001','2026-2027','2026-07-28','ACTIVA',NULL,NULL);

INSERT INTO Matricula VALUES ('M012','AL008','G004','2026-2027','2026-07-20','ACTIVA',NULL,NULL);

-- Cancelada entera por fuerza mayor (pagada, sin devolucion)
INSERT INTO Matricula VALUES ('M013','AL009','G001','2026-2027','2026-07-12','CANCELADA','FUERZA_MAYOR','2026-10-01');

-- Cancelada al cancelar su unica asignatura
INSERT INTO Matricula VALUES ('M014','AL010','G002','2026-2027','2026-08-01','CANCELADA','INCOMPATIBILIDAD','2026-09-15');

-- Pagada despues de iniciar el anio: pendiente de activar
INSERT INTO Matricula VALUES ('M015','AL011','G001','2026-2027','2026-09-29','SOLICITADA',NULL,NULL);

-- Alumna de doble grado
INSERT INTO Matricula VALUES ('M016','AL013','G006','2026-2027','2026-07-15','ACTIVA',NULL,NULL);

-- AL012 no tiene matriculas (perfil vacio)



-- AL001: una activa y otra cancelada a mitad (desmatricula parcial, queda registrada)
INSERT INTO MatriculaCurso VALUES ('M006','G001','C013','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-05',1);

INSERT INTO MatriculaCurso VALUES ('M006','G001','C001','CANCELADA','INCOMPATIBILIDAD','2026-09-20',120,'TARJETA','2026-07-05',1);

INSERT INTO MatriculaCurso VALUES ('M007','G002','C005','ACTIVA',NULL,NULL,120,'TRANSFERENCIA','2026-07-09',1);

INSERT INTO MatriculaCurso VALUES ('M007','G002','C007','ACTIVA',NULL,NULL,120,'TRANSFERENCIA','2026-07-09',1);

INSERT INTO MatriculaCurso VALUES ('M008','G001','C004','ACTIVA',NULL,NULL,120,'CARTA_PAGO','2026-07-06',1);

INSERT INTO MatriculaCurso VALUES ('M009','G002','C008','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-21',1);

INSERT INTO MatriculaCurso VALUES ('M010','G001','C005','SOLICITADA',NULL,NULL,120,'TRANSFERENCIA','2026-09-26',0);

INSERT INTO MatriculaCurso VALUES ('M010','G001','C006','SOLICITADA',NULL,NULL,120,'TRANSFERENCIA','2026-09-26',0);

INSERT INTO MatriculaCurso VALUES ('M011','G001','C005','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-28',1);

INSERT INTO MatriculaCurso VALUES ('M011','G001','C006','SOLICITADA',NULL,NULL,120,NULL,NULL,0);

INSERT INTO MatriculaCurso VALUES ('M012','G004','C011','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-20',1);

INSERT INTO MatriculaCurso VALUES ('M013','G001','C005','CANCELADA','FUERZA_MAYOR','2026-10-01',120,'TARJETA','2026-07-12',1);

INSERT INTO MatriculaCurso VALUES ('M013','G001','C006','CANCELADA','FUERZA_MAYOR','2026-10-01',120,'TARJETA','2026-07-12',1);

INSERT INTO MatriculaCurso VALUES ('M014','G002','C007','CANCELADA','INCOMPATIBILIDAD','2026-09-15',120,'TARJETA','2026-08-01',1);

INSERT INTO MatriculaCurso VALUES ('M015','G001','C005','PAGADA',NULL,NULL,120,'TARJETA','2026-09-29',1);

INSERT INTO MatriculaCurso VALUES ('M016','G006','C005','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-15',1);

INSERT INTO MatriculaCurso VALUES ('M016','G006','C009','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-15',1);



INSERT INTO MatriculaGrupo VALUES ('M006','C013','TE','TE1');

INSERT INTO MatriculaGrupo VALUES ('M006','C013','PL','PL1');

INSERT INTO MatriculaGrupo VALUES ('M007','C005','TE','TE1');

INSERT INTO MatriculaGrupo VALUES ('M007','C005','PA','PA1');

INSERT INTO MatriculaGrupo VALUES ('M007','C005','PL','PL1');

INSERT INTO MatriculaGrupo VALUES ('M007','C007','TE','TE1');

INSERT INTO MatriculaGrupo VALUES ('M007','C007','PL','PL1');

INSERT INTO MatriculaGrupo VALUES ('M008','C004','TE','TE1');

INSERT INTO MatriculaGrupo VALUES ('M008','C004','PL','PL1');

INSERT INTO MatriculaGrupo VALUES ('M009','C008','TE','TE1');

-- AL007 solo tiene TE en Fundamentos: le faltan PA y PL (pendiente del secretario)
INSERT INTO MatriculaGrupo VALUES ('M011','C005','TE','TE1');

INSERT INTO MatriculaGrupo VALUES ('M012','C011','TE','TE1');

INSERT INTO MatriculaGrupo VALUES ('M012','C011','PL','PL1');

-- AL013 (doble grado) comparte grupos con alumnos de G001 y G002
INSERT INTO MatriculaGrupo VALUES ('M016','C005','TE','TE1');

INSERT INTO MatriculaGrupo VALUES ('M016','C005','PA','PA1');

INSERT INTO MatriculaGrupo VALUES ('M016','C005','PL','PL2');

INSERT INTO MatriculaGrupo VALUES ('M016','C009','TE','TE1');

-- M015 (pagada, sin activar) todavia no tiene grupos



-- Actas 2026-2027: abiertas, todavia sin notas
INSERT INTO Acta VALUES ('C005','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0);

INSERT INTO Acta VALUES ('C006','2026-2027','ORDINARIA_JUNIO','ABIERTA',NULL,0);

INSERT INTO Acta VALUES ('C004','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0);

INSERT INTO Acta VALUES ('C007','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0);

INSERT INTO Acta VALUES ('C008','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0);

INSERT INTO Acta VALUES ('C009','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0);

INSERT INTO Acta VALUES ('C011','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0);

INSERT INTO Acta VALUES ('C013','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0);
