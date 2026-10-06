# task-api

API REST de gestión de tareas (To-Do) construida con Spring Boot 3 y Maven. La persistencia es
en memoria (no hay base de datos).

## Requisitos

- JDK 21 (`java -version`)
- Maven 3.9+ (`mvn -version`)

## Compilar y ejecutar

```bash
mvn clean package          # compila, ejecuta los tests y genera el fat JAR
java -jar target/task-api-1.0.0.jar
```

La API escucha en `http://localhost:8080`.

```bash
# Crear una tarea
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Comprar pan","priority":"HIGH","dueDate":"2026-10-20","tags":["casa"]}'

# Listar, filtrar y estadísticas
curl http://localhost:8080/api/tasks
curl "http://localhost:8080/api/tasks?status=TODO&priority=HIGH"
curl http://localhost:8080/api/tasks/stats
curl http://localhost:8080/api/tasks/overdue

# Un recurso inexistente devuelve 404 con JSON, nunca una traza de excepción
curl -i http://localhost:8080/api/tasks/999
```

## Tests

```bash
mvn test          # tests unitarios (JUnit 5)
mvn verify        # tests + comprobaciones de calidad
```

Solo hay tests unitarios: no se levanta el contexto de Spring.

## Formato del código

El proyecto usa Spotless con el estilo de Google:

```bash
mvn spotless:check   # falla si algo no está formateado
mvn spotless:apply   # lo arregla
```

Además hay un hook de pre-commit versionado en `.githooks/pre-commit` que ejecuta
`mvn spotless:apply` en cada commit, de modo que no puede entrar código sin formatear.

**Importante:** los hooks viven en `.git/hooks/`, que no se versiona. Por eso están en
`.githooks/` y se apunta `core.hooksPath` hacia ahí. Cada persona que clone el repositorio debe
ejecutar una sola vez:

```bash
git config core.hooksPath .githooks
```

El hook local nunca sustituye a la comprobación en CI, porque siempre se puede saltar con
`git commit --no-verify`.

## Estructura

```
src/main/java/com/example/todo
├── domain        # Task, TaskStatus, TaskPriority, TaskStats
├── repository    # interfaz + implementación en memoria
├── service       # reglas de negocio y validaciones
├── web           # controlador y manejador global de errores
└── web/dto       # peticiones y respuestas
```

## Reglas de negocio destacadas

- La fecha límite no puede ser anterior a hoy ni superar un año vista.
- El estado por defecto es `TODO`; para `DONE` hay que pasar antes por `IN_PROGRESS`.
- `DONE` y `CANCELLED` son estados finales: la tarea ya no se puede modificar.
- Las etiquetas no pueden estar en blanco ni repetirse (sin distinguir mayúsculas).
