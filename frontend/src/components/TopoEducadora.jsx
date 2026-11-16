import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Menu, UserRound } from 'lucide-react'
import icone from '../assets/icone.svg'
import MenuPopup from './MenuPopup'
import { secoes } from './secoes'
import './TopoLogado.css'
import '../pages/DashboardEducadora.css'

export default function TopoEducadora({ ativa = 'dashboard-educadora', confirmarSaida }) {
    const [menu, setMenu] = useState(false)
    const secao = { chat: ['CyberChat', '/chat'], enviar: ['Enviar Material', '/educadora/enviar-material'], materiais: ['Meus Materiais', '/educadora/meus-materiais'], alunas: ['Minhas Alunas', '/educadora/minhas-alunas'] }[ativa] || ['Dashboard', '/educadora/dashboard']
    return <>
        <header className="topo-logado ed-topo">
            <button type="button" aria-label="Abrir menu da Educadora" aria-haspopup="dialog" className="ed-menu-botao" aria-expanded={menu} onClick={() => setMenu(true)}><Menu size={20} /></button>
            <Link to="/educadora/dashboard" aria-label="Dashboard da Educadora"><img src={icone} alt="" className="topo-logado__logo" /></Link>
            <nav className="topo-logado__nav ed-topo-nav" aria-label="Seções do site">
                <Link className="topo-logado__link ativa" to={secao[1]} aria-current="page">{secao[0]}</Link>
                {secoes.filter((secao) => secao.rota).map((secao) => <Link key={secao.id} className="topo-logado__link" to={secao.rota}>{secao.titulo}</Link>)}
            </nav>
            <button className="topo-logado__avatar ed-avatar" type="button" aria-label="Abrir menu da conta" aria-haspopup="dialog" aria-expanded={menu} onClick={() => setMenu(true)}><UserRound size={18} /></button>
        </header>
        {menu && <MenuPopup ativa={ativa} confirmarSaida={confirmarSaida} onFechar={() => setMenu(false)} />}
    </>
}
