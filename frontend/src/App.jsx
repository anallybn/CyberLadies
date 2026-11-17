import DashboardEducadora from './pages/DashboardEducadora'
import EnviarMaterial from './pages/EnviarMaterial'
import MeusMateriais from './pages/MeusMateriais'
import MinhasAlunas from './pages/MinhasAlunas'
import Vagas from './pages/Vagas'
import Perfil from './pages/Perfil'
import PerfilAutora from './pages/PerfilAutora'
import Academy from './pages/Academy'
import Trilha from './pages/Trilha'
import Aula from './pages/Aula'
import Mentorias from './pages/Mentorias'
import CertificadoAcademy from './pages/CertificadoAcademy'
import CyberChat from './pages/CyberChat'
import { Routes, Route, Navigate, useLocation } from 'react-router-dom'
import Home from './pages/Home'
import Login from './pages/Login'
import Cadastro from './pages/Cadastro'
import CadastroConcluido from './pages/CadastroConcluido'
import Comunidade from './pages/Comunidade'
import RotaProtegida from './components/RotaProtegida'
import RotaEducadoraAprovada from './components/RotaEducadoraAprovada'
import Educadora from './pages/Educadora'
import OrientacaoEducadora from './pages/OrientacaoEducadora'
import AdminEducadoras from './pages/AdminEducadoras'
import AdminEducadora from './pages/AdminEducadora'

export default function App() {
    const { pathname } = useLocation()
    return (
        <Routes>
            <Route path="/chat" element={<CyberChat />} />
            <Route path="/vagas" element={<Vagas />} />
            <Route path="/perfil" element={<Perfil />} />
            <Route path="/academy" element={<Academy />} />
            <Route path="/academy/trilhas/:id" element={<Trilha />} />
            <Route path="/academy/aulas/:id" element={<Aula />} />
            <Route path="/academy/mentorias" element={<Mentorias />} />
            <Route path="/academy/certificados/:id" element={<CertificadoAcademy />} />
            <Route path="/educadora/dashboard" element={<RotaEducadoraAprovada><DashboardEducadora /></RotaEducadoraAprovada>} />
            <Route path="/educadora/enviar-material" element={<RotaEducadoraAprovada><EnviarMaterial /></RotaEducadoraAprovada>} />
            <Route path="/educadora/meus-materiais" element={<RotaEducadoraAprovada><MeusMateriais /></RotaEducadoraAprovada>} />
            <Route path="/educadora/minhas-alunas" element={<RotaEducadoraAprovada><MinhasAlunas /></RotaEducadoraAprovada>} />
            <Route path= "/" element={<Home />} />
            <Route path= "/login" element={<Login />} />
            <Route path="/cadastro/educadora" element={<OrientacaoEducadora />} />
            <Route path= "/cadastro/:perfil" element={<Cadastro />} />
            <Route path= "/cadastro-concluido" element={<CadastroConcluido />} />
            <Route path="/comunidade" element={<RotaProtegida papeis={['USUARIA','EDUCADORA']}><Comunidade /></RotaProtegida>} />
            <Route path="/comunidade/perfis/:id" element={<RotaProtegida papeis={['USUARIA','EDUCADORA']}><PerfilAutora /></RotaProtegida>} />
            <Route path="/educadora" element={<RotaProtegida papeis={['USUARIA']}><Educadora /></RotaProtegida>} />
            <Route path="/admin/educadoras" element={<RotaProtegida papeis={['ADMIN']}><AdminEducadoras /></RotaProtegida>} />
            <Route path="/admin/educadoras/:id" element={<RotaProtegida papeis={['ADMIN']}><AdminEducadora key={pathname} /></RotaProtegida>} />
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    )
}
