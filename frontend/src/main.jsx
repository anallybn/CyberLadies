import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './styles/global.css'
import App from './App.jsx'
import ContaProvider from './components/ContaProvider'
import Acessibilidade from './components/Acessibilidade'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
    <ContaProvider><Acessibilidade /><App /></ContaProvider>
    </BrowserRouter>
  </StrictMode>,
)
