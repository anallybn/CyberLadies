import { useContext } from 'react'
import ContaContext from '../context/contaContext'
export function useConta() { return useContext(ContaContext) }
