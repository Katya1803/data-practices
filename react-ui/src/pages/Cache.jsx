import { useState, useEffect, useCallback } from 'react'
import { api } from '../api'

export default function Cache() {
  const [entries, setEntries]           = useState([])
  const [loadingEntries, setLoadingEntries] = useState(false)
  const [entriesError, setEntriesError] = useState('')

  const [probeId, setProbeId]     = useState('')
  const [probeResult, setProbeResult] = useState(null)
  const [probing, setProbing]     = useState(false)
  const [probeError, setProbeError] = useState('')

  const [warmId, setWarmId]       = useState('')
  const [warmResult, setWarmResult] = useState(null)
  const [warming, setWarming]     = useState(false)
  const [warmError, setWarmError] = useState('')

  const [evictMsg, setEvictMsg]   = useState('')

  const loadEntries = useCallback(() => {
    setLoadingEntries(true)
    setEntriesError('')
    api.get('/api/cache/entries')
      .then(r => setEntries(r.data.data))
      .catch(() => setEntriesError('Failed to load cache entries'))
      .finally(() => setLoadingEntries(false))
  }, [])

  useEffect(() => { loadEntries() }, [loadEntries])

  // Group by cache name
  const grouped = entries.reduce((acc, e) => {
    ;(acc[e.cacheName] = acc[e.cacheName] || []).push(e)
    return acc
  }, {})

  const evictEntry = (redisKey) => {
    api.delete(`/api/cache/evict?key=${encodeURIComponent(redisKey)}`)
      .then(() => { setEvictMsg(`Evicted: ${redisKey}`); loadEntries() })
      .catch(() => setEvictMsg(`Failed to evict ${redisKey}`))
  }

  const evictCache = (cacheName) => {
    api.delete(`/api/cache/evict?cacheName=${encodeURIComponent(cacheName)}`)
      .then(r => {
        const n = r.data.data.deleted
        setEvictMsg(`Evicted ${n} ${n === 1 ? 'entry' : 'entries'} from "${cacheName}"`)
        loadEntries()
      })
      .catch(() => setEvictMsg(`Failed to evict "${cacheName}"`))
  }

  const probe = () => {
    setProbing(true)
    setProbeResult(null)
    setProbeError('')
    api.get(`/api/cache/film/${probeId}/probe`)
      .then(r => { setProbeResult(r.data.data); loadEntries() })
      .catch(e => setProbeError(e.response?.data?.message || 'Film not found'))
      .finally(() => setProbing(false))
  }

  const warm = (id) => {
    setWarming(true)
    setWarmResult(null)
    setWarmError('')
    api.post(`/api/cache/film/${id}/warm`)
      .then(r => { setWarmResult(r.data.data); loadEntries() })
      .catch(e => setWarmError(e.response?.data?.message || 'Warm failed'))
      .finally(() => setWarming(false))
  }

  return (
    <div className="page">
      <h1>Cache Inspector</h1>

      {/* ── Active Entries ─────────────────────────────────────── */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
          <h2 style={{ margin: 0 }}>Active Redis Entries</h2>
          <button className="secondary" onClick={loadEntries} disabled={loadingEntries} style={{ padding: '6px 14px', fontSize: 13 }}>
            {loadingEntries ? 'Loading…' : 'Refresh'}
          </button>
        </div>

        {evictMsg && <div className="success" style={{ marginBottom: 10 }}>{evictMsg}</div>}
        {entriesError && <div className="error">{entriesError}</div>}

        {!loadingEntries && Object.keys(grouped).length === 0 && (
          <div className="loading">Redis cache is empty — browse films or load reports to populate it.</div>
        )}

        {Object.entries(grouped).map(([cacheName, items]) => (
          <div key={cacheName} style={{ marginBottom: 20 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
              <code style={{ fontWeight: 700, fontSize: 13, color: '#1a1a2e' }}>{cacheName}</code>
              <span className="badge gray">{items.length} {items.length === 1 ? 'entry' : 'entries'}</span>
              <button
                className="secondary"
                style={{ marginLeft: 'auto', padding: '4px 10px', fontSize: 12 }}
                onClick={() => evictCache(cacheName)}
              >
                Evict All
              </button>
            </div>
            <table>
              <thead>
                <tr>
                  <th>Key</th>
                  <th>TTL (s)</th>
                  <th style={{ width: 80 }}></th>
                </tr>
              </thead>
              <tbody>
                {items.map(item => (
                  <tr key={item.redisKey}>
                    <td><code>{item.key}</code></td>
                    <td>
                      <span className={`badge ${item.ttlSeconds > 60 ? 'green' : 'red'}`}>
                        {item.ttlSeconds}s
                      </span>
                    </td>
                    <td>
                      <button
                        className="secondary"
                        style={{ padding: '3px 8px', fontSize: 12 }}
                        onClick={() => evictEntry(item.redisKey)}
                      >
                        Evict
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
      </div>

      {/* ── Probe + Warm ───────────────────────────────────────── */}
      <div className="grid-2">

        {/* Probe */}
        <div className="card">
          <h2>Cache Probe</h2>
          <p style={{ fontSize: 13, color: '#666', marginBottom: 14 }}>
            Check whether a film is served from <strong>Redis</strong> or <strong>PostgreSQL</strong>.
            On a cache miss the result is written back to Redis automatically.
          </p>
          <div className="search-bar">
            <input
              type="number"
              placeholder="Film ID (e.g. 1)"
              value={probeId}
              onChange={e => { setProbeId(e.target.value); setProbeResult(null) }}
              onKeyDown={e => e.key === 'Enter' && probeId && probe()}
              min="1"
              style={{ maxWidth: 180 }}
            />
            <button onClick={probe} disabled={!probeId || probing}>
              {probing ? 'Probing…' : 'Probe'}
            </button>
          </div>

          {probeError && <div className="error">{probeError}</div>}

          {probeResult && (
            <div style={{ marginTop: 16, padding: 14, background: '#f8f8f8', borderRadius: 8 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
                <span style={{ fontWeight: 600, fontSize: 13 }}>Source:</span>
                {probeResult.source === 'cache'
                  ? <span className="badge green">⚡ Redis Cache</span>
                  : <span className="badge blue">🗄 PostgreSQL</span>
                }
              </div>

              {probeResult.ttlSeconds != null && (
                <div style={{ fontSize: 13, marginBottom: 6 }}>
                  TTL remaining: <strong>{probeResult.ttlSeconds}s</strong>
                </div>
              )}

              <div style={{ fontSize: 13, marginBottom: 4 }}>
                Film: <strong>{probeResult.film?.title}</strong>
              </div>
              <div style={{ fontSize: 13, color: '#555' }}>
                Rating: <span className="tag">{probeResult.film?.rating}</span>
                &nbsp;Length: {probeResult.film?.length} min
              </div>

              {probeResult.source === 'database' && (
                <div style={{ marginTop: 12, fontSize: 13, color: '#888' }}>
                  Cache miss — result has been written to Redis. Probe again to see a cache hit.
                </div>
              )}
            </div>
          )}
        </div>

        {/* Warm */}
        <div className="card">
          <h2>Cache Warm</h2>
          <p style={{ fontSize: 13, color: '#666', marginBottom: 14 }}>
            Evict a film from Redis, reload from PostgreSQL, and write it back
            — enforcing consistency between cache and database.
          </p>
          <div className="search-bar">
            <input
              type="number"
              placeholder="Film ID (e.g. 1)"
              value={warmId}
              onChange={e => { setWarmId(e.target.value); setWarmResult(null) }}
              onKeyDown={e => e.key === 'Enter' && warmId && warm(warmId)}
              min="1"
              style={{ maxWidth: 180 }}
            />
            <button onClick={() => warm(warmId)} disabled={!warmId || warming}>
              {warming ? 'Warming…' : 'Warm Cache'}
            </button>
          </div>

          {warmError && <div className="error">{warmError}</div>}

          {warmResult && (
            <div style={{ marginTop: 16, padding: 14, background: '#f8f8f8', borderRadius: 8 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
                <span className="badge green">✓ Cache Warmed</span>
              </div>
              <div style={{ fontSize: 13, marginBottom: 4 }}>
                Film: <strong>{warmResult.film?.title}</strong>
              </div>
              <div style={{ fontSize: 13, marginBottom: 8, color: '#555' }}>
                New TTL: <strong>{warmResult.ttlSeconds}s</strong>
              </div>
              <div style={{ fontSize: 12, color: '#888', lineHeight: 1.5 }}>
                {warmResult.message}
              </div>
            </div>
          )}

          {/* Quick-warm from probe result */}
          {probeResult?.source === 'database' && probeResult.filmId && warmId === '' && (
            <div style={{ marginTop: 14, fontSize: 13, color: '#888' }}>
              Tip: film {probeResult.filmId} was just a cache miss — enter its ID above to warm it.
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
