import { Routes, Route, Navigate } from 'react-router-dom'
import Home from './pages/Home'
import CyberChat from './pages/CyberChat'
import DashboardEducadora from './pages/DashboardEducadora'
import EnviarMaterial from './pages/EnviarMaterial'
import MeusMateriais from './pages/MeusMateriais'
import MinhasAlunas from './pages/MinhasAlunas'
import Vagas from './pages/Vagas'
import Perfil from './pages/Perfil'
import Academy from './pages/Academy'
import Trilha from './pages/Trilha'
import Aula from './pages/Aula'
import Mentorias from './pages/Mentorias'
import CertificadoAcademy from './pages/CertificadoAcademy'
import Login from './pages/Login'
import Cadastro from './pages/Cadastro'
import CadastroConcluido from './pages/CadastroConcluido'
import Comunidade from './pages/Comunidade'

export default function App() {
    return (
        <Routes>
            <Route path="/chat" element={<CyberChat />} />
            <Route path="/educadora/dashboard" element={<DashboardEducadora />} />
            <Route path="/educadora/enviar-material" element={<EnviarMaterial />} />
            <Route path="/educadora/meus-materiais" element={<MeusMateriais />} />
            <Route path="/educadora/minhas-alunas" element={<MinhasAlunas />} />
            <Route path= "/" element={<Home />} />
            <Route path= "/login" element={<Login />} />
            <Route path= "/cadastro/:perfil" element={<Cadastro />} />
            <Route path= "/cadastro-concluido" element={<CadastroConcluido />} />
            <Route path= "/comunidade" element={<Comunidade />} />
            <Route path="/vagas" element={<Vagas />} />
            <Route path="/perfil" element={<Perfil />} />
            <Route path="/academy" element={<Academy />} />
            <Route path="/academy/trilhas/:id" element={<Trilha />} />
            <Route path="/academy/aulas/:id" element={<Aula />} />
            <Route path="/academy/mentorias" element={<Mentorias />} />
            <Route path="/academy/certificados/:id" element={<CertificadoAcademy />} />
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    )
}