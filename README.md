# TaskFlow API

TaskFlow is a Spring Boot task management API with a browser-based frontend for creating, viewing, and searching tasks. The same API is what the React task manager calls.

## Tech Stack

- Java 21
- Spring Boot 3.5.16
- Spring Web
- Spring Data JPA
- H2 in-memory database for the default profile and for tests
- PostgreSQL for the `prod` profile on Railway
- Maven
- Lombok
- HTML, Bootstrap 5, and vanilla JavaScript

The Docker image, CircleCI pipeline, version tags, and dependency scans are documented in [docs/DEVOPS.md](docs/DEVOPS.md).

## How to Run the Application

### Prerequisites

Install the following tools:

- Java Development Kit (JDK) 21
- Maven 3.6 or later

Confirm that they are available:

```bash
java -version
mvn -version
```

From the project root, run:

```bash
mvn spring-boot:run
```

The application starts at [http://localhost:8080](http://localhost:8080).

To run the automated tests:

```bash
mvn -B test
```

Tests use the default profile and the in-memory H2 database. They do not need Postgres or a network.

On startup the API inserts six sample tasks when the `tasks` table is empty. The default H2 database is in memory, so those tasks are cleared when the application stops and seeded again on the next start. A persistent Postgres database keeps its rows, so a restart does not insert them again.

## Task JSON

```json
{
  "id": 2,
  "title": "Draft the API contract",
  "description": "Agree the task JSON fields and the list, create, update, and delete endpoints.",
  "status": "in-progress",
  "priority": "high",
  "dueDate": "2026-10-24"
}
```

- `status` is one of `todo`, `in-progress`, `done`
- `priority` is one of `low`, `medium`, `high`
- `title` is required
- `description` and `dueDate` are optional. `dueDate` may be `null`

A blank title returns 400. An unknown status or priority returns 400. A missing task id returns 404.

## How to Use the Frontend to Test the Application

1. Start the application with `mvn spring-boot:run`.
2. Open [http://localhost:8080](http://localhost:8080) in a browser.
3. Enter a title and optionally provide a description, priority, due date, and status.
4. Choose a status of To Do, In Progress, or Done. The API stores that choice as `todo`, `in-progress`, or `done`.
5. Choose a priority of Low, Medium, or High. The API stores that choice as `low`, `medium`, or `high`.
6. Select **Add Task** to send a `POST /api/tasks` request.
7. Review the **Task Created** popup, which displays the task returned by the API, including its generated ID.
8. Use the search field to filter the task list by title. Clear the field to display all tasks again.
9. Select **Refresh Tasks** to reload tasks with `GET /api/tasks`.
10. Select a task in the list to open its details using `GET /api/tasks/{id}`.

The page shows each task's title, description, priority, due date, and status.

### Available API Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/tasks` | Retrieve all tasks |
| `GET` | `/api/tasks/{id}` | Retrieve one task by ID |
| `POST` | `/api/tasks` | Create a task |
| `PUT` | `/api/tasks/{id}` | Update a task |
| `DELETE` | `/api/tasks/{id}` | Delete a task |

It is also possible to test the API using Postman or curl.

## Production profile on Railway

The default profile stays on H2. Set `SPRING_PROFILES_ACTIVE=prod` on Railway so the app uses Postgres. `application-prod.properties` builds the JDBC URL from the Postgres service variables, because Railway's `DATABASE_URL` is not a JDBC URL. That profile also turns the H2 console off. The process listens on `PORT` when Railway sets it, and on 8080 otherwise.

| Variable | What to set |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `ALLOWED_ORIGINS` | The frontend origin, for example the Netlify site. Separate extra origins with commas. If this is unset, only `http://localhost:5173` is allowed. |
| `PGHOST` | Reference to the Railway Postgres service |
| `PGPORT` | Reference to the Railway Postgres service |
| `PGDATABASE` | Reference to the Railway Postgres service |
| `PGUSER` | Reference to the Railway Postgres service |
| `PGPASSWORD` | Reference to the Railway Postgres service |

Reference `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD` from the Postgres service in the same Railway project. Do not commit those values.

How to build the image, what CircleCI publishes, and how `VERSION` (`1.1.0`) becomes the Docker Hub tag are in [docs/DEVOPS.md](docs/DEVOPS.md).
