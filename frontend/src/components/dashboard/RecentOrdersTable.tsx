'use client'

import Link from 'next/link'
import { ArrowRight, AlertTriangle } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useRestockAlerts } from '@/hooks/useForecasts'
import { formatDate } from '@/lib/utils'
import { cn } from '@/lib/utils'

export function RecentOrdersTable() {
  const { data: alerts, isLoading } = useRestockAlerts()

  const recent = [...(alerts ?? [])]
    .sort((a, b) => new Date(b.generatedAt).getTime() - new Date(a.generatedAt).getTime())
    .slice(0, 8)

  return (
    <Card className="border-slate-800 bg-slate-900/50">
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="flex items-center gap-2 text-base font-semibold text-slate-200">
          <AlertTriangle className="h-4 w-4 text-amber-400" />
          Recent Restock Alerts
        </CardTitle>
        <Link
          href="/forecasts"
          className="flex items-center gap-1 text-xs text-[#d4af37] hover:text-[#e8c95a] transition-colors"
        >
          View all <ArrowRight className="h-3 w-3" />
        </Link>
      </CardHeader>
      <CardContent className="p-0">
        {isLoading ? (
          <div className="space-y-2 p-6">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow className="border-slate-800 hover:bg-transparent">
                <TableHead className="text-slate-500">Product</TableHead>
                <TableHead className="text-slate-500">Stock</TableHead>
                <TableHead className="text-slate-500">Days Left</TableHead>
                <TableHead className="text-slate-500">Restock Qty</TableHead>
                <TableHead className="text-slate-500">Generated</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {recent.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-slate-500 py-8">
                    No restock alerts — all products healthy
                  </TableCell>
                </TableRow>
              ) : (
                recent.map((alert) => {
                  const isCritical = alert.daysUntilStockout <= 7
                  return (
                    <TableRow key={alert.id} className="border-slate-800 hover:bg-slate-800/40">
                      <TableCell className="font-medium text-slate-200">{alert.productName}</TableCell>
                      <TableCell className="text-sm text-slate-300">{alert.currentStock}</TableCell>
                      <TableCell>
                        <span className={cn('font-semibold', isCritical ? 'text-red-400' : 'text-amber-400')}>
                          {Math.round(alert.daysUntilStockout)}d
                        </span>
                      </TableCell>
                      <TableCell className="font-medium text-[#d4af37]">
                        +{alert.recommendedQuantity}
                      </TableCell>
                      <TableCell className="text-xs text-slate-500">
                        {formatDate(alert.generatedAt)}
                      </TableCell>
                    </TableRow>
                  )
                })
              )}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  )
}
