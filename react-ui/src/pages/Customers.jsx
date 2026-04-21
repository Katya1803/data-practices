import { useEffect, useState } from 'react'
import { api } from '../api'

export default function Customers() {
  const [customers, setCustomers] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [overdue, setOverdue] = useState([])
  const [showOverdue, setShowOverdue] = useState(false)
  const [stats, setStats] = useState(null)
  const [statsId, setStatsId] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const size = 20

  useEffect(() => {
    setLoading(true)
    api.get(`/api/customers?page=${page}&size=${size}`)
      .then(r => { setCustomers(r.data.data.content); setTotal(r.data.data.totalElements) })
      .catch(() => setError('Failed to load customers'))
      .finally(() => setLoading(false))
  }, [page])

  const loadOverdue = () => {
    api.get('/api/customers/overdue')
      .then(r => { setOverdue(r.data.data); setShowOverdue(true) })
      .catch(() => setError('Failed to load overdue customers'))
  }

  const loadStats = () => {
    if (!statsId) return
    api.get(`/api/customers/${statsId}/stats`)
      .then(r => setStats(r.data.data))
      .catch(() => setError(`Customer ${statsId} not found`))
  }

  const totalPages = Math.ceil(total / size)

  return (
    <div className="page">
      <h1>Customers</h1>

      <div className="card">
        <h2>Customer Stats Lookup</h2>
        <div className="form-row">
          <label>Customer ID
            <input type="number" value={statsId} onChange={e => setStatsId(e.target.value)} placeholder="e.g. 1" style={{ width: 120 }} />
          </label>
          <button onClick={loadStats}>Lookup</button>
        </div>
        {stats && (
          <div className="grid-3" style={{ marginTop: 12 }}>
            <div className="stat-card"><div className="value">{stats.totalRentals}</div><div className="label">Total Rentals</div></div>
            <div className="stat-card"><div className="value">${Number(stats.totalSpent).toFixed(2)}</div><div className="label">Total Spent</div></div>
            <div className="stat-card"><div className="value">${Number(stats.avgPayment).toFixed(2)}</div><div className="label">Avg Payment</div></div>
          </div>
        )}
      </div>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h2>All Customers</h2>
          <button className="secondary" onClick={loadOverdue}>Show Overdue</button>
        </div>

        {error && <div className="error">{error}</div>}
        {loading && <div className="loading">Loading...</div>}

        {showOverdue && (
          <div style={{ marginBottom: 16 }}>
            <h2 style={{ color: '#c0392b', marginBottom: 8 }}>Overdue Customers ({overdue.length})</h2>
            <table>
              <thead><tr><th>ID</th><th>Name</th><th>Email</th></tr></thead>
              <tbody>
                {overdue.map(c => (
                  <tr key={c.customerId}>
                    <td>{c.customerId}</td>
                    <td>{c.firstName} {c.lastName}</td>
                    <td>{c.email}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <button className="secondary" style={{ marginTop: 8 }} onClick={() => setShowOverdue(false)}>Hide</button>
          </div>
        )}

        {!loading && (
          <>
            <table>
              <thead>
                <tr><th>ID</th><th>Name</th><th>Email</th><th>Active</th></tr>
              </thead>
              <tbody>
                {customers.map(c => (
                  <tr key={c.customerId}>
                    <td>{c.customerId}</td>
                    <td>{c.firstName} {c.lastName}</td>
                    <td>{c.email}</td>
                    <td><span className={`badge ${c.active ? 'green' : 'red'}`}>{c.active ? 'Active' : 'Inactive'}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="pagination">
              <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>Prev</button>
              <span>Page {page + 1} / {totalPages || 1}</span>
              <button disabled={page + 1 >= totalPages} onClick={() => setPage(p => p + 1)}>Next</button>
              <span style={{ marginLeft: 8, color: '#888' }}>{total} customers</span>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
