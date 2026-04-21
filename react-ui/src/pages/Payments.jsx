import { useState } from 'react'
import { api } from '../api'

export default function Payments() {
  const [customerId, setCustomerId] = useState('')
  const [payments, setPayments] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')

  const [form, setForm] = useState({ customerId: '', rentalId: '', staffId: '1', amount: '' })

  const size = 20

  const loadPayments = (p = 0) => {
    if (!customerId) return
    setLoading(true)
    setError('')
    api.get(`/api/payments/customer/${customerId}?page=${p}&size=${size}`)
      .then(r => { setPayments(r.data.data.content); setTotal(r.data.data.totalElements); setPage(p) })
      .catch(() => setError('Customer not found or no payments'))
      .finally(() => setLoading(false))
  }

  const handleCreate = async () => {
    const { customerId: cid, rentalId, staffId, amount } = form
    if (!cid || !rentalId || !amount) return setError('Fill in all fields')
    try {
      await api.post('/api/payments', {
        customerId: Number(cid),
        rentalId: Number(rentalId),
        staffId: Number(staffId),
        amount: Number(amount),
      })
      setMsg(`Payment of $${amount} created`)
      setForm({ customerId: '', rentalId: '', staffId: '1', amount: '' })
      setTimeout(() => setMsg(''), 4000)
      if (customerId === cid) loadPayments(0)
    } catch (e) {
      setError(e.response?.data?.message || 'Payment failed')
    }
  }

  const totalPages = Math.ceil(total / size)

  return (
    <div className="page">
      <h1>Payments</h1>

      <div className="card">
        <h2>Create Payment</h2>
        <div className="form-row">
          <label>Customer ID<input type="number" value={form.customerId} onChange={e => setForm(f => ({ ...f, customerId: e.target.value }))} style={{ width: 100 }} /></label>
          <label>Rental ID<input type="number" value={form.rentalId} onChange={e => setForm(f => ({ ...f, rentalId: e.target.value }))} style={{ width: 100 }} /></label>
          <label>Staff ID<input type="number" value={form.staffId} onChange={e => setForm(f => ({ ...f, staffId: e.target.value }))} style={{ width: 80 }} /></label>
          <label>Amount ($)<input type="number" step="0.01" value={form.amount} onChange={e => setForm(f => ({ ...f, amount: e.target.value }))} style={{ width: 100 }} /></label>
          <button onClick={handleCreate}>Submit</button>
        </div>
        {msg && <div className="success">{msg}</div>}
        {error && <div className="error">{error}</div>}
      </div>

      <div className="card">
        <h2>Customer Payment History</h2>
        <div className="search-bar" style={{ marginBottom: 16 }}>
          <input
            type="number"
            value={customerId}
            onChange={e => setCustomerId(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && loadPayments(0)}
            placeholder="Customer ID"
            style={{ maxWidth: 160 }}
          />
          <button onClick={() => loadPayments(0)}>Load</button>
        </div>

        {loading && <div className="loading">Loading...</div>}

        {payments.length > 0 && (
          <>
            <table>
              <thead>
                <tr><th>ID</th><th>Rental ID</th><th>Amount</th><th>Date</th></tr>
              </thead>
              <tbody>
                {payments.map(p => (
                  <tr key={p.paymentId}>
                    <td>{p.paymentId}</td>
                    <td>{p.rentalId}</td>
                    <td>${Number(p.amount).toFixed(2)}</td>
                    <td>{p.paymentDate?.slice(0, 10)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="pagination">
              <button disabled={page === 0} onClick={() => loadPayments(page - 1)}>Prev</button>
              <span>Page {page + 1} / {totalPages || 1}</span>
              <button disabled={page + 1 >= totalPages} onClick={() => loadPayments(page + 1)}>Next</button>
              <span style={{ marginLeft: 8, color: '#888' }}>{total} payments</span>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
