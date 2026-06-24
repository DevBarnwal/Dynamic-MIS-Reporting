package com.example.mis.service;

import com.example.mis.dto.CurrentUser;
import com.example.mis.dto.StudentRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentService {
    private final NamedParameterJdbcTemplate jdbc;
    private final AuditService auditService;

    public StudentService(NamedParameterJdbcTemplate jdbc, AuditService auditService) {
        this.jdbc = jdbc;
        this.auditService = auditService;
    }

    public List<Map<String, Object>> listDepartments() {
        return jdbc.queryForList("SELECT department_id AS id, department_name AS name FROM department ORDER BY department_name", Map.of());
    }

    public List<Map<String, Object>> listCourses() {
        return jdbc.queryForList("SELECT course_id AS id, course_name AS name, department_id AS \"departmentId\" FROM course ORDER BY course_name", Map.of());
    }

    public void addStudent(StudentRequest request, CurrentUser user) {
        if (!user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can add students");
        }

        String deptName = getDepartmentName(request.departmentId());
        String courseName = getCourseName(request.courseId());

        jdbc.update("""
                INSERT INTO student (student_roll_no, student_name, department_id, department_name, course_id, course_name, semester, marks, attendance_percentage)
                VALUES (:rollNo, :name, :deptId, :deptName, :courseId, :courseName, :semester, :marks, :attendance)
                """, Map.of(
                "rollNo", request.studentRollNo(),
                "name", request.studentName(),
                "deptId", request.departmentId(),
                "deptName", deptName,
                "courseId", request.courseId(),
                "courseName", courseName,
                "semester", request.semester(),
                "marks", request.marks(),
                "attendance", request.attendancePercentage()
        ));

        auditService.log(user, "ADD_STUDENT", null, "Added student " + request.studentRollNo() + " - " + request.studentName());
    }

    public void editStudent(Long studentId, StudentRequest request, CurrentUser user) {
        if (!user.isAdmin() && !"HOD".equals(user.roleCode()) && !"FACULTY".equals(user.roleCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized to edit student");
        }

        Map<String, Object> existing = getStudentById(studentId);
        Long existingDeptId = ((Number) existing.get("department_id")).longValue();
        Long existingCourseId = ((Number) existing.get("course_id")).longValue();

        if ("HOD".equals(user.roleCode()) && !existingDeptId.equals(user.departmentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "HOD can only edit students in their department");
        }
        if ("FACULTY".equals(user.roleCode()) && !existingCourseId.equals(user.courseId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Faculty can only edit students in their course");
        }

        String deptName = getDepartmentName(request.departmentId());
        String courseName = getCourseName(request.courseId());

        jdbc.update("""
                UPDATE student
                SET student_roll_no = :rollNo,
                    student_name = :name,
                    department_id = :deptId,
                    department_name = :deptName,
                    course_id = :courseId,
                    course_name = :courseName,
                    semester = :semester,
                    marks = :marks,
                    attendance_percentage = :attendance
                WHERE student_id = :studentId
                """, Map.of(
                "studentId", studentId,
                "rollNo", request.studentRollNo(),
                "name", request.studentName(),
                "deptId", request.departmentId(),
                "deptName", deptName,
                "courseId", request.courseId(),
                "courseName", courseName,
                "semester", request.semester(),
                "marks", request.marks(),
                "attendance", request.attendancePercentage()
        ));

        auditService.log(user, "EDIT_STUDENT", null, "Edited student ID " + studentId + " (" + request.studentRollNo() + ")");
    }

    public void removeStudent(Long studentId, CurrentUser user) {
        if (!user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can remove students");
        }

        Map<String, Object> existing = getStudentById(studentId);
        String rollNo = (String) existing.get("student_roll_no");

        jdbc.update("UPDATE app_user SET student_id = NULL WHERE student_id = :studentId", Map.of("studentId", studentId));
        jdbc.update("DELETE FROM student WHERE student_id = :studentId", Map.of("studentId", studentId));

        auditService.log(user, "REMOVE_STUDENT", null, "Removed student " + rollNo + " (ID: " + studentId + ")");
    }

    private Map<String, Object> getStudentById(Long studentId) {
        try {
            return jdbc.queryForMap("SELECT student_id, student_roll_no, department_id, course_id FROM student WHERE student_id = :studentId", Map.of("studentId", studentId));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }
    }

    private String getDepartmentName(Long deptId) {
        try {
            return jdbc.queryForObject("SELECT department_name FROM department WHERE department_id = :deptId", Map.of("deptId", deptId), String.class);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid department ID");
        }
    }

    private String getCourseName(Long courseId) {
        try {
            return jdbc.queryForObject("SELECT course_name FROM course WHERE course_id = :courseId", Map.of("courseId", courseId), String.class);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid course ID");
        }
    }
}
