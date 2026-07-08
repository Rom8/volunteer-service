# AGENTS.md — Руководство по репозиторию `volunteer-service`

## Описание проекта

Pet-проект: микросервис управления волонтёрами в системе поиска людей (Rescue Service).  
Стек: **Java 25**, **Spring Boot 4.1**, **PostgreSQL 17**, **Kafka**, **Liquibase**, **MapStruct**, **Testcontainers**.

## Структура проекта

```
src/
├── main/java/ru/rom8/rescue/volunteer/
│   ├── config/           — Конфигурационные классы (CORS и др.)
│   ├── controller/       — REST-контроллеры
│   ├── domain/entity/    — JPA-сущности
│   ├── mapper/           — MapStruct-мапперы
│   ├── repository/       — Spring Data JPA репозитории
│   └── service/          — Бизнес-логика
├── main/resources/
│   ├── db/changelog/     — Liquibase миграции
│   ├── openapi/          — OpenAPI 3.1 спецификация (contract-first)
│   └── application.yml   — Основная конфигурация
└── test/                 — Тесты (JUnit 5 + Testcontainers + EmbeddedKafka)
```

API-контракт генерируется из `openapi/volunteer-service.yaml` плагином `openapi-generator-maven-plugin` в пакеты `ru.rom8.rescue.volunteer.api` и `ru.rom8.rescue.volunteer.api.model`.

## Команды

| Команда | Назначение |
|---|---|
| `./mvnw clean install` | Полная сборка проекта, прогон тестов |
| `docker compose up -d` | Запуск PostgreSQL + Kafka для разработки |
| `./mvnw spring-boot:run` | Запуск приложения (порт 8080) |

Профили:
- `dev` — включает CORS, Kafka bootstrap на `localhost:9092`
- По умолчанию — `application.yml` (параметры БД через переменные окружения)

## Стиль кода и именование

- Java-код в пакете `ru.rom8.rescue.volunteer.*`
- API-интерфейсы генерируются, контроллеры реализуют их вручную
- Маппинг сущностей через **MapStruct**, getter/setter через **Lombok**
- Контроллеры: `VolunteerController`, `AdminVolunteerController`, `InternalVolunteerController`
- Сущности: `Volunteer`, `ContactInfo`, `Location` — наследуют `AbstractAuditableEntity`
- Сообщения в Kafka: `VolunteerIncidentAssignEvent`

### Тесты

- JUnit 5, `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)`
- `@Testcontainers` для PostgreSQL, `@EmbeddedKafka` для Kafka
- Java `HttpClient` для интеграционных HTTP-запросов, Jackson `ObjectMapper` для десериализации
- Фикстуры — JSON-файлы в `src/test/resources/volunteer/`
- Assertions через AssertJ (`assertThat`)
- Нумерация тестов через `@Order` и `@DisplayName` в формате: `"N. Описание на русском"`
- Имя тестового класса: `<Controller>Test`

## VCS: коммиты и PR

- **Язык коммитов**: русский (см. историю: `git log`)
- **Формат**: краткое, ёмкое описание на русском, начинающееся с глагола:
  - `добавить endpoint: ...`
  - `рефакторинг теста ...`
  - `вынести в сообщения`
  - `test: cover assigned volunteer accepting another incident`
- **PR**: описание на русском, ссылка на issue/задачу, при возможности — скриншоты или примеры запросов

## Docker Compose

- `docker-compose.yml` — PostgreSQL 17.10 + Kafka 4.1.1
- Параметры: `POSTGRES_PORT` (по умолчанию `5432`), `KAFKA_PORT` (по умолчанию `9092`)
- Healthcheck для PostgreSQL