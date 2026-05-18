import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime: 5 * 60_000,
      retry: 2,
      refetchOnWindowFocus: false,
    },
  },
})

export const queryKeys = {
  orders: {
    all: ['orders'] as const,
    detail: (id: string) => ['orders', id] as const,
  },
  products: {
    all: ['products'] as const,
    detail: (id: string) => ['products', id] as const,
  },
  inventory: ['inventory'] as const,
  payments: {
    all: ['payments'] as const,
    byOrder: (orderId: string) => ['payments', 'order', orderId] as const,
  },
  forecasts: {
    all: ['forecasts'] as const,
    detail: (id: string) => ['forecasts', id] as const,
  },
}
