'use client'

import { CheckCircle2, XCircle, Clock, RotateCcw } from 'lucide-react'
import { cn, formatDate } from '@/lib/utils'
import type { SagaEvent } from '@/types'

const STATUS_CONFIG = {
  SUCCESS: {
    icon: CheckCircle2,
    iconClass: 'text-emerald-400',
    dotClass: 'bg-emerald-500 shadow-emerald-500/50',
    lineClass: 'bg-emerald-500/30',
  },
  FAILED: {
    icon: XCircle,
    iconClass: 'text-red-400',
    dotClass: 'bg-red-500 shadow-red-500/50',
    lineClass: 'bg-red-500/30',
  },
  PENDING: {
    icon: Clock,
    iconClass: 'text-amber-400',
    dotClass: 'bg-amber-500 shadow-amber-500/50',
    lineClass: 'bg-amber-500/30',
  },
  COMPENSATED: {
    icon: RotateCcw,
    iconClass: 'text-slate-400',
    dotClass: 'bg-slate-500 shadow-slate-500/50',
    lineClass: 'bg-slate-500/30',
  },
}

const EVENT_LABELS: Record<string, string> = {
  ORDER_CREATED: 'Order Created',
  PAYMENT_INITIATED: 'Payment Initiated',
  PAYMENT_CONFIRMED: 'Payment Confirmed',
  PAYMENT_FAILED: 'Payment Failed',
  INVENTORY_RESERVED: 'Inventory Reserved',
  INVENTORY_RELEASED: 'Inventory Released',
  ORDER_CONFIRMED: 'Order Confirmed',
  ORDER_CANCELLED: 'Order Cancelled',
  SAGA_COMPENSATING: 'Saga Compensating',
  SAGA_COMPLETED: 'Saga Completed',
}

export function OrderSagaTimeline({ events }: { events: SagaEvent[] }) {
  if (!events || events.length === 0) {
    return (
      <div className="flex items-center justify-center py-8 text-sm text-slate-500">
        No saga events recorded
      </div>
    )
  }

  const sorted = [...events].sort(
    (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
  )

  return (
    <div className="relative space-y-0">
      {sorted.map((event, idx) => {
        const config = STATUS_CONFIG[event.status as keyof typeof STATUS_CONFIG] ?? STATUS_CONFIG.PENDING
        const Icon = config.icon
        const isLast = idx === sorted.length - 1
        const label = EVENT_LABELS[event.eventType] ?? event.eventType

        return (
          <div key={event.eventId ?? idx} className="flex gap-4">
            {/* Timeline spine */}
            <div className="flex flex-col items-center">
              <div
                className={cn(
                  'flex h-8 w-8 shrink-0 items-center justify-center rounded-full shadow-lg',
                  config.dotClass
                )}
              >
                <Icon className="h-4 w-4 text-white" />
              </div>
              {!isLast && <div className={cn('w-0.5 flex-1 my-1 rounded-full', config.lineClass)} />}
            </div>

            {/* Content */}
            <div className={cn('pb-5 flex-1 min-w-0', isLast && 'pb-0')}>
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="text-sm font-medium text-slate-200">{label}</p>
                  {event.service && (
                    <p className="text-xs text-slate-500 mt-0.5">{event.service}</p>
                  )}
                  {event.errorMessage && (
                    <p className="mt-1 text-xs text-red-400 bg-red-500/10 rounded px-2 py-1">
                      {event.errorMessage}
                    </p>
                  )}
                </div>
                <time className="shrink-0 text-xs text-slate-600">
                  {formatDate(event.timestamp)}
                </time>
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}
