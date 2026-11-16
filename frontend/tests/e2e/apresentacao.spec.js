import { test, expect } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { execFileSync } from 'node:child_process'
import process from 'node:process'

test.skip(process.env.E2E_ISOLATED !== 'true', 'Somente API 18081 e banco descartável 55439.')

function sql(comando) {
    return execFileSync(process.platform === 'win32' ? 'C:/Program Files/PostgreSQL/16/bin/psql.exe' : 'psql',
        ['-X', '-q', '-t', '-A', '-h', '127.0.0.1', '-p', '55439', '-U', 'migration_test', '-d', 'cyberladies_frontend_e2e', '-v', 'ON_ERROR_STOP=1'],
        { input: `DO $$ BEGIN IF current_database()<>'cyberladies_frontend_e2e' THEN RAISE EXCEPTION 'Banco incorreto'; END IF; END $$;\n${comando}`, encoding: 'utf8' }).trim()
}
async function conta(request, educadora = false) {
    const dados = { nome: 'Fixture ' + randomUUID().slice(0, 8), email: randomUUID() + '@example.invalid', senha: randomUUID() }
    expect((await request.post('/api/auth/cadastro/usuaria', { data: dados })).status()).toBe(201)
    const token = (await (await request.post('/api/auth/login', { data: { email: dados.email, senha: dados.senha } })).json()).token
    const headers = { Authorization: 'Bearer ' + token }
    const { id } = await (await request.get('/api/auth/me', { headers })).json()
    if (educadora) {
        expect((await request.post('/api/educadora/solicitacao', { headers, data: { especialidade: 'Java', instituicao: 'Instituição de teste' } })).status()).toBe(201)
        // Fixture exclusivamente isolada. Decisão ADMIN real é exercitada em fluxos.spec.js.
        sql(`UPDATE educadora SET status='APROVADA' WHERE conta_id=${id};`)
    }
    return { ...dados, id, headers }
}
async function login(page, pessoa) {
    await page.goto('/login')
    await page.getByLabel('E-mail', { exact: true }).fill(pessoa.email)
    await page.getByLabel('Senha', { exact: true }).fill(pessoa.senha)
    await page.getByRole('button', { name: 'ENTRAR', exact: true }).click()
    await expect(page).toHaveURL(/\/comunidade$/)
}

test('percurso salvo de vagas, perfil, estudo, certificado e mentoria', async ({ page, request }) => {
    test.setTimeout(120000)
    page.setDefaultTimeout(15000)
    const aluna = await conta(request)
    const educadora = await conta(request, true)
    const marca = randomUUID().slice(0, 8)
    const empresa = Number(sql(`INSERT INTO cyber_empresa(nome) VALUES ('Empresa ${marca}') RETURNING id;`))
    const vaga = Number(sql(`INSERT INTO cyber_vaga(empresa_id,titulo,descricao,requisitos,modalidade,nivel) VALUES (${empresa},'Vaga ${marca}','Descrição salva','Java','REMOTO','JUNIOR') RETURNING id;`))
    const trilha = Number(sql(`INSERT INTO academy_trilha(titulo,descricao,categoria,nivel,elegivel_certificado) VALUES ('Trilha ${marca}','Conteúdo de teste','BACKEND','INICIANTE',true) RETURNING id;`))
    const aula = Number(sql(`INSERT INTO academy_aula(trilha_id,titulo,descricao,ordem,duracao_minutos,video_provedor,video_id,uso_autorizado) VALUES (${trilha},'Aula ${marca}','Aula de teste',1,15,'YOUTUBE','M7lc1UVf-VE',true) RETURNING id;`))
    sql(`INSERT INTO academy_autoria(trilha_id,educadora_id) VALUES (${trilha},${educadora.id});`)
    const mentora = Number(sql(`INSERT INTO academy_mentora(usuaria_id,nome,especialidade) VALUES (${educadora.id},'Mentora ${marca}','Java') RETURNING id;`))
    sql(`INSERT INTO academy_disponibilidade(mentora_id,inicio,fim) VALUES (${mentora},CURRENT_TIMESTAMP+INTERVAL '3 days',CURRENT_TIMESTAMP+INTERVAL '3 days 1 hour');`)

    await login(page, aluna)
    await page.goto('/vagas?vaga=' + vaga)
    await expect(page.getByRole('heading', { name: 'Vaga ' + marca })).toBeVisible()
    await page.getByRole('button', { name: 'Candidatar-se', exact: true }).click()
    await expect(page.getByRole('button', { name: 'Candidatura enviada', exact: true })).toBeDisabled()
    await page.reload()
    await expect(page.getByRole('button', { name: 'Candidatura enviada', exact: true })).toBeDisabled()
    expect((await (await request.get('/api/candidaturas', { headers: aluna.headers })).json()).some(item => item.vagaId === vaga)).toBe(true)
    await page.screenshot({ path: 'test-results/apresentacao-vagas.png', fullPage: true })

    await page.goto('/perfil')
    await page.getByRole('button', { name: 'Editar dados', exact: true }).click()
    await page.getByRole('textbox', { name: 'Sobre você', exact: true }).fill('Perfil salvo na apresentação ' + marca)
    await page.getByRole('button', { name: 'Salvar dados', exact: true }).click()
    await expect(page.getByText('Perfil salvo na apresentação ' + marca, { exact: true })).toBeVisible()
    await page.reload()
    await expect(page.getByText('Perfil salvo na apresentação ' + marca, { exact: true })).toBeVisible()

    await page.goto('/academy/trilhas/' + trilha)
    await page.getByRole('button', { name: 'Iniciar trilha', exact: true }).click()
    await expect(page.getByRole('link', { name: 'Continuar estudos' })).toBeVisible()
    await page.getByRole('link', { name: 'Continuar estudos' }).click()
    await expect(page).toHaveURL(new RegExp('/academy/aulas/' + aula + '$'))
    await page.getByRole('button', { name: 'Marcar como concluída' }).click()
    await expect(page.getByRole('button', { name: 'Aula concluída', exact: true })).toBeDisabled()
    await page.reload()
    await expect(page.getByRole('button', { name: 'Aula concluída', exact: true })).toBeDisabled()
    await page.goto('/perfil')
    await page.getByRole('link', { name: 'Consultar certificado', exact: true }).click()
    await expect(page.getByRole('heading', { name: 'Certificado da plataforma' })).toBeVisible()
    await page.screenshot({ path: 'test-results/apresentacao-certificado.png', fullPage: true })

    await page.goto('/academy/mentorias')
    await page.getByRole('combobox', { name: 'Mentora', exact: true }).selectOption(String(mentora))
    await expect(page.getByRole('combobox', { name: 'Horário (Brasília)', exact: true })).toBeVisible()
    const horario = await page.getByRole('combobox', { name: 'Horário (Brasília)', exact: true }).locator('option').nth(1).getAttribute('value')
    await page.getByRole('combobox', { name: 'Horário (Brasília)', exact: true }).selectOption(horario)
    await page.getByRole('button', { name: 'Confirmar agendamento', exact: true }).click()
    await expect(page.getByRole('button', { name: 'Cancelar agendamento', exact: true })).toBeVisible()
    await page.reload()
    await page.getByRole('button', { name: 'Cancelar agendamento', exact: true }).click()
    await page.getByRole('button', { name: 'Confirmar cancelamento', exact: true }).click()
    await expect(page.getByText('Cancelado', { exact: true })).toBeVisible()

    await login(page, educadora)
    await page.goto('/vagas?vaga=' + vaga)
    await expect(page.getByRole('button', { name: 'Exclusivo para CyberLadie', exact: true })).toBeDisabled()
    expect((await request.post('/api/candidaturas', { headers: educadora.headers, data: { vagaId: vaga } })).status()).toBe(403)
    await page.goto('/educadora/dashboard')
    await expect(page.getByRole('heading', { name: new RegExp(educadora.nome) })).toBeVisible()
    await page.screenshot({ path: 'test-results/apresentacao-dashboard.png', fullPage: true })
    await page.goto('/educadora/minhas-alunas')
    await expect(page.getByRole('row').filter({ hasText: aluna.nome })).toBeVisible()
    await page.getByRole('button', { name: 'Ver progresso', exact: true }).click()
    await expect(page.getByRole('dialog')).toContainText('Aula ' + marca)
    await page.getByRole('button', { name: 'Fechar progresso' }).click()

    await page.goto('/educadora/enviar-material')
    await page.getByLabel('Título do material', { exact: true }).fill('Rascunho ' + marca)
    await page.getByRole('combobox', { name: /Trilha de aprendizagem/ }).selectOption(String(trilha))
    await page.getByRole('button', { name: 'Salvar rascunho', exact: true }).click()
    await expect(page).toHaveURL(/rascunho=/)
    await page.reload()
    await expect(page.getByLabel('Título do material', { exact: true })).toHaveValue('Rascunho ' + marca)
    await expect(page.getByRole('button', { name: 'Publicar material', exact: true })).toBeDisabled()
    await page.goto('/educadora/meus-materiais')
    await expect(page.getByRole('heading', { name: 'Rascunho ' + marca, exact: true })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Aula ' + marca, exact: true })).toBeVisible()
    await page.setViewportSize({ width: 390, height: 844 })
    await page.screenshot({ path: 'test-results/apresentacao-materiais-mobile.png', fullPage: true })
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
})
