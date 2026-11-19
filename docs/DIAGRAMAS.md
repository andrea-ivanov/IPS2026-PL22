# 1. Ciclo de Vida de la Oferta Académica

## 1.1 Estado de un Grado Universitario

```mermaid
stateDiagram-v2
    direction LR
    [*] --> VIGENTE : Aprobación inicial
    VIGENTE --> PENDIENTE_RENOVACION : Al expirar período de acreditación (4º año)
    PENDIENTE_RENOVACION --> VIGENTE : FAVORABLE (Revisor Externo)
    PENDIENTE_RENOVACION --> EN_EXTINCION : DESFAVORABLE (Revisor Externo)
    PENDIENTE_RENOVACION --> EXTINTO : Sin dictamen al comenzar nuevo curso
    EN_EXTINCION --> EXTINTO : Al iniciarse el curso académico
    EXTINTO --> [*]
```

- **VIGENTE:** Ofertado para nuevas matrículas.
- **PENDIENTE_RENOVACION:** Sigue ofertado mientras se audita la calidad académica (HU 36411).
- **EN_EXTINCION / EXTINTO:** No admite nuevos alumnos; se cancelan las matrículas del nuevo año con motivo `EXTINCION_GRADO`.

---

## 1.2 Ciclo de Vida de una Microcredencial

```mermaid
stateDiagram-v2
    direction LR
    [*] --> PROPUESTA : Profesor propone (HU 35988)
    PROPUESTA --> RECHAZADA : Secretario rechaza con motivo (HU 35994)
    PROPUESTA --> ACEPTADA : Secretario aprueba (HU 35994)

    state ACEPTADA {
        direction TB
        MatriculacionAbierta : Período de matrícula (2 semanas)
        EvaluacionDemanda : Revisión umbral 10% (día previo a inicio)
        Imparticion : Curso en desarrollo
        MatriculacionAbierta --> EvaluacionDemanda
        EvaluacionDemanda --> Imparticion : Plazas pagadas >= 10%
    }

    ACEPTADA --> CANCELADA_BAJA_DEMANDA : Plazas pagadas < 10% (HU 36410)
    ACEPTADA --> FINALIZADA : Cierre de acta por profesor o auto-cierre (HU 36459)

    RECHAZADA --> [*]
    CANCELADA_BAJA_DEMANDA --> [*]
    FINALIZADA --> [*]
```

- **PROPUESTA:** Creada por el profesor indicando rama, plazas, créditos y calendario.
- **RECHAZADA:** Descartada por secretaría guardando el motivo de rechazo; no se puede reabrir.
- **ACEPTADA:** Se abre la matrícula pública para estudiantes de la misma rama.
- **CANCELADA_BAJA_DEMANDA:** Proceso batch al cierre de matrícula si $\text{pagadas} \times 10 < \text{plazas}$. Notificación automática en vistas de profesor y alumnos.
- **FINALIZADA:** Acta cerrada por el docente (o auto-cierre a los 15 días tras concluir).

---

# 2. Diagramas de Estados de la Matrícula del Estudiante

## 2.1 Matrícula de Asignaturas de Grado (HU 35235)

```mermaid
stateDiagram-v2
    direction TB
    [*] --> SOLICITADA : Estudiante confirma selección

    state SOLICITADA {
        Tarjeta : Pago Tarjeta (validación inmediata)
        Transferencia : Transferencia / Carta de Pago (pendiente)
    }

    Tarjeta --> PAGADA : Inmediato
    Transferencia --> PAGADA : Secretaría valida justificante (HU 35229)

    PAGADA --> ACTIVA : Comienza el año académico
    ACTIVA --> SUPERADA : Calificación >= 5.0 (computa en permanencia)
    ACTIVA --> SUSPENSA : Calificación < 5.0 (consume convocatoria, máx 6)

    SOLICITADA --> CANCELADA : Impago o anulación
    ACTIVA --> CANCELADA : Extinción del grado o baja
    
    CANCELADA --> [*]
    SUSPENSA --> [*]
    SUPERADA --> [*]
```

---

## 2.2 Inscripción en Microcredencial (HU 35987 & HU 36410)

```mermaid
stateDiagram-v2
    direction TB
    [*] --> SOLICITADA : Alumno solicita inscripción

    SOLICITADA --> INSCRITA : Pago con tarjeta validado al instante
    SOLICITADA --> INSCRITA : Transferencia/Carta validada por secretaría

    SOLICITADA --> CANCELADA_IMPAGO : El día antes de inicio sin pagar (motivo: IMPAGO)

    INSCRITA --> CANCELADA_BAJA_DEMANDA : Si total curso < 10% plazas (motivo: BAJA_DEMANDA)
    INSCRITA --> ABANDONADA : Alumno se da de baja durante el curso
    INSCRITA --> FINALIZADA : Acta cerrada con nota (0 - 10)

    state FINALIZADA {
        Aprobado : Nota >= 5.0
        Suspenso : Nota < 5.0
        NoPresentado : Sin nota al cierre
    }
    
    CANCELADA_BAJA_DEMANDA --> [*]
    ABANDONADA --> [*]
    FINALIZADA --> [*]
```