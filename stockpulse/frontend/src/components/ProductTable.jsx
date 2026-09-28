import { useState } from 'react'
import { api } from '../api'

const STATUS = { ACTIVE: 'Active', PRICE_REVIEW_PENDING: 'Price review', OUT_OF_STOCK: 'Out of stock' }

function ProductRow({ product: p, onChanged }) {
  const [busy, setBusy] = useState(null)
  const [error, setError] = useState(null)
  const [stock, setStock] = useState('')

  const run = async (label, action) => {
    setBusy(label)
    setError(null)
    try {
      await action()
      onChanged()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(null)
    }
  }

  const submitStock = (e) => {
    e.preventDefault()
    if (stock === '') return
    run('stock', () => api.setStock(p.id, Number(stock))).then(() => setStock(''))
  }

  const low = p.stockLevel < p.reorderThreshold
  return (
    <tr>
      <td>
        <div className="name">{p.name}</div>
        <div className="sku">
          {p.sku} · {p.category.toLowerCase()}
        </div>
      </td>
      <td className="num">${Number(p.currentPrice).toFixed(2)}</td>
      <td className="num">
        <span className={low ? 'low' : ''}>{p.stockLevel}</span>
        <span className="muted"> / {p.reorderThreshold}</span>
      </td>
      <td className="num">{p.demandVelocity}</td>
      <td>
        <span className={`badge status-${p.status}`}>{STATUS[p.status]}</span>
      </td>
      <td>
        <div className="row-actions">
          <button disabled={!!busy || p.stockLevel === 0} onClick={() => run('sale', () => api.placeOrder(p.id, 1))}>
            {busy === 'sale' ? 'Selling…' : 'Sale'}
          </button>
          <button disabled={!!busy || p.stockLevel < 5} onClick={() => run('sell5', () => api.placeOrder(p.id, 5))}>
            {busy === 'sell5' ? 'Selling…' : 'Sell 5'}
          </button>
          <button disabled={!!busy} onClick={() => run('suggest', () => api.suggestPricing(p.id))} title="Ask for a manual price suggestion">
            {busy === 'suggest' ? 'Asking…' : 'Suggest price'}
          </button>
          <form onSubmit={submitStock} className="stock-form">
            <input
              type="number"
              min="0"
              placeholder="Set stock"
              value={stock}
              onChange={(e) => setStock(e.target.value)}
              aria-label={`Set stock for ${p.name}`}
            />
          </form>
        </div>
        {error && <div className="inline-error">{error}</div>}
      </td>
    </tr>
  )
}

export default function ProductTable({ products, category, onCategoryChange, onChanged }) {
  return (
    <section className="panel">
      <div className="panel-head">
        <h2>Products</h2>
        <select value={category} onChange={(e) => onCategoryChange(e.target.value)} aria-label="Category filter">
          <option value="">All categories</option>
          <option value="ELECTRONICS">Electronics</option>
          <option value="APPAREL">Apparel</option>
          <option value="HOME">Home</option>
        </select>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Product</th>
              <th className="num">Price</th>
              <th className="num">Stock / threshold</th>
              <th className="num">Velocity (24h)</th>
              <th>Status</th>
              <th>Simulate</th>
            </tr>
          </thead>
          <tbody>
            {products.map((p) => (
              <ProductRow key={p.id} product={p} onChanged={onChanged} />
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
