'use client'

import { useState } from 'react'
import { Plus, Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { CreateOrderModal } from '@/components/orders/CreateOrderModal'
import { OrderSagaTimeline } from '@/components/orders/OrderSagaTimeline'
import { useOrdersByCustomer, useOrder } from '@/hooks/useOrders'
import { formatCurrency, formatDate } from '@/lib/utils'
import type { Order, OrderStatus } from '@/types'

const STATUS_VARIANT: Record<string, string> = {
  PENDING: 'pending',
  CONFIRMED: 'confirmed',
  CANCELLED: 'cancelled',
  PROCESSING: 'processing',
  SHIPPED: 'shipped',
  DELIVERED: 'delivered',
}

function OrderDetailModal({
  orderId,
  onClose,
}: {
  orderId: string | null
  onClose: () => void
}) {
  const { data: order, isLoading } = useOrder(orderId ?? '')

  return (
    <Dialog open={!!orderId} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-xl border-slate-700 bg-slate-900 max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-slate-100">
            Order Details
            {order && (
              <span className="ml-2 font-mono text-sm text-slate-500">
                #{order.id.slice(0, 8)}
              </span>
            )}
          </DialogTitle>
        </DialogHeader>
        {isLoading ? (
          <div className="space-y-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : order ? (
          <div className="space-y-6">
            <div className="grid grid-cols-2 gap-4 rounded-lg border border-slate-800 bg-slate-800/50 p-4 text-sm">
              <div>
                <p className="text-slate-500">Customer</p>
                <p className="font-mono text-xs text-slate-200 mt-0.5">{order.customerId}</p>
              </div>
              <div>
                <p className="text-slate-500">Status</p>
                <Badge variant={STATUS_VARIANT[order.status] as never} className="mt-1">
                  {order.status}
                </Badge>
              </div>
              <div>
                <p className="text-slate-500">Total Amount</p>
                <p className="font-semibold text-[#d4af37]">{formatCurrency(order.totalAmount)}</p>
              </div>
              <div>
                <p className="text-slate-500">Created</p>
                <p className="text-slate-300">{formatDate(order.createdAt)}</p>
              </div>
              {order.shippingAddress && (
                <div className="col-span-2">
                  <p className="text-slate-500">Shipping Address</p>
                  <p className="text-slate-300">{order.shippingAddress}</p>
                </div>
              )}
            </div>

            <div>
              <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-slate-500">
                Saga Event Timeline
              </h3>
              <OrderSagaTimeline events={order.items?.length ? [] : []} />
              <p className="text-xs text-slate-600 mt-2">
                Saga events are published via Kafka — check logs for real-time event flow.
              </p>
            </div>

            {order.items && order.items.length > 0 && (
              <div>
                <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-slate-500">
                  Order Items
                </h3>
                <div className="space-y-2">
                  {order.items.map((item, i) => (
                    <div key={i} className="flex justify-between rounded-md border border-slate-800 px-3 py-2 text-sm">
                      <span className="text-slate-300">
                        {item.productName ?? item.productId} × {item.quantity}
                      </span>
                      <span className="font-medium text-slate-200">
                        {item.subtotal != null
                          ? formatCurrency(item.subtotal)
                          : formatCurrency(item.unitPrice * item.quantity)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        ) : (
          <p className="py-8 text-center text-sm text-slate-500">Order not found</p>
        )}
      </DialogContent>
    </Dialog>
  )
}

function OrdersTable({
  orders,
  onSelect,
}: {
  orders: Order[]
  onSelect: (id: string) => void
}) {
  return (
    <Table>
      <TableHeader>
        <TableRow className="border-slate-800 hover:bg-transparent">
          <TableHead className="text-slate-500">Order ID</TableHead>
          <TableHead className="text-slate-500">Status</TableHead>
          <TableHead className="text-slate-500 text-right">Amount</TableHead>
          <TableHead className="text-slate-500">Date</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {orders.length === 0 ? (
          <TableRow>
            <TableCell colSpan={4} className="py-8 text-center text-slate-500">
              No orders found for this customer
            </TableCell>
          </TableRow>
        ) : (
          [...orders]
            .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
            .map((order) => (
              <TableRow
                key={order.id}
                className="cursor-pointer border-slate-800 hover:bg-slate-800/60"
                onClick={() => onSelect(order.id)}
              >
                <TableCell className="font-mono text-xs text-slate-300">
                  {order.id.slice(0, 16)}…
                </TableCell>
                <TableCell>
                  <Badge variant={STATUS_VARIANT[order.status] as never}>{order.status}</Badge>
                </TableCell>
                <TableCell className="text-right font-medium text-slate-200">
                  {formatCurrency(order.totalAmount)}
                </TableCell>
                <TableCell className="text-xs text-slate-500">
                  {formatDate(order.createdAt)}
                </TableCell>
              </TableRow>
            ))
        )}
      </TableBody>
    </Table>
  )
}

export default function OrdersPage() {
  const [customerInput, setCustomerInput] = useState('')
  const [searchedCustomer, setSearchedCustomer] = useState('')
  const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null)
  const [createOpen, setCreateOpen] = useState(false)

  const { data: orders, isLoading, error } = useOrdersByCustomer(searchedCustomer)

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    setSearchedCustomer(customerInput.trim())
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-100">Orders</h1>
          <p className="mt-1 text-sm text-slate-500">
            Search by customer ID to view their orders
          </p>
        </div>
        <Button variant="gold" onClick={() => setCreateOpen(true)}>
          <Plus className="mr-2 h-4 w-4" /> New Order
        </Button>
      </div>

      {/* Customer search */}
      <form onSubmit={handleSearch} className="flex gap-2">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
          <Input
            value={customerInput}
            onChange={(e) => setCustomerInput(e.target.value)}
            placeholder="Enter Customer UUID (e.g. 550e8400-e29b-41d4-a716-446655440000)"
            className="pl-9 border-slate-700 bg-slate-800 text-slate-100 placeholder:text-slate-500 font-mono text-sm"
          />
        </div>
        <Button type="submit" variant="gold" disabled={!customerInput.trim()}>
          Search
        </Button>
      </form>

      {/* Results */}
      <div className="rounded-lg border border-slate-800 bg-slate-900/50">
        {!searchedCustomer ? (
          <div className="flex flex-col items-center justify-center py-16 text-center">
            <Search className="mb-3 h-10 w-10 text-slate-700" />
            <p className="text-slate-400 font-medium">Enter a Customer ID to view orders</p>
            <p className="text-sm text-slate-600 mt-1">
              The order service returns orders per customer
            </p>
          </div>
        ) : isLoading ? (
          <div className="space-y-2 p-6">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : error ? (
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <p className="text-red-400 font-medium">Failed to fetch orders</p>
            <p className="text-xs text-slate-500 mt-1">
              {error instanceof Error ? error.message : 'Check that the customer UUID is valid'}
            </p>
          </div>
        ) : (
          <>
            <div className="px-4 py-3 border-b border-slate-800">
              <p className="text-sm text-slate-400">
                Customer:{' '}
                <span className="font-mono text-slate-300">{searchedCustomer}</span>
                {orders && (
                  <span className="ml-2 text-slate-600">
                    · {orders.length} order{orders.length !== 1 ? 's' : ''}
                  </span>
                )}
              </p>
            </div>
            <OrdersTable orders={orders ?? []} onSelect={setSelectedOrderId} />
          </>
        )}
      </div>

      <CreateOrderModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={(cid) => {
          setCustomerInput(cid)
          setSearchedCustomer(cid)
        }}
      />
      <OrderDetailModal orderId={selectedOrderId} onClose={() => setSelectedOrderId(null)} />
    </div>
  )
}
