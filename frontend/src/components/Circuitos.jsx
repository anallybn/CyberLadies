import circuitos from '../assets/circuitos.svg'
import './Circuitos.css'

export default function Circuitos() {
    return (
        <>
            <img src={circuitos} alt="" className="circuitos circuitos--tl"/>
            <img src={circuitos} alt="" className="circuitos circuitos--tr" />
            <img src={circuitos} alt="" className="circuitos circuitos--bl" />
            <img src={circuitos} alt="" className="circuitos circuitos--br" />
        </>
    )
}

