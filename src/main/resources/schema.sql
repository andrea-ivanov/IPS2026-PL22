-- schema.sql
-- Borrado de tablas en orden inverso a sus dependencias (para evitar errores de foreign keys)
DROP TABLE IF EXISTS Profesor_Curso;
DROP TABLE IF EXISTS Matricula;
DROP TABLE IF EXISTS AnoAcademico;
DROP TABLE IF EXISTS Curso;
DROP TABLE IF EXISTS Grado;
DROP TABLE IF EXISTS Alumno;
DROP TABLE IF EXISTS Profesor;
DROP TABLE IF EXISTS Administrador;
DROP TABLE IF EXISTS Secretario;

CREATE TABLE Administrador (
    id_admin INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE Secretario (
    id_secretario INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE Alumno (
    id_alumno INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE Grado (
    id_grado INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(100) NOT NULL
);

CREATE TABLE Profesor (
    id_profesor INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL
);

-- Un curso pertenece a un grado y tiene un profesor coordinador
CREATE TABLE Curso (
    id_curso INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(100) NOT NULL,
    id_grado INTEGER,
    id_coordinador INTEGER,
    FOREIGN KEY(id_grado) REFERENCES Grado(id_grado),
    FOREIGN KEY(id_coordinador) REFERENCES Profesor(id_profesor)
);

-- Año académico con periodos y estado actual
CREATE TABLE AnoAcademico (
    id_ano_academico INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(50),
    inicio_ano_academico DATE NOT NULL,
    fin_ano_academico DATE NOT NULL,
    inicio_periodo_matriculacion DATE NOT NULL,
    fin_periodo_matriculacion DATE NOT NULL,
    actual BOOLEAN NOT NULL DEFAULT 0
);

-- Una matrícula está asociada a un alumno, a un grado y a un año académico
CREATE TABLE Matricula (
    id_matricula INTEGER PRIMARY KEY AUTOINCREMENT,
    id_alumno INTEGER,
    id_grado INTEGER,
    id_ano_academico INTEGER,
    estado VARCHAR(50),
    fecha DATE,
    FOREIGN KEY(id_alumno) REFERENCES Alumno(id_alumno),
    FOREIGN KEY(id_grado) REFERENCES Grado(id_grado),
    FOREIGN KEY(id_ano_academico) REFERENCES AnoAcademico(id_ano_academico)
);

-- Tabla intermedia para la relación "imparte" (Muchos profesores pueden impartir muchos cursos)
CREATE TABLE Profesor_Curso (
    id_profesor INTEGER,
    id_curso INTEGER,
    PRIMARY KEY(id_profesor, id_curso),
    FOREIGN KEY(id_profesor) REFERENCES Profesor(id_profesor),
    FOREIGN KEY(id_curso) REFERENCES Curso(id_curso)
);
