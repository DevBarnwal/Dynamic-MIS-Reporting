UPDATE dynamic_report
SET query = 'SELECT student_id, department_id, course_id, student_roll_no, student_name, department_name, course_name, semester, marks, attendance_percentage, admission_datetime
     FROM student
     WHERE (CAST(:from_date AS DATE) IS NULL OR CAST(admission_datetime AS date) >= CAST(:from_date AS DATE))
       AND (CAST(:to_date AS DATE) IS NULL OR CAST(admission_datetime AS date) <= CAST(:to_date AS DATE))
       AND (CAST(:department_id AS BIGINT) IS NULL OR department_id = CAST(:department_id AS BIGINT))
       AND (CAST(:course_id AS BIGINT) IS NULL OR course_id = CAST(:course_id AS BIGINT))
       AND (CAST(:student_name AS TEXT) IS NULL OR LOWER(student_name) LIKE LOWER(CONCAT(''%'', CAST(:student_name AS TEXT), ''%'')))
       AND (CAST(:student_roll_no AS TEXT) IS NULL OR student_roll_no = CAST(:student_roll_no AS TEXT))
     ORDER BY admission_datetime DESC'
WHERE report_name = 'Student MIS';

UPDATE dynamic_report
SET query = 'SELECT student_id, department_id, course_id, student_roll_no,
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
WHERE report_name = 'Result Report';
