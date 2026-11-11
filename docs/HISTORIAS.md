# Historias de usuario implementadas

Cada fila dice qué pantalla y qué clases implementan la historia y qué tests la prueban.
Las consultas SQL de referencia de cada historia están en [DATABASE.md](DATABASE.md#8-consultas-de-referencia-por-historia-de-usuario).

## Sprint 1

| HU | Rol | Historia | Responsable | Controlador → modelo | Tests |
|---|---|---|---|---|---|
| 35220 | Administrador | Consultar una vista general de los grados, cursos y matriculaciones de la universidad. | Alejandro Rivada Rodríguez | `AdminOfertaController` → `GradoRepository`, `CursoRepository`, `MatriculacionModel` | Parcial: solo las consultas (`GradoModelGestionTest` TF-06, `CursoModelGestionTest` TF-01 y TF-02) |
| 35223 | Administrador | Gestionar el ciclo de vida de los grados y los cursos. | Alejandro Rivada Rodríguez | `AdminOfertaController` → `GradoModel`, `CursoModel`, `AnoAcademicoModel` | `GradoModelGestionTest`, `GradoModelModificacionTest`, `CursoModelGestionTest`, `CursoModelModificacionTest`, `AnoAcademicoModelTest`, `AnoAcademicoModelNuevoAnoTest` |
| 35226 | Secretario | Consultar las matriculaciones de los cursos, grados y grupos. | Alejandro El Harchi Argüelles | `SecretarioController` → `GestionMatriculaModel` | `GestionMatriculaModelTest` |
| 35229 | Secretario | Gestionar el ciclo de vida de las matrículas y grupos. | Alejandro El Harchi Argüelles | `SecretarioController`, `MainController` (al arrancar) → `GestionMatriculaModel` | `GestionMatriculaModelTest` |
| 35232 | Estudiante | Consultar mis matriculaciones. | Andrea Ivanov García | `MisMatriculasController` → `MatriculacionModel` | `MatriculacionConsultaModelTest`, `ReglasDTOTest` (y `EstudianteNavegacionTest`, que no se ejecuta en `mvn test`) |
| 35235 | Estudiante | Solicitar mi matriculación en los grados y cursos ofertados por la universidad. | Andrea Ivanov García | `SolicitudMatriculaController` → `MatriculacionModel` | `MatriculacionSolicitudModelTest` (y `SolicitudMatriculaViewTest`, que no se ejecuta en `mvn test`) |
| 35239 | Profesor | Gestionar los cursos y grupos en los que imparto docencia. | Nicolás Pérez Fernández | `ProfesorController`, `MainController` (cierre automático al arrancar), `SecretarioController` (avisos) → `ActaModel` | `ActaModelTest` |
| 35243 | Profesor | Proponer nuevas asignaturas para los grados en los que imparto docencia. | Nicolás Pérez Fernández | `ProfesorController` → `PropuestaModel` | `PropuestaModelTest` |

## Sprint 2

| HU | Rol | Historia | Responsable | Controlador → modelo | Tests |
|---|---|---|---|---|---|
| 35987 | Estudiante | Matricularme a las microcredenciales para completar mi curriculum de estudiante. | Andrea Ivanov García | `SolicitudMicrocredencialController` → `InscripcionMicrocredencialModel`, `MatriculacionModel` | `InscripcionMicrocredencialModelTest` (y `SolicitudMicrocredencialViewTest`, que no se ejecuta en `mvn test`) |
| 36410 | Sistema | Cancelar de manera automática las microcredenciales que no alcancen el porcentaje mínimo de plazas cubiertas al concluir su periodo de matriculación para optimizar recursos académicos. | Andrea Ivanov García | `MainController` (al arrancar), `ProfesorController`, `MisMatriculasController` → `MicrocredencialModel`, `InscripcionMicrocredencialModel` | `MicrocredencialBajaDemandaTest` |

## Notas

- **35243:** el profesor rellena un formulario con los datos de la propuesta (curso) para los grados en los que imparte docencia. Las propuestas podrán incluirse en cursos posteriores del grado o grados a los que se adscriben; su revisión queda para un sprint futuro.
- **35987:**
  - **Asistente por pasos y arquitectura desacoplada:** la matriculación en microcredenciales cuenta con su propia vista y controlador (`SolicitudMicrocredencialView` y `SolicitudMicrocredencialController`) respaldados por `InscripcionMicrocredencialModel` y `MatriculaMicrocredencialRepository`, siguiendo un flujo guiado de selección, resumen consolidado, pasarela de pago telemático y diálogo de confirmación con botones localizados en castellano («Sí» / «No»).
  - **Acceso por rama de conocimiento:** se valida que el estudiante solo acceda a microcredenciales de la misma rama en la que tiene una matrícula de grado no cancelada en el año académico actual (`getRamasHabilitadas`).
  - **Exclusión de permanencia y sin tope de créditos:** los créditos de las microcredenciales no computan para los hitos de permanencia anuales ($\ge 12$ ECTS) ni acumulados ($\ge 90$ ECTS), y no existe límite máximo de créditos ECTS totales que el alumno pueda cursar mediante microcredenciales.
  - **Precios y pasarela de pago:** se calcula a razón de 20,00 € / crédito ECTS (`creditos × PRECIO_CREDITO`). El pago con tarjeta valida la plaza inmediatamente (`INSCRITA`, `pagoValidado = 1`); la transferencia o carta de pago queda en estado `SOLICITADA` (`pagoValidado = 0`) para su posterior cotejo por secretaría.
  - **Persistencia en base de datos:** inserción transaccional atómica en `MatriculaMicrocredencial` ligada a la matrícula anual del alumno, validando cupo de plazas disponibles (`pagoValidado = 1 < plazas`) y bloqueando solicitudes duplicadas en la misma microcredencial.
- **36410:**
  - **Proceso por lotes en arranque:** `MainController.actualizarEstadosPorFecha()` invoca `MicrocredencialModel.cancelarPorBajaDemanda(fechaHoy)` al iniciar la aplicación y al recargar la base de datos.
  - **Criterio del 10% el día previo al cierre de matriculación:** conforme a la aclaración docente, para toda microcredencial `ACEPTADA` con `date(finMatriculacion, '-1 day') <= fechaHoy`, primero se cancelan por `IMPAGO` las inscripciones en estado `SOLICITADA` (`pagoValidado = 0`) y a continuación se evalúa si las inscripciones con `pagoValidado = 1` alcanzan al menos el 10% de `plazas`. Si no alcanzan el 10%, la microcredencial pasa a `CANCELADA_BAJA_DEMANDA` (registrando `fechaCancelacion`) y sus inscripciones pasan a `CANCELADA` con `motivoCancelacion = 'BAJA_DEMANDA'` y `fechaBaja`.
  - **Notificación en vistas de Profesor y Estudiante:** el profesor visualiza la microcredencial como «Cancelada por baja demanda» junto a su fecha de cancelación en la pestaña «Microcredenciales» de `ProfesorView`, y el estudiante visualiza sus microcredenciales y el estado «Cancelada por baja demanda» en la pestaña «Microcredenciales» de `MisMatriculasView`.
- **Tests de vista:** los de `src/test/java/view` abren ventanas, así que `pom.xml` no los incluye en `mvn test` ni en el CI. Se ejecutan a mano con `mvn test -Dtest=EstudianteNavegacionTest,SolicitudMatriculaViewTest,SolicitudMicrocredencialViewTest`.
