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
- Login with role-based report access
- Admin, HOD, Faculty, Student, and Report Viewer demo roles
- Automatic department/course/student scoping on report results
- Audit log report for Admin users
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

## Demo Logins

```text
admin   / admin123    -> all reports, exports, audit log
hod     / hod123      -> Computer Science department scope
faculty / faculty123  -> DBMS course scope
student / student123  -> Rahul Singh own-record scope
viewer  / viewer123   -> read/export report viewer
```

After adding these changes to an existing database, restart the backend. Flyway will apply `V3__role_based_access.sql` automatically.

## Suggested Improvements

- Add a report-builder admin page for creating new report definitions.
- Add pagination for large reports.
- Add user-management screens for Admin.
- Add chart output for department and result summaries.
- Add safer SQL governance by restricting report queries to approved views.
