package com.example.todo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.todo.domain.Task;
import com.example.todo.domain.TaskPriority;
import com.example.todo.domain.TaskStatus;
import com.example.todo.exception.InvalidStatusTransitionException;
import com.example.todo.exception.TaskNotFoundException;
import com.example.todo.exception.TaskStateException;
import com.example.todo.exception.TaskValidationException;
import com.example.todo.repository.InMemoryTaskRepository;
import com.example.todo.web.dto.CreateTaskRequest;
import com.example.todo.web.dto.UpdateTaskRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);

  private InMemoryTaskRepository repository;
  private TaskService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryTaskRepository();
    service = new TaskService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private CreateTaskRequest createRequest(LocalDate dueDate, Set<String> tags) {
    return new CreateTaskRequest(
        "Preparar el informe",
        "Recopilar los datos del trimestre",
        TaskPriority.HIGH,
        dueDate,
        "ana@example.com",
        tags);
  }

  private UpdateTaskRequest updateRequest(String title, LocalDate dueDate) {
    return new UpdateTaskRequest(
        title,
        "Descripcion actualizada",
        TaskPriority.LOW,
        dueDate,
        "luis@example.com",
        Set.of("trabajo"));
  }

  private Task createValidTask() {
    return service.create(createRequest(TODAY.plusDays(7), Set.of("trabajo", "informes")));
  }

  @Test
  void noSePuedeCrearUnaTareaConFechaLimitePasada() {
    CreateTaskRequest request = createRequest(TODAY.minusDays(1), Set.of("trabajo"));

    TaskValidationException ex =
        assertThrows(TaskValidationException.class, () -> service.create(request));

    assertTrue(ex.getMessage().contains("fecha limite"));
    assertEquals(0, repository.count());
  }

  @Test
  void seAdmiteCrearTareaConFechaLimiteHoy() {
    Task task = service.create(createRequest(TODAY, Set.of("urgente")));

    assertEquals(TODAY, task.getDueDate());
    assertEquals(1, repository.count());
  }

  @Test
  void noSePuedeCrearUnaTareaConFechaLimiteMasDeUnAnoEnElFuturo() {
    CreateTaskRequest request = createRequest(TODAY.plusYears(1).plusDays(1), Set.of("trabajo"));

    TaskValidationException ex =
        assertThrows(TaskValidationException.class, () -> service.create(request));

    assertTrue(ex.getMessage().contains("un ano"));
    assertEquals(0, repository.count());
  }

  @Test
  void seAdmiteCrearTareaConFechaLimiteJustaDeUnAno() {
    Task task = service.create(createRequest(TODAY.plusYears(1), Set.of("trabajo")));

    assertEquals(TODAY.plusYears(1), task.getDueDate());
    assertEquals(1, repository.count());
  }

  @Test
  void crearTareaValidaAsignaIdEstadoPorDefectoYMarcasDeTiempo() {
    Task task = service.create(createRequest(TODAY.plusDays(3), Set.of("casa")));

    assertEquals(1L, task.getId());
    assertEquals(TaskStatus.TODO, task.getStatus());
    assertEquals(NOW, task.getCreatedAt());
    assertEquals(NOW, task.getUpdatedAt());
    assertNull(task.getCompletedAt());
    assertEquals("Preparar el informe", task.getTitle());
    assertEquals(TaskPriority.HIGH, task.getPriority());
  }

  @Test
  void noSePuedeCrearTareaConEtiquetasDuplicadas() {
    CreateTaskRequest request = createRequest(TODAY.plusDays(3), Set.of("casa", "CASA"));

    TaskValidationException ex =
        assertThrows(TaskValidationException.class, () -> service.create(request));

    assertTrue(ex.getMessage().contains("duplicada"));
    assertEquals(0, repository.count());
  }

  @Test
  void noSePuedeCompletarUnaTareaDirectamenteDesdeTodo() {
    Task task = createValidTask();

    assertThrows(
        InvalidStatusTransitionException.class,
        () -> service.changeStatus(task.getId(), TaskStatus.DONE));

    assertEquals(TaskStatus.TODO, service.get(task.getId()).getStatus());
  }

  @Test
  void completarUnaTareaTrasIniciarlaAsignaFechaDeFinalizacion() {
    Task task = createValidTask();

    service.changeStatus(task.getId(), TaskStatus.IN_PROGRESS);
    Task done = service.changeStatus(task.getId(), TaskStatus.DONE);

    assertEquals(TaskStatus.DONE, done.getStatus());
    assertEquals(NOW, done.getCompletedAt());
  }

  @Test
  void unaTareaBloqueadaNoRegistraFechaDeFinalizacion() {
    Task task = createValidTask();
    service.changeStatus(task.getId(), TaskStatus.IN_PROGRESS);

    Task blocked = service.changeStatus(task.getId(), TaskStatus.BLOCKED);

    assertEquals(TaskStatus.BLOCKED, blocked.getStatus());
    assertNull(blocked.getCompletedAt());
  }

  @Test
  void unaTareaFinalizadaEsTerminalYNoAdmiteMasTransiciones() {
    Task task = createValidTask();
    service.changeStatus(task.getId(), TaskStatus.IN_PROGRESS);
    service.changeStatus(task.getId(), TaskStatus.DONE);

    assertThrows(
        InvalidStatusTransitionException.class,
        () -> service.changeStatus(task.getId(), TaskStatus.IN_PROGRESS));
    assertThrows(
        InvalidStatusTransitionException.class,
        () -> service.changeStatus(task.getId(), TaskStatus.CANCELLED));
  }

  @Test
  void unaTareaFinalizadaNoPuedeModificarse() {
    Task task = createValidTask();
    service.changeStatus(task.getId(), TaskStatus.IN_PROGRESS);
    service.changeStatus(task.getId(), TaskStatus.DONE);

    assertThrows(
        TaskStateException.class,
        () -> service.update(task.getId(), updateRequest("Titulo nuevo", TODAY.plusDays(10))));
  }

  @Test
  void actualizarUnaTareaInexistenteDevuelveErrorNoEncontrado() {
    assertThrows(TaskNotFoundException.class, () -> service.get(999L));
    assertThrows(
        TaskNotFoundException.class,
        () -> service.update(999L, updateRequest("Titulo", TODAY.plusDays(5))));
    assertThrows(
        TaskNotFoundException.class, () -> service.changeStatus(999L, TaskStatus.IN_PROGRESS));
    assertThrows(TaskNotFoundException.class, () -> service.delete(999L));
  }

  @Test
  void actualizarUnaTareaActivaModificaLosCamposYRefrescaLaMarcaDeTiempo() {
    Task task = createValidTask();

    Task updated =
        service.update(task.getId(), updateRequest("  Titulo corregido  ", TODAY.plusDays(20)));

    assertEquals("Titulo corregido", updated.getTitle());
    assertEquals("Descripcion actualizada", updated.getDescription());
    assertEquals(TaskPriority.LOW, updated.getPriority());
    assertEquals(TODAY.plusDays(20), updated.getDueDate());
    assertEquals(Set.of("trabajo"), updated.getTags());
    assertEquals(NOW, updated.getUpdatedAt());
    assertEquals(TaskStatus.TODO, updated.getStatus());
  }

  @Test
  void actualizarUnaTareaNoPuedeRetrocederLaFechaLimiteAntesDeHoy() {
    Task task = createValidTask();

    assertThrows(
        TaskValidationException.class,
        () -> service.update(task.getId(), updateRequest("Titulo", TODAY.minusDays(1))));

    assertEquals(TODAY.plusDays(7), service.get(task.getId()).getDueDate());
  }

  @Test
  void eliminarUnaTareaExistenteLaEliminaYLaEliminadaNoSeEncuentra() {
    Task task = createValidTask();

    service.delete(task.getId());

    assertThrows(TaskNotFoundException.class, () -> service.get(task.getId()));
    assertEquals(0, repository.count());
  }

  @Test
  void elListadoFiltraPorEstadoPrioridadYEtiqueta() {
    Task first = createValidTask();
    first.setTitle("Tarea de baja prioridad");
    repository.save(first);
    service.changeStatus(first.getId(), TaskStatus.IN_PROGRESS);

    Task second = service.create(createRequest(TODAY.plusDays(10), Set.of("personal")));
    service.changeStatus(second.getId(), TaskStatus.IN_PROGRESS);
    service.changeStatus(second.getId(), TaskStatus.DONE);

    Task third = service.create(createRequest(TODAY.plusDays(15), Set.of("Casa")));

    List<Task> inProgress = service.list(TaskStatus.IN_PROGRESS, null, null);
    assertEquals(1, inProgress.size());
    assertEquals(first.getId(), inProgress.get(0).getId());

    List<Task> doneHigh = service.list(TaskStatus.DONE, TaskPriority.HIGH, null);
    assertEquals(1, doneHigh.size());
    assertEquals(second.getId(), doneHigh.get(0).getId());

    List<Task> byTag = service.list(null, null, "casa");
    assertEquals(1, byTag.size());
    assertEquals(third.getId(), byTag.get(0).getId());
    assertTrue(byTag.get(0).getTags().stream().anyMatch(tag -> tag.equalsIgnoreCase("casa")));

    assertEquals(0, service.list(null, TaskPriority.CRITICAL, null).size());
  }

  @Test
  void lasEstadisticasAgrupanLasTareasPorEstado() {
    Task task = createValidTask();
    service.create(createRequest(TODAY.plusDays(9), Set.of("personal")));
    service.changeStatus(task.getId(), TaskStatus.IN_PROGRESS);
    service.changeStatus(task.getId(), TaskStatus.DONE);

    var stats = service.stats();

    assertEquals(2, stats.total());
    assertEquals(1L, stats.byStatus().get(TaskStatus.DONE));
    assertEquals(1L, stats.byStatus().get(TaskStatus.TODO));
    assertNotNull(stats.byStatus().get(TaskStatus.TODO));
  }

  @Test
  void noHayTareasVencidasMientrasNoPaseSuFechaLimite() {
    service.create(createRequest(TODAY.plusDays(1), Set.of("urgente")));

    assertTrue(service.findOverdue().isEmpty());
  }

  @Test
  void lasTareasVencidasIncluyenSoloLasNoFinalizadasConFechaLimitePasada() {
    Task pending = service.create(createRequest(TODAY.plusDays(1), Set.of("urgente")));
    service.create(createRequest(TODAY.plusDays(5), Set.of("normal")));
    Task finished = service.create(createRequest(TODAY.plusDays(1), Set.of("hecha")));
    service.changeStatus(finished.getId(), TaskStatus.IN_PROGRESS);
    service.changeStatus(finished.getId(), TaskStatus.DONE);

    TaskService twoDaysLater =
        new TaskService(repository, Clock.fixed(NOW.plus(Duration.ofDays(2)), ZoneOffset.UTC));

    List<Task> overdueTasks = twoDaysLater.findOverdue();

    assertEquals(1, overdueTasks.size());
    assertEquals(pending.getId(), overdueTasks.get(0).getId());
    assertEquals(TaskStatus.TODO, overdueTasks.get(0).getStatus());
  }

  @Test
  void cambiarElEstadoAlMismoValorNoProduceError() {
    Task task = createValidTask();

    Task same = service.changeStatus(task.getId(), TaskStatus.TODO);

    assertEquals(TaskStatus.TODO, same.getStatus());
    assertNull(same.getCompletedAt());
  }
}
