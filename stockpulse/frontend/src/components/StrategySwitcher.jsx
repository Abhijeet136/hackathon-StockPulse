import { useState } from 'react'
import { api } from '../api'

const LABELS = { ai: 'AI', ruleBased: 'Rules' }

export default function StrategySwitcher({ strategy, onChanged }) {
  const [error, setError] = useState(null)

  const change = async (type, name) => {
    try {
      await api.switchStrategy({ [type]: name })
      setError(null)
      onChanged()
    } catch (e) {
      setError(e.message)
    }
  }

  const group = (type, active, options) => (
    <div className="switch-group">
      <span className="switch-label">{type === 'pricing' ? 'Pricing' : 'Reorder'}</span>
      <div className="segmented">
        {options.map((name) => (
          <button key={name} className={name === active ? 'on' : ''} onClick={() => change(type, name)}>
            {LABELS[name] ?? name}
          </button>
        ))}
      </div>
    </div>
  )

  return (
    <div className="strategy">
      {group('pricing', strategy.pricing, strategy.availablePricing)}
      {group('reorder', strategy.reorder, strategy.availableReorder)}
      {error && <span className="inline-error">{error}</span>}
    </div>
  )
}
