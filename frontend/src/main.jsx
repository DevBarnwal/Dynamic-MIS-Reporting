import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Download, FileSpreadsheet, FileText, Image, LogOut, RefreshCcw, Search, ShieldCheck, Edit2, Trash2, Plus, X } from 'lucide-react';
import './styles.css';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
const STORED_AUTH = 'student_mis_auth';

function App() {
  const [auth, setAuth] = useState(() => readStoredAuth());
  const [reports, setReports] = useState([]);
  const [selectedReportId, setSelectedReportId] = useState('');
  const [report, setReport] = useState(null);
  const [filters, setFilters] = useState({});
  const [options, setOptions] = useState({});
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  // Student CRUD modal state
  const [studentModal, setStudentModal] = useState({ isOpen: false, mode: 'add', student: null });
  const [departments, setDepartments] = useState([]);
  const [courses, setCourses] = useState([]);

  useEffect(() => {
    if (!auth?.token) {
      return;
    }

    apiFetch('/auth/me', auth.token)
      .then((user) => setAuth((current) => storeAuth({ ...current, user })))
      .then(loadReports)
      .catch(() => {
        clearAuth();
        setAuth(null);
      });
  }, []);

  useEffect(() => {
    if (!auth?.token) {
      return;
    }
    loadReports();
  }, [auth?.token]);

  useEffect(() => {
    if (!selectedReportId || !auth?.token) {
      return;
    }

    setLoading(true);
    setResult(null);
    setMessage('');

    apiFetch(`/reports/${selectedReportId}`, auth.token)
      .then(async (definition) => {
        setReport(definition);
        setFilters(createEmptyFilters(definition.inputFilters));
        await loadDropdownOptions(definition);
      })
      .catch((error) => setMessage(error.message))
      .finally(() => setLoading(false));
  }, [selectedReportId, auth?.token]);

  // Load departments and courses when the student management modal is open
  useEffect(() => {
    if (studentModal.isOpen && auth?.token) {
      apiFetch('/students/departments', auth.token)
        .then(setDepartments)
        .catch((err) => setMessage(err.message));
      apiFetch('/students/courses', auth.token)
        .then(setCourses)
        .catch((err) => setMessage(err.message));
    }
  }, [studentModal.isOpen, auth?.token]);

  async function loadReports() {
    if (!auth?.token) {
      return;
    }

    try {
      const data = await apiFetch('/reports', auth.token);
      setReports(data);
      if (data.length > 0) {
        setSelectedReportId((current) => current || String(data[0].reportId));
      }
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function loadDropdownOptions(definition) {
    const dropdowns = definition.inputFilters.filter((field) => field.type === 'dropdown');
    const loaded = {};

    await Promise.all(
      dropdowns.map(async (field) => {
        loaded[field.name] = await apiFetch(`/reports/${definition.reportId}/options/${field.name}`, auth.token);
      })
    );

    setOptions(loaded);
  }

  async function handleLogin(username, password) {
    setLoading(true);
    setMessage('');
    try {
      const data = await apiFetch('/auth/login', null, {
        method: 'POST',
        body: JSON.stringify({ username, password })
      });
      setAuth(storeAuth(data));
      setSelectedReportId('');
      setReport(null);
      setResult(null);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleLogout() {
    if (auth?.token) {
      await apiFetch('/auth/logout', auth.token, { method: 'POST' }).catch(() => null);
    }
    clearAuth();
    setAuth(null);
    setReports([]);
    setSelectedReportId('');
    setReport(null);
    setResult(null);
  }

  async function runReport() {
    if (!report) {
      return;
    }

    const validationMessage = validateFilters(filters);
    if (validationMessage) {
      setMessage(validationMessage);
      return;
    }

    setLoading(true);
    setMessage('');

    try {
      const data = await apiFetch(`/reports/${report.reportId}/run`, auth.token, {
        method: 'POST',
        body: JSON.stringify({ filters: cleanFilters(filters) })
      });
      setResult(data);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function exportReport(format) {
    if (!report) {
      return;
    }

    setMessage('');
    try {
      const response = await fetch(`${API_BASE}/reports/${report.reportId}/export/${format}`, {
        method: 'POST',
        headers: authHeaders(auth.token),
        body: JSON.stringify({ filters: cleanFilters(filters) })
      });

      if (!response.ok) {
        throw new Error(await response.text() || `Export failed with status ${response.status}`);
      }

      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filenameFor(report.reportName, format);
      link.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      setMessage(error.message);
    }
  }

  // Handle saving new or edited student details
  async function handleSaveStudent(studentData) {
    setLoading(true);
    setMessage('');
    try {
      const isEdit = studentModal.mode === 'edit';
      const url = isEdit ? `/students/${studentModal.student.student_id}` : '/students';
      const method = isEdit ? 'PUT' : 'POST';
      
      await apiFetch(url, auth.token, {
        method,
        body: JSON.stringify(studentData)
      });
      
      setStudentModal({ isOpen: false, mode: 'add', student: null });
      
      // Auto refresh current report if one is selected
      if (result) {
        runReport();
      }
    } catch (error) {
      alert(error.message);
    } finally {
      setLoading(false);
    }
  }

  // Handle student deletion
  async function handleDeleteStudent(row) {
    if (!window.confirm(`Are you sure you want to remove student ${row.student_name} (${row.student_roll_no})?`)) {
      return;
    }
    setLoading(true);
    setMessage('');
    try {
      await apiFetch(`/students/${row.student_id}`, auth.token, {
        method: 'DELETE'
      });
      // Refresh report
      if (result) {
        runReport();
      }
    } catch (error) {
      alert(error.message);
    } finally {
      setLoading(false);
    }
  }

  const outputColumns = useMemo(() => result?.outputColumns || report?.outputColumns || [], [report, result]);
  const rows = result?.rows || [];

  // Determine if Actions column (Edit / Delete) should be displayed
  const showActions = useMemo(() => {
    return (report?.reportName === 'Student MIS' || report?.reportName === 'Result Report') &&
           ['ADMIN', 'HOD', 'FACULTY'].includes(auth?.user?.roleCode);
  }, [report, auth]);

  function canEditRow(row) {
    if (!auth?.user) return false;
    if (auth.user.roleCode === 'ADMIN') return true;
    if (auth.user.roleCode === 'HOD') return String(row.department_id) === String(auth.user.departmentId);
    if (auth.user.roleCode === 'FACULTY') return String(row.course_id) === String(auth.user.courseId);
    return false;
  }

  function canDeleteRow(row) {
    if (!auth?.user) return false;
    return auth.user.roleCode === 'ADMIN';
  }

  if (!auth?.token) {
    return <LoginScreen onLogin={handleLogin} loading={loading} message={message} />;
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Dynamic Reporting Service</p>
          <h1>Student MIS</h1>
        </div>
        <div className="user-strip">
          <div className="role-pill" title="Current user role">
            <ShieldCheck size={18} />
            <span>{auth.user.fullName}</span>
            <strong>{auth.user.roleCode}</strong>
          </div>
          <select
            className="report-select"
            value={selectedReportId}
            onChange={(event) => setSelectedReportId(event.target.value)}
            aria-label="Select report"
          >
            {reports.map((item) => (
              <option key={item.reportId} value={item.reportId}>
                {item.reportName}
              </option>
            ))}
          </select>
          <button type="button" className="icon-button" title="Logout" onClick={handleLogout}>
            <LogOut size={18} />
          </button>
        </div>
      </header>

      <section className="workspace">
        <aside className="filter-panel">
          <div className="panel-heading">
            <h2>Filters</h2>
            <button
              type="button"
              className="icon-button"
              title="Reset filters"
              onClick={() => report && setFilters(createEmptyFilters(report.inputFilters))}
            >
              <RefreshCcw size={18} />
            </button>
          </div>

          <RoleScope user={auth.user} />

          <div className="filter-grid">
            {report?.inputFilters.map((field) => (
              <label className="field" key={field.name}>
                <span>{field.label}</span>
                {field.type === 'dropdown' ? (
                  <select
                    value={filters[field.name] || ''}
                    onChange={(event) => setFilters({ ...filters, [field.name]: event.target.value })}
                  >
                    <option value="">All</option>
                    {(options[field.name] || []).map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                ) : (
                  <input
                    type={field.type === 'date' ? 'date' : field.type === 'number' ? 'number' : 'text'}
                    value={filters[field.name] || ''}
                    onChange={(event) => setFilters({ ...filters, [field.name]: event.target.value })}
                  />
                )}
              </label>
            ))}
          </div>

          <button type="button" className="primary-action" onClick={runReport} disabled={loading || !report}>
            <Search size={18} />
            <span>{loading ? 'Loading' : 'Search'}</span>
          </button>

          {message && <p className="error-text">{message}</p>}
        </aside>

        <section className="result-area">
          <div className="result-toolbar">
            <div>
              <h2>{report?.reportName || 'Report'}</h2>
              <p>{result ? `${result.totalRows} rows generated` : 'Apply filters to generate the report'}</p>
            </div>
            <div className="toolbar-actions">
              {auth?.user?.roleCode === 'ADMIN' && (report?.reportName === 'Student MIS' || report?.reportName === 'Result Report') && (
                <button
                  type="button"
                  className="add-student-btn"
                  onClick={() => setStudentModal({ isOpen: true, mode: 'add', student: null })}
                >
                  <Plus size={16} />
                  <span>Add Student</span>
                </button>
              )}
              <div className="export-buttons" aria-label="Export report">
                <button type="button" title="Export PDF" onClick={() => exportReport('pdf')} disabled={!report}>
                  <FileText size={18} />
                </button>
                <button type="button" title="Export XLSX" onClick={() => exportReport('xlsx')} disabled={!report}>
                  <FileSpreadsheet size={18} />
                </button>
                <button type="button" title="Export JPG" onClick={() => exportReport('jpg')} disabled={!report}>
                  <Image size={18} />
                </button>
                <button type="button" title="Download current report" onClick={() => exportReport('xlsx')} disabled={!report}>
                  <Download size={18} />
                </button>
              </div>
            </div>
          </div>

          <div className="table-frame">
            <table>
              <thead>
                <tr>
                  {outputColumns.map((column) => (
                    <th key={column.column}>{column.label}</th>
                  ))}
                  {showActions && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {rows.map((row, rowIndex) => (
                  <tr key={`${row.student_roll_no || row.department_name || row.created_at || 'row'}-${rowIndex}`}>
                    {outputColumns.map((column) => (
                      <td key={column.column}>{formatCell(row[column.column])}</td>
                    ))}
                    {showActions && (
                      <td>
                        <div className="row-actions">
                          {canEditRow(row) && (
                            <button
                              type="button"
                              className="action-btn edit-btn"
                              title="Edit Student"
                              onClick={() => setStudentModal({ isOpen: true, mode: 'edit', student: row })}
                            >
                              <Edit2 size={14} />
                            </button>
                          )}
                          {canDeleteRow(row) && (
                            <button
                              type="button"
                              className="action-btn delete-btn"
                              title="Remove Student"
                              onClick={() => handleDeleteStudent(row)}
                            >
                              <Trash2 size={14} />
                            </button>
                          )}
                        </div>
                      </td>
                    )}
                  </tr>
                ))}
                {!rows.length && (
                  <tr>
                    <td className="empty-cell" colSpan={Math.max(outputColumns.length + (showActions ? 1 : 0), 1)}>
                      No report data yet
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </section>

      {studentModal.isOpen && (
        <StudentModal
          mode={studentModal.mode}
          student={studentModal.student}
          user={auth.user}
          departments={departments}
          courses={courses}
          onClose={() => setStudentModal({ isOpen: false, mode: 'add', student: null })}
          onSave={handleSaveStudent}
        />
      )}
    </main>
  );
}

function LoginScreen({ onLogin, loading, message }) {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');

  return (
    <main className="login-shell">
      <section className="login-panel">
        <p className="eyebrow">Dynamic Reporting Service</p>
        <h1>Student MIS</h1>
        <form
          onSubmit={(event) => {
            event.preventDefault();
            onLogin(username, password);
          }}
        >
          <label className="field">
            <span>Username</span>
            <input value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" />
          </label>
          <label className="field">
            <span>Password</span>
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" />
          </label>
          <button type="submit" className="primary-action" disabled={loading}>
            <ShieldCheck size={18} />
            <span>{loading ? 'Signing in' : 'Login'}</span>
          </button>
        </form>
        <div className="demo-users">
          <strong>Demo users</strong>
          <span>admin/admin123</span>
          <span>hod/hod123</span>
          <span>faculty/faculty123</span>
          <span>student/student123</span>
          <span>viewer/viewer123</span>
        </div>
        {message && <p className="error-text">{message}</p>}
      </section>
    </main>
  );
}

function RoleScope({ user }) {
  const text = {
    ADMIN: 'Admin access: all reports and audit logs.',
    REPORT_VIEWER: 'Viewer access: read and export reports.',
    HOD: 'HOD access: results are restricted to your department.',
    FACULTY: 'Faculty access: results are restricted to your course.',
    STUDENT: 'Student access: results are restricted to your own record.'
  }[user.roleCode] || 'Role-based access is active.';

  return <p className="scope-note">{text}</p>;
}

function StudentModal({ mode, student, user, departments, courses, onClose, onSave }) {
  const [rollNo, setRollNo] = useState(student?.student_roll_no || '');
  const [name, setName] = useState(student?.student_name || '');
  const [deptId, setDeptId] = useState(() => {
    if (student?.department_id) return String(student.department_id);
    if (user.roleCode === 'HOD' || user.roleCode === 'FACULTY') return String(user.departmentId);
    return '';
  });
  const [courseId, setCourseId] = useState(() => {
    if (student?.course_id) return String(student.course_id);
    if (user.roleCode === 'FACULTY') return String(user.courseId);
    return '';
  });
  const [semester, setSemester] = useState(student?.semester || '');
  const [marks, setMarks] = useState(student?.marks || '');
  const [attendance, setAttendance] = useState(student?.attendance_percentage || '');

  const filteredCourses = useMemo(() => {
    if (!deptId) return [];
    return courses.filter((c) => {
      const courseDeptId = c.departmentId !== undefined ? c.departmentId : c.departmentid;
      return String(courseDeptId) === String(deptId);
    });
  }, [courses, deptId]);

  function handleSubmit(e) {
    e.preventDefault();
    if (!rollNo.trim() || !name.trim() || !deptId || !courseId || !semester || marks === '' || attendance === '') {
      alert('Please fill in all fields');
      return;
    }
    onSave({
      studentRollNo: rollNo.trim(),
      studentName: name.trim(),
      departmentId: Number(deptId),
      courseId: Number(courseId),
      semester: Number(semester),
      marks: Number(marks),
      attendancePercentage: Number(attendance)
    });
  }

  const isHod = user.roleCode === 'HOD';
  const isFaculty = user.roleCode === 'FACULTY';

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <div className="modal-header">
          <h2>{mode === 'edit' ? 'Edit Student' : 'Add New Student'}</h2>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Close modal">
            <X size={18} />
          </button>
        </div>
        <form onSubmit={handleSubmit} className="modal-form">
          <div className="modal-form-row">
            <label className="field">
              <span>Roll Number</span>
              <input
                type="text"
                value={rollNo}
                onChange={(e) => setRollNo(e.target.value)}
                placeholder="e.g. CSE101"
                required
              />
            </label>
            <label className="field">
              <span>Student Name</span>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. John Doe"
                required
              />
            </label>
          </div>

          <div className="modal-form-row">
            <label className="field">
              <span>Department</span>
              <select
                value={deptId}
                onChange={(e) => {
                  setDeptId(e.target.value);
                  setCourseId('');
                }}
                disabled={isHod || isFaculty}
                required
              >
                <option value="">Select Department</option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Course</span>
              <select
                value={courseId}
                onChange={(e) => setCourseId(e.target.value)}
                disabled={isFaculty}
                required
              >
                <option value="">Select Course</option>
                {filteredCourses.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="modal-form-row">
            <label className="field">
              <span>Semester</span>
              <input
                type="number"
                min="1"
                max="8"
                value={semester}
                onChange={(e) => setSemester(e.target.value)}
                required
              />
            </label>
            <label className="field">
              <span>Marks</span>
              <input
                type="number"
                step="0.01"
                min="0"
                max="100"
                value={marks}
                onChange={(e) => setMarks(e.target.value)}
                required
              />
            </label>
          </div>

          <label className="field">
            <span>Attendance %</span>
            <input
              type="number"
              step="0.01"
              min="0"
              max="100"
              value={attendance}
              onChange={(e) => setAttendance(e.target.value)}
              required
            />
          </label>

          <div className="modal-footer">
            <button type="button" className="secondary-action" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="primary-action" style={{ width: 'auto', minWidth: '120px' }}>
              Save Changes
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function createEmptyFilters(inputFilters = []) {
  return inputFilters.reduce((acc, field) => ({ ...acc, [field.name]: '' }), {});
}

function cleanFilters(filters) {
  return Object.fromEntries(Object.entries(filters).map(([key, value]) => [key, value === '' ? null : value]));
}

function validateFilters(filters) {
  if (filters.from_date && filters.to_date && filters.from_date > filters.to_date) {
    return 'From Date cannot be after To Date.';
  }
  return '';
}

function formatCell(value) {
  if (value === null || value === undefined) {
    return '';
  }
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value)) {
    return value.replace('T', ' ').slice(0, 16);
  }
  return String(value);
}

function filenameFor(reportName, format) {
  const extension = format === 'jpeg' ? 'jpg' : format;
  return `${reportName.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '')}.${extension}`;
}

function authHeaders(token) {
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  };
}

async function apiFetch(path, token, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      ...authHeaders(token),
      ...(options.headers || {})
    }
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Request failed with status ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

function readStoredAuth() {
  try {
    return JSON.parse(localStorage.getItem(STORED_AUTH));
  } catch {
    return null;
  }
}

function storeAuth(auth) {
  localStorage.setItem(STORED_AUTH, JSON.stringify(auth));
  return auth;
}

function clearAuth() {
  localStorage.removeItem(STORED_AUTH);
}

createRoot(document.getElementById('root')).render(<App />);
