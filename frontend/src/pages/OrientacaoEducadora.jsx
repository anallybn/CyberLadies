import { Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Circuitos from '../components/Circuitos'
import { useSessao } from '../hooks/useSessao'
import { temPapel } from '../services/sessaoService'
import './Comunidade.css'
import './AreaConta.css'

export default function OrientacaoEducadora() {
    const { conta } = useSessao()
    return <div className="cm"><Navbar /><Circuitos /><main className="area-conta__conteudo">
        <section className="cm-cartao area-conta__secao"><h1>Quero atuar como educadora</h1>
            <p>Você usa a mesma conta pessoal para participar da comunidade e atuar como educadora.</p>
            <p>Primeiro entre ou crie sua conta pessoal. Depois, acesse “Atuar como educadora”, informe especialidade e instituição e acompanhe a análise.</p>
            <div className="area-conta__acoes">
                {temPapel(conta, 'USUARIA') ? <Link className="cm-botao" to="/educadora">Ver minha solicitação</Link> : <>
                    <Link className="cm-botao" to="/login">Já tenho conta — entrar</Link>
                    <Link className="cm-botao cm-botao--contorno" to="/cadastro/usuaria">Criar conta pessoal</Link>
                </>}
            </div>
        </section>
    </main></div>
}
