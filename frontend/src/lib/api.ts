import type {
  Order,
  Product,
  RestockAlert,
  Payment,
  CreateOrderRequest,
  CreateProductRequest,
} from '@/types'

interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
  error?: string
}

async function apiFetch<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
    ...options,
  })

  let body: ApiResponse<T> | null = null
  const text = await response.text()
  if (text) {
    try {
      body = JSON.parse(text)
    } catch {
      // not JSON
    }
  }

  if (!response.ok) {
    const msg = body?.error || body?.message || `HTTP ${response.status}`
    throw new Error(msg)
  }

  if (body && !body.success && body.error) {
    throw new Error(body.error)
  }

  // Unwrap ApiResponse<T>.data; fall back to body itself for non-wrapped responses
  return (body?.data ?? body) as T
}

// Orders — no "list all" endpoint; use per-customer or per-ID
export const ordersApi = {
  getById: (id: string) => apiFetch<Order>(`/api/orders/${id}`),
  getByCustomer: (customerId: string) =>
    apiFetch<Order[]>(`/api/orders/customer/${customerId}`),
  create: (data: CreateOrderRequest) =>
    apiFetch<Order>('/api/orders', { method: 'POST', body: JSON.stringify(data) }),
  cancel: (id: string, reason = 'Admin cancelled') =>
    apiFetch<void>(`/api/orders/${id}?reason=${encodeURIComponent(reason)}`, {
      method: 'DELETE',
    }),
}

// Inventory / Products
export const inventoryApi = {
  getProducts: () => apiFetch<Product[]>('/api/products'),
  getProductById: (id: string) => apiFetch<Product>(`/api/products/${id}`),
  createProduct: (data: CreateProductRequest) =>
    apiFetch<Product>('/api/products', { method: 'POST', body: JSON.stringify(data) }),
  updateProduct: (id: string, data: CreateProductRequest) =>
    apiFetch<Product>(`/api/products/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  // quantity = new absolute stock level (backend sets it, not adds to it)
  updateStock: (productId: string, newQuantity: number) =>
    apiFetch<Product>(
      `/api/inventory/${productId}/stock?quantity=${newQuantity}`,
      { method: 'PATCH' }
    ),
}

// Payments — per-order lookup only
export const paymentsApi = {
  getByOrderId: (orderId: string) => apiFetch<Payment>(`/api/payments/order/${orderId}`),
  getById: (paymentId: string) => apiFetch<Payment>(`/api/payments/${paymentId}`),
}

// Forecasts — uses /api/forecasts/alerts
export const forecastsApi = {
  getAlerts: () => apiFetch<RestockAlert[]>('/api/forecasts/alerts'),
  getLatestAlert: (productId: string) =>
    apiFetch<RestockAlert>(`/api/forecasts/alerts/${productId}/latest`),
  runForecast: (productId: string, productName: string, currentStock: number) =>
    apiFetch<unknown>(
      `/api/forecasts/run/${productId}?productName=${encodeURIComponent(productName)}&currentStock=${currentStock}`,
      { method: 'POST' }
    ),
}
