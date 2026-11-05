const API_URL = import.meta.env.VITE_API_URL
const MODO_TESTE = import.meta.env.VITE_MODO_TESTE === 'true'

//dados de exemplos para testes
const dadosTeste = {
        usuaria: {
            nome: 'Maria Silva',
            curso: 'Estudante de ADS',
            conexoes: 128,
            publicacoes: 12,
            grupos: 3,
    },
    grupos: [
        {id: 1, sigla: 'MD', nome: 'Mulheres em Dev', membros: '1,2 mil membros' },
        {id: 2, sigla: 'UX', nome: 'UX/UI Design', membros: '860 membros' },
        {id: 3, sigla: 'DS', nome: 'Data Science', membros: '500 membros' },
    ],
    posts: [
        {
            id: 1,
            autora: 'Camila Rodrigues',
            papel: 'Educadora',
            categoria: 'educadora',
            cargo: 'Professora de Back-end',
            tempo: '2h atrás',
            texto: '[DICA] 3 erros que quase todo mundo comete na primeira API REST: esquecer a validação dos dados, devolver o código de status errado e guardar senha sem criptografia. Quer que eu monte um passo a passo?',
            tags: ['java', 'springboot', 'api'],
            curtidas: 42,
            comentarios: 8,
        },
        {
            id: 2,
            autora: 'Fernanda Lima',
            papel: 'Empresa',
            categoria: 'vaga',
            cargo: 'Tech Recruiter',
            tempo: '5h atrás',
            texto: 'Estamos contratando desenvolvedores front-end para nosso time de produto. Vaga disponível em nossa página de carreiras.',
            tags: ['vagas', 'junior'],
            curtidas: 67,
            comentarios: 14,
        },
        {
            id: 3,
            autora: 'Juliana Alves',
            papel: 'Usuária',
            categoria: 'usuaria',
            cargo: 'Estudante de ADS',
            tempo: '10h atrás',
            texto: 'Estou começando na área de desenvolvimento e preciso de dicas para me ajudar a aprender melhor.',
            tags: ['iniciante', 'dicas'],
            curtidas: 12,
            comentarios: 5,
        },
    ],
    sugestoes: [
        {id: 1, nome: 'Beatriz Nunes', papel: 'Educadora', info: 'Front-end' },
        {id: 2, nome: 'Larissa Costa', papel: 'Usuária', info: 'Dados' },
        {id: 3, nome: 'Studio Pixel', papel: 'Empresa', info: 'Design' },
    ],
    eventos: [
        {id: 1, mes: 'NOV', dia: '12', titulo: 'Workshop de Git e GitHub', info: 'Online · 19h' },
        {id: 2, mes: 'NOV', dia: '15', titulo: 'Palestra sobre UX/UI', info: 'Presencial · 14h' },
        {id: 3, mes: 'DEZ', dia: '20', titulo: 'Hackathon de Inovação', info: 'Online · 10h' }
    ],
}

function pausa(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms))
}

function cabecalhos() {
    const token = localStorage.getItem('token')
    return {
        'Content-Type': 'application/json',
        ...(token ? {Authorization: `Bearer ${token}`} : {}),
    }
}

async function buscar(caminho) {
    let resposta 

    try {
        resposta = await fetch(`${API_URL}${caminho}`, { headers: cabecalhos() })
    } catch {
        throw new Error('Não foi possível conectar ao servidor. Tente novamente em instantes.')
    }

    if (resposta.status === 401 || resposta.status === 403) {
        throw new Error('Sua sessão expirou. Entre novamente.')
    }
    if (!resposta.ok) {
        throw new Error('Não foi possível carregar a comunidade. Tente novamente.')
    }
    
    return resposta.json()
}

export async function carregarComunidade() {
    if (MODO_TESTE) {
        await pausa(600)
        return JSON.parse(JSON.stringify(dadosTeste))
    }

    const [usuaria, grupos, posts, sugestoes, eventos] = await Promise.all([
        buscar('/usuaria'),
        buscar('/grupos'),
        buscar('/posts'),
        buscar('/sugestoes'),
        buscar('/eventos'),
    ])

    return { usuaria, grupos, posts, sugestoes, eventos }
}

export async function publicarPost(texto) {
    if (MODO_TESTE) {
        await pausa(400)
        return { 
            id: Date.now(),
            autora: dadosTeste.usuaria.nome,
            papel: 'Usuária',
            categoria: 'usuaria',
            cargo: dadosTeste.usuaria.curso,
            tempo: 'agora',
            texto,
            tags: [],
            curtidas: 0,
            comentarios: 0,
        }
    }

    let resposta 

    try {
        resposta = await fetch(`${API_URL}/posts`, {
            method: 'POST',
            headers: cabecalhos(),
            body: JSON.stringify({ texto }),
        })
    } catch {
        throw new Error('Não foi possível conectar ao servidor. Tente novamente em instantes.')
    }

    if (!resposta.ok) {
        throw new Error('Não foi possível publicar o post. Tente novamente.')
    }

    return resposta.json()
}
