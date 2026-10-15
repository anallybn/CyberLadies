import mascote from '../assets/mascote.svg'
import './Navbar.css'

export default function Navbar(){
    return (
        <header className="navbar">
            <img src={mascote} alt="Mascote no menu" className="navbar__icone" />
            <nav className="navbar__links">
                    <a href="#">Sou empresa</a>
                    <a href="#">Sou educadora</a>
                    <a href="#">Sobre</a>
                    <div className= "navbar__avatar" aria-label="Perfil">
                    </div>
            </nav>
        </header>
    )
}