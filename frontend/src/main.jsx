import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Download, FileSpreadsheet, FileText, Image, RefreshCcw, Search } from 'lucide-react';
import './styles.css';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

function App() {
  const [reports, setReports] = useState([]);
  const [selectedReportId, setSelectedReportId] = useState('');
  const [report, setReport] = useState(null);
  const [filters, setFilters] = useState({});
  const [options, setOptions] = useState({});
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    fetch(`${API_BASE}/reports`)
      .then(checkResponse)
      .then((data) => {
        setReports(data);
        if (data.length > 0) {
          setSelectedReportId(String(data[0].reportId));
        }
      })
      .catch((error) => setMessage(error.message));
  }, []);

  useEffect(() => {
    if (!selectedReportId) {
      return;
    }

    setLoading(true);
    setResult(null);
    setMessage('');

    fetch(`${API_BASE}/reports/${selectedReportId}`)
      .then(checkResponse)
      .then(async (definition) => {
        setReport(definition);
        setFilters(createEmptyFilters(definition.inputFilters));
        await loadDropdownOptions(definition);
      })
      .catch((error) => setMessage(error.message))
      .finally(() => setLoading(false));
  }, [selectedReportId]);

  async function loadDropdownOptions(definition) {
    const dropdowns = definition.inputFilters.filter((field) => field.type === 'dropdown');
    const loaded = {};

    await Promise.all(
      dropdowns.map(async (field) => {
        const response = await fetch(`${API_BASE}/reports/${definition.reportId}/options/${field.name}`);
        loaded[field.name] = await checkResponse(response);
      })
    );

    setOptions(loaded);
  }

  async function runReport() {
    if (!report) {
      return;
    }

    setLoading(true);
    setMessage('');

    try {
      const response = await fetch(`${API_BASE}/reports/${report.reportId}/run`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ filters: cleanFilters(filters) })
      });
      setResult(await checkResponse(response));
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
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ filters: cleanFilters(filters) })
      });

      if (!response.ok) {
        throw new Error(`Export failed with status ${response.status}`);
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

  const outputColumns = useMemo(() => result?.outputColumns || report?.outputColumns || [], [report, result]);
  const rows = result?.rows || [];

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Dynamic Reporting Service</p>
          <h1>Student MIS</h1>
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

          <div className="table-frame">
            <table>
              <thead>
                <tr>
                  {outputColumns.map((column) => (
                    <th key={column.column}>{column.label}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((row, rowIndex) => (
                  <tr key={`${row.student_roll_no || row.department_name || 'row'}-${rowIndex}`}>
                    {outputColumns.map((column) => (
                      <td key={column.column}>{formatCell(row[column.column])}</td>
                    ))}
                  </tr>
                ))}
                {!rows.length && (
                  <tr>
                    <td className="empty-cell" colSpan={Math.max(outputColumns.length, 1)}>
                      No report data yet
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </section>
    </main>
  );
}

function createEmptyFilters(inputFilters = []) {
  return inputFilters.reduce((acc, field) => ({ ...acc, [field.name]: '' }), {});
}

function cleanFilters(filters) {
  return Object.fromEntries(Object.entries(filters).map(([key, value]) => [key, value === '' ? null : value]));
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

async function checkResponse(response) {
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Request failed with status ${response.status}`);
  }
  return response.json();
}

createRoot(document.getElementById('root')).render(<App />);
