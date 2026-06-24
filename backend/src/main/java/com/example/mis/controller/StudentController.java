package com.example.mis.controller;

import com.example.mis.dto.CurrentUser;
import com.example.mis.dto.StudentRequest;
import com.example.mis.service.AuthService;
import com.example.mis.service.StudentService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService studentService;
    private final AuthService authService;

    public StudentController(StudentService studentService, AuthService authService) {
        this.studentService = studentService;
        this.authService = authService;
    }

    @GetMapping("/departments")
    public List<Map<String, Object>> getDepartments(@RequestHeader("Authorization") String authorization) {
        authService.requireUser(authorization);
        return studentService.listDepartments();
    }

    @GetMapping("/courses")
    public List<Map<String, Object>> getCourses(@RequestHeader("Authorization") String authorization) {
        authService.requireUser(authorization);
        return studentService.listCourses();
    }

    @PostMapping
    public void addStudent(
            @RequestBody StudentRequest request,
            @RequestHeader("Authorization") String authorization
    ) {
        CurrentUser user = authService.requireUser(authorization);
        studentService.addStudent(request, user);
    }

    @PutMapping("/{studentId}")
    public void editStudent(
            @PathVariable Long studentId,
            @RequestBody StudentRequest request,
            @RequestHeader("Authorization") String authorization
    ) {
        CurrentUser user = authService.requireUser(authorization);
        studentService.editStudent(studentId, request, user);
    }

    @DeleteMapping("/{studentId}")
    public void removeStudent(
            @PathVariable Long studentId,
            @RequestHeader("Authorization") String authorization
    ) {
        CurrentUser user = authService.requireUser(authorization);
        studentService.removeStudent(studentId, user);
    }
}
