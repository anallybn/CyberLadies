import { test, expect } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import process from 'node:process'

test.skip(process.env.E2E_ISOLATED !== 'true', 'Exige API 18081 ligada exclusivamente ao banco de navegador isolado.')

async function criar(request) {
    const dados = { nome: 'Autora ' + randomUUID().slice(0, 8), email: randomUUID() + '@example.invalid', senha: randomUUID() }
    expect((await request.post('/api/auth/cadastro/usuaria', { data: dados })).status()).toBe(201)
    const login = await request.post('/api/auth/login', { data: { email: dados.email, senha: dados.senha } })
    expect(login.status()).toBe(200)
    const headers = { Authorization: 'Bearer ' + (await login.json()).token }
    const { id } = await (await request.get('/api/auth/me', { headers })).json()
    return { ...dados, headers, id }
}

test('duas contas reais isoladas: autoria, privacidade, páginas, teclado e celular', async ({ page, request }) => {
    test.setTimeout(90000)
    const autora = await criar(request)
    const leitora = await criar(request)
    expect((await request.put('/api/comunidade/adesao', { headers: autora.headers })).status()).toBe(204)
    expect((await request.put('/api/perfil', { headers: autora.headers, data: {
        nome: autora.nome, sobre: 'Apresentação privada não compartilhar', competencias: 'Competências privadas não compartilhar', formacao: 'Formação privada',
    } })).status()).toBe(200)
    let ultimo
    for (let i = 1; i <= 11; i++) {
        const resposta = await request.post('/api/posts', { headers: autora.headers, data: { texto: 'Publicação do perfil ' + i } })
        expect(resposta.status()).toBe(201)
        ultimo = await resposta.json()
    }
    const caminho = '/comunidade/perfis/' + autora.id
    const apiPerfil = '/api' + caminho
    expect((await request.get(apiPerfil)).status()).toBe(401)
    expect((await request.get(apiPerfil, { headers: leitora.headers })).status()).toBe(403)
    await page.goto('/login')
    await page.getByLabel('E-mail', { exact: true }).fill(leitora.email)
    await page.getByLabel('Senha', { exact: true }).fill(leitora.senha)
    await page.getByRole('button', { name: 'ENTRAR', exact: true }).click()
    await expect(page).toHaveURL(/\/comunidade$/)
    await page.goto(caminho)
    await expect(page.getByRole('alert')).toContainText('Entre na comunidade primeiro')
    await page.getByRole('link', { name: 'Participar da comunidade' }).click()
    await page.getByRole('button', { name: 'Participar da comunidade' }).click()
    const post = page.locator('.cm-post').filter({ hasText: 'Publicação do perfil 11' })
    const avatar = post.getByRole('link', { name: 'Ver perfil de ' + autora.nome })
    await expect(avatar).toHaveAttribute('href', caminho)
    await avatar.focus()
    await page.keyboard.press('Enter')
    await expect(page).toHaveURL(new RegExp(caminho + '$'))
    await expect(page.getByRole('heading', { name: autora.nome, exact: true })).toBeFocused()
    await expect(page.locator('.cm-post')).toHaveCount(10)
    await expect(page.getByRole('button', { name: 'Editar', exact: true })).toHaveCount(0)
    await expect(page.getByRole('button', { name: 'Editar dados' })).toHaveCount(0)
    const perfil = await (await request.get(apiPerfil, { headers: leitora.headers })).json()
    expect(perfil).toEqual({ id: autora.id, nome: autora.nome })
    await expect(page.locator('main')).not.toContainText(autora.email)
    await expect(page.locator('main')).not.toContainText('não compartilhar')
    expect((await request.put(apiPerfil, { headers: leitora.headers, data: {} })).status()).toBe(405)
    expect((await request.put('/api/posts/' + ultimo.id, { headers: leitora.headers, data: { texto: 'Não permitido' } })).status()).toBe(403)
    await page.getByRole('link', { name: 'Próxima', exact: true }).click()
    await expect(page.locator('.cm-post')).toHaveCount(1)
    await expect(page.locator('.cm-post__texto')).toHaveText('Publicação do perfil 1')
    await page.getByRole('link', { name: 'Anterior', exact: true }).click()
    await expect(page.locator('.cm-post')).toHaveCount(10)
    await page.evaluate(() => window.scrollTo(0, 0))
    await page.screenshot({ path: 'test-results/perfil-autora-desktop.png', fullPage: true })
    await page.getByRole('button', { name: 'Acessibilidade', exact: true }).click()
    await page.getByRole('switch', { name: 'Alto contraste' }).click()
    for (let i = 0; i < 10; i++) await page.getByRole('button', { name: 'Aumentar tamanho do texto' }).click()
    await page.keyboard.press('Escape')
    await expect(page.getByRole('button', { name: 'Acessibilidade', exact: true })).toBeFocused()
    await page.setViewportSize({ width: 390, height: 844 })
    await expect(page.locator('main')).toBeVisible()
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true)
    await page.evaluate(() => window.scrollTo(0, 0))
    await page.screenshot({ path: 'test-results/perfil-autora-mobile-contraste-200.png', fullPage: true })
    await page.reload()
    await expect(page.locator('.cm-post')).toHaveCount(10)
    await expect(page.locator('html')).toHaveAttribute('data-a11y-contraste', '')
    await page.getByRole('link', { name: 'Voltar ao CyberTalk' }).click()
    const nome = page.locator('.cm-post').filter({ hasText: 'Publicação do perfil 11' }).getByRole('link', { name: autora.nome, exact: true })
    await nome.click()
    await expect(page).toHaveURL(new RegExp(caminho + '$'))
    await page.goto('/comunidade/perfis/9223372036854775807')
    await expect(page.getByRole('heading', { name: 'Perfil não encontrado' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Voltar ao CyberTalk' })).toBeVisible()
})
