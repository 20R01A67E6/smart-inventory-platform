'use client'

import { Package, AlertTriangle, TrendingDown, Bell } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { useProducts } from '@/hooks/useInventory'
import { useRestockAlerts } from '@/hooks/useForecasts'
import { getStockLevel } from '@/lib/utils'

function KPICard({
  title,
  value,
  subtitle,
  icon: Icon,
  iconColor,
  iconBg,
  loading,
}: {
  title: string
  value: string | number
  subtitle?: string
  icon: React.ElementType
  iconColor: string
  iconBg: string
  loading?: boolean
}) {
  return (
    <Card className="border-slate-800 bg-slate-900/50">
      <CardContent className="p-6">
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <p className="text-xs font-medium uppercase tracking-wider text-slate-500">{title}</p>
            {loading ? (
              <Skeleton className="h-8 w-24" />
            ) : (
              <p className="text-3xl font-bold text-slate-100">{value}</p>
            )}
            {subtitle && <p className="text-xs text-slate-500">{subtitle}</p>}
          </div>
          <div className={`flex h-11 w-11 items-center justify-center rounded-xl ${iconBg}`}>
            <Icon className={`h-5 w-5 ${iconColor}`} />
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

export function KPICards() {
  const { data: products, isLoading: productsLoading } = useProducts()
  const { data: alerts, isLoading: alertsLoading } = useRestockAlerts()

  const totalProducts = products?.length ?? 0
  const lowStockCount =
    products?.filter(
      (p) => getStockLevel(p.stockQuantity, p.reorderThreshold) !== 'healthy'
    ).length ?? 0
  const criticalCount =
    products?.filter(
      (p) => getStockLevel(p.stockQuantity, p.reorderThreshold) === 'critical'
    ).length ?? 0
  const alertCount = alerts?.filter((a) => !a.acknowledged).length ?? 0

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <KPICard
        title="Total Products"
        value={totalProducts}
        subtitle="Active SKUs in system"
        icon={Package}
        iconColor="text-blue-400"
        iconBg="bg-blue-500/10"
        loading={productsLoading}
      />
      <KPICard
        title="Low Stock Items"
        value={lowStockCount}
        subtitle="Below reorder threshold"
        icon={AlertTriangle}
        iconColor="text-amber-400"
        iconBg="bg-amber-500/10"
        loading={productsLoading}
      />
      <KPICard
        title="Critical Stock"
        value={criticalCount}
        subtitle="At zero or near-zero"
        icon={TrendingDown}
        iconColor="text-red-400"
        iconBg="bg-red-500/10"
        loading={productsLoading}
      />
      <KPICard
        title="Restock Alerts"
        value={alertCount}
        subtitle="Unacknowledged ML alerts"
        icon={Bell}
        iconColor="text-[#d4af37]"
        iconBg="bg-[#d4af37]/10"
        loading={alertsLoading}
      />
    </div>
  )
}
