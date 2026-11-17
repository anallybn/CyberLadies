import { test, beforeEach } from 'node:test'
import assert from 'node:assert/strict'
import { api } from '../../src/services/api.js'
import { entrar, sair, obterSessao, destinoAposLogin, temPapel } from '../../src/services/sessaoService.js'
import { solicitarEducadora, decidirSolicitacao } from '../../src/services/educadoraService.js'

const dados = new Map()
globalThis.sessionStorage = { getItem: chave => dados.get(chave) ?? null, setItem: (chave, valor) => dados.set(chave, valor), removeItem: chave => dados.delete(chave) }
globalThis.window = { location: { pathname: '/comunidade', search: '' } }
const json = (corpo, status = 200) => new Response(JSON.stringify(corpo), { status, headers: { 'Content-Type': 'application/json' } })
beforeEach(async () => {
    globalThis.fetch = async () => new Response(null, { status: 204 })
    await sair(); dados.clear(); window.location.search = ''
})
async function autenticar(papeis = ['USUARIA','EDUCADORA']) {
    globalThis.fetch = async url => url.endsWith('/auth/login') ? json({ token: 'token-teste' }) : json({ id: 1, papeis })
    return entrar('teste@example.com', 'senha somente de teste')
}
test('papéis vêm de me, acumulam e ADMIN tem destino específico', async () => {
    const conta = await autenticar()
    assert.equal(temPapel(conta, 'USUARIA'), true); assert.equal(temPapel(conta, 'EDUCADORA'), true)
    assert.equal(destinoAposLogin(conta), '/comunidade')
    assert.equal(destinoAposLogin({ id: 2, papeis: ['ADMIN'] }), '/admin/educadoras')
})
test('403 preserva token e identidade', async () => {
    await autenticar()
    globalThis.fetch = async () => json({ detail: 'Acesso negado.' }, 403)
    await assert.rejects(api('/admin/educadoras/solicitacoes'), erro => erro.status === 403)
    assert.equal(sessionStorage.getItem('token'), 'token-teste'); assert.equal(obterSessao().conta.id, 1)
})
test('401 protegido encerra sessão e preserva retorno ao post', async () => {
    await autenticar(); window.location.search = '?post=42'
    globalThis.fetch = async () => json({}, 401)
    await assert.rejects(api('/posts/42'))
    assert.equal(sessionStorage.getItem('token'), null); assert.equal(obterSessao().expirada, true)
    assert.equal(destinoAposLogin({ papeis: ['USUARIA'] }), '/comunidade?post=42')
})
test('401 do login não encerra outra sessão válida', async () => {
    await autenticar(); globalThis.fetch = async () => json({ detail: 'E-mail ou senha incorretos.' }, 401)
    await assert.rejects(entrar('teste@example.com', 'errada'))
    assert.equal(sessionStorage.getItem('token'), 'token-teste')
})
test('resposta antiga não apaga token novo', async () => {
    await autenticar()
    let concluir
    globalThis.fetch = () => new Promise(resolve => { concluir = resolve })
    const anterior = api('/posts')
    sessionStorage.setItem('token', 'novo-token')
    concluir(json({}, 401)); await assert.rejects(anterior)
    assert.equal(sessionStorage.getItem('token'), 'novo-token')
})
test('logout remoto limpa a sessão; falha de conexão permite repetir', async () => {
    await autenticar(); globalThis.fetch = async () => { throw new Error('rede') }
    await assert.rejects(sair()); assert.equal(sessionStorage.getItem('token'), 'token-teste')
    globalThis.fetch = async () => new Response(null, { status: 204 })
    await sair(); assert.equal(sessionStorage.getItem('token'), null); assert.equal(obterSessao().conta, null)
})
test('solicitação e decisão enviam somente os campos permitidos', async () => {
    const corpos = []
    globalThis.fetch = async (_url, opcoes) => { corpos.push(JSON.parse(opcoes.body)); return json({}) }
    await solicitarEducadora('Java', 'Escola'); await decidirSolicitacao(1, 'APROVADA', 'não enviar')
    await decidirSolicitacao(1, 'REJEITADA', 'Motivo')
    assert.deepEqual(corpos, [{ especialidade: 'Java', instituicao: 'Escola' }, { status: 'APROVADA' }, { status: 'REJEITADA', motivo: 'Motivo' }])
})
test('cadastro com 201 sem corpo é uma resposta válida', async () => {
    globalThis.fetch = async () => new Response(null, { status: 201 })
    assert.equal(await api('/auth/cadastro/usuaria', { method: 'POST', dados: {}, autenticado: false }), null)
})
