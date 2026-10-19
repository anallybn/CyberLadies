import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import PerfilPopup from '../components/PerfilPopup'
import logo from '../assets/logo.svg'
import mascote from '../assets/mascote.svg'
import Circuitos from '../components/Circuitos'
import './Home.css'


export default function Home(){
    const [popupAberto, setPopupAberto] = useState(false)
    const navigate = useNavigate()

    return (
        <div className= "home">
            <Navbar />
            <Circuitos/>
         

            <main className="home__conteudo">
                <img src={logo} alt="CyberLadies - Compilando futuros, juntas." className="home__logo" />
                
                <div className= "home__login">
                <img src={mascote} alt="Mascote" className="home__mascote" /> 
                <button className="home__botao" onClick={() => setPopupAberto(true)}> 
                Login
                </button>
            </div>
            </main>
            
            {popupAberto && (
            <PerfilPopup 
                onFechar={() => setPopupAberto(false)}
                onEscolher={(perfil) => navigate(`/login?perfil=${perfil}`)}
                onLogin={() => navigate('/login')}
            /> 
            )}
            
            
        </div>
    )
}