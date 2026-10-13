-- Modelo de datos IPS (SQLite)
-- Tablas y campos en camelCase sin subrayados (convenio Java exigido para DbUtils)
-- Los nombres de las restricciones (pk_, fk_, ck_, uq_) si llevan subrayado: no se mapean a Java
--
-- IMPORTANTE: los comentarios van siempre en su propia linea. DbUtil.executeScript une las lineas
-- de cada sentencia con un espacio, asi que un comentario al final de una linea dentro de una
-- sentencia comentaria todo lo que viene detras.
--
-- Reglas de negocio que van como constantes en el codigo (no en la BD):
--   PRECIO_CREDITO = 20 euros (fijo, igual en todos los grados; importe = creditos * 20)
--   CREDITOS_MINIMOS_ANIO = 12 (superados cada anio, salvo si la matricula del anio esta CANCELADA)
--   CREDITOS_MINIMOS_TERCER_ANIO = 90 (superados al acabar el 3er anio en el grado; los anios cancelados cuentan)
--   MAX_CONVOCATORIAS = 6 por asignatura (solo cuentan las calificaciones con nota)
--
-- Reglas que aplica el modelo (no se pueden expresar con CHECK):
--   - Matricula y MatriculaCurso cambian de estado siempre en el mismo metodo
--   - Cancelar la matricula cancela todas sus asignaturas no finalizadas (mismo motivo y fecha)
--   - Cancelar la ultima asignatura no cancelada cancela la matricula
--   - Al iniciar el anio: la matricula pasa a ACTIVA si tiene alguna asignatura PAGADA, y las PAGADAS a ACTIVA;
--     las asignaturas SOLICITADAS (sin pagar) quedan pendientes hasta que las revise el secretario
--   - El secretario asigna grupos solo a asignaturas PAGADAS o ACTIVAS, sin superar Grupo.capacidad
--   - Solo se califica en actas ABIERTAS y solo el coordinador del curso cierra el acta

DROP TABLE IF EXISTS Calificacion;

DROP TABLE IF EXISTS MatriculaGrupo;

DROP TABLE IF EXISTS MatriculaCurso;

DROP TABLE IF EXISTS Matricula;

DROP TABLE IF EXISTS Acta;

DROP TABLE IF EXISTS Convocatoria;

DROP TABLE IF EXISTS Imparte;

DROP TABLE IF EXISTS Grupo;

DROP TABLE IF EXISTS PropuestaCurso;

DROP TABLE IF EXISTS GradoCurso;

DROP TABLE IF EXISTS Curso;

DROP TABLE IF EXISTS Grado;

DROP TABLE IF EXISTS AnioAcademico;

DROP TABLE IF EXISTS Profesor;

DROP TABLE IF EXISTS Alumno;



-- =========================
-- ALUMNO
-- =========================
CREATE TABLE Alumno (
    alumnoId VARCHAR(10) NOT NULL,
    dni VARCHAR(9) NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    CONSTRAINT pk_alumno PRIMARY KEY (alumnoId),
    CONSTRAINT uq_alumno_dni UNIQUE (dni),
    CONSTRAINT uq_alumno_email UNIQUE (email)
);

-- =========================
-- PROFESOR
-- =========================
CREATE TABLE Profesor (
    profesorId VARCHAR(10) NOT NULL,
    dni VARCHAR(9) NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    CONSTRAINT pk_profesor PRIMARY KEY (profesorId),
    CONSTRAINT uq_profesor_dni UNIQUE (dni),
    CONSTRAINT uq_profesor_email UNIQUE (email)
);

-- =========================
-- ANIO ACADEMICO
-- (lo configura el administrador)
-- anioAcademicoId: p.e. '2026-2027'
-- =========================
CREATE TABLE AnioAcademico (
    anioAcademicoId VARCHAR(10) NOT NULL,
    fechaInicio DATE NOT NULL,
    fechaFin DATE NOT NULL,
    inicioMatriculacion DATE,
    finMatriculacion DATE,
    CONSTRAINT pk_anioAcademico PRIMARY KEY (anioAcademicoId),
    CONSTRAINT ck_anioAcademico_fechas CHECK (fechaInicio < fechaFin),
    CONSTRAINT ck_anioAcademico_matriculacion CHECK (inicioMatriculacion IS NULL OR finMatriculacion IS NULL OR inicioMatriculacion <= finMatriculacion)
);

-- =========================
-- GRADO
-- (un doble grado es un Grado mas, con sus propias asignaturas asociadas en GradoCurso)
-- (fechaFinVigencia es derivada: fechaInicioVigencia + 4 anios)
-- =========================
CREATE TABLE Grado (
    gradoId VARCHAR(10) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    facultad VARCHAR(150) NOT NULL,
    ramaConocimiento VARCHAR(100) NOT NULL,
    estado VARCHAR(25) NOT NULL,
    fechaInicioVigencia DATE NOT NULL,
    CONSTRAINT pk_grado PRIMARY KEY (gradoId),
    CONSTRAINT ck_grado_estado CHECK (estado IN ('VIGENTE', 'EN_EXTINCION', 'EXTINTO', 'PENDIENTE_RENOVACION'))
);

-- =========================
-- CURSO (asignatura)
-- Solo lo propio de la asignatura; lo que depende del grado esta en GradoCurso
-- Relacion coordina (Profesor 1:N)
-- coordinadorId: nulo si no tiene coordinador
-- =========================
CREATE TABLE Curso (
    cursoId VARCHAR(10) NOT NULL,
    coordinadorId VARCHAR(10),
    nombre VARCHAR(150) NOT NULL,
    creditos DECIMAL(4,1) NOT NULL,
    CONSTRAINT pk_curso PRIMARY KEY (cursoId),
    CONSTRAINT fk_curso_profesor FOREIGN KEY (coordinadorId) REFERENCES Profesor (profesorId),
    CONSTRAINT ck_curso_creditos CHECK (creditos > 0)
);

-- =========================
-- GRADO CURSO (Grado <-> Curso)
-- Relacion pertenece N:M: una asignatura puede estar en varios grados
-- anio, itinerario y estado dependen del grado (activa en uno, inactiva en otro en extincion)
-- El estado se sobrescribe cada anio, sin historial
-- =========================
CREATE TABLE GradoCurso (
    gradoId VARCHAR(10) NOT NULL,
    cursoId VARCHAR(10) NOT NULL,
    anio INT NOT NULL,
    itinerario VARCHAR(100),
    estado VARCHAR(10) NOT NULL,
    CONSTRAINT pk_gradoCurso PRIMARY KEY (gradoId, cursoId),
    CONSTRAINT fk_gradoCurso_grado FOREIGN KEY (gradoId) REFERENCES Grado (gradoId),
    CONSTRAINT fk_gradoCurso_curso FOREIGN KEY (cursoId) REFERENCES Curso (cursoId),
    CONSTRAINT ck_gradoCurso_anio CHECK (anio BETWEEN 1 AND 6),
    CONSTRAINT ck_gradoCurso_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

-- =========================
-- PROPUESTA CURSO
-- (de momento solo la crea el profesor; la revision queda para un sprint futuro)
-- =========================
CREATE TABLE PropuestaCurso (
    propuestaId VARCHAR(10) NOT NULL,
    profesorId VARCHAR(10) NOT NULL,
    gradoId VARCHAR(10) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    anio INT NOT NULL,
    creditos DECIMAL(4,1) NOT NULL,
    descripcion VARCHAR(1000),
    fechaPropuesta DATE NOT NULL,
    estado VARCHAR(10) NOT NULL,
    CONSTRAINT pk_propuestaCurso PRIMARY KEY (propuestaId),
    CONSTRAINT fk_propuestaCurso_profesor FOREIGN KEY (profesorId) REFERENCES Profesor (profesorId),
    CONSTRAINT fk_propuestaCurso_grado FOREIGN KEY (gradoId) REFERENCES Grado (gradoId),
    CONSTRAINT ck_propuestaCurso_anio CHECK (anio BETWEEN 1 AND 6),
    CONSTRAINT ck_propuestaCurso_creditos CHECK (creditos > 0),
    CONSTRAINT ck_propuestaCurso_estado CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA'))
);

-- =========================
-- GRUPO (weak entity de Curso)
-- El grupo es de la asignatura: alumnos de distintos grados pueden compartirlo
-- grupoCodigo: discriminante (p.e. PL1)
-- uq_grupo_tipo: destino de la FK de MatriculaGrupo
-- =========================
CREATE TABLE Grupo (
    cursoId VARCHAR(10) NOT NULL,
    grupoCodigo VARCHAR(10) NOT NULL,
    tipo VARCHAR(3) NOT NULL,
    capacidad INT NOT NULL,
    CONSTRAINT pk_grupo PRIMARY KEY (cursoId, grupoCodigo),
    CONSTRAINT uq_grupo_tipo UNIQUE (cursoId, grupoCodigo, tipo),
    CONSTRAINT fk_grupo_curso FOREIGN KEY (cursoId) REFERENCES Curso (cursoId),
    CONSTRAINT ck_grupo_tipo CHECK (tipo IN ('TE', 'PL', 'PA')),
    CONSTRAINT ck_grupo_capacidad CHECK (capacidad > 0)
);

-- =========================
-- IMPARTE (Profesor <-> Grupo)
-- El rol COORDINADOR/DOCENTE se deduce de Curso.coordinadorId
-- =========================
CREATE TABLE Imparte (
    profesorId VARCHAR(10) NOT NULL,
    cursoId VARCHAR(10) NOT NULL,
    grupoCodigo VARCHAR(10) NOT NULL,
    CONSTRAINT pk_imparte PRIMARY KEY (profesorId, cursoId, grupoCodigo),
    CONSTRAINT fk_imparte_profesor FOREIGN KEY (profesorId) REFERENCES Profesor (profesorId),
    CONSTRAINT fk_imparte_grupo FOREIGN KEY (cursoId, grupoCodigo) REFERENCES Grupo (cursoId, grupoCodigo)
);

-- =========================
-- CONVOCATORIA (weak entity de AnioAcademico)
-- tipoConvocatoria: discriminante
-- =========================
CREATE TABLE Convocatoria (
    anioAcademicoId VARCHAR(10) NOT NULL,
    tipoConvocatoria VARCHAR(30) NOT NULL,
    fechaInicio DATE NOT NULL,
    fechaFin DATE NOT NULL,
    CONSTRAINT pk_convocatoria PRIMARY KEY (anioAcademicoId, tipoConvocatoria),
    CONSTRAINT fk_convocatoria_anioAcademico FOREIGN KEY (anioAcademicoId) REFERENCES AnioAcademico (anioAcademicoId),
    CONSTRAINT ck_convocatoria_tipo CHECK (tipoConvocatoria IN ('ORDINARIA_ENERO', 'ORDINARIA_JUNIO', 'EXTRAORDINARIA_JULIO')),
    CONSTRAINT ck_convocatoria_fechas CHECK (fechaInicio <= fechaFin)
);

-- =========================
-- ACTA (weak entity de Curso y Convocatoria, sin discriminante)
-- cierreAutomatico: 0 = cerrada por el coordinador, 1 = cerrada por el sistema
-- =========================
CREATE TABLE Acta (
    cursoId VARCHAR(10) NOT NULL,
    anioAcademicoId VARCHAR(10) NOT NULL,
    tipoConvocatoria VARCHAR(30) NOT NULL,
    estado VARCHAR(7) NOT NULL,
    fechaCierre DATE,
    cierreAutomatico INT NOT NULL,
    CONSTRAINT pk_acta PRIMARY KEY (cursoId, anioAcademicoId, tipoConvocatoria),
    CONSTRAINT fk_acta_curso FOREIGN KEY (cursoId) REFERENCES Curso (cursoId),
    CONSTRAINT fk_acta_convocatoria FOREIGN KEY (anioAcademicoId, tipoConvocatoria) REFERENCES Convocatoria (anioAcademicoId, tipoConvocatoria),
    CONSTRAINT ck_acta_estado CHECK (estado IN ('ABIERTA', 'CERRADA')),
    CONSTRAINT ck_acta_cierre CHECK ((estado = 'CERRADA') = (fechaCierre IS NOT NULL)),
    CONSTRAINT ck_acta_cierreAutomatico CHECK (cierreAutomatico IN (0, 1))
);

-- =========================
-- MATRICULA (el todo: un alumno en un grado durante un anio academico)
-- Es lo que cuenta como "anio en el grado" para la permanencia
-- No tiene estado PAGADA: el pago es de cada asignatura (MatriculaCurso)
-- uq_matricula_solicitud: impide solicitudes duplicadas
-- uq_matricula_grado: destino de la FK de MatriculaCurso
-- =========================
CREATE TABLE Matricula (
    matriculaId VARCHAR(10) NOT NULL,
    alumnoId VARCHAR(10) NOT NULL,
    gradoId VARCHAR(10) NOT NULL,
    anioAcademicoId VARCHAR(10) NOT NULL,
    fechaSolicitud DATE NOT NULL,
    estado VARCHAR(10) NOT NULL,
    motivoCancelacion VARCHAR(20),
    fechaCancelacion DATE,
    CONSTRAINT pk_matricula PRIMARY KEY (matriculaId),
    CONSTRAINT uq_matricula_solicitud UNIQUE (alumnoId, gradoId, anioAcademicoId),
    CONSTRAINT uq_matricula_grado UNIQUE (matriculaId, gradoId),
    CONSTRAINT fk_matricula_alumno FOREIGN KEY (alumnoId) REFERENCES Alumno (alumnoId),
    CONSTRAINT fk_matricula_grado FOREIGN KEY (gradoId) REFERENCES Grado (gradoId),
    CONSTRAINT fk_matricula_anioAcademico FOREIGN KEY (anioAcademicoId) REFERENCES AnioAcademico (anioAcademicoId),
    CONSTRAINT ck_matricula_estado CHECK (estado IN ('SOLICITADA', 'ACTIVA', 'FINALIZADA', 'CANCELADA')),
    CONSTRAINT ck_matricula_motivo CHECK (motivoCancelacion IN ('IMPAGO', 'FUERZA_MAYOR', 'INCOMPATIBILIDAD')),
    CONSTRAINT ck_matricula_cancelacionMotivo CHECK ((estado = 'CANCELADA') = (motivoCancelacion IS NOT NULL)),
    CONSTRAINT ck_matricula_cancelacionFecha CHECK ((estado = 'CANCELADA') = (fechaCancelacion IS NOT NULL))
);

-- =========================
-- MATRICULA CURSO (Matricula <-> Curso)
-- Relacion incluye, tratada como agregacion
-- Cada asignatura tiene su propio estado, cancelacion y pago (sin devolucion al cancelar)
-- gradoId se repite para que la BD garantice que la asignatura pertenece al grado de la matricula
-- importe: creditos * PRECIO_CREDITO en el momento de solicitar
-- pagoValidado: aprobacion del secretario (automatica con tarjeta)
-- =========================
CREATE TABLE MatriculaCurso (
    matriculaId VARCHAR(10) NOT NULL,
    gradoId VARCHAR(10) NOT NULL,
    cursoId VARCHAR(10) NOT NULL,
    estado VARCHAR(10) NOT NULL,
    motivoCancelacion VARCHAR(20),
    fechaCancelacion DATE,
    importe DECIMAL(10,2) NOT NULL,
    metodoPago VARCHAR(15),
    fechaPago DATE,
    pagoValidado INT NOT NULL,
    CONSTRAINT pk_matriculaCurso PRIMARY KEY (matriculaId, cursoId),
    CONSTRAINT fk_matriculaCurso_matricula FOREIGN KEY (matriculaId, gradoId) REFERENCES Matricula (matriculaId, gradoId),
    CONSTRAINT fk_matriculaCurso_gradoCurso FOREIGN KEY (gradoId, cursoId) REFERENCES GradoCurso (gradoId, cursoId),
    CONSTRAINT ck_matriculaCurso_estado CHECK (estado IN ('SOLICITADA', 'PAGADA', 'ACTIVA', 'FINALIZADA', 'CANCELADA')),
    CONSTRAINT ck_matriculaCurso_motivo CHECK (motivoCancelacion IN ('IMPAGO', 'FUERZA_MAYOR', 'INCOMPATIBILIDAD')),
    CONSTRAINT ck_matriculaCurso_cancelacionMotivo CHECK ((estado = 'CANCELADA') = (motivoCancelacion IS NOT NULL)),
    CONSTRAINT ck_matriculaCurso_cancelacionFecha CHECK ((estado = 'CANCELADA') = (fechaCancelacion IS NOT NULL)),
    CONSTRAINT ck_matriculaCurso_importe CHECK (importe >= 0),
    CONSTRAINT ck_matriculaCurso_metodoPago CHECK (metodoPago IN ('TARJETA', 'TRANSFERENCIA', 'CARTA_PAGO')),
    CONSTRAINT ck_matriculaCurso_pagoValidado CHECK (pagoValidado IN (0, 1)),
    CONSTRAINT ck_matriculaCurso_pagada CHECK (estado NOT IN ('PAGADA', 'ACTIVA', 'FINALIZADA') OR (metodoPago IS NOT NULL AND fechaPago IS NOT NULL AND pagoValidado = 1))
);

-- =========================
-- MATRICULA GRUPO (aggregation)
-- Relacion asignadoA: [MatriculaCurso] <-> Grupo. La asigna el secretario
-- La PK con tipo garantiza un unico grupo de cada tipo por asignatura
-- La FK con cursoId y tipo garantiza que el grupo es de esa asignatura y de ese tipo
-- =========================
CREATE TABLE MatriculaGrupo (
    matriculaId VARCHAR(10) NOT NULL,
    cursoId VARCHAR(10) NOT NULL,
    tipo VARCHAR(3) NOT NULL,
    grupoCodigo VARCHAR(10) NOT NULL,
    CONSTRAINT pk_matriculaGrupo PRIMARY KEY (matriculaId, cursoId, tipo),
    CONSTRAINT fk_matriculaGrupo_matriculaCurso FOREIGN KEY (matriculaId, cursoId) REFERENCES MatriculaCurso (matriculaId, cursoId),
    CONSTRAINT fk_matriculaGrupo_grupo FOREIGN KEY (cursoId, grupoCodigo, tipo) REFERENCES Grupo (cursoId, grupoCodigo, tipo)
);

-- =========================
-- CALIFICACION (aggregation)
-- Relacion calificada: [MatriculaCurso] <-> Acta
-- nota nula = no presentado (no consume convocatoria)
-- Al compartir cursoId, la BD garantiza que el acta es del mismo curso
-- =========================
CREATE TABLE Calificacion (
    matriculaId VARCHAR(10) NOT NULL,
    cursoId VARCHAR(10) NOT NULL,
    anioAcademicoId VARCHAR(10) NOT NULL,
    tipoConvocatoria VARCHAR(30) NOT NULL,
    nota DECIMAL(4,2),
    CONSTRAINT pk_calificacion PRIMARY KEY (matriculaId, cursoId, anioAcademicoId, tipoConvocatoria),
    CONSTRAINT fk_calificacion_matriculaCurso FOREIGN KEY (matriculaId, cursoId) REFERENCES MatriculaCurso (matriculaId, cursoId),
    CONSTRAINT fk_calificacion_acta FOREIGN KEY (cursoId, anioAcademicoId, tipoConvocatoria) REFERENCES Acta (cursoId, anioAcademicoId, tipoConvocatoria),
    CONSTRAINT ck_calificacion_nota CHECK (nota BETWEEN 0 AND 10)
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
