CREATE TABLE role (
    role_id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(40) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL
);

CREATE TABLE app_user (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(64) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    role_id BIGINT NOT NULL REFERENCES role(role_id),
    department_id BIGINT REFERENCES department(department_id),
    course_id BIGINT REFERENCES course(course_id),
    student_id BIGINT REFERENCES student(student_id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_session (
    token VARCHAR(120) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(user_id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_log (
    audit_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES app_user(user_id),
    action VARCHAR(80) NOT NULL,
    report_id BIGINT REFERENCES dynamic_report(report_id),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO role (role_code, role_name) VALUES
('ADMIN', 'Administrator'),
('HOD', 'Head of Department'),
('FACULTY', 'Faculty'),
('STUDENT', 'Student'),
('REPORT_VIEWER', 'Report Viewer');

INSERT INTO app_user (username, password_hash, full_name, role_id, department_id, course_id, student_id) VALUES
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'MIS Admin', (SELECT role_id FROM role WHERE role_code = 'ADMIN'), NULL, NULL, NULL),
('hod', '5c8473579466adb756fa9e042efc8d7756217c5f4c950731fcf96bd65ba184e9', 'Computer Science HOD', (SELECT role_id FROM role WHERE role_code = 'HOD'), 1, NULL, NULL),
('faculty', '27041f5856c7387a997252694afb048d1aa939228ffcdbd6285b979b8da20e7a', 'DBMS Faculty', (SELECT role_id FROM role WHERE role_code = 'FACULTY'), 1, 101, NULL),
('student', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'Rahul Singh', (SELECT role_id FROM role WHERE role_code = 'STUDENT'), 1, 101, (SELECT student_id FROM student WHERE student_roll_no = 'CSE001')),
('viewer', '65375049b9e4d7cad6c9ba286fdeb9394b28135a3e84136404cfccfdcc438894', 'Office Report Viewer', (SELECT role_id FROM role WHERE role_code = 'REPORT_VIEWER'), NULL, NULL, NULL);

INSERT INTO dynamic_report (report_name, input_filters, output_columns, query)
VALUES (
    'Audit Log',
    '[
        {"name":"from_date","label":"From Date","type":"date"},
        {"name":"to_date","label":"To Date","type":"date"},
        {"name":"action","label":"Action","type":"textbox"}
    ]'::jsonb,
    '[
        {"column":"created_at","label":"Created At"},
        {"column":"username","label":"Username"},
        {"column":"role_code","label":"Role"},
        {"column":"action","label":"Action"},
        {"column":"report_name","label":"Report"},
        {"column":"details","label":"Details"}
    ]'::jsonb,
    'SELECT a.created_at,
            u.username,
            r.role_code,
            a.action,
            COALESCE(dr.report_name, '''') AS report_name,
            a.details
     FROM audit_log a
     LEFT JOIN app_user u ON u.user_id = a.user_id
     LEFT JOIN role r ON r.role_id = u.role_id
     LEFT JOIN dynamic_report dr ON dr.report_id = a.report_id
     WHERE (CAST(:from_date AS DATE) IS NULL OR CAST(a.created_at AS DATE) >= CAST(:from_date AS DATE))
       AND (CAST(:to_date AS DATE) IS NULL OR CAST(a.created_at AS DATE) <= CAST(:to_date AS DATE))
       AND (CAST(:action AS TEXT) IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT(''%'', CAST(:action AS TEXT), ''%'')))
     ORDER BY a.created_at DESC'
);
