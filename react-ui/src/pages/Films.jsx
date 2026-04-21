import { useEffect, useState } from 'react'
import { api } from '../api'

const RATINGS = ['', 'G', 'PG', 'PG-13', 'R', 'NC-17']

export default function Films() {
  const [films, setFilms] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [rating, setRating] = useState('')
  const [storeId, setStoreId] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [detail, setDetail] = useState(null)
  const [cast, setCast] = useState([])
  const [detailLoading, setDetailLoading] = useState(false)

  const size = 20

  useEffect(() => {
    setLoading(true)
    setError('')

    let req
    if (storeId) {
      req = api.get(`/api/films/available?storeId=${storeId}`)
        .then(r => { setFilms(r.data.data); setTotal(r.data.data.length) })
    } else {
      const params = new URLSearchParams({ page, size })
      if (rating) params.set('rating', rating)
      if (search) {
        req = api.get(`/api/films/search?keyword=${encodeURIComponent(search)}&${params}`)
          .then(r => { setFilms(r.data.data); setTotal(r.data.data.length) })
      } else {
        req = api.get(`/api/films?${params}`)
          .then(r => { setFilms(r.data.data.content); setTotal(r.data.data.totalElements) })
      }
    }

    req.catch(() => setError('Failed to load films'))
       .finally(() => setLoading(false))
  }, [page, search, rating, storeId])

  const handleSearch = () => { setPage(0); setSearch(searchInput); setStoreId('') }

  const openDetail = (film) => {
    setDetail(film)
    setCast([])
    setDetailLoading(true)
    api.get(`/api/films/${film.filmId}/cast`)
      .then(r => setCast(r.data.data))
      .catch(() => setCast([]))
      .finally(() => setDetailLoading(false))
  }

  const totalPages = storeId ? 1 : Math.ceil(total / size)

  return (
    <div className="page">
      <h1>Films</h1>

      {detail && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.5)', zIndex: 100, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
          onClick={() => setDetail(null)}>
          <div style={{ background: '#fff', borderRadius: 10, padding: 28, maxWidth: 520, width: '90%', maxHeight: '80vh', overflowY: 'auto' }}
            onClick={e => e.stopPropagation()}>
            <h2 style={{ marginBottom: 4 }}>{detail.title}</h2>
            <div style={{ marginBottom: 12, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              <span className="badge gray">{detail.rating}</span>
              <span className="badge blue">{detail.length} min</span>
              <span className="badge green">${Number(detail.rentalRate).toFixed(2)}/rental</span>
            </div>
            <p style={{ fontSize: 13, color: '#555', marginBottom: 14 }}>{detail.description}</p>
            <div style={{ fontSize: 13, marginBottom: 14 }}>
              {detail.categories?.map(c => <span key={c} className="tag">{c}</span>)}
            </div>
            <h2 style={{ marginBottom: 8 }}>Cast</h2>
            {detailLoading
              ? <div className="loading">Loading cast...</div>
              : cast.length === 0
                ? <div style={{ color: '#888', fontSize: 13 }}>No cast info</div>
                : <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                    {cast.map((a, i) => <span key={i} className="tag">{a.firstName} {a.lastName}</span>)}
                  </div>
            }
            <button className="secondary" style={{ marginTop: 18 }} onClick={() => setDetail(null)}>Close</button>
          </div>
        </div>
      )}

      <div className="card">
        <div className="search-bar">
          <input
            value={searchInput}
            onChange={e => setSearchInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSearch()}
            placeholder="Full-text search..."
          />
          <select value={rating} onChange={e => { setRating(e.target.value); setPage(0); setStoreId('') }}>
            {RATINGS.map(r => <option key={r} value={r}>{r || 'All Ratings'}</option>)}
          </select>
          <select value={storeId} onChange={e => { setStoreId(e.target.value); setSearch(''); setSearchInput(''); setRating(''); setPage(0) }}>
            <option value="">All Stores</option>
            <option value="1">Store 1 (Available)</option>
            <option value="2">Store 2 (Available)</option>
          </select>
          <button onClick={handleSearch}>Search</button>
          <button className="secondary" onClick={() => { setSearchInput(''); setSearch(''); setRating(''); setStoreId(''); setPage(0) }}>Clear</button>
        </div>

        {error && <div className="error">{error}</div>}
        {loading && <div className="loading">Loading...</div>}

        {!loading && (
          <>
            <table>
              <thead>
                <tr><th>Title</th><th>Rating</th><th>Length</th><th>Rental Rate</th><th>Categories</th><th></th></tr>
              </thead>
              <tbody>
                {films.map(f => (
                  <tr key={f.filmId}>
                    <td>{f.title}</td>
                    <td><span className="badge gray">{f.rating}</span></td>
                    <td>{f.length} min</td>
                    <td>${Number(f.rentalRate).toFixed(2)}</td>
                    <td>{f.categories?.map(c => <span key={c} className="tag">{c}</span>)}</td>
                    <td><button className="secondary" style={{ padding: '4px 10px', fontSize: 12 }} onClick={() => openDetail(f)}>Detail</button></td>
                  </tr>
                ))}
              </tbody>
            </table>

            {!storeId && (
              <div className="pagination">
                <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>Prev</button>
                <span>Page {page + 1} / {totalPages || 1}</span>
                <button disabled={page + 1 >= totalPages} onClick={() => setPage(p => p + 1)}>Next</button>
                <span style={{ marginLeft: 8, color: '#888' }}>{total} films</span>
              </div>
            )}
            {storeId && <div style={{ marginTop: 10, color: '#888', fontSize: 13 }}>{total} available films in Store {storeId}</div>}
          </>
        )}
      </div>
    </div>
  )
}
