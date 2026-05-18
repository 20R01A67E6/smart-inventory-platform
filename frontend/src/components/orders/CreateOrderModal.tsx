'use client'

import { useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useCreateOrder } from '@/hooks/useOrders'
import { useProducts } from '@/hooks/useInventory'
import { toast } from '@/components/ui/use-toast'

interface OrderItem {
  productId: string
  quantity: number
}

interface Props {
  open: boolean
  onClose: () => void
  onCreated?: (customerId: string) => void
}

export function CreateOrderModal({ open, onClose, onCreated }: Props) {
  const [customerId, setCustomerId] = useState('')
  const [shippingAddress, setShippingAddress] = useState('')
  const [items, setItems] = useState<OrderItem[]>([{ productId: '', quantity: 1 }])
  const { data: products } = useProducts()
  const createOrder = useCreateOrder()

  function addItem() {
    setItems((prev) => [...prev, { productId: '', quantity: 1 }])
  }

  function removeItem(idx: number) {
    setItems((prev) => prev.filter((_, i) => i !== idx))
  }

  function updateItem(idx: number, field: keyof OrderItem, value: string | number) {
    setItems((prev) =>
      prev.map((item, i) => (i === idx ? { ...item, [field]: value } : item))
    )
  }

  function handleClose() {
    setCustomerId('')
    setShippingAddress('')
    setItems([{ productId: '', quantity: 1 }])
    onClose()
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!customerId.trim() || !shippingAddress.trim()) return
    const validItems = items.filter((i) => i.productId && i.quantity > 0)
    if (validItems.length === 0) return

    try {
      await createOrder.mutateAsync({
        customerId: customerId.trim(),
        shippingAddress: shippingAddress.trim(),
        items: validItems,
      })
      toast({
        title: 'Order created',
        description: 'Order has been placed and saga initiated.',
        variant: 'success' as never,
      })
      onCreated?.(customerId.trim())
      handleClose()
    } catch (err) {
      toast({
        title: 'Failed to create order',
        description: err instanceof Error ? err.message : 'Please try again.',
        variant: 'destructive',
      })
    }
  }

  return (
    <Dialog open={open} onOpenChange={(v) => !v && handleClose()}>
      <DialogContent className="sm:max-w-lg border-slate-700 bg-slate-900">
        <DialogHeader>
          <DialogTitle className="text-slate-100">Create New Order</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label className="text-slate-300">Customer ID (UUID) *</Label>
              <Input
                value={customerId}
                onChange={(e) => setCustomerId(e.target.value)}
                placeholder="e.g. 550e8400-e29b-41d4..."
                required
                className="border-slate-700 bg-slate-800 text-slate-100 font-mono text-xs"
              />
            </div>
            <div className="space-y-2">
              <Label className="text-slate-300">Shipping Address *</Label>
              <Input
                value={shippingAddress}
                onChange={(e) => setShippingAddress(e.target.value)}
                placeholder="123 Warehouse St, City"
                required
                className="border-slate-700 bg-slate-800 text-slate-100"
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label className="text-slate-300">Order Items</Label>
            <div className="space-y-2">
              {items.map((item, idx) => (
                <div key={idx} className="flex items-center gap-2">
                  <select
                    value={item.productId}
                    onChange={(e) => updateItem(idx, 'productId', e.target.value)}
                    className="flex-1 h-10 rounded-md border border-slate-700 bg-slate-800 px-3 text-sm text-slate-100 focus:outline-none focus:ring-2 focus:ring-[#d4af37]"
                    required
                  >
                    <option value="">Select product…</option>
                    {(products ?? []).map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name} (stock: {p.stockQuantity})
                      </option>
                    ))}
                  </select>
                  <Input
                    type="number"
                    min={1}
                    value={item.quantity}
                    onChange={(e) => updateItem(idx, 'quantity', parseInt(e.target.value) || 1)}
                    className="w-20 border-slate-700 bg-slate-800 text-slate-100"
                    required
                  />
                  {items.length > 1 && (
                    <Button type="button" variant="ghost" size="icon" onClick={() => removeItem(idx)}
                      className="text-slate-500 hover:text-red-400">
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  )}
                </div>
              ))}
            </div>
            <Button type="button" variant="ghost" size="sm" onClick={addItem}
              className="text-[#d4af37] hover:text-[#e8c95a] hover:bg-[#d4af37]/10">
              <Plus className="mr-1 h-4 w-4" /> Add Item
            </Button>
          </div>

          <DialogFooter className="gap-2 pt-2">
            <Button type="button" variant="ghost" onClick={handleClose} className="text-slate-400 hover:text-slate-200">
              Cancel
            </Button>
            <Button type="submit" variant="gold" disabled={createOrder.isPending}>
              {createOrder.isPending ? 'Creating…' : 'Create Order'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
