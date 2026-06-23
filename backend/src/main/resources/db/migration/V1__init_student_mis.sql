CREATE TABLE department (
    department_id BIGINT PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE course (
    course_id BIGINT PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL REFERENCES department(department_id),
    credits INTEGER NOT NULL DEFAULT 4
);

CREATE TABLE student (
    student_id BIGSERIAL PRIMARY KEY,
    student_roll_no VARCHAR(30) NOT NULL UNIQUE,
    student_name VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL REFERENCES department(department_id),
    department_name VARCHAR(100) NOT NULL,
    course_id BIGINT NOT NULL REFERENCES course(course_id),
    course_name VARCHAR(100) NOT NULL,
    semester INTEGER NOT NULL,
    marks NUMERIC(10, 2) NOT NULL,
    attendance_percentage NUMERIC(5, 2) NOT NULL,
    admission_datetime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dynamic_report (
    report_id BIGSERIAL PRIMARY KEY,
    report_name VARCHAR(100) NOT NULL,
    input_filters JSONB NOT NULL,
    output_columns JSONB NOT NULL,
    query TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO department (department_id, department_name) VALUES
(1, 'Computer Science'),
(2, 'Mechanical'),
(3, 'Commerce'),
(4, 'Mathematics');

INSERT INTO course (course_id, course_name, department_id, credits) VALUES
(101, 'DBMS', 1, 4),
(102, 'Operating System', 1, 4),
(103, 'Computer Networks', 1, 4),
(104, 'Data Structures', 1, 4),
(201, 'Thermodynamics', 2, 4),
(202, 'Fluid Mechanics', 2, 4),
(301, 'Accounting', 3, 3),
(302, 'Business Economics', 3, 3),
(401, 'Linear Algebra', 4, 4),
(402, 'Statistics', 4, 4);

INSERT INTO student (
    student_roll_no,
    student_name,
    department_id,
    department_name,
    course_id,
    course_name,
    semester,
    marks,
    attendance_percentage,
    admission_datetime
) VALUES
('CSE001', 'Rahul Singh', 1, 'Computer Science', 101, 'DBMS', 3, 88.50, 92, '2026-01-10 09:30:00'),
('CSE002', 'Priya Kapoor', 1, 'Computer Science', 102, 'Operating System', 5, 91.00, 95, '2026-01-15 10:15:00'),
('ME001', 'Aman Verma', 2, 'Mechanical', 201, 'Thermodynamics', 2, 69.00, 80, '2026-02-01 11:00:00'),
('COM001', 'Sneha Das', 3, 'Commerce', 301, 'Accounting', 4, 82.00, 89, '2026-02-10 01:45:00'),
('MTH001', 'Vikas Sharma', 4, 'Mathematics', 401, 'Linear Algebra', 1, 95.00, 98, '2026-03-05 08:20:00'),
('CSE003', 'Anjali Roy', 1, 'Computer Science', 103, 'Computer Networks', 6, 87.00, 90, '2026-03-12 10:00:00'),
('ME002', 'Rohit Yadav', 2, 'Mechanical', 202, 'Fluid Mechanics', 3, 74.50, 84, '2026-03-18 09:10:00'),
('COM002', 'Neha Gupta', 3, 'Commerce', 302, 'Business Economics', 2, 78.00, 88, '2026-04-01 11:20:00'),
('MTH002', 'Karan Mehta', 4, 'Mathematics', 402, 'Statistics', 5, 89.00, 93, '2026-04-05 12:15:00'),
('CSE004', 'Simran Kaur', 1, 'Computer Science', 104, 'Data Structures', 2, 93.00, 97, '2026-04-12 08:45:00');

INSERT INTO dynamic_report (report_name, input_filters, output_columns, query) VALUES
(
    'Student MIS',
    '[
        {"name":"from_date","label":"From Date","type":"date"},
        {"name":"to_date","label":"To Date","type":"date"},
        {"name":"department_id","label":"Department","type":"dropdown","dropdown_query":"SELECT department_id AS value, department_name AS label FROM department ORDER BY department_name"},
        {"name":"course_id","label":"Course","type":"dropdown","dropdown_query":"SELECT course_id AS value, course_name AS label FROM course ORDER BY course_name"},
        {"name":"student_name","label":"Student Name","type":"textbox"},
        {"name":"student_roll_no","label":"Student Roll No","type":"textbox"}
    ]'::jsonb,
    '[
        {"column":"student_roll_no","label":"Roll No"},
        {"column":"student_name","label":"Student Name"},
        {"column":"department_name","label":"Department"},
        {"column":"course_name","label":"Course"},
        {"column":"semester","label":"Semester"},
        {"column":"marks","label":"Marks"},
        {"column":"attendance_percentage","label":"Attendance %"},
        {"column":"admission_datetime","label":"Admission Date"}
    ]'::jsonb,
    'SELECT student_roll_no, student_name, department_name, course_name, semester, marks, attendance_percentage, admission_datetime
     FROM student
     WHERE (CAST(:from_date AS DATE) IS NULL OR CAST(admission_datetime AS date) >= CAST(:from_date AS DATE))
       AND (CAST(:to_date AS DATE) IS NULL OR CAST(admission_datetime AS date) <= CAST(:to_date AS DATE))
       AND (CAST(:department_id AS BIGINT) IS NULL OR department_id = CAST(:department_id AS BIGINT))
       AND (CAST(:course_id AS BIGINT) IS NULL OR course_id = CAST(:course_id AS BIGINT))
       AND (CAST(:student_name AS TEXT) IS NULL OR LOWER(student_name) LIKE LOWER(CONCAT(''%'', CAST(:student_name AS TEXT), ''%'')))
       AND (CAST(:student_roll_no AS TEXT) IS NULL OR student_roll_no = CAST(:student_roll_no AS TEXT))
     ORDER BY admission_datetime DESC'
),
(
    'Department Report',
    '[
        {"name":"department_id","label":"Department","type":"dropdown","dropdown_query":"SELECT department_id AS value, department_name AS label FROM department ORDER BY department_name"}
    ]'::jsonb,
    '[
        {"column":"department_name","label":"Department"},
        {"column":"student_count","label":"Students"},
        {"column":"avg_marks","label":"Average Marks"},
        {"column":"avg_attendance","label":"Average Attendance"}
    ]'::jsonb,
    'SELECT department_name,
            COUNT(*) AS student_count,
            ROUND(AVG(marks), 2) AS avg_marks,
            ROUND(AVG(attendance_percentage), 2) AS avg_attendance
     FROM student
     WHERE (CAST(:department_id AS BIGINT) IS NULL OR department_id = CAST(:department_id AS BIGINT))
     GROUP BY department_name
     ORDER BY department_name'
),
(
    'Result Report',
    '[
        {"name":"department_id","label":"Department","type":"dropdown","dropdown_query":"SELECT department_id AS value, department_name AS label FROM department ORDER BY department_name"},
        {"name":"semester","label":"Semester","type":"number"},
        {"name":"min_marks","label":"Minimum Marks","type":"number"}
    ]'::jsonb,
    '[
        {"column":"student_roll_no","label":"Roll No"},
        {"column":"student_name","label":"Student Name"},
        {"column":"department_name","label":"Department"},
        {"column":"semester","label":"Semester"},
        {"column":"marks","label":"Marks"},
        {"column":"result_status","label":"Result"}
    ]'::jsonb,
    'SELECT student_roll_no,
            student_name,
            department_name,
            semester,
            marks,
            CASE WHEN marks >= 40 THEN ''Pass'' ELSE ''Fail'' END AS result_status
     FROM student
     WHERE (CAST(:department_id AS BIGINT) IS NULL OR department_id = CAST(:department_id AS BIGINT))
       AND (CAST(:semester AS NUMERIC) IS NULL OR semester = CAST(:semester AS INTEGER))
       AND (CAST(:min_marks AS NUMERIC) IS NULL OR marks >= CAST(:min_marks AS NUMERIC))
     ORDER BY marks DESC, student_name'
);
