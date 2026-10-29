import { useState } from "react"
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, ArrowRight, Eye, EyeOff } from 'lucide-react'
import Navbar from '../components/Navbar'
import Circuitos from '../components/Circuitos'
import gatinho from '../assets/gatinho_sorrindo.png'
import { cadastrar } from "../services/authService"
import { perfis } from "./cadastro/perfis"
import './Login.css'
import './Cadastro.css'

export default function Cadastro() {
    const { perfil } = useParams()
    const navigate = useNavigate()
    const config = perfis[perfil]

    const [valores, setValores] = useState({})
    const [visiveis, setVisiveis] = useState({})
    const [erros, setErros] = useState({})
    const [mensagem, setMensagem] = useState(null)
    const [carregando, setCarregando] = useState(false)

    if (!config) return <Navigate to="/" replace />

    function alterar(campo, valor) {
        const final = campo.mascara ? campo.mascara(valor) : valor
        setValores((atual) => ({ ...atual, [campo.name]: final }))
    }

    function validar() {
        const novos = {}

        config.campos.forEach((campo) => {
            const bruto = valores[campo.name] || ''
            const valor = campo.tipo === 'password' ? bruto : bruto.trim()

            if (campo.obrigatorio && !valor) {
                novos[campo.name] = 'Preencha este campo.'
                return
            }
            if (campo.tipo === 'email' && valor && !/^\S+@\S+\.\S+$/.test(valor)) {
                novos[campo.name] = 'Digite um e-mail válido.'
            }
            if (valor && campo.validar) {
            const mensagemErro = campo.validar(valor)
            if (mensagemErro) novos[campo.name] = mensagemErro
            }
        })

        if (!novos.senha && (valores.senha || '').length < 8) {
            novos.senha = 'A senha deve ter pelo menos 8 caracteres.'
        }
        if (!novos.confirmarSenha && valores.confirmarSenha !== valores.senha) {
            novos.confirmarSenha = 'As senhas não coincidem.'
        }

        return novos
    }

    async function aoEnviar(e) {
        e.preventDefault()
        setMensagem(null)

        const novos = validar()
        setErros(novos)
        if (Object.keys(novos).length > 0) return

        const dados = {}
        config.campos
            .filter((campo) => campo.name !== 'confirmarSenha')
            .forEach((campo) => {
                const bruto = valores[campo.name] || ''
                const valor = campo.tipo === 'password' ? bruto : bruto.trim()
                dados[campo.name] = campo.somenteNumeros ? valor.replace(/\D/g, '') : valor
            })

        setCarregando(true)
        try {
            await cadastrar(perfil, dados)   // TODO: reativar quando o back-end existir
            navigate(`/cadastro-concluido?perfil=${perfil}`)
        } catch (erro) {
            setMensagem({ tipo: 'erro', texto: erro.message })
        } finally {
            setCarregando(false)
        }
        
    }

    return (
        <div className="login cadastro">
            <Navbar />
            <Circuitos />

            <main className="login__conteudo">
                <div className="login__grupo">
                    <img src={ gatinho } alt="" className="cadastro__gatinho" />

                    <section className="login__cartao">
                        <button
                            type="button"
                            className="login__voltar"
                            onClick={() => navigate(`/login?perfil=${perfil}`)}
                            aria-label="Voltar para o login"
                        >
                            <ArrowLeft size={20} />
                        </button>

                        <h1 className="login__titulo">{config.titulo}</h1>
                        <p className="login__subtitulo">{config.subtitulo}</p>

                        <form className="cadastro__form" onSubmit={aoEnviar} noValidate>
                            {config.campos.map((campo) => {
                                const Icone = campo.icone
                                const ehSenha = campo.tipo === 'password'
                                const tipo = ehSenha && visiveis[campo.name] ? 'text' : campo.tipo

                                return (
                                    <div
                                        key={campo.name}
                                        className={`campo ${campo.metade ? 'campo--metade' : ''} `}
                                    >
                                        <label htmlFor={campo.name}>{campo.label}</label>
                                        <div className={`campo__caixa ${erros[campo.name] ? 'campo__caixa--erro' : ''}`}>

                                            <Icone size={18} />
                                            <input
                                                id={campo.name}
                                                type={tipo}
                                                value={valores[campo.name] || ''}
                                                onChange={(e) => alterar(campo, e.target.value)}
                                                placeholder={campo.placeholder}
                                                autoComplete={campo.autoComplete}
                                            />

                                            {ehSenha && (
                                                <button
                                                    type="button"
                                                    className="campo__olho"
                                                    onClick={() =>
                                                        setVisiveis((atual) => ({ ...atual, [campo.name] : !atual[campo.name] }))
                                                    }
                                                    aria-label={visiveis[campo.name] ? 'Ocultar senha' : 'Mostrar senha'}
                                                >
                                                    {visiveis[campo.name] ? <EyeOff size={18} /> : <Eye size={18} />}
                                                </button>
                                            )}
                                        </div>
                                        {erros[campo.name] && (
                                            <span className="campo__erro" role="alert">{erros[campo.name]}</span>
                                        )}
                                    </div>
                                
                                )
                            })}

                            {mensagem && (
                                <div className={`login__mensagem login__mensagem--${mensagem.tipo}`} role="alert">
                                    {mensagem.texto}
                                </div>
                            )}

                            <button
                                type="submit"
                                className="login__botao"
                                disabled={carregando}
                            >

                            {carregando ? (
                            <span className="login__spinner" aria-label="Criando conta" />
                            ) : (
                                <>
                                    CRIAR CONTA <ArrowRight size={18} />
                                </>
                            )}
                            </button>
                        </form>
                    </section>          
                </div>
            </main>
        </div>
    )
}
