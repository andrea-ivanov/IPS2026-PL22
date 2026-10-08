-- schema.sql
-- Borrado de tablas en orden inverso a sus dependencias
DROP TABLE IF EXISTS Alumno_Hito;
DROP TABLE IF EXISTS Hito;
DROP TABLE IF EXISTS Convocatoria;
DROP TABLE IF EXISTS Matricula_Curso;
DROP TABLE IF EXISTS Profesor_Curso;
DROP TABLE IF EXISTS Matricula;
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
    uo VARCHAR(20) UNIQUE NOT NULL,
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

-- Un curso (asignatura) pertenece a un grado y tiene un profesor coordinador
CREATE TABLE Curso (
    id_curso INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(100) NOT NULL,
    id_grado INTEGER,
    id_coordinador INTEGER,
    FOREIGN KEY(id_grado) REFERENCES Grado(id_grado),
    FOREIGN KEY(id_coordinador) REFERENCES Profesor(id_profesor)
);

-- Tabla intermedia para la relación "imparte"
CREATE TABLE Profesor_Curso (
    id_profesor INTEGER,
    id_curso INTEGER,
    PRIMARY KEY(id_profesor, id_curso),
    FOREIGN KEY(id_profesor) REFERENCES Profesor(id_profesor),
    FOREIGN KEY(id_curso) REFERENCES Curso(id_curso)
);

-- Una matrícula general asocia al alumno con un grado en un año específico
CREATE TABLE Matricula (
    id_matricula INTEGER PRIMARY KEY AUTOINCREMENT,
    id_alumno INTEGER,
    id_grado INTEGER,
    estado VARCHAR(50), -- Ej: Pendiente de pago, Pagada, Anulada
    fecha DATE,
    FOREIGN KEY(id_alumno) REFERENCES Alumno(id_alumno),
    FOREIGN KEY(id_grado) REFERENCES Grado(id_grado)
);

-- Desglose de en qué asignaturas (Cursos) exactas se ha matriculado el alumno
CREATE TABLE Matricula_Curso (
    id_matricula INTEGER,
    id_curso INTEGER,
    numero_matricula INTEGER, -- Ej: 1 (primera matrícula), 2 (segunda matrícula...)
    PRIMARY KEY(id_matricula, id_curso),
    FOREIGN KEY(id_matricula) REFERENCES Matricula(id_matricula),
    FOREIGN KEY(id_curso) REFERENCES Curso(id_curso)
);

-- Convocatorias y Notas para una asignatura concreta de una matrícula
CREATE TABLE Convocatoria (
    id_convocatoria INTEGER PRIMARY KEY AUTOINCREMENT,
    id_matricula INTEGER,
    id_curso INTEGER,
    tipo VARCHAR(50), -- Ej: Ordinaria, Extraordinaria
    nota DECIMAL(4,2), -- Puede ser NULL si aún no se ha presentado
    activa BOOLEAN, -- 1 si es la convocatoria actual en curso, 0 si ya pasó
    FOREIGN KEY(id_matricula, id_curso) REFERENCES Matricula_Curso(id_matricula, id_curso)
);

-- Hitos de permanencia (reglas de la universidad)
CREATE TABLE Hito (
    id_hito INTEGER PRIMARY KEY AUTOINCREMENT,
    descripcion VARCHAR(200) NOT NULL
);

-- Hitos que ha superado cada alumno
CREATE TABLE Alumno_Hito (
    id_alumno INTEGER,
    id_hito INTEGER,
    fecha_superacion DATE,
    PRIMARY KEY(id_alumno, id_hito),
    FOREIGN KEY(id_alumno) REFERENCES Alumno(id_alumno),
    FOREIGN KEY(id_hito) REFERENCES Hito(id_hito)
);
