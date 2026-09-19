# TaskFlow API

TaskFlow is a Spring Boot task management API with a browser-based frontend for creating, viewing, and searching tasks.

## Tech Stack

- Java 21
- Spring Boot 3.2.2
- Spring Web
- Spring Data JPA
- H2 in-memory database
- Maven
- Lombok
- HTML, Bootstrap 5, and vanilla JavaScript

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
mvn test
```

The application uses an in-memory H2 database. Task data is cleared whenever the application stops.

## How to Use the Frontend to Test the Application

1. Start the application with `mvn spring-boot:run`.
2. Open [http://localhost:8080](http://localhost:8080) in a browser.
3. Enter a title and optionally provide a description, priority, due date, and completed value.
4. Select `False` or `True` from the **Completed** dropdown. The default is `False`.
5. Select **Add Task** to send a `POST /api/tasks` request.
6. Review the **Task Created** popup, which displays the task returned by the API, including its generated ID.
7. Use the search field to filter the task list by title. Clear the field to display all tasks again.
8. Select **Refresh Tasks** to reload tasks with `GET /api/tasks`.
9. Select a task in the list to open its details using `GET /api/tasks/{id}`.

The frontend displays each task's title, description, priority, due date, and completed value.

### Available API Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/tasks` | Retrieve all tasks |
| `GET` | `/api/tasks/{id}` | Retrieve one task by ID |
| `POST` | `/api/tasks` | Create a task |

It is also possible to test the api using Postman or curl.