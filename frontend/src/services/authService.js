const API_URL = import.meta.env.VITE_API_URL

export async function login(email, senha) {
    let resposta 

    try {
        resposta = await fetch (`${API_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, senha }),
        })
    } catch {
        throw new Error('Não foi possível conectar ao servidor. Tente novamente em instantes')
    }

    if (resposta.status === 401 || resposta.status == 403) {
        throw new Error('E-mail ou senha incorretos.')
    }
    if (!resposta.ok) {
        throw new Error('Ocorreu um erro ao entrar. Tente novamente.');
        
    }

    return resposta.json()
}