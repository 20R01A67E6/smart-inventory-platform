import { NextRequest, NextResponse } from 'next/server'

const SERVICE_URLS: Record<string, string> = {
  orders: process.env.ORDER_SERVICE_URL || 'http://localhost:8081',
  products: process.env.INVENTORY_SERVICE_URL || 'http://localhost:8082',
  inventory: process.env.INVENTORY_SERVICE_URL || 'http://localhost:8082',
  payments: process.env.PAYMENT_SERVICE_URL || 'http://localhost:8083',
  forecasts: process.env.FORECAST_SERVICE_URL || 'http://localhost:8085',
}

async function proxy(req: NextRequest, { params }: { params: { path: string[] } }) {
  const [service, ...rest] = params.path
  const baseUrl = SERVICE_URLS[service]

  if (!baseUrl) {
    return NextResponse.json({ error: `Unknown service: ${service}` }, { status: 404 })
  }

  const suffix = rest.length > 0 ? `/${rest.join('/')}` : ''
  const targetUrl = `${baseUrl}/api/${service}${suffix}${req.nextUrl.search}`

  try {
    const body =
      req.method !== 'GET' && req.method !== 'HEAD' ? await req.text() : undefined

    const upstream = await fetch(targetUrl, {
      method: req.method,
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body,
    })

    const text = await upstream.text()
    return new NextResponse(text, {
      status: upstream.status,
      headers: { 'Content-Type': 'application/json' },
    })
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Connection failed'
    return NextResponse.json(
      { success: false, error: `Cannot reach ${service} service: ${message}` },
      { status: 503 }
    )
  }
}

export const GET = proxy
export const POST = proxy
export const PUT = proxy
export const PATCH = proxy
export const DELETE = proxy
