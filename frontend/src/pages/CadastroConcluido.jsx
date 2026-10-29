import { useNavigate, useSearchParams } from 'react-router-dom'
import { Check, Sparkle, ArrowRight } from 'lucide-react'
import Navbar from '../components/Navbar'
import Circuitos from '../components/Circuitos'
import gatinho from '../assets/gatinho-cadastroconcluido.png'
import './Login.css'
import './CadastroConcluido.css'


//ajustar destinos com o back
const perfis = {
    usuaria: {
        destino: '/comunidade',
        botao: 'ENTRAR NA COMUNIDADE',
        texto: 'Bem-vinda à CyberLadies. Estamos animadas em ter você aqui.'
    },
    educadora: {
        destino: '/educadora',
        botao: 'IR PARA A MINHA ÁREA',
        texto: 'Bem-vinda à CyberLadies. Estamos animadas em ter você ensinando com a gente.',
    },
    empresa: {
        destino: '/empresa',
        botao: 'IR PARA A MINHA ÁREA',
        texto: 'Bem-vinda à CyberLadies. Estamos animadas em ter vocês por aqui.'
    },
    
}

export default function CadastroConcluido() {  //educadoratroConcluido() {
    const navigate = useNavigate()
    const [params] = useSearchParams()
    const dados = perfis[params.get('perfil')] || perfis.usuaria  
    
    return (
        <div className="login concluido">
            <Navbar />
            <Circuitos />

            <main className="login__conteudo">
                <div className="login__grupo">   
                    <img src={gatinho} alt="" className="concluido__gatinho" />

                    <section className="login__cartao concluido__cartao">
                        <div className="concluido__selo" aria-hidden="true">
                            <Sparkle className="concluido__brilho concluido__brilho--a" size={16} />
                            <Sparkle className="concluido__brilho concluido__brilho--b" size={24} />
                            <Sparkle className="concluido__brilho concluido__brilho--c" size={12} />
                            <span className="concluido__icone">
                                <Check size={44} strokeWidth={3.5} />
                            </span>
                        </div>

                        <h1 className="login__titulo">Cadastro completo!</h1>
                        <p className="concluido__texto">{dados.texto}</p>

                        <button
                            type="button"
                            className="login__botao concluido__botao"
                            onClick={() => navigate(dados.destino)}
                        >
                            { dados.botao } <ArrowRight size={18} />
                        </button>
                    </section>
                </div>
            </main>
        </div>
    )
}