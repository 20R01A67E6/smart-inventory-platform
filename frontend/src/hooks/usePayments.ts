import { useQuery } from '@tanstack/react-query'
import { paymentsApi } from '@/lib/api'
import { queryKeys } from '@/lib/queryClient'

export function usePaymentByOrder(orderId: string) {
  return useQuery({
    queryKey: queryKeys.payments.byOrder(orderId),
    queryFn: () => paymentsApi.getByOrderId(orderId),
    enabled: !!orderId,
  })
}
