import { useState, useEffect, useRef } from 'react'
import { api } from '../api'

export default function Stats() {
  const today = new Date().toISOString().split('T')[0]
  const [date, setDate] = useState(today)
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(false)
  const [autoRefresh, setAutoRefresh] = useState(false)
  const intervalRef = useRef(null)

  function fetchStats(d) {
    setLoading(true)
    api.get(`/api/stats/realtime?date=${d}`)
      .then(r => setStats(r.data.data))
      .catch(() => setStats(null))
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchStats(date) }, [date])

  useEffect(() => {
    if (autoRefresh) {
      intervalRef.current = setInterval(() => fetchStats(date), 5000)
    } else {
      clearInterval(intervalRef.current)
    }
    return () => clearInterval(intervalRef.current)
  }, [autoRefresh, date])

  return (
    <div className="page">
      <div className="page-header">
        <h1>Realtime Stats</h1>
        <p className="subtitle">Counters aggregated by Kafka consumer → Redis</p>
      </div>

      <div className="controls" style={{ display: 'flex', gap: '1rem', alignItems: 'center', marginBottom: '1.5rem' }}>
        <label>
          Date&nbsp;
          <input type="date" value={date} onChange={e => setDate(e.target.value)} />
        </label>
        <button onClick={() => fetchStats(date)} disabled={loading}>
          {loading ? 'Loading…' : 'Refresh'}
        </button>
        <button
          onClick={() => setAutoRefresh(v => !v)}
          style={{ background: autoRefresh ? '#e74c3c' : undefined }}
        >
          {autoRefresh ? 'Stop Auto-refresh' : 'Auto-refresh (5s)'}
        </button>
      </div>

      {stats ? (
        <div className="grid-2">
          <div className="stat-card">
            <div className="value">{stats.rentalsCreated}</div>
            <div className="label">Rentals Created</div>
          </div>
          <div className="stat-card">
            <div className="value">{stats.rentalsReturned}</div>
            <div className="label">Rentals Returned</div>
          </div>
          <div className="stat-card">
            <div className="value">{stats.paymentsProcessed}</div>
            <div className="label">Payments Processed</div>
          </div>
          <div className="stat-card">
            <div className="value">${Number(stats.totalRevenue).toFixed(2)}</div>
            <div className="label">Total Revenue</div>
          </div>
        </div>
      ) : (
        <p>No data for {date}.</p>
      )}
    </div>
  )
}
