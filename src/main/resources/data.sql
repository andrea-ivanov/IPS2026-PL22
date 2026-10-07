-- data.sql
-- Datos de prueba para iniciar la aplicación

INSERT INTO Administrador (nombre, email) VALUES ('Admin Principal', 'admin@uni.es');
INSERT INTO Secretario (nombre, email) VALUES ('Secretaria Uno', 'sec1@uni.es');

INSERT INTO Alumno (nombre, email) VALUES ('Juan Perez', 'juan@uni.es');
INSERT INTO Alumno (nombre, email) VALUES ('Maria Gomez', 'maria@uni.es');

INSERT INTO Grado (nombre) VALUES ('Ingeniería Informática');
INSERT INTO Grado (nombre) VALUES ('Ingeniería de Telecomunicaciones');

INSERT INTO Profesor (nombre, email) VALUES ('Profesor Martinez', 'martinez@uni.es');
INSERT INTO Profesor (nombre, email) VALUES ('Profesora Lopez', 'lopez@uni.es');

-- Cursos asignados al grado de Informática (1) y coordinados por profesores
INSERT INTO Curso (nombre, id_grado, id_coordinador) VALUES ('Programación', 1, 1);
INSERT INTO Curso (nombre, id_grado, id_coordinador) VALUES ('Bases de Datos', 1, 2);

-- Matrícula de Juan Perez (1) en Informática (1)
INSERT INTO Matricula (id_alumno, id_grado, estado, fecha) VALUES (1, 1, 'Activa', '2026-09-01');

-- Relación de profesores que imparten cursos
INSERT INTO Profesor_Curso (id_profesor, id_curso) VALUES (1, 1); -- Martinez imparte Programación
INSERT INTO Profesor_Curso (id_profesor, id_curso) VALUES (2, 2); -- Lopez imparte Bases de Datos
