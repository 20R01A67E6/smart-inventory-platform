'use client'

import Link from 'next/link'
import { AlertTriangle, ArrowRight } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { Skeleton } from '@/components/ui/skeleton'
import { useProducts } from '@/hooks/useInventory'
import { getStockLevel, getStockPercentage } from '@/lib/utils'
import { cn } from '@/lib/utils'

export function LowStockAlerts() {
  const { data: products, isLoading } = useProducts()

  const lowStock = (products ?? [])
    .filter((p) => getStockLevel(p.stockQuantity, p.reorderThreshold) !== 'healthy')
    .sort((a, b) => a.stockQuantity - b.stockQuantity)
    .slice(0, 7)

  return (
    <Card className="border-slate-800 bg-slate-900/50">
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="flex items-center gap-2 text-base font-semibold text-slate-200">
          <AlertTriangle className="h-4 w-4 text-amber-400" />
          Low Stock Alerts
        </CardTitle>
        <Link
          href="/inventory"
          className="flex items-center gap-1 text-xs text-[#d4af37] hover:text-[#e8c95a] transition-colors"
        >
          Manage <ArrowRight className="h-3 w-3" />
        </Link>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="space-y-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-14 w-full" />
            ))}
          </div>
        ) : lowStock.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <div className="mb-2 h-10 w-10 rounded-full bg-emerald-500/10 flex items-center justify-center">
              <AlertTriangle className="h-5 w-5 text-emerald-400" />
            </div>
            <p className="text-sm font-medium text-slate-300">All stock levels healthy</p>
            <p className="text-xs text-slate-500">No products below reorder threshold</p>
          </div>
        ) : (
          <div className="space-y-3">
            {lowStock.map((product) => {
              const isCritical = getStockLevel(product.stockQuantity, product.reorderThreshold) === 'critical'
              const pct = getStockPercentage(
                product.stockQuantity,
                product.reorderThreshold * 3
              )
              return (
                <div key={product.id} className="space-y-1.5">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-medium text-slate-300 truncate max-w-[60%]">
                      {product.name}
                    </span>
                    <span className={cn('text-xs font-semibold', isCritical ? 'text-red-400' : 'text-amber-400')}>
                      {product.stockQuantity} / {product.reorderThreshold} min
                    </span>
                  </div>
                  <Progress
                    value={pct}
                    className="h-1.5 bg-slate-800"
                    indicatorClassName={cn(isCritical ? 'bg-red-500' : 'bg-amber-500')}
                  />
                </div>
              )
            })}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
