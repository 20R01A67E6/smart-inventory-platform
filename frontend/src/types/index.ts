// Matches backend OrderController.OrderResponse exactly
export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED'

export interface SagaEvent {
  eventId?: string
  eventType: string
  timestamp: string
  status: 'SUCCESS' | 'FAILED' | 'PENDING' | 'COMPENSATED'
  service?: string
  payload?: Record<string, unknown>
  errorMessage?: string
}

export interface OrderItem {
  id?: string
  productId: string
  productName?: string
  quantity: number
  unitPrice: number
  subtotal?: number
}

export interface Order {
  id: string
  customerId: string
  status: OrderStatus
  totalAmount: number
  shippingAddress?: string
  paymentId?: string
  reservationId?: string
  createdAt: string
  items?: OrderItem[]
}

// Matches backend InventoryController.ProductResponse exactly
export interface Product {
  id: string
  name: string
  sku: string
  description?: string
  price: number
  stockQuantity: number     // backend field name
  reorderThreshold: number  // backend field name
  category: string
  active: boolean
  createdAt: string
}

// Matches backend RestockAlert entity exactly
export interface RestockAlert {
  id: string
  productId: string
  productName: string
  currentStock: number
  recommendedQuantity: number
  daysUntilStockout: number
  avgDailyDemand: number
  confidenceScore: number
  acknowledged: boolean
  generatedAt: string
}

// Matches backend PaymentController.PaymentResponse exactly
export interface Payment {
  id: string
  orderId: string
  customerId: string
  amount: number
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED'
  paymentMethod?: string
  externalTransactionId?: string
  failureReason?: string
  createdAt: string
}

// Matches backend OrderRequest DTO exactly
export interface CreateOrderRequest {
  customerId: string     // UUID string
  shippingAddress: string
  items: {
    productId: string    // UUID string
    quantity: number
  }[]
}

// Matches backend ProductRequest DTO exactly
export interface CreateProductRequest {
  name: string
  sku: string
  description?: string
  price: number
  stockQuantity: number
  reorderThreshold: number
  category: string
}
