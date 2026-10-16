import Navbar from '../components/Navbar'
import logo from '../assets/logo.svg'
import mascote from '../assets/mascote.svg'
import circuitos from '../assets/circuitos.svg'
import './Home.css'

export default function Home(){
    return (
        <div className= "home">
            <Navbar />
            <main className="home__conteudo">
                <img src={logo} alt="CyberLadies - Compilando futuros, juntas." className="home__mascote" />
                <img src={mascote} alt="Mascote" className="home__mascote" />
            <div className="home__login">   
                <button className="home__botao"> Login</button>
            </div>
            </main>
            <img src= {circuitos} alt="" className="home__circuitos" />
        </div>
    )
}