# Student MIS Dynamic Reporting System

This project is a full-stack MIS reporting service built with:

- Spring Boot APIs
- ReactJS frontend
- PostgreSQL database
- Dynamic report configuration stored in `dynamic_report`
- Role-Based Access Control (RBAC)
- Audit Logging
- Export support for PDF, JPG, and XLSX

## Features

- **Authentication & Login**: JWT-style secure header token session storage (`user_session` table).
- **Role-Based Access Control (RBAC)**: Enforces access limits on reports and operations based on user roles:
  - **ADMIN**: Access to all reports (including Audit Log), Add/Remove/Edit students.
  - **HOD (Head of Department)**: Access to reports, with filtering locked to their department; can Edit department students.
  - **FACULTY**: Access to reports, with filtering locked to their course; can Edit course students.
  - **STUDENT**: Access to reports, with filtering locked to their own record.
  - **REPORT_VIEWER**: Read-only access to reports and exports.
- **Student CRUD Management**: Interactive visual form to add, edit, and delete student records:
  - Smart lockups enforce HOD and Faculty scope bounds in forms automatically.
- **Audit Logging**: Logs all report run, export, and student edit/create/delete events in the `audit_log` table.
- **Dynamic filter rendering** from database metadata.
- **Dynamic output table rendering** from database metadata.
- **Export support** for `pdf`, `jpg`, and `xlsx`.

## Demo Users

The database is pre-seeded with these demo credentials (password is username + `123`):

| Username | Password | Role | Scopes |
|---|---|---|---|
| `admin` | `admin123` | Administrator | Full access, Audit logs, Add/Remove/Edit students |
| `hod` | `hod123` | HOD | Computer Science department only, Edit CS students |
| `faculty` | `faculty123` | Faculty | DBMS course only, Edit DBMS students |
| `student` | `student123` | Student | Scoped to own record (Rahul Singh) |
| `viewer` | `viewer123` | Report Viewer | Read-only reports & exports |

## Project Structure

```text
backend/
  pom.xml
  src/main/java/com/example/mis/
    controller/
      AuthController.java
      ReportController.java
      StudentController.java
    service/
      AuthService.java
      AuditService.java
      ReportExportService.java
      ReportService.java
      StudentService.java
  src/main/resources/application.yml
  src/main/resources/db/migration/

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
- Docker Desktop or a local PostgreSQL installation

## Run PostgreSQL

Start the database container:

```bash
docker compose up -d
```

The database settings:

```text
database: student_mis
username: postgres
password: postgres
port: 5432
```

Flyway automatically seeds the tables and demo data on backend startup.

## Run Backend

```bash
cd backend
mvn spring-boot:run
```

Backend URL: `http://localhost:8080`

Main Endpoints:

```text
POST /api/auth/login
GET  /api/auth/me
POST /api/auth/logout

GET  /api/reports
GET  /api/reports/{reportId}
GET  /api/reports/{reportId}/options/{filterName}
POST /api/reports/{reportId}/run
POST /api/reports/{reportId}/export/{format}

GET  /api/students/departments
GET  /api/students/courses
POST /api/students
PUT  /api/students/{studentId}
DELETE /api/students/{studentId}
```

## Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend URL: `http://localhost:5173` (or the port Vite outputs in your console)

## Suggested Improvements

- Add a report-builder admin page for creating new report definitions.
- Add pagination for large reports.
- Add chart output for department and result summaries.
- Add safer SQL governance by restricting report queries to approved views.
