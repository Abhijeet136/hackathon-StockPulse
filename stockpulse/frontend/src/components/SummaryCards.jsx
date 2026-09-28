export default function SummaryCards({ products, pricing, reorder }) {
  const lowStock = products.filter((p) => p.stockLevel > 0 && p.stockLevel < p.reorderThreshold).length
  const outOfStock = products.filter((p) => p.status === 'OUT_OF_STOCK').length
  const cards = [
    { label: 'Pending suggestions', value: pricing.length + reorder.length, tone: 'accent' },
    { label: 'Low-stock products', value: lowStock, tone: lowStock ? 'warn' : '' },
    { label: 'Out of stock', value: outOfStock, tone: outOfStock ? 'danger' : '' },
  ]
  return (
    <section className="summary">
      {cards.map((c) => (
        <div key={c.label} className={`stat ${c.tone}`}>
          <span className="stat-value">{c.value}</span>
          <span className="stat-label">{c.label}</span>
        </div>
      ))}
    </section>
  )
}
