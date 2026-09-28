import { useCallback, useState } from 'react'
import { api } from './api'
import { usePolling } from './usePolling'
import SummaryCards from './components/SummaryCards'
import StrategySwitcher from './components/StrategySwitcher'
import SuggestionList from './components/SuggestionList'
import ProductTable from './components/ProductTable'

export default function App() {
  const [category, setCategory] = useState('')

  const load = useCallback(async () => {
    const [products, pricing, reorder, strategy] = await Promise.all([
      api.products(category),
      api.pendingPricing(),
      api.pendingReorder(),
      api.strategy(),
    ])
    return { products, pricing, reorder, strategy }
  }, [category])

  const { data, error, loading, refresh } = usePolling(load, 3000)

  return (
    <div className="app">
      <header className="topbar">
        <div>
          <h1>StockPulse</h1>
          <p className="subtitle">Merchandising console · the system proposes, you publish</p>
        </div>
        {data && <StrategySwitcher strategy={data.strategy} onChanged={refresh} />}
      </header>

      {error && <div className="banner error">Backend unreachable: {error}</div>}
      {loading && !data && <div className="banner">Loading…</div>}

      {data && (
        <>
          <SummaryCards products={data.products} pricing={data.pricing} reorder={data.reorder} />
          <SuggestionList pricing={data.pricing} reorder={data.reorder} onDecided={refresh} />
          <ProductTable
            products={data.products}
            category={category}
            onCategoryChange={setCategory}
            onChanged={refresh}
          />
        </>
      )}
    </div>
  )
}
