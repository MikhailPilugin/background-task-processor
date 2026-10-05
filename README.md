# Background Task Processor

Сервис фоновых задач с отслеживанием прогресса в реальном времени через WebSocket.

**Стек:** Kotlin, Ktor (Netty), Exposed, HikariCP, H2, kotlinx.serialization, Gradle (Kotlin DSL)

## О проекте

Пользователь запускает длительную задачу через REST API. Сервер выполняет её в фоне,
обновляет прогресс в БД и отправляет изменения подключённым клиентам по WebSocket.
Каждый пользователь видит прогресс только своих задач.

Функциональность:
- Создание задачи: `POST /tasks`
- Список задач: `GET /tasks`
- Статус задачи: `GET /tasks/{id}`
- Прогресс в реальном времени: `WS /tasks/{id}/progress`

## Запуск

### Локально

Требования: JDK 21, Gradle (или использовать встроенный wrapper — `./gradlew`).

```bash
# Сборка
./gradlew build -x test

# Запуск
./gradlew run
```

Сервер стартует на `http://localhost:8080`.

### Через Docker

```bash
docker build -t background-task-processor .
docker run -p 8080:8080 background-task-processor
```

### Переменные окружения

| Переменная | По умолчанию | Описание |
|---|---|---|
| `PORT` | `8080` | Порт HTTP-сервера |
| `JDBC_URL` | `jdbc:h2:mem:tasks` | Строка подключения к БД |

## Использование

### Веб-интерфейс

Открой `http://localhost:8080` — страница позволяет создать задачу и следить за прогрессом.

### Через curl

Создать задачу:

```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"durationSeconds": 30, "userId": "user-1"}'
```

Получить статус:

```bash
curl http://localhost:8080/tasks/{taskId}
```

Подписаться на прогресс (WebSocket):

```
ws://localhost:8080/tasks/{taskId}/progress
```

Пример сообщения:

```json
{"progress": 45, "status": "RUNNING"}
```

## История разработки

Проект сгенерирован с помощью GitHub Copilot по промту из [`prompt.md`](prompt.md),
затем доработан вручную: исправлены ошибки сгенерированного кода, добавлены тесты,
улучшена архитектура. Список исправлений с обоснованиями — в истории коммитов.

## Структура проекта

```
src/main/kotlin/
  Application.kt          — точка входа, настройка сервера
  plugins/
    Routing.kt            — маршруты REST и WebSocket
    Serialization.kt      — настройка JSON
    Databases.kt          — инициализация БД и таблиц
  models/
    Task.kt               — модель задачи и DTO
    TaskStatus.kt         — enum статусов
  services/
    TaskService.kt        — бизнес-логика
    TaskExecutor.kt       — фоновое выполнение задач
  repositories/
    TaskRepository.kt     — доступ к БД через Exposed
```
