import { useSyncExternalStore } from 'react'
import { obterSessao, observarSessao } from '../services/sessaoService'
export function useSessao() { return useSyncExternalStore(observarSessao, obterSessao) }
