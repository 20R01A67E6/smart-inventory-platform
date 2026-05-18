import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
  }).format(amount)
}

export function formatDate(dateString: string): string {
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(dateString))
}

export function formatShortDate(dateString: string): string {
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(new Date(dateString))
}

// Uses backend field names: stockQuantity, reorderThreshold
export function getStockLevel(
  stockQuantity: number,
  reorderThreshold: number
): 'critical' | 'low' | 'healthy' {
  if (stockQuantity <= 0) return 'critical'
  if (stockQuantity <= reorderThreshold) return 'low'
  return 'healthy'
}

export function getStockPercentage(current: number, max: number): number {
  if (max <= 0) return 0
  return Math.min(Math.round((current / max) * 100), 100)
}

export function truncate(str: string, length: number): string {
  if (str.length <= length) return str
  return str.slice(0, length) + '...'
}
