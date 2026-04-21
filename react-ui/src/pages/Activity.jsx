import { useEffect, useState } from 'react'
import { djangoApi } from '../api'

const EVENT_COLORS = {
  rental_created: 'green',
  rental_returned: 'blue',
  payment_processed: 'gray',
}

export default function Activity() {
  const [logs, setLogs] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [autoRefresh, setAutoRefresh] = useState(false)

  const size = 20

  const load = (p = page) => {
    setLoading(true)
    setError('')
    djangoApi.get(`/api/activity-log?page=${p}&page_size=${size}`)
      .then(r => {
        setLogs(r.data.results || r.data)
        setTotal(r.data.count || 0)
      })
      .catch(() => setError('Failed to load activity log — is Django running on :8000?'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [page])

  useEffect(() => {
    if (!autoRefresh) return
    const id = setInterval(() => load(page), 3000)
    return () => clearInterval(id)
  }, [autoRefresh, page])

  const totalPages = Math.ceil(total / size)

  return (
    <div className="page">
      <h1>Kafka Activity Log</h1>
      <div className="card">
        <div style={{ display: 'flex', gap: 10, marginBottom: 14, alignItems: 'center' }}>
          <button onClick={() => load()}>Refresh</button>
          <button
            className={autoRefresh ? '' : 'secondary'}
            onClick={() => setAutoRefresh(v => !v)}
          >
            {autoRefresh ? 'Auto-refresh ON' : 'Auto-refresh OFF'}
          </button>
          <span style={{ color: '#888', fontSize: 13 }}>{total} total events</span>
        </div>

        {error && <div className="error">{error}</div>}
        {loading && <div className="loading">Loading...</div>}

        {!loading && (
          <>
            <table>
              <thead>
                <tr><th>ID</th><th>Event</th><th>Ref ID</th><th>Payload</th><th>Received</th></tr>
              </thead>
              <tbody>
                {logs.map(log => (
                  <tr key={log.id}>
                    <td>{log.id}</td>
                    <td><span className={`badge ${EVENT_COLORS[log.event_type] || 'gray'}`}>{log.event_type}</span></td>
                    <td>{log.reference_id}</td>
                    <td style={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: 12, color: '#555' }}>
                      {typeof log.payload === 'object' ? JSON.stringify(log.payload) : log.payload}
                    </td>
                    <td>{log.created_at?.slice(0, 19).replace('T', ' ')}</td>
                  </tr>
                ))}
                {logs.length === 0 && (
                  <tr><td colSpan={5} style={{ textAlign: 'center', color: '#888' }}>No activity yet — try renting a film!</td></tr>
                )}
              </tbody>
            </table>

            {totalPages > 1 && (
              <div className="pagination">
                <button disabled={page === 1} onClick={() => setPage(p => p - 1)}>Prev</button>
                <span>Page {page} / {totalPages}</span>
                <button disabled={page >= totalPages} onClick={() => setPage(p => p + 1)}>Next</button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
