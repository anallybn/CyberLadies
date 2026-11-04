import { Routes, Route, Navigate } from 'react-router-dom'
import Home from './pages/Home'
import Login from './pages/Login'
import Cadastro from './pages/Cadastro'
import CadastroConcluido from './pages/CadastroConcluido'
import Comunidade from './pages/Comunidade'

export default function App() {
    return (
        <Routes>
            <Route path= "/" element={<Home />} />
            <Route path= "/login" element={<Login />} />
            <Route path= "/cadastro/:perfil" element={<Cadastro />} />
            <Route path= "/cadastro-concluido" element={<CadastroConcluido />} />
            <Route path= "/comunidade" element={<Comunidade />} />
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    )
}