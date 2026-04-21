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
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const size = 20

  useEffect(() => {
    setLoading(true)
    setError('')
    const params = new URLSearchParams({ page, size })
    if (rating) params.set('rating', rating)

    const req = search
      ? api.get(`/api/films/search?q=${encodeURIComponent(search)}&${params}`)
      : api.get(`/api/films?${params}`)

    req
      .then(r => {
        setFilms(r.data.data.content)
        setTotal(r.data.data.totalElements)
      })
      .catch(() => setError('Failed to load films'))
      .finally(() => setLoading(false))
  }, [page, search, rating])

  const handleSearch = () => {
    setPage(0)
    setSearch(searchInput)
  }

  const totalPages = Math.ceil(total / size)

  return (
    <div className="page">
      <h1>Films</h1>
      <div className="card">
        <div className="search-bar">
          <input
            value={searchInput}
            onChange={e => setSearchInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSearch()}
            placeholder="Full-text search..."
          />
          <select value={rating} onChange={e => { setRating(e.target.value); setPage(0) }}>
            {RATINGS.map(r => <option key={r} value={r}>{r || 'All Ratings'}</option>)}
          </select>
          <button onClick={handleSearch}>Search</button>
          <button className="secondary" onClick={() => { setSearchInput(''); setSearch(''); setRating(''); setPage(0) }}>Clear</button>
        </div>

        {error && <div className="error">{error}</div>}
        {loading && <div className="loading">Loading...</div>}

        {!loading && (
          <>
            <table>
              <thead>
                <tr>
                  <th>Title</th><th>Rating</th><th>Length</th><th>Rental Rate</th><th>Categories</th>
                </tr>
              </thead>
              <tbody>
                {films.map(f => (
                  <tr key={f.filmId}>
                    <td>{f.title}</td>
                    <td><span className="badge gray">{f.rating}</span></td>
                    <td>{f.length} min</td>
                    <td>${Number(f.rentalRate).toFixed(2)}</td>
                    <td>{f.categories?.map(c => <span key={c} className="tag">{c}</span>)}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="pagination">
              <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>Prev</button>
              <span>Page {page + 1} / {totalPages || 1}</span>
              <button disabled={page + 1 >= totalPages} onClick={() => setPage(p => p + 1)}>Next</button>
              <span style={{ marginLeft: 8, color: '#888' }}>{total} films</span>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
