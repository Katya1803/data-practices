import { useEffect, useState } from 'react'
import { api } from '../api'

export default function Rentals() {
  const [active, setActive] = useState([])
  const [overdue, setOverdue] = useState([])
  const [loadingActive, setLoadingActive] = useState(false)
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')

  const [rentForm, setRentForm] = useState({ customerId: '', inventoryId: '', staffId: '1' })
  const [returnId, setReturnId] = useState('')

  const loadActive = () => {
    setLoadingActive(true)
    Promise.all([
      api.get('/api/rentals/active'),
      api.get('/api/rentals/overdue'),
    ])
      .then(([a, o]) => { setActive(a.data.data.content); setOverdue(o.data.data) })
      .finally(() => setLoadingActive(false))
  }

  useEffect(() => { loadActive() }, [])

  const flash = (text, isErr = false) => {
    setMsg(''); setErr('')
    if (isErr) setErr(text); else setMsg(text)
    setTimeout(() => { setMsg(''); setErr('') }, 4000)
  }

  const handleRent = async () => {
    const { customerId, inventoryId, staffId } = rentForm
    if (!customerId || !inventoryId) return flash('Customer ID and Inventory ID required', true)
    try {
      await api.post('/api/rentals', {
        customerId: Number(customerId),
        inventoryId: Number(inventoryId),
        staffId: Number(staffId),
      })
      flash(`Rental created for customer ${customerId}`)
      setRentForm({ customerId: '', inventoryId: '', staffId: '1' })
      loadActive()
    } catch (e) {
      flash(e.response?.data?.message || 'Rental failed', true)
    }
  }

  const handleReturn = async () => {
    if (!returnId) return flash('Rental ID required', true)
    try {
      await api.put(`/api/rentals/${returnId}/return`, { staffId: 1 })
      flash(`Rental ${returnId} returned`)
      setReturnId('')
      loadActive()
    } catch (e) {
      flash(e.response?.data?.message || 'Return failed', true)
    }
  }

  return (
    <div className="page">
      <h1>Rentals</h1>

      <div className="grid-2">
        <div className="card">
          <h2>New Rental</h2>
          <div className="form-row">
            <label>Customer ID<input type="number" value={rentForm.customerId} onChange={e => setRentForm(f => ({ ...f, customerId: e.target.value }))} style={{ width: 100 }} /></label>
            <label>Inventory ID<input type="number" value={rentForm.inventoryId} onChange={e => setRentForm(f => ({ ...f, inventoryId: e.target.value }))} style={{ width: 100 }} /></label>
            <label>Staff ID<input type="number" value={rentForm.staffId} onChange={e => setRentForm(f => ({ ...f, staffId: e.target.value }))} style={{ width: 80 }} /></label>
            <button onClick={handleRent}>Rent</button>
          </div>
        </div>

        <div className="card">
          <h2>Return Rental</h2>
          <div className="form-row">
            <label>Rental ID<input type="number" value={returnId} onChange={e => setReturnId(e.target.value)} style={{ width: 120 }} /></label>
            <button onClick={handleReturn}>Return</button>
          </div>
        </div>
      </div>

      {msg && <div className="success">{msg}</div>}
      {err && <div className="error">{err}</div>}

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h2>Active Rentals ({active.length})</h2>
          <button className="secondary" onClick={loadActive} disabled={loadingActive}>Refresh</button>
        </div>
        {loadingActive && <div className="loading">Loading...</div>}
        {!loadingActive && (
          <table>
            <thead>
              <tr><th>ID</th><th>Customer</th><th>Film</th><th>Rented</th></tr>
            </thead>
            <tbody>
              {active.map(r => (
                <tr key={r.rentalId}>
                  <td>{r.rentalId}</td>
                  <td>{r.customerName}</td>
                  <td>{r.filmTitle}</td>
                  <td>{r.rentalDate?.slice(0, 10)}</td>
                </tr>
              ))}
              {active.length === 0 && <tr><td colSpan={4} style={{ textAlign: 'center', color: '#888' }}>No active rentals</td></tr>}
            </tbody>
          </table>
        )}
      </div>

      {overdue.length > 0 && (
        <div className="card">
          <h2 style={{ color: '#c0392b' }}>Overdue Rentals ({overdue.length})</h2>
          <table>
            <thead>
              <tr><th>ID</th><th>Customer</th><th>Film</th><th>Rented</th></tr>
            </thead>
            <tbody>
              {overdue.map(r => (
                <tr key={r.rentalId}>
                  <td>{r.rentalId}</td>
                  <td>{r.customerName}</td>
                  <td>{r.filmTitle}</td>
                  <td>{r.rentalDate?.slice(0, 10)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
