'use client'

import { Package, RefreshCw } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { Button } from '@/components/ui/button'
import { cn, getStockLevel, getStockPercentage, formatCurrency } from '@/lib/utils'
import type { Product } from '@/types'

const STOCK_CONFIG = {
  critical: {
    label: 'Critical',
    labelClass: 'text-red-400',
    progressClass: 'bg-red-500',
    badgeClass: 'bg-red-500/10 text-red-400 border-red-500/20',
  },
  low: {
    label: 'Low Stock',
    labelClass: 'text-amber-400',
    progressClass: 'bg-amber-500',
    badgeClass: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  },
  healthy: {
    label: 'Healthy',
    labelClass: 'text-emerald-400',
    progressClass: 'bg-emerald-500',
    badgeClass: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  },
}

interface Props {
  product: Product
  onRestock: (product: Product) => void
}

export function ProductCard({ product, onRestock }: Props) {
  // Use backend field names: stockQuantity, reorderThreshold
  const level = getStockLevel(product.stockQuantity, product.reorderThreshold)
  const config = STOCK_CONFIG[level]
  const maxEstimate = product.reorderThreshold * 4
  const pct = getStockPercentage(product.stockQuantity, maxEstimate)

  return (
    <Card className="border-slate-800 bg-slate-900/50 transition-shadow hover:shadow-lg">
      <CardContent className="p-5">
        <div className="flex items-start justify-between mb-3">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-slate-800 border border-slate-700">
              <Package className="h-5 w-5 text-slate-400" />
            </div>
            <div>
              <p className="font-semibold text-slate-100 leading-tight">{product.name}</p>
              {product.sku && (
                <p className="text-xs text-slate-500 font-mono">{product.sku}</p>
              )}
            </div>
          </div>
          <span className={cn('text-xs font-medium px-2 py-0.5 rounded-full border', config.badgeClass)}>
            {config.label}
          </span>
        </div>

        {product.category && (
          <p className="text-xs text-slate-500 mb-3">{product.category}</p>
        )}

        <div className="space-y-1.5 mb-4">
          <div className="flex justify-between text-sm">
            <span className="text-slate-400">Stock Level</span>
            <span className={cn('font-semibold', config.labelClass)}>
              {product.stockQuantity} units
            </span>
          </div>
          <Progress
            value={pct}
            className="h-2 bg-slate-800"
            indicatorClassName={config.progressClass}
          />
          <div className="flex justify-between text-xs text-slate-600">
            <span>0</span>
            <span>Reorder at: {product.reorderThreshold}</span>
          </div>
        </div>

        <div className="flex items-center justify-between">
          <span className="text-sm font-medium text-[#d4af37]">
            {formatCurrency(product.price)} / unit
          </span>
          <Button
            size="sm"
            variant="outline"
            onClick={() => onRestock(product)}
            className="h-8 border-slate-700 bg-slate-800 text-slate-300 hover:bg-slate-700 hover:text-slate-100 text-xs"
          >
            <RefreshCw className="mr-1.5 h-3.5 w-3.5" /> Restock
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
