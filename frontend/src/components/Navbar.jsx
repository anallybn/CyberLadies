import icone from '../assets/icone.svg'
import './Navbar.css'

export default function Navbar(){
    return (
        <header className="navbar">
            <img src={icone} alt="Ícone no menu" className="navbar__icone" />
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