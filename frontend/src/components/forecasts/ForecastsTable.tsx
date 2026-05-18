'use client'

import { TrendingDown, AlertTriangle, CheckCircle2 } from 'lucide-react'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Progress } from '@/components/ui/progress'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import type { RestockAlert } from '@/types'

function DaysBar({ days }: { days: number }) {
  const isCritical = days <= 7
  const isWarning = days <= 14

  return (
    <div className="flex items-center gap-2">
      {isCritical && <AlertTriangle className="h-3.5 w-3.5 text-red-400 shrink-0" />}
      <span
        className={cn(
          'font-semibold tabular-nums',
          isCritical ? 'text-red-400' : isWarning ? 'text-amber-400' : 'text-emerald-400'
        )}
      >
        {days <= 0 ? 'Stockout!' : `${Math.round(days)}d`}
      </span>
    </div>
  )
}

function ConfidenceBar({ score }: { score: number }) {
  const pct = Math.round(score * 100)
  const color = pct >= 80 ? 'bg-emerald-500' : pct >= 60 ? 'bg-amber-500' : 'bg-red-500'
  return (
    <div className="flex items-center gap-2 min-w-[100px]">
      <Progress value={pct} className="h-1.5 flex-1 bg-slate-800" indicatorClassName={color} />
      <span className="text-xs font-medium text-slate-400 w-8 text-right">{pct}%</span>
    </div>
  )
}

export function ForecastsTable({ alerts }: { alerts: RestockAlert[] }) {
  const sorted = [...alerts].sort((a, b) => a.daysUntilStockout - b.daysUntilStockout)

  return (
    <Table>
      <TableHeader>
        <TableRow className="border-slate-800 hover:bg-transparent">
          <TableHead className="text-slate-500">Product</TableHead>
          <TableHead className="text-slate-500">Current Stock</TableHead>
          <TableHead className="text-slate-500">Days Until Stockout</TableHead>
          <TableHead className="text-slate-500">Recommended Restock</TableHead>
          <TableHead className="text-slate-500">Confidence</TableHead>
          <TableHead className="text-slate-500">Status</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {sorted.length === 0 ? (
          <TableRow>
            <TableCell colSpan={6} className="py-12 text-center">
              <div className="flex flex-col items-center gap-2">
                <CheckCircle2 className="h-8 w-8 text-emerald-500/50" />
                <span className="text-slate-500">No restock alerts — all products healthy</span>
              </div>
            </TableCell>
          </TableRow>
        ) : (
          sorted.map((alert) => {
            const isCritical = alert.daysUntilStockout <= 7
            return (
              <TableRow
                key={alert.id}
                className={cn(
                  'border-slate-800 transition-colors',
                  isCritical ? 'hover:bg-red-950/20 bg-red-950/10' : 'hover:bg-slate-800/40'
                )}
              >
                <TableCell>
                  <div>
                    <p className="font-medium text-slate-200">{alert.productName}</p>
                    <p className="text-xs text-slate-500">
                      {alert.avgDailyDemand.toFixed(1)} units/day avg
                    </p>
                  </div>
                </TableCell>
                <TableCell className="font-medium text-slate-300">
                  {alert.currentStock.toLocaleString()}
                </TableCell>
                <TableCell>
                  <DaysBar days={alert.daysUntilStockout} />
                </TableCell>
                <TableCell>
                  <span className="font-semibold text-[#d4af37]">
                    +{alert.recommendedQuantity.toLocaleString()}
                  </span>
                  <span className="ml-1 text-xs text-slate-500">units</span>
                </TableCell>
                <TableCell>
                  <ConfidenceBar score={alert.confidenceScore} />
                </TableCell>
                <TableCell>
                  {alert.acknowledged ? (
                    <Badge variant="confirmed">Acknowledged</Badge>
                  ) : (
                    <Badge variant="pending">
                      <TrendingDown className="mr-1 h-3 w-3" /> Open
                    </Badge>
                  )}
                </TableCell>
              </TableRow>
            )
          })
        )}
      </TableBody>
    </Table>
  )
}
