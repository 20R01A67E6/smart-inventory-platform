'use client'

import { useState } from 'react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useUpdateStock } from '@/hooks/useInventory'
import { toast } from '@/components/ui/use-toast'
import type { Product } from '@/types'

interface Props {
  product: Product | null
  onClose: () => void
}

export function RestockModal({ product, onClose }: Props) {
  const [addQty, setAddQty] = useState(50)
  const updateStock = useUpdateStock()

  const newTotal = (product?.stockQuantity ?? 0) + addQty

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!product || addQty <= 0) return

    try {
      // Backend PATCH /api/inventory/{id}/stock?quantity=N sets the absolute new level
      await updateStock.mutateAsync({ productId: product.id, newQuantity: newTotal })
      toast({
        title: 'Restock successful',
        description: `${product.name} stock updated to ${newTotal} units.`,
        variant: 'success' as never,
      })
      setAddQty(50)
      onClose()
    } catch (err) {
      toast({
        title: 'Restock failed',
        description: err instanceof Error ? err.message : 'Please try again.',
        variant: 'destructive',
      })
    }
  }

  return (
    <Dialog open={!!product} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-sm border-slate-700 bg-slate-900">
        <DialogHeader>
          <DialogTitle className="text-slate-100">Restock Product</DialogTitle>
          {product && (
            <DialogDescription className="text-slate-400">
              {product.name} — current stock: {product.stockQuantity} units
            </DialogDescription>
          )}
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="qty" className="text-slate-300">
              Quantity to Add
            </Label>
            <Input
              id="qty"
              type="number"
              min={1}
              value={addQty}
              onChange={(e) => setAddQty(parseInt(e.target.value) || 0)}
              className="border-slate-700 bg-slate-800 text-slate-100"
              required
            />
          </div>
          {product && (
            <p className="text-xs text-slate-500">
              New total will be:{' '}
              <span className="font-semibold text-emerald-400">{newTotal} units</span>
            </p>
          )}
          <DialogFooter className="gap-2">
            <Button type="button" variant="ghost" onClick={onClose} className="text-slate-400 hover:text-slate-200">
              Cancel
            </Button>
            <Button type="submit" variant="gold" disabled={updateStock.isPending}>
              {updateStock.isPending ? 'Restocking…' : 'Confirm Restock'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
