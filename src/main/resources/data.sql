-- data.sql
-- Datos de prueba para iniciar la aplicación

INSERT INTO Administrador (nombre, email) VALUES ('Admin Principal', 'admin@uni.es');
INSERT INTO Secretario (nombre, email) VALUES ('Secretaria Uno', 'sec1@uni.es');

INSERT INTO Alumno (uo, nombre, email) VALUES ('UO250000', 'Juan Perez', 'juan@uni.es');
INSERT INTO Alumno (uo, nombre, email) VALUES ('UO260000', 'Maria Gomez', 'maria@uni.es');

INSERT INTO Grado (nombre) VALUES ('Ingeniería Informática');
INSERT INTO Grado (nombre) VALUES ('Ingeniería de Telecomunicaciones');

INSERT INTO Profesor (nombre, email) VALUES ('Profesor Martinez', 'martinez@uni.es');
INSERT INTO Profesor (nombre, email) VALUES ('Profesora Lopez', 'lopez@uni.es');

-- Cursos asignados al grado de Informática (1)
INSERT INTO Curso (nombre, id_grado, id_coordinador) VALUES ('Programación', 1, 1);
INSERT INTO Curso (nombre, id_grado, id_coordinador) VALUES ('Bases de Datos', 1, 2);
INSERT INTO Curso (nombre, id_grado, id_coordinador) VALUES ('Ingeniería del Software', 1, 1);

-- Relación de profesores que imparten cursos
INSERT INTO Profesor_Curso (id_profesor, id_curso) VALUES (1, 1);
INSERT INTO Profesor_Curso (id_profesor, id_curso) VALUES (2, 2);

-- ==========================================
-- MATRICULACIONES Y NOTAS (Para HU35232)
-- ==========================================

-- Matrícula general de Juan Perez (1) en Informática (1)
INSERT INTO Matricula (id_alumno, id_grado, estado, fecha) VALUES (1, 1, 'Pagada', '2026-09-01');

-- Juan se matricula en las asignaturas concretas (Cursos)
-- Primera matrícula en Programación y BD
INSERT INTO Matricula_Curso (id_matricula, id_curso, numero_matricula) VALUES (1, 1, 1);
INSERT INTO Matricula_Curso (id_matricula, id_curso, numero_matricula) VALUES (1, 2, 2); -- Segunda vez que se matricula en BD

-- Convocatorias para Juan (id_matricula=1)
-- En Programación (curso 1), se presentó a la ordinaria y sacó un 8.5
INSERT INTO Convocatoria (id_matricula, id_curso, tipo, nota, activa) VALUES (1, 1, 'Ordinaria', 8.5, 0);

-- En Bases de Datos (curso 2), suspendió la ordinaria (3.0), y ahora está en la extraordinaria (activa, sin nota aún)
INSERT INTO Convocatoria (id_matricula, id_curso, tipo, nota, activa) VALUES (1, 2, 'Ordinaria', 3.0, 0);
INSERT INTO Convocatoria (id_matricula, id_curso, tipo, nota, activa) VALUES (1, 2, 'Extraordinaria', NULL, 1);

-- ==========================================
-- HITOS DE PERMANENCIA (Para HU35232 y HU35235)
-- ==========================================

INSERT INTO Hito (descripcion) VALUES ('Superar 12 créditos el primer año');
INSERT INTO Hito (descripcion) VALUES ('Superar el 50% de los créditos del grado');

-- Juan (1) ha superado el primer hito
INSERT INTO Alumno_Hito (id_alumno, id_hito, fecha_superacion) VALUES (1, 1, '2026-06-15');
