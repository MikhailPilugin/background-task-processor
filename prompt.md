# Background Task Processing System

## Описание
Создай backend-приложение на Kotlin + Ktor для управления фоновыми задачами с отслеживанием прогресса через WebSocket.

## Требования

### Стек
- Kotlin
- Ktor (server: Netty)
- Gradle (Kotlin DSL)
- H2 Database (in-memory, через HikariCP)
-Exposed ORM для работы с БД
- kotlinx.serialization для JSON

### Функциональность
1. REST API:
   - POST /tasks — создать новую задачу (параметр: duration в секундах). Возвращает taskId.
   - GET /tasks — список задач пользователя со статусами.
   - GET /tasks/{id} — статус конкретной задачи.

2. WebSocket:
   - WS /tasks/{taskId}/progress — подключение для получения прогресса задачи в реальном времени.
   - Сервер отправляет сообщения о прогрессе: {"progress": 0-100, "status": "RUNNING|COMPLETED|FAILED"}.

3. Логика задачи:
   - При создании задачи запускается фоновый поток (Coroutine), который имитирует долгую работу (delay с шагами).
   - Прогресс обновляется в БД и отправляется через WebSocket всем подключённым клиентам для этой задачи.
   - Несколько пользователей могут запускать задачи параллельно и получать только свой прогресс.

### Модель данных
- Таблица tasks: id (UUID), status (CREATED, RUNNING, COMPLETED, FAILED), progress (Int), durationSeconds (Int), createdAt (Timestamp), userId (String).

### Структура проекта
src/main/kotlin/ Application.kt — точка входа, настройка сервера plugins/ Routing.kt — маршруты REST и WebSocket Serialization.kt — настройка JSON Databases.kt — инициализация БД и таблиц models/ Task.kt — data class + DTO TaskStatus.kt — enum services/ TaskService.kt — бизнес-логика создания и получения задач TaskExecutor.kt — запуск фоновых задач, обновление прогресса repositories/ TaskRepository.kt — работа с БД через Exposed

### Дополнительно
- README.md с инструкцией сборки и запуска.
- Простая HTML-страница (src/main/resources/index.html) для тестирования: форма создания задачи + отображение прогресса через WebSocket.