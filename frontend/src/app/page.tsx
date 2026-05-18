import { KPICards } from '@/components/dashboard/KPICards'
import { OrderStatusChart } from '@/components/dashboard/OrderStatusChart'
import { RecentOrdersTable } from '@/components/dashboard/RecentOrdersTable'
import { LowStockAlerts } from '@/components/dashboard/LowStockAlerts'

export default function DashboardPage() {
  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-slate-100">Dashboard</h1>
        <p className="mt-1 text-sm text-slate-500">
          Real-time overview of your warehouse operations
        </p>
      </div>

      {/* KPI Cards */}
      <KPICards />

      {/* Charts row */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
        <div className="lg:col-span-3">
          <OrderStatusChart />
        </div>
        <div className="lg:col-span-2">
          <LowStockAlerts />
        </div>
      </div>

      {/* Recent orders */}
      <RecentOrdersTable />
    </div>
  )
}
