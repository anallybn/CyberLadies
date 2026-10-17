import { useState } from 'react'
import Navbar from '../components/Navbar'
import PerfilPopup from '../components/PerfilPopup'
import logo from '../assets/logo.svg'
import mascote from '../assets/mascote.svg'
import circuitos from '../assets/circuitos.svg'
import './Home.css'


export default function Home(){
    const [popupAberto, setPopupAberto] = useState(false)

    return (
        <div className= "home">
            <Navbar />
            <img src ={circuitos} alt="" className="circuito circuito--tl"/>
            <img src ={circuitos} alt="" className="circuito circuito--tr"/>
            <img src ={circuitos} alt="" className="circuito circuito--br"/>
            <img src ={circuitos} alt="" className="circuito circuito--bl"/>
         

            <main className="home__conteudo">
                <img src={logo} alt="CyberLadies - Compilando futuros, juntas." className="home__logo" />
                
                <div className= "home__login">
                <img src={mascote} alt="Mascote" className="home__mascote" /> 
                <button className="home__botao" onClick={() => setPopupAberto(true)}> 
                Login
                </button>
            </div>
            </main>
            
            {popupAberto && <PerfilPopup onFechar={() => setPopupAberto(false)} /> }
            
        </div>
    )
}