import { useEffect, useState } from 'react'
import { api } from '../api'

export default function Dashboard() {
  const [topFilms, setTopFilms] = useState([])
  const [revenue, setRevenue] = useState([])
  const [topCustomers, setTopCustomers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([
      api.get('/api/reports/films/top?limit=5'),
      api.get('/api/reports/revenue/monthly?year=2007'),
      api.get('/api/reports/customers/top?limit=5'),
    ])
      .then(([films, rev, customers]) => {
        setTopFilms(films.data.data)
        setRevenue(rev.data.data)
        setTopCustomers(customers.data.data)
      })
      .catch(() => setError('Failed to load dashboard data'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="loading">Loading dashboard...</div>
  if (error) return <div className="page"><div className="error">{error}</div></div>

  return (
    <div className="page">
      <h1>Dashboard</h1>

      <div className="grid-2">
        <div className="card">
          <h2>Top 5 Rented Films</h2>
          <table>
            <thead>
              <tr><th>#</th><th>Title</th><th>Rentals</th></tr>
            </thead>
            <tbody>
              {topFilms.map((f, i) => (
                <tr key={i}>
                  <td>{i + 1}</td>
                  <td>{f.title}</td>
                  <td>{f.rentalCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="card">
          <h2>Top 5 Customers</h2>
          <table>
            <thead>
              <tr><th>#</th><th>Name</th><th>Rentals</th><th>Spent</th></tr>
            </thead>
            <tbody>
              {topCustomers.map((c, i) => (
                <tr key={i}>
                  <td>{i + 1}</td>
                  <td>{c.name}</td>
                  <td>{c.totalRentals}</td>
                  <td>${Number(c.totalSpent).toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card">
        <h2>Monthly Revenue (2007)</h2>
        <table>
          <thead>
            <tr><th>Month</th><th>Transactions</th><th>Revenue</th></tr>
          </thead>
          <tbody>
            {revenue.map((r, i) => (
              <tr key={i}>
                <td>2007-{String(r.month).padStart(2, '0')}</td>
                <td>{r.transactionCount}</td>
                <td>${Number(r.revenue).toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
