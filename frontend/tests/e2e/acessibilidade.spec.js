import { test, expect } from '@playwright/test'

// UI isolada: todas as APIs são respondidas no navegador, sem conectar ao banco.
async function preparar(page, perfil = 'USUARIA', autenticada = true) {
    await page.route('**/api/**', async rota => {
        const caminho = new URL(rota.request().url()).pathname
        const id = perfil === 'ADMIN' ? 3 : 1
        const papeis = perfil === 'EDUCADORA' ? ['USUARIA', 'EDUCADORA'] : [perfil]
        const dados = {
            '/api/auth/login': { token: 'a'.repeat(64) },
            '/api/auth/me': { id, papeis },
            '/api/auth/conta': { id, nome: 'Educadora de acessibilidade', email: 'fixture@example.invalid', perfil: perfil === 'USUARIA' ? 'CYBERLADIE' : perfil, statusEducadora: perfil === 'EDUCADORA' ? 'APROVADA' : null },
            '/api/usuaria': { id, nome: 'Participante da comunidade', competencias: 'Tecnologia', membro: true },
            '/api/posts': [{ id: 10, autora: 'Participante da comunidade', texto: 'Publicação para verificar leitura, contraste e tamanho do texto sem alterar o banco.', criadoEm: '2026-11-16T12:00:00Z', curtidas: 2, comentarios: 0, curtido: false, propria: false }],
            '/api/educadora/dashboard': { nome: 'Educadora de acessibilidade', statusEducadora: 'APROVADA', podePublicar: false, indicadores: { materiaisPublicados: 1, alunasAlcancadas: 3, conclusoes: 1 }, trilhas: [{ id: 11, titulo: 'Introdução ao desenvolvimento de software', inscricoes: 3 }], mentorias: [], materiais: [{ id: 21, titulo: 'Fundamentos de desenvolvimento', trilha: 'Introdução ao desenvolvimento de software', aulas: 1, status: 'Publicado' }] },
            '/api/admin/educadoras/solicitacoes': { total: 1, itens: [{ contaId: 2, nome: 'Solicitante de acessibilidade', especialidade: 'Desenvolvimento de software', status: 'PENDENTE' }] },
        }
        if (caminho === '/api/auth/sessao') return rota.fulfill({ status: 204 })
        if (!(caminho in dados)) throw new Error('API não prevista no teste de UI: ' + caminho)
        await rota.fulfill({ json: dados[caminho] })
    })
    if (autenticada) await page.addInitScript(() => sessionStorage.setItem('token', 'a'.repeat(64)))
}
const abrir = page => page.getByRole('button', { name: 'Acessibilidade', exact: true }).click()
async function ampliar(page) {
    for (let i = 0; i < 10; i++) await page.getByRole('button', { name: 'Aumentar tamanho do texto' }).click()
    await expect(page.locator('#a11y-percentual')).toHaveText('200%')
}
const fonte = locator => locator.evaluate(el => Number.parseFloat(getComputedStyle(el).fontSize))

for (const largura of [1280, 390, 320]) {
    test(`popover flutuante em ${largura}px: abrir, fechar e controles ampliados`, async ({ page }) => {
        await page.setViewportSize({ width: largura, height: 740 })
        await preparar(page, 'USUARIA', false)
        await page.goto('/login')
        const gatilho = page.getByRole('button', { name: 'Acessibilidade', exact: true })
        await expect(gatilho).toHaveAttribute('aria-expanded', 'false')
        const posicao = await page.locator('main').boundingBox()
        await page.screenshot({ path: `test-results/popover-${largura}-fechado.png` })
        await abrir(page)
        expect(await page.locator('main').boundingBox()).toEqual(posicao)
        await page.screenshot({ path: `test-results/popover-${largura}-aberto.png` })
        await page.mouse.click(largura - 5, 5)
        await expect(gatilho).toHaveAttribute('aria-expanded', 'false')
        await abrir(page)
        await page.getByRole('button', { name: 'Fechar acessibilidade' }).click()
        await expect(gatilho).toBeFocused()
        await abrir(page)
        await page.getByRole('switch', { name: 'Alto contraste' }).click()
        await ampliar(page)
        const painel = page.locator('#a11y-painel')
        const limites = await painel.boundingBox()
        expect(limites.x).toBeGreaterThanOrEqual(0)
        expect(limites.y).toBeGreaterThanOrEqual(0)
        expect(limites.x + limites.width).toBeLessThanOrEqual(largura)
        expect(limites.y + limites.height).toBeLessThanOrEqual(740)
        expect(await painel.evaluate(el => el.scrollWidth <= el.clientWidth + 1)).toBe(true)
        await page.screenshot({ path: `test-results/popover-${largura}-combinado.png` })
        await page.keyboard.press('Escape')
        await expect(gatilho).toBeFocused()
        await page.reload()
        await expect(gatilho).toHaveAttribute('aria-expanded', 'false')
        await abrir(page)
        await expect(page.locator('#a11y-percentual')).toHaveText('200%')
        await expect(page.getByRole('switch', { name: 'Alto contraste' })).toBeChecked()
    })
}

test('teclado, pular conteúdo, Escape, foco e persistência/restauração', async ({ page }) => {
    await preparar(page, 'USUARIA', false)
    await page.goto('/login')
    await page.keyboard.press('Tab')
    await expect(page.getByRole('link', { name: 'Pular para o conteúdo' })).toBeFocused()
    await page.keyboard.press('Enter')
    expect(await page.evaluate(() => document.activeElement.tagName)).toBe('MAIN')
    const gatilho = page.getByRole('button', { name: 'Acessibilidade', exact: true })
    await gatilho.focus()
    await page.keyboard.press('Space')
    const contraste = page.getByRole('switch', { name: 'Alto contraste' })
    await expect(contraste).toBeFocused()
    expect(await contraste.evaluate(el => getComputedStyle(el).outlineStyle)).not.toBe('none')
    await page.keyboard.press('Space')
    await expect(page.getByRole('switch', { name: 'Alto contraste' })).toHaveAttribute('aria-checked', 'true')
    await ampliar(page)
    await expect(page.getByRole('button', { name: 'Aumentar tamanho do texto' })).toBeDisabled()
    await page.keyboard.press('Escape')
    await expect(gatilho).toBeFocused()
    await expect(gatilho).toHaveAttribute('aria-expanded', 'false')
    await page.reload()
    await abrir(page)
    await expect(page.locator('#a11y-percentual')).toHaveText('200%')
    await expect(page.getByRole('switch', { name: 'Alto contraste' })).toHaveAttribute('aria-checked', 'true')
    await page.goto('/')
    await abrir(page)
    await expect(page.locator('#a11y-percentual')).toHaveText('200%')
    await page.getByRole('button', { name: 'Restaurar padrão' }).click()
    await expect(page.locator('#a11y-percentual')).toHaveText('100%')
    await expect(page.getByRole('button', { name: 'Diminuir tamanho do texto' })).toBeDisabled()
    expect(await page.evaluate(() => localStorage.getItem('cyberladies.acessibilidade'))).toBeNull()
    await page.reload()
    expect(await page.locator('html').getAttribute('data-a11y-contraste')).toBeNull()
    expect(await page.locator('html').getAttribute('data-a11y-ampliado')).toBeNull()
    const viewport = await page.locator('meta[name="viewport"]').getAttribute('content')
    expect(viewport).not.toMatch(/user-scalable\s*=\s*no|maximum-scale\s*=\s*1(?:\D|$)/)
})

for (const largura of [1280, 390]) {
    for (const [nome, caminho, perfil] of [
        ['login', '/login', 'USUARIA'],
        ['comunidade', '/comunidade', 'USUARIA'],
        ['educadora', '/educadora/dashboard', 'EDUCADORA'],
        ['admin', '/admin/educadoras', 'ADMIN'],
    ]) {
        test(`${nome} em ${largura}px: padrão, contraste e texto real a 200%`, async ({ page }) => {
            await page.setViewportSize({ width: largura, height: 900 })
            await preparar(page, perfil, nome !== 'login')
            await page.goto(caminho)
            const principal = page.locator('main')
            await expect(principal.locator('h1').first()).toBeVisible()
            if (nome === 'comunidade') await expect(page.locator('.cm-post__texto')).toBeVisible()
            if (nome === 'educadora') await expect(page.locator('.ed-saudacao p').first()).toBeVisible()
            if (nome === 'admin') await expect(page.getByRole('link', { name: /Analisar solicitação/ })).toBeVisible()
            const titulo = principal.locator('h1').first()
            const original = await fonte(titulo)
            const texto = nome === 'comunidade' ? page.locator('.cm-post__texto') : principal.locator('p').first()
            const textoOriginal = await fonte(texto)
            const imagem = page.locator('img').first()
            const filtroOriginal = await imagem.evaluate(el => getComputedStyle(el).filter)
            await page.screenshot({ path: `test-results/a11y-${nome}-${largura}-padrao.png`, fullPage: true })
            await abrir(page)
            await page.getByRole('switch', { name: 'Alto contraste' }).click()
            expect(await titulo.evaluate(el => getComputedStyle(el).color)).toBe('rgb(255, 255, 255)')
            expect(await page.locator('body').evaluate(el => getComputedStyle(el).backgroundColor)).toBe('rgb(0, 0, 0)')
            expect(await imagem.evaluate(el => getComputedStyle(el).filter)).toBe(filtroOriginal)
            await ampliar(page)
            expect(await fonte(titulo)).toBeCloseTo(original * 2, 0)
            expect(await fonte(texto)).toBeCloseTo(textoOriginal * 2, 0)
            await page.getByRole('button', { name: 'Fechar acessibilidade' }).click()
            expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true)
            const cortes = await principal.locator('h1,h2,p,button,label,dt,dd').evaluateAll(elementos => elementos.filter(el => {
                const estilo = getComputedStyle(el)
                return el.getClientRects().length && el.clientWidth > 0 && el.textContent.trim() &&
                    !['auto', 'scroll'].includes(estilo.overflowX) && el.scrollWidth > el.clientWidth + 2
            }).map(el => ({ tag: el.tagName, classe: el.className, texto: el.textContent.slice(0, 60) })))
            expect(cortes).toEqual([])
            await page.screenshot({ path: `test-results/a11y-${nome}-${largura}-combinado.png`, fullPage: true })
            await abrir(page)
            await page.getByRole('switch', { name: 'Alto contraste' }).click()
            expect(await fonte(titulo)).toBeCloseTo(original * 2, 0)
            await page.getByRole('button', { name: 'Fechar acessibilidade' }).click()
            await page.screenshot({ path: `test-results/a11y-${nome}-${largura}-ampliado.png`, fullPage: true })
            await abrir(page)
            await page.getByRole('button', { name: 'Restaurar padrão' }).click()
            await page.getByRole('button', { name: 'Fechar acessibilidade' }).click()
            expect(await fonte(titulo)).toBeCloseTo(original, 1)
            expect(await fonte(texto)).toBeCloseTo(textoOriginal, 1)
        })
    }
}

test('login do frontend preservado e erro legível com contraste', async ({ page }) => {
    await preparar(page, 'USUARIA', false)
    await page.goto('/login')
    await abrir(page)
    await page.getByRole('switch', { name: 'Alto contraste' }).click()
    await page.getByRole('button', { name: 'Fechar acessibilidade' }).click()
    await page.getByRole('button', { name: 'ENTRAR', exact: true }).click()
    await expect(page.getByText('Informe seu e-mail.', { exact: true })).toBeVisible()
    await page.getByLabel('E-mail', { exact: true }).fill('fixture@example.invalid')
    await page.getByLabel('Senha', { exact: true }).fill('FixtureSomenteNoNavegador')
    await page.getByRole('button', { name: 'ENTRAR', exact: true }).click()
    await expect(page).toHaveURL(/\/comunidade$/)
    await expect(page.locator('.cm-post__texto')).toBeVisible()
    await abrir(page)
    await expect(page.getByRole('switch', { name: 'Alto contraste' })).toHaveAttribute('aria-checked', 'true')
})
