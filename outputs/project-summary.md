# Student MIS Project Summary

Created a full-stack dynamic MIS reporting starter with Spring Boot APIs, ReactJS frontend, and PostgreSQL schema/seed data.

Added role-based access:

- Admin, HOD, Faculty, Student, and Report Viewer demo users.
- Bearer-token login sessions stored in PostgreSQL.
- Automatic report scoping by department, course, or student.
- Audit logging for report run/export actions.
- Admin-only Audit Log report.

Key paths:

- `backend/` contains Spring Boot controllers, services, export logic, and Flyway SQL migration.
- `frontend/` contains the React dynamic reporting UI.
- `docker-compose.yml` starts PostgreSQL.
- `README.md` has run instructions.

Run order:

1. `docker compose up -d`
2. `cd backend && mvn spring-boot:run`
3. `cd frontend && npm install && npm run dev`

Note: this machine currently does not have Java or Maven installed, so backend execution was not possible locally in this session.
