'use client'

import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { useProducts } from '@/hooks/useInventory'
import { getStockLevel } from '@/lib/utils'

const CATEGORY_COLORS = [
  '#d4af37', '#3b82f6', '#10b981', '#f59e0b',
  '#8b5cf6', '#ef4444', '#06b6d4', '#ec4899',
]

const CustomTooltip = ({
  active,
  payload,
  label,
}: {
  active?: boolean
  payload?: { value: number }[]
  label?: string
}) => {
  if (!active || !payload?.length) return null
  return (
    <div className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 shadow-lg">
      <p className="text-xs font-medium text-slate-400">{label}</p>
      <p className="text-lg font-bold text-slate-100">{payload[0].value} products</p>
    </div>
  )
}

export function OrderStatusChart() {
  const { data: products, isLoading } = useProducts()

  const chartData = Object.entries(
    (products ?? []).reduce<Record<string, number>>((acc, p) => {
      const cat = p.category || 'Uncategorised'
      acc[cat] = (acc[cat] ?? 0) + 1
      return acc
    }, {})
  )
    .map(([category, count]) => ({ category, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 8)

  // Also build stock-level bar data
  const stockData = (products ?? []).reduce(
    (acc, p) => {
      const level = getStockLevel(p.stockQuantity, p.reorderThreshold)
      acc[level]++
      return acc
    },
    { healthy: 0, low: 0, critical: 0 }
  )

  const stockChartData = [
    { label: 'Healthy', count: stockData.healthy, color: '#10b981' },
    { label: 'Low', count: stockData.low, color: '#f59e0b' },
    { label: 'Critical', count: stockData.critical, color: '#ef4444' },
  ]

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      {/* Category distribution */}
      <Card className="border-slate-800 bg-slate-900/50">
        <CardHeader className="pb-2">
          <CardTitle className="text-base font-semibold text-slate-200">
            Products by Category
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <Skeleton className="h-52 w-full" />
          ) : chartData.length === 0 ? (
            <div className="flex h-52 items-center justify-center text-sm text-slate-500">
              No product data available
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={210}>
              <BarChart data={chartData} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
                <XAxis dataKey="category" tick={{ fill: '#64748b', fontSize: 10 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: '#64748b', fontSize: 11 }} axisLine={false} tickLine={false} allowDecimals={false} />
                <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(255,255,255,0.03)' }} />
                <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                  {chartData.map((_, i) => (
                    <Cell key={i} fill={CATEGORY_COLORS[i % CATEGORY_COLORS.length]} fillOpacity={0.85} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </CardContent>
      </Card>

      {/* Stock health */}
      <Card className="border-slate-800 bg-slate-900/50">
        <CardHeader className="pb-2">
          <CardTitle className="text-base font-semibold text-slate-200">
            Stock Health
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <Skeleton className="h-52 w-full" />
          ) : (
            <ResponsiveContainer width="100%" height={210}>
              <BarChart data={stockChartData} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
                <XAxis dataKey="label" tick={{ fill: '#64748b', fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: '#64748b', fontSize: 11 }} axisLine={false} tickLine={false} allowDecimals={false} />
                <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(255,255,255,0.03)' }} />
                <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                  {stockChartData.map((entry, i) => (
                    <Cell key={i} fill={entry.color} fillOpacity={0.85} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
