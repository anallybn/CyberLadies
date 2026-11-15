import { Routes, Route, Navigate, useLocation } from 'react-router-dom'
import Home from './pages/Home'
import Login from './pages/Login'
import Cadastro from './pages/Cadastro'
import CadastroConcluido from './pages/CadastroConcluido'
import Comunidade from './pages/Comunidade'
import RotaProtegida from './components/RotaProtegida'
import Educadora from './pages/Educadora'
import OrientacaoEducadora from './pages/OrientacaoEducadora'
import AdminEducadoras from './pages/AdminEducadoras'
import AdminEducadora from './pages/AdminEducadora'

export default function App() {
    const { pathname } = useLocation()
    return (
        <Routes>
            <Route path= "/" element={<Home />} />
            <Route path= "/login" element={<Login />} />
            <Route path="/cadastro/educadora" element={<OrientacaoEducadora />} />
            <Route path= "/cadastro/:perfil" element={<Cadastro />} />
            <Route path= "/cadastro-concluido" element={<CadastroConcluido />} />
            <Route path="/comunidade" element={<RotaProtegida papeis={['USUARIA','EDUCADORA']}><Comunidade /></RotaProtegida>} />
            <Route path="/educadora" element={<RotaProtegida papeis={['USUARIA']}><Educadora /></RotaProtegida>} />
            <Route path="/admin/educadoras" element={<RotaProtegida papeis={['ADMIN']}><AdminEducadoras /></RotaProtegida>} />
            <Route path="/admin/educadoras/:id" element={<RotaProtegida papeis={['ADMIN']}><AdminEducadora key={pathname} /></RotaProtegida>} />
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    )
}
