import { api, put } from './client'
import type { Bandeja, Notificacion } from './types'

/** Notificaciones del usuario logueado (sale del token). */
export const obtenerBandeja = (signal?: AbortSignal) => api<Bandeja>('/notificaciones', { signal })

export const marcarLeida = (id: number) => put<Notificacion>(`/notificaciones/${id}/leida`, {})

export const marcarTodasLeidas = () => put<void>('/notificaciones/leidas', {})
