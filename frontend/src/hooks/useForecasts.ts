import { useQuery } from '@tanstack/react-query'
import { forecastsApi } from '@/lib/api'
import { queryKeys } from '@/lib/queryClient'

export function useRestockAlerts() {
  return useQuery({
    queryKey: queryKeys.forecasts.all,
    queryFn: forecastsApi.getAlerts,
    refetchInterval: 5 * 60_000,
  })
}

export function useLatestAlert(productId: string) {
  return useQuery({
    queryKey: queryKeys.forecasts.detail(productId),
    queryFn: () => forecastsApi.getLatestAlert(productId),
    enabled: !!productId,
  })
}
