// O listener nasce antes do BrowserRouter, mas só atua enquanto um editor o registra.
// Assim o roteador não desmonta o formulário antes da confirmação de Voltar.
const bloqueios = new Set()
window.addEventListener('popstate', (evento) => {
    for (const bloquear of bloqueios) {
        if (bloquear(evento)) break
    }
}, true)
export function registrarBloqueioDeVoltar(bloquear) {
    bloqueios.add(bloquear)
    return () => bloqueios.delete(bloquear)
}