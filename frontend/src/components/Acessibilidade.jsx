import { useLayoutEffect, useRef, useState } from 'react'
import { Accessibility, X } from 'lucide-react'
import { aplicarPreferencias, lerPreferencias, salvarPreferencias, PADRAO_ACESSIBILIDADE, prepararTipografia } from '../services/acessibilidade'
import '../styles/acessibilidade.css'

export default function Acessibilidade() {
    const [preferencias, setPreferencias] = useState(lerPreferencias)
    const [aberto, setAberto] = useState(false)
    const [aviso, setAviso] = useState('')
    const abertura = useRef(null)
    const contraste = useRef(null)
    const painel = useRef(null)

    useLayoutEffect(() => { aplicarPreferencias(preferencias) }, [preferencias])
    useLayoutEffect(() => { if (aberto) contraste.current?.focus() }, [aberto])
    useLayoutEffect(() => {
        if (!aberto) return
        const escapar = evento => {
            if (evento.key !== 'Escape' || evento.defaultPrevented) return
            setAberto(false)
            abertura.current?.focus()
        }
        document.addEventListener('keydown', escapar)
        const fora = evento => {
            if (!painel.current?.contains(evento.target) && !abertura.current?.contains(evento.target)) setAberto(false)
        }
        document.addEventListener('pointerdown', fora)
        return () => {
            document.removeEventListener('keydown', escapar)
            document.removeEventListener('pointerdown', fora)
        }
    }, [aberto])
    useLayoutEffect(() => {
        // Também cobre estilos adicionados por navegação/HMR, sem observar o conteúdo da aplicação.
        const observador = new MutationObserver(prepararTipografia)
        observador.observe(document.head, { childList: true, subtree: true })
        return () => observador.disconnect()
    }, [])

    function atualizar(valor) {
        setAviso(salvarPreferencias(valor) ? '' : 'Não foi possível salvar neste navegador. As opções continuam válidas nesta página.')
        setPreferencias(valor)
    }
    function fechar() { setAberto(false); abertura.current?.focus() }
    function pular(evento) {
        const principal = document.querySelector('#root main')
        if (!principal) return
        evento.preventDefault()
        if (!principal.id) principal.id = 'conteudo-principal'
        if (!principal.hasAttribute('tabindex')) principal.tabIndex = -1
        principal.focus({ preventScroll: true })
        principal.scrollIntoView({ block: 'start' })
    }

    return <div className="a11y">
        <a className="a11y-pular" href="#conteudo-principal" onClick={pular}>Pular para o conteúdo</a>
        <div className="a11y-barra">
            <button ref={abertura} className="a11y-abrir" type="button" aria-label="Acessibilidade" aria-expanded={aberto}
                aria-controls="a11y-painel" onClick={() => aberto ? fechar() : setAberto(true)}>
                <Accessibility size={24} aria-hidden="true" />
            </button>
        </div>
        {aberto && <section ref={painel} id="a11y-painel" className="a11y-painel" aria-labelledby="a11y-titulo">
            <div className="a11y-cabecalho"><h2 id="a11y-titulo">Acessibilidade</h2>
                <button type="button" className="a11y-fechar" aria-label="Fechar acessibilidade" onClick={fechar}><X size={20} aria-hidden="true" /></button>
            </div>
            <button ref={contraste} className="a11y-contraste" type="button" role="switch" aria-checked={preferencias.altoContraste}
                onClick={() => atualizar({ ...preferencias, altoContraste: !preferencias.altoContraste })}>
                <span>Alto contraste</span><span className="a11y-interruptor" aria-hidden="true" />
            </button>
            <span className="a11y-rotulo">Tamanho do texto</span>
            <div className="a11y-tamanho" role="group" aria-label="Tamanho do texto">
                <button type="button" aria-label="Diminuir tamanho do texto" aria-describedby="a11y-percentual"
                    disabled={preferencias.tamanhoTexto <= 100} onClick={() => atualizar({ ...preferencias, tamanhoTexto: Math.max(100, preferencias.tamanhoTexto - 10) })}>A−</button>
                <output id="a11y-percentual" aria-live="polite">{preferencias.tamanhoTexto}%</output>
                <button type="button" aria-label="Aumentar tamanho do texto" aria-describedby="a11y-percentual"
                    disabled={preferencias.tamanhoTexto >= 200} onClick={() => atualizar({ ...preferencias, tamanhoTexto: Math.min(200, preferencias.tamanhoTexto + 10) })}>A+</button>
            </div>
            <button className="a11y-restaurar" type="button" onClick={() => atualizar({ ...PADRAO_ACESSIBILIDADE })}>Restaurar padrão</button>
            {aviso && <p role="status">{aviso}</p>}
        </section>}
    </div>
}
