'use client'

import { useState } from 'react'
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
import { useCreateProduct } from '@/hooks/useInventory'
import { toast } from '@/components/ui/use-toast'

interface Props {
  open: boolean
  onClose: () => void
}

const DEFAULTS = {
  name: '', sku: '', category: '', description: '',
  price: '', stockQuantity: '', reorderThreshold: '',
}

export function AddProductModal({ open, onClose }: Props) {
  const [form, setForm] = useState(DEFAULTS)
  const createProduct = useCreateProduct()

  function set(field: keyof typeof DEFAULTS, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  function handleClose() {
    setForm(DEFAULTS)
    onClose()
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    try {
      await createProduct.mutateAsync({
        name: form.name,
        sku: form.sku,
        category: form.category,
        description: form.description || undefined,
        price: parseFloat(form.price),
        stockQuantity: parseInt(form.stockQuantity),
        reorderThreshold: parseInt(form.reorderThreshold),
      })
      toast({
        title: 'Product added',
        description: `${form.name} has been added to inventory.`,
        variant: 'success' as never,
      })
      handleClose()
    } catch (err) {
      toast({
        title: 'Failed to add product',
        description: err instanceof Error ? err.message : 'Please try again.',
        variant: 'destructive',
      })
    }
  }

  return (
    <Dialog open={open} onOpenChange={(v) => !v && handleClose()}>
      <DialogContent className="sm:max-w-lg border-slate-700 bg-slate-900">
        <DialogHeader>
          <DialogTitle className="text-slate-100">Add New Product</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="col-span-2 space-y-1.5">
              <Label className="text-slate-300">Product Name *</Label>
              <Input value={form.name} onChange={(e) => set('name', e.target.value)}
                placeholder="e.g. Industrial Bearings" required
                className="border-slate-700 bg-slate-800 text-slate-100" />
            </div>
            <div className="space-y-1.5">
              <Label className="text-slate-300">SKU *</Label>
              <Input value={form.sku} onChange={(e) => set('sku', e.target.value)}
                placeholder="e.g. BRNG-001" required
                className="border-slate-700 bg-slate-800 text-slate-100" />
            </div>
            <div className="space-y-1.5">
              <Label className="text-slate-300">Category *</Label>
              <Input value={form.category} onChange={(e) => set('category', e.target.value)}
                placeholder="e.g. Hardware" required
                className="border-slate-700 bg-slate-800 text-slate-100" />
            </div>
            <div className="space-y-1.5">
              <Label className="text-slate-300">Unit Price ($) *</Label>
              <Input type="number" step="0.01" min="0.01" value={form.price}
                onChange={(e) => set('price', e.target.value)} placeholder="0.00" required
                className="border-slate-700 bg-slate-800 text-slate-100" />
            </div>
            <div className="space-y-1.5">
              <Label className="text-slate-300">Initial Stock</Label>
              <Input type="number" min="0" value={form.stockQuantity}
                onChange={(e) => set('stockQuantity', e.target.value)} placeholder="0"
                className="border-slate-700 bg-slate-800 text-slate-100" />
            </div>
            <div className="col-span-2 space-y-1.5">
              <Label className="text-slate-300">Reorder Threshold *</Label>
              <Input type="number" min="0" value={form.reorderThreshold}
                onChange={(e) => set('reorderThreshold', e.target.value)} placeholder="50" required
                className="border-slate-700 bg-slate-800 text-slate-100" />
              <p className="text-xs text-slate-500">Alert triggers when stock falls below this number</p>
            </div>
            <div className="col-span-2 space-y-1.5">
              <Label className="text-slate-300">Description</Label>
              <Input value={form.description} onChange={(e) => set('description', e.target.value)}
                placeholder="Optional product description"
                className="border-slate-700 bg-slate-800 text-slate-100" />
            </div>
          </div>
          <DialogFooter className="gap-2 pt-2">
            <Button type="button" variant="ghost" onClick={handleClose} className="text-slate-400 hover:text-slate-200">
              Cancel
            </Button>
            <Button type="submit" variant="gold" disabled={createProduct.isPending}>
              {createProduct.isPending ? 'Adding…' : 'Add Product'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
