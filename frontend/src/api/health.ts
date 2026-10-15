export type HealthStatus = 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN'

/** Respuesta de GET /api/health (Spring Boot Actuator). */
export interface HealthResponse {
  status: HealthStatus
  components?: Record<string, { status: HealthStatus }>
}

const TIMEOUT_MS = 10_000

/**
 * Consulta el estado del backend.
 * Devuelve null si el backend no responde (apagado, sin red, error del proxy)
 * o si tarda más de 10 segundos.
 */
export async function fetchHealth(signal?: AbortSignal): Promise<HealthResponse | null> {
  const timeout = AbortSignal.timeout(TIMEOUT_MS)
  try {
    const res = await fetch('/api/health', {
      signal: signal ? AbortSignal.any([signal, timeout]) : timeout,
    })
    // 200 = todo UP; 503 = el backend responde pero algún componente (p. ej. la base) está DOWN
    if (res.status !== 200 && res.status !== 503) return null
    return (await res.json()) as HealthResponse
  } catch (error) {
    // Cancelación externa (p. ej. al desmontar): se propaga. Timeout o error de red: sin conexión.
    if (signal?.aborted) throw error
    return null
  }
}
