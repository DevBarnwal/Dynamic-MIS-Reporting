# Student MIS Dynamic Reporting System

This project is a full-stack MIS reporting starter built with:

- Spring Boot APIs
- ReactJS frontend
- PostgreSQL database
- Dynamic report configuration stored in `dynamic_report`
- Export support for PDF, JPG, and XLSX

## Features

- Dynamic filter rendering from database metadata
- Dynamic output table rendering from database metadata
- Student MIS report
- Department summary report
- Result report
- Dropdown options loaded from backend queries
- Report export endpoints for `pdf`, `jpg`, and `xlsx`

## Project Structure

```text
backend/
  pom.xml
  src/main/java/com/example/mis/
  src/main/resources/application.yml
  src/main/resources/db/migration/V1__init_student_mis.sql

frontend/
  package.json
  index.html
  src/main.jsx
  src/styles.css

docker-compose.yml
```

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+
- Docker Desktop, or a local PostgreSQL installation

## Run PostgreSQL

```bash
docker compose up -d
```

The database is created as:

```text
database: student_mis
username: postgres
password: postgres
port: 5432
```

Flyway creates and seeds the tables when the backend starts.

## Run Backend

```bash
cd backend
mvn spring-boot:run
```

Backend URL:

```text
http://localhost:8080
```

Main endpoints:

```text
GET  /api/reports
GET  /api/reports/{reportId}
GET  /api/reports/{reportId}/options/{filterName}
POST /api/reports/{reportId}/run
POST /api/reports/{reportId}/export/{format}
```

Export formats:

```text
pdf
jpg
xlsx
```

## Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend URL:

```text
http://localhost:5173
```

## Suggested Improvements

- Add login and role-based report access.
- Add a report-builder admin page for creating new report definitions.
- Add pagination for large reports.
- Add audit logs for generated and exported reports.
- Add chart output for department and result summaries.
- Add safer SQL governance by restricting report queries to approved views.
