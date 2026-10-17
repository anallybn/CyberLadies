import { useEffect} from 'react'
import { UserRound, Layers, Briefcase, ChevronRight, Sparkle } from 'lucide-react'
import gato from '../assets/icone.svg'
import './PerfilPopup.css'

const perfis = [
    {
        id: 'usuaria', 
        titulo: 'CyberLadie', 
        descricao: 'Acesse vagas, trilhas de estudo e a nossa comunidade.', 
        selo: 'Sou usuária', 
        icone: UserRound, 
        cor: 'lilas',
    },

    {
        id: 'educadora', 
        titulo: 'Educadora', 
        descricao: 'Publique conteúdos e acompanhe suas alunas.', 
        selo: 'Sou educadora', 
        icone: Layers, 
        cor: 'lilas',
    },

    {
       id: 'empresa',
       titulo: 'Empresa',
       descricao: 'Publique vagas e encontre talentos femininos em tech.',
       selo: 'Sou empresa',
       icone: Briefcase,
       cor: 'lilas',

    },
]


export default function PerfilPopup({ onFechar, onEscolher, onLogin }) {
    useEffect(() => {
        const aoApertar = (e) => {
            if (e.key === 'Escape') onFechar ()
        }
        document.addEventListener('keydown', aoApertar)
        return () => document.removeEventListener('keydown' , aoApertar)
    }, [onFechar])

    return (
        <div className="perfil__fundo" onClick={onFechar}>
            <div
                className="perfil"
                role="dialog"
                aria-modal= "true"
                aria-labelledby="perfil-titulo"
                onClick={(e) => e.stopPropagation()}
            >
                <Sparkle className="perfil__brilho perfil__brilho--p" size={14} />
                <Sparkle className="perfil__brilho perfil__brilho--g" size={26} />

                <div className= "perfil__icone-topo">
                    <img src={gato} alt="CyberLadies Icone" className="perfil__gato" />
                </div>
               
                <h2 id= "perfil-titulo" className="perfil__titulo">Qual é o seu perfil?</h2>
                <p className="perfil__subtitulo">
                    Escolha como você vai utilizar a plataforma para personalizarmos sua experiência!
                </p>

                <div className="perfil__lista">
                    {perfis.map((p) => {
                        const Icone = p.icone
                        return (
                            <button
                            key={p.id}
                            type="button"
                            className="perfil__card"
                            onClick={() => onEscolher?.(p.id)}
                            >
                              <span className={`perfil__icone perfil__icone--${p.cor}`}>
                                <Icone size={22} />
                              </span>
                              <span className= "perfil__texto">
                                <strong>{p.titulo}</strong>
                                <small>{p.descricao}</small>
                              </span>
                              <span className={`perfil__selo perfil__selo--${p.cor}`}>{p.selo}</span>
                              <ChevronRight className= "perfil__seta" size={20} />
                            </button>
                        )
                    })
                    
                }
                </div>

                <p className="perfil__rodape">
                    Já tem uma conta?{' '}
                    <button type="button" className="perfil__link" onClick={() => onLogin?.()}>
                      Fazer login diretamente
                    </button>
                </p>
            </div>
        </div>
    )

}