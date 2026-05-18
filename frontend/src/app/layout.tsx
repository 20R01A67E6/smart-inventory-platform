import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'
import { QueryProvider } from '@/providers/QueryProvider'
import { MainLayout } from '@/components/layout/MainLayout'
import { Toaster } from '@/components/ui/toaster'

const inter = Inter({ subsets: ['latin'] })

export const metadata: Metadata = {
  title: 'Smart Inventory — Admin Dashboard',
  description: 'Warehouse and inventory management platform',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className="dark">
      <body className={inter.className}>
        <QueryProvider>
          <MainLayout>{children}</MainLayout>
          <Toaster />
        </QueryProvider>
      </body>
    </html>
  )
}
