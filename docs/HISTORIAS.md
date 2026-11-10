# Historias de usuario implementadas

Sprint 1. Cada fila dice qué pantalla y qué clases implementan la historia y qué tests la prueban.
Las consultas SQL de referencia de cada historia están en [DATABASE.md](DATABASE.md#8-consultas-de-referencia-por-historia-de-usuario).

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
| 35994 | Secretario | Revisar las propuestas de microcredenciales para aprobarlas o rechazarlas. | Alejandro El Harchi Argüelles | `RevisionMicrocredencialController` (pestaña de `SecretarioController`) → `RevisionMicrocredencialModel` → `RevisionMicrocredencialRepository` | `RevisionMicrocredencialModelTest` (y `RevisionMicrocredencialViewTest`, que no se ejecuta en `mvn test`) |

## Notas

- **35243:** el profesor rellena un formulario con los datos de la propuesta (curso) para los grados en los que imparte docencia. Las propuestas podrán incluirse en cursos posteriores del grado o grados a los que se adscriben; su revisión queda para un sprint futuro.
- **35994 (sprint 2):** pestaña "Propuestas de Microcredenciales" de Secretaría. Lista las microcredenciales PROPUESTAS (curso, profesor, créditos, periodo de impartición, rama y plazas). Al aceptarla pasa a ACEPTADA y queda en la oferta; al rechazarla se exige el motivo (máximo 500 caracteres), pasa a RECHAZADA con ese motivo y queda invalidada: el profesor tendría que crear otra propuesta. En los dos casos se guarda la fecha de resolución, pero no qué secretario la resolvió. El profesor no ve todavía el motivo del rechazo en su pantalla.
- **Tests de vista:** los de `src/test/java/view` abren ventanas, así que `pom.xml` no los incluye en `mvn test` ni en el CI. Se ejecutan a mano con `mvn test -Dtest=EstudianteNavegacionTest,SolicitudMatriculaViewTest`.
