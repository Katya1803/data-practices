import { useEffect, useState } from 'react'
import { api } from '../api'

export default function Reports() {
  const [topFilms, setTopFilms] = useState([])
  const [revenue, setRevenue] = useState([])
  const [topCustomers, setTopCustomers] = useState([])
  const [category, setCategory] = useState([])
  const [filmLimit, setFilmLimit] = useState(10)
  const [custLimit, setCustLimit] = useState(10)
  const [year, setYear] = useState(2007)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = () => {
    setLoading(true)
    setError('')
    Promise.all([
      api.get(`/api/reports/films/top?limit=${filmLimit}`),
      api.get(`/api/reports/revenue/monthly?year=${year}`),
      api.get(`/api/reports/customers/top?limit=${custLimit}`),
      api.get('/api/reports/rentals/by-category'),
    ])
      .then(([f, r, c, cat]) => {
        setTopFilms(f.data.data)
        setRevenue(r.data.data)
        setTopCustomers(c.data.data)
        setCategory(cat.data.data)
      })
      .catch(() => setError('Failed to load reports'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  return (
    <div className="page">
      <h1>Reports</h1>

      <div className="card">
        <div className="form-row">
          <label>Top Films limit<input type="number" value={filmLimit} onChange={e => setFilmLimit(e.target.value)} style={{ width: 70 }} /></label>
          <label>Top Customers limit<input type="number" value={custLimit} onChange={e => setCustLimit(e.target.value)} style={{ width: 70 }} /></label>
          <label>Revenue year<input type="number" value={year} onChange={e => setYear(e.target.value)} style={{ width: 90 }} /></label>
          <button onClick={load} disabled={loading}>Reload</button>
        </div>
        {error && <div className="error">{error}</div>}
      </div>

      {loading && <div className="loading">Loading...</div>}

      {!loading && (
        <>
          <div className="grid-2">
            <div className="card">
              <h2>Top Rented Films</h2>
              <table>
                <thead><tr><th>#</th><th>Title</th><th>Category</th><th>Rentals</th><th>Revenue</th></tr></thead>
                <tbody>
                  {topFilms.map((f, i) => (
                    <tr key={i}>
                      <td>{i + 1}</td>
                      <td>{f.title}</td>
                      <td>{f.category}</td>
                      <td>{f.rentalCount}</td>
                      <td>${Number(f.revenue).toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="card">
              <h2>Top Customers</h2>
              <table>
                <thead><tr><th>#</th><th>Name</th><th>Rentals</th><th>Spent</th></tr></thead>
                <tbody>
                  {topCustomers.map((c, i) => (
                    <tr key={i}>
                      <td>{i + 1}</td>
                      <td>{c.firstName} {c.lastName}</td>
                      <td>{c.rentalCount}</td>
                      <td>${Number(c.totalSpent).toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="grid-2">
            <div className="card">
              <h2>Monthly Revenue ({year})</h2>
              <table>
                <thead><tr><th>Month</th><th>Transactions</th><th>Revenue</th></tr></thead>
                <tbody>
                  {revenue.map((r, i) => (
                    <tr key={i}>
                      <td>{r.year}-{String(r.month).padStart(2, '0')}</td>
                      <td>{r.paymentCount}</td>
                      <td>${Number(r.totalRevenue).toFixed(2)}</td>
                    </tr>
                  ))}
                  {revenue.length === 0 && <tr><td colSpan={3} style={{ textAlign: 'center', color: '#888' }}>No data for {year}</td></tr>}
                </tbody>
              </table>
            </div>

            <div className="card">
              <h2>Rentals by Category</h2>
              <table>
                <thead><tr><th>Category</th><th>Rentals</th><th>Revenue</th></tr></thead>
                <tbody>
                  {category.map((c, i) => (
                    <tr key={i}>
                      <td>{c.category}</td>
                      <td>{c.rentalCount}</td>
                      <td>${Number(c.totalRevenue).toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
