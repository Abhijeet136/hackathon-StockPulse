import { useState } from 'react'
import { api } from '../api'

const TRIGGER = {
  INVENTORY_LOW: 'Inventory low',
  DEMAND_SPIKE: 'Demand spike',
  MANUAL: 'Manual',
  INITIAL: 'Initial review',
}
const SOURCE = { AI: 'AI', RULE_BASED: 'Rules', RULE_FALLBACK: 'Rule fallback' }

const money = (v) => `$${Number(v).toFixed(2)}`

export default function SuggestionCard({ suggestion: s, onDecided }) {
  const [busy, setBusy] = useState(null)
  const [error, setError] = useState(null)
  const isPricing = s.type === 'pricing'

  const decide = async (status) => {
    setBusy(status)
    try {
      await api.decide(s.type, s.id, status)
      onDecided()
    } catch (e) {
      setError(e.message)
      setBusy(null)
    }
  }

  return (
    <article className="card">
      <div className="card-head">
        <span className={`badge trigger-${s.triggerReason}`}>{TRIGGER[s.triggerReason]}</span>
        <span className="badge type">{isPricing ? 'Pricing' : 'Reorder'}</span>
        <span className={`badge source-${s.source}`}>{SOURCE[s.source]}</span>
      </div>
      <h3>
        {s.productName} <span className="sku">{s.sku}</span>
      </h3>
      <div className="change">
        {isPricing ? (
          <>
            <span className="old">{money(s.currentPrice)}</span>
            <span className="arrow">→</span>
            <span className={`new dir-${s.direction}`}>{money(s.recommendedPrice)}</span>
            <span className="dir">{s.direction}</span>
          </>
        ) : (
          <>
            <span className="old">{s.currentStock} in stock</span>
            <span className="arrow">+</span>
            <span className="new">{s.recommendedQuantity} units</span>
            <span className="dir">{s.leadTimeDays}-day lead time</span>
          </>
        )}
      </div>
      <div className="confidence" title="Confidence">
        <div className="bar" style={{ width: `${Math.round(s.confidence * 100)}%` }} />
        <span>{Math.round(s.confidence * 100)}% confidence</span>
      </div>
      <p className="reasoning">{s.reasoning}</p>
      {error && <p className="inline-error">{error}</p>}
      <div className="actions">
        <button className="primary" disabled={!!busy} onClick={() => decide('ACCEPTED')}>
          {busy === 'ACCEPTED' ? 'Accepting…' : 'Accept'}
        </button>
        <button disabled={!!busy} onClick={() => decide('REJECTED')}>
          {busy === 'REJECTED' ? 'Rejecting…' : 'Reject'}
        </button>
      </div>
    </article>
  )
}
