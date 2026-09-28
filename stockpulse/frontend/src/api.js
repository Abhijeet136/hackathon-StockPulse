const BASE = '/api'

async function request(path, { method = 'GET', body } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  })
  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (!res.ok) {
    throw new Error(data?.message || `${res.status} ${res.statusText}`)
  }
  return data
}

export const api = {
  products: (category) => request('/products' + (category ? `?category=${category}` : '')),
  placeOrder: (id, quantity = 1) => request(`/products/${id}/orders`, { method: 'POST', body: { quantity } }),
  setStock: (id, stockLevel) => request(`/products/${id}/stock`, { method: 'PATCH', body: { stockLevel } }),
  suggestPricing: (id) => request(`/products/${id}/suggest-pricing`, { method: 'POST' }),
  suggestReorder: (id) => request(`/products/${id}/suggest-reorder`, { method: 'POST' }),
  pendingPricing: () => request('/pricing-suggestions?status=PENDING'),
  pendingReorder: () => request('/reorder-suggestions?status=PENDING'),
  decide: (type, id, status) => request(`/${type}-suggestions/${id}`, { method: 'PATCH', body: { status } }),
  strategy: () => request('/admin/strategy'),
  switchStrategy: (change) => request('/admin/strategy', { method: 'PUT', body: change }),
}
