import { createContext, useContext } from 'react'

export interface DataContextValue {
  /** Cambia cada vez que se modifica algo en el backend; los useApi lo usan para recargar. */
  version: number
  invalidate: () => void
}

export const DataContext = createContext<DataContextValue>({ version: 0, invalidate: () => {} })

export const useDataVersion = () => useContext(DataContext)
