import { useCallback, useEffect, useRef } from 'react'
import { registrarBloqueioDeVoltar } from '../services/rascunhoSaida'

export function useAlteracoesPendentes(pendente) {
    const sujo = useRef(pendente)
    useEffect(() => { sujo.current = pendente }, [pendente])
    const confirmar = useCallback(() => !sujo.current || window.confirm('Há alterações não salvas neste rascunho. Deseja sair e descartá-las?'), [])
    useEffect(() => {
        const fechar = (evento) => { if (sujo.current) { evento.preventDefault(); evento.returnValue = '' } }
        const link = (evento) => {
            const elemento = evento.target.closest?.('a[href]')
            if (!elemento || evento.defaultPrevented || evento.button !== 0 || evento.metaKey || evento.ctrlKey || evento.shiftKey || evento.altKey || elemento.target === '_blank' || elemento.hasAttribute('download')) return
            const destino = new URL(elemento.href, location.href)
            if (destino.href !== location.href && !confirmar()) { evento.preventDefault(); evento.stopPropagation() }
        }
        let indice = history.state?.idx, restaurando = false
        const voltar = (evento) => {
            if (restaurando) { restaurando=false; evento.stopImmediatePropagation(); return true }
            const proximo=evento.state?.idx, delta=proximo-indice
            if (sujo.current && Number.isInteger(delta) && delta!==0 && !confirmar()) {
                evento.stopImmediatePropagation(); restaurando=true; history.go(-delta); return true
            }
            indice=proximo
        }
        window.addEventListener('beforeunload', fechar)
        document.addEventListener('click', link, true)
        const removerBloqueio=registrarBloqueioDeVoltar(voltar)
        return () => { window.removeEventListener('beforeunload',fechar); document.removeEventListener('click',link,true); removerBloqueio() }
    }, [confirmar])
    return confirmar
}
