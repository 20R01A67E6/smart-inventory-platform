import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ordersApi } from '@/lib/api'
import { queryKeys } from '@/lib/queryClient'
import type { CreateOrderRequest } from '@/types'

export function useOrdersByCustomer(customerId: string) {
  return useQuery({
    queryKey: [...queryKeys.orders.all, 'customer', customerId],
    queryFn: () => ordersApi.getByCustomer(customerId),
    enabled: customerId.trim().length > 0,
  })
}

export function useOrder(id: string) {
  return useQuery({
    queryKey: queryKeys.orders.detail(id),
    queryFn: () => ordersApi.getById(id),
    enabled: !!id,
  })
}

export function useCreateOrder() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateOrderRequest) => ordersApi.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.orders.all })
    },
  })
}

export function useCancelOrder() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      ordersApi.cancel(id, reason),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: queryKeys.orders.all })
      qc.invalidateQueries({ queryKey: queryKeys.orders.detail(id) })
    },
  })
}
