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
| 35987 | Estudiante | Matricularme a las microcredenciales para completar mi curriculum de estudiante. | Andrea Ivanov García | `SolicitudMatriculaController` → `MatriculacionModel` | `MatriculacionMicrocredencialModelTest` (y `SolicitudMicrocredencialViewTest`, que no se ejecuta en `mvn test`) |

## Notas

- **35243:** el profesor rellena un formulario con los datos de la propuesta (curso) para los grados en los que imparte docencia. Las propuestas podrán incluirse en cursos posteriores del grado o grados a los que se adscriben; su revisión queda para un sprint futuro.
- **35987:**
  - **Asistente unificado y refactorización:** para evitar duplicidad de código y componentes Swing redundantes, se unificó la matriculación en un único asistente (`SolicitudMatriculaView` y `SolicitudMatriculaController`), permitiendo alternar entre el modo Grado y Microcredencial mediante selectores de modo y reutilizando los pasos de resumen consolidado, pasarela de pago telemático y diálogo de confirmación con botones localizados en castellano («Sí» / «No»).
  - **Acceso por rama de conocimiento:** se valida que el estudiante solo acceda a microcredenciales de la misma rama en la que está o ha estado matriculado (`getRamasHabilitadasPorEstudiante`), admitiendo tanto a estudiantes con curso activo como a graduados para ampliación de estudios.
  - **Exclusión de permanencia y sin tope de créditos:** los créditos de las microcredenciales no computan para los hitos de permanencia anuales ($\ge 12$ ECTS) ni acumulados ($\ge 90$ ECTS), y no existe límite máximo de créditos ECTS totales que el alumno pueda cursar mediante microcredenciales.
  - **Precios y pasarela de pago:** se calcula a razón de 20,00 € / crédito ECTS (`creditos × PRECIO_CREDITO`). El pago con tarjeta valida la plaza inmediatamente (`INSCRITA`, `pagoValidado = 1`); la transferencia o carta de pago queda en estado `SOLICITADA` (`pagoValidado = 0`) para su posterior cotejo por secretaría.
  - **Persistencia en base de datos:** inserción transaccional atómica en `MatriculaMicrocredencial` ligada a la matrícula anual del alumno, validando cupo de plazas disponibles (`pagoValidado = 1 < plazas`) y bloqueando solicitudes duplicadas en la misma microcredencial.
- **Tests de vista:** los de `src/test/java/view` abren ventanas, así que `pom.xml` no los incluye en `mvn test` ni en el CI. Se ejecutan a mano con `mvn test -Dtest=EstudianteNavegacionTest,SolicitudMatriculaViewTest,SolicitudMicrocredencialViewTest`.
