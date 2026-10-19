import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom'
import PerfilPopup from '../components/PerfilPopup'
import { ArrowLeft, ArrowRight, AtSign, Lock, Eye, EyeOff } from 'lucide-react'
import Navbar from '../components/Navbar'
import Circuitos from '../components/Circuitos'
import gatinho from '../assets/gatinho-login.png'
import { login } from '../services/authService';
import './Login.css'

export default function Login() {
    const navigate = useNavigate()
    const [params] = useSearchParams()
    const perfil = params.get('perfil')
    const [popupAberto, setPopupAberto] = useState(false)
    const [email, setEmail] = useState('')
    const [senha, setSenha] = useState ('')
    const [mostrarSenha, setMostrarSenha] = useState(false)
    const [erros, setErros] = useState({})
    const [mensagem, setMensagem] = useState(null)
    const [carregando, setCarregando] =useState(false)


    function irParaCadastro() {
    if (perfil) navigate(`/cadastro/${perfil}`)
    else setPopupAberto(true)
}

    function validar() {
        const novos = {}
        if (!email.trim()) {
            novos.email = 'Informe seu e-mail.'
        } else if (!/^\S+@\S+\.\S+$/.test(email.trim())) {
            novos.email = 'Digite um e-mail válido.'
        }
        if (!senha) {
            novos.senha = 'Informe sua senha.'
        }
        return novos 
     }

    async function aoEnviar(e) {
        e.preventDefault()
        setMensagem(null)

        const novos = validar()
        setErros(novos)
        if (Object.keys(novos).length > 0) return

        setCarregando(true)
        try {
            await login(email.trim(), senha)
            setMensagem({ tipo: 'sucesso', texto: 'Login realizado com sucesso!'})
        } catch (erro) {
            setMensagem({ tipo: 'erro', texto: erro.message })
        } finally {
            setCarregando(false)
        }
    }

    return (
        <div className='login'>
            <Navbar />
                <Circuitos />

                    <main className="login__conteudo">
                        <div className="login__grupo">
                            <img src={gatinho} alt="" className="login__gatinho"/>

                        <section className="login__cartao">
                           

                            <button
                                type="button"
                                className="login__voltar"
                                onClick={() => navigate('/')}
                                aria-label="Voltar para a página inicial."
                            >
                                <ArrowLeft size={20} />
                            </button>
                            
                            <h1 className="login__titulo">Bem-vinda de volta!</h1>
                            <p className="login__subtitulo">Entre para continuar sua jornada na tech</p>

                            <form className="login__form" onSubmit={aoEnviar} noValidate>
                                <div className="campo">
                                    <label htmlFor="email">E-mail</label>
                                    <div className={`campo__caixa ${erros.email ? 'campo__caixa--erro' : ''} `}>
                                        <AtSign size={18} />
                                        <input
                                            id="email"
                                            type="email"
                                            value={email}
                                            onChange={(e) => setEmail(e.target.value)}
                                            placeholder="seuemail@exemplo.com"
                                            autoComplete="email"    
                                        />                                    
                                    </div>
                                    {erros.email && <span className="campo__erro" role="alert">{erros.email} </span>}
                                </div>

                                <div className="campo">
                                    <label htmlFor="senha">Senha</label>
                                    <div className={`campo__caixa ${erros.senha ? 'campo__caixa--erro' : '' }`}>
                                        <Lock size={18} />
                                        <input
                                            id="senha"
                                            type={mostrarSenha ? 'text' : 'password'}
                                            value={senha}
                                            onChange={(e) => setSenha(e.target.value)}
                                            placeholder="Sua senha"
                                            autoComplete="current-password"
                                        />
                                        <button
                                            type="button"
                                            className="campo__olho"
                                            onClick={() => setMostrarSenha(!mostrarSenha)}
                                            aria-label={mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'}
                                        >
                                            {mostrarSenha ? <EyeOff size={18} /> : <Eye size={18} />}
                                        </button>
                                    </div>
                                    {erros.senha && <span className="campo__erro" role="alert">{erros.senha}</span>}
                                    </div>

                                    {mensagem && (
                                        <div className={`login__mensagem login__mensagem--${mensagem.tipo}`} role="alert">
                                            {mensagem.texto}
                                        </div>
                                    )}

                                    <button type="submit" className="login__botao" disabled={carregando}>
                                        {carregando ? (
                                            <span className="login__spinner" aria-label="Entrando" />
                                        ) : (
                                            <>
                                                ENTRAR <ArrowRight size={18} />
                                            </>
                                        )}
                                    </button>
                                </form>

                                <p className="login__cadastro">É nova por aqui?</p>

                                <button type="button" className="login__cadastrar" onClick={irParaCadastro}>
                                    CADASTRE-SE
                                </button>  
                        </section>
                    </div>
                </main>
                {popupAberto && (
                    <PerfilPopup
                        onFechar={() => setPopupAberto(false)}
                        onEscolher={(p) => navigate(`/cadastro/${p}`)}
                        onLogin={() => setPopupAberto(false)}
                    />
                )}  
        </div>
    )
}