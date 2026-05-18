'use client'

import { useState } from 'react'
import { Plus, RefreshCw, Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { ProductCard } from '@/components/inventory/ProductCard'
import { RestockModal } from '@/components/inventory/RestockModal'
import { AddProductModal } from '@/components/inventory/AddProductModal'
import { useProducts } from '@/hooks/useInventory'
import { getStockLevel } from '@/lib/utils'
import type { Product } from '@/types'

type StockFilter = 'ALL' | 'critical' | 'low' | 'healthy'

export default function InventoryPage() {
  const { data: products, isLoading, refetch, isFetching } = useProducts()
  const [search, setSearch] = useState('')
  const [stockFilter, setStockFilter] = useState<StockFilter>('ALL')
  const [restockProduct, setRestockProduct] = useState<Product | null>(null)
  const [addOpen, setAddOpen] = useState(false)

  const filtered = (products ?? []).filter((p) => {
    const matchesSearch =
      !search ||
      p.name.toLowerCase().includes(search.toLowerCase()) ||
      p.sku.toLowerCase().includes(search.toLowerCase()) ||
      p.category.toLowerCase().includes(search.toLowerCase())

    // Use backend field names
    const level = getStockLevel(p.stockQuantity, p.reorderThreshold)
    const matchesStock = stockFilter === 'ALL' || level === stockFilter

    return matchesSearch && matchesStock
  })

  const counts = (products ?? []).reduce(
    (acc, p) => {
      const level = getStockLevel(p.stockQuantity, p.reorderThreshold)
      acc[level]++
      return acc
    },
    { critical: 0, low: 0, healthy: 0 }
  )

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-100">Inventory</h1>
          <p className="mt-1 text-sm text-slate-500">
            {filtered.length} product{filtered.length !== 1 ? 's' : ''}
            {counts.critical > 0 && (
              <span className="ml-2 text-red-400 font-medium">· {counts.critical} critical</span>
            )}
            {counts.low > 0 && (
              <span className="ml-2 text-amber-400 font-medium">· {counts.low} low stock</span>
            )}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="icon" onClick={() => refetch()} disabled={isFetching}
            className="text-slate-400 hover:text-slate-200">
            <RefreshCw className={`h-4 w-4 ${isFetching ? 'animate-spin' : ''}`} />
          </Button>
          <Button variant="gold" onClick={() => setAddOpen(true)}>
            <Plus className="mr-2 h-4 w-4" /> Add Product
          </Button>
        </div>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by name, SKU, or category…"
            className="pl-9 border-slate-700 bg-slate-800 text-slate-100 placeholder:text-slate-500"
          />
        </div>
        <Select value={stockFilter} onValueChange={(v) => setStockFilter(v as StockFilter)}>
          <SelectTrigger className="w-44 border-slate-700 bg-slate-800 text-slate-200">
            <SelectValue placeholder="Stock level" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Levels</SelectItem>
            <SelectItem value="critical">Critical ({counts.critical})</SelectItem>
            <SelectItem value="low">Low Stock ({counts.low})</SelectItem>
            <SelectItem value="healthy">Healthy ({counts.healthy})</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="h-52 w-full rounded-lg" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-slate-800 bg-slate-900/50 py-16">
          <p className="text-slate-400 font-medium">No products found</p>
          <p className="text-sm text-slate-600 mt-1">Try adjusting your search or filter</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {filtered.map((product) => (
            <ProductCard key={product.id} product={product} onRestock={setRestockProduct} />
          ))}
        </div>
      )}

      <RestockModal product={restockProduct} onClose={() => setRestockProduct(null)} />
      <AddProductModal open={addOpen} onClose={() => setAddOpen(false)} />
    </div>
  )
}
