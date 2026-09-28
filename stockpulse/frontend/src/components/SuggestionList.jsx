import SuggestionCard from './SuggestionCard'

export default function SuggestionList({ pricing, reorder, onDecided }) {
  const items = [
    ...pricing.map((s) => ({ ...s, type: 'pricing' })),
    ...reorder.map((s) => ({ ...s, type: 'reorder' })),
  ].sort((a, b) => b.createdAt.localeCompare(a.createdAt))

  return (
    <section className="panel">
      <h2>Pending suggestions</h2>
      {items.length === 0 ? (
        <p className="empty">Nothing to review. Sell some stock below to trigger the advisor.</p>
      ) : (
        <div className="cards">
          {items.map((s) => (
            <SuggestionCard key={`${s.type}-${s.id}`} suggestion={s} onDecided={onDecided} />
          ))}
        </div>
      )}
    </section>
  )
}
