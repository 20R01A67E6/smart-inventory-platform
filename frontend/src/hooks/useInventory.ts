import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { inventoryApi } from '@/lib/api'
import { queryKeys } from '@/lib/queryClient'
import type { CreateProductRequest } from '@/types'

export function useProducts() {
  return useQuery({
    queryKey: queryKeys.products.all,
    queryFn: inventoryApi.getProducts,
    refetchInterval: 60_000,
  })
}

export function useProduct(id: string) {
  return useQuery({
    queryKey: queryKeys.products.detail(id),
    queryFn: () => inventoryApi.getProductById(id),
    enabled: !!id,
  })
}

export function useCreateProduct() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateProductRequest) => inventoryApi.createProduct(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.products.all })
    },
  })
}

export function useUpdateStock() {
  const qc = useQueryClient()
  return useMutation({
    // newQuantity = absolute new stock level
    mutationFn: ({ productId, newQuantity }: { productId: string; newQuantity: number }) =>
      inventoryApi.updateStock(productId, newQuantity),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.products.all })
    },
  })
}
