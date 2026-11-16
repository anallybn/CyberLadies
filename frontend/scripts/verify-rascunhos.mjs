import { spawn, spawnSync } from 'node:child_process'
import { randomUUID } from 'node:crypto'
import { mkdtemp } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import assert from 'node:assert/strict'

const base='http://localhost:5175'
const api='cyberladies-auth-fase1-api'
const destino=spawnSync('docker',['exec',api,'printenv','DATABASE_URL'],{windowsHide:true,encoding:'utf8'})
assert.equal(destino.status,0)
assert.match(destino.stdout.trim(),/^jdbc:postgresql:\/\/banco:5432\/cyberladies_auth_[a-z0-9_]+$/)
const proxy=spawnSync('docker',['exec','cyberladies-auth-fase1-site','printenv','API_PROXY'],{windowsHide:true,encoding:'utf8'})
assert.equal(proxy.stdout.trim(),'http://'+api+':8080')
function cnpjTeste() {
    let raiz=String(100000000000+Math.floor(Math.random()*800000000000))
    for(let i=0;i<2;i++) {
        let soma=0,peso=raiz.length-7
        for(const digito of raiz) { soma+=Number(digito)*peso--; if(peso<2)peso=9 }
        raiz+=soma%11<2?0:11-soma%11
    }
    return raiz
}
const contas=[]
const chrome = process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const profile = await mkdtemp(join(tmpdir(), 'auth-perfis-browser-'))
const child = spawn(chrome, ['--headless=new', '--disable-gpu', '--no-first-run',
    '--remote-debugging-port=9346', '--user-data-dir=' + profile, 'about:blank'],
{ windowsHide: true, stdio: 'ignore' })
const pause = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
let ws
try {
    let pages
    for (let i = 0; i < 100; i++) {
        try { pages = await fetch('http://127.0.0.1:9346/json/list').then((r) => r.json()); break }
        catch { await pause(100) }
    }
    assert.ok(pages, 'Chrome não iniciou')
    ws = new WebSocket(pages.find((p) => p.type === 'page').webSocketDebuggerUrl)
    await new Promise((resolve, reject) => { ws.onopen = resolve; ws.onerror = reject })
    let id = 0
    const pending = new Map()
    const errors = []
    ws.onmessage = (event) => {
        const data = JSON.parse(event.data)
        if (data.id) {
            const request = pending.get(data.id)
            pending.delete(data.id)
            if (data.error) request?.reject(new Error(JSON.stringify(data.error)))
            else request?.resolve(data.result)
        }
        if (data.method === 'Runtime.exceptionThrown') errors.push(data.params.exceptionDetails.text)
        if (data.method === 'Runtime.consoleAPICalled' && data.params.type === 'error')
            errors.push(JSON.stringify(data.params.args))
    }
    function cdp(method, params = {}) {
        return new Promise((resolve, reject) => {
            const requestId = ++id
            const timer = setTimeout(() => { pending.delete(requestId); reject(new Error('CDP timeout: ' + method)) }, 15000)
            pending.set(requestId, {
                resolve: (value) => { clearTimeout(timer); resolve(value) },
                reject: (error) => { clearTimeout(timer); reject(error) },
            })
            ws.send(JSON.stringify({ id: requestId, method, params }))
        })
    }
    const evaluate = async (expression) => {
        const result = await cdp('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
        if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails))
        return result.result.value
    }
    await cdp('Runtime.enable')
    await cdp('Page.enable')
    async function waitFor(expression) {
        for(let i=0;i<200;i++) { if(await evaluate(expression)) return; await pause(100) }
        throw new Error('Tempo esgotado: '+expression+'; '+JSON.stringify(await evaluate("({path:location.pathname,text:document.body.innerText.slice(0,1200)})")))
    }
    async function fill(selector,value) {
        await waitFor("!!document.querySelector("+JSON.stringify(selector)+")")
        await evaluate("(()=>{const e=document.querySelector("+JSON.stringify(selector)+");const p=e.tagName==='TEXTAREA'?HTMLTextAreaElement.prototype:HTMLInputElement.prototype;Object.getOwnPropertyDescriptor(p,'value').set.call(e,"+JSON.stringify(value)+");e.dispatchEvent(new Event('input',{bubbles:true}));})()")
    }
    async function click(selector) {
        await waitFor("!!document.querySelector("+JSON.stringify(selector)+")")
        await evaluate("document.querySelector("+JSON.stringify(selector)+").click()")
    }
    async function minhaConta() {
        return evaluate("(async()=>{const r=await fetch('/api/auth/conta',{headers:{Authorization:'Bearer '+sessionStorage.getItem('token')}});return {status:r.status,conta:await r.json()}})()")
    }
    await cdp('Network.enable')

    await cdp('Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false})

    let aceitarDialogo=false
    ws.addEventListener('message',async(e)=>{
        const m=JSON.parse(e.data)
        if(m.method==='Page.javascriptDialogOpening') await cdp('Page.handleJavaScriptDialog',{accept:aceitarDialogo})
    })
    let pronta=false
    for(let i=0;i<360;i++){try{if((await fetch(base+'/api/academy/catalogo')).ok){pronta=true;break}}catch{}await pause(500)}
    assert.ok(pronta,'API isolada não iniciou')
    const perfis={}
    for(const perfil of ['educadora','educadora2','usuaria','empresa']) {
        const tipo=perfil==='educadora2'?'educadora':perfil
        const dados={nome:'Validação rascunhos '+perfil,email:'rascunhos-'+randomUUID()+'@example.invalid',senha:randomUUID()+'Aa1!'}
        if(tipo==='educadora')Object.assign(dados,{especialidade:'Tecnologia',instituicao:'Ambiente isolado'})
        if(tipo==='usuaria')dados.competencias='Tecnologia'
        if(tipo==='empresa')Object.assign(dados,{cnpj:cnpjTeste(),areaAtuacao:'Tecnologia'})
        assert.equal((await fetch(base+'/api/auth/cadastro/'+tipo,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(dados)})).status,201)
        perfis[perfil]=dados
    }
    async function entrar(perfil) {
        const p=perfis[perfil]
        await cdp('Page.navigate',{url:base+'/login'})
        await fill('#email',p.email);await fill('#senha',p.senha)
        await evaluate("document.querySelector('.login__form').requestSubmit()")
        await waitFor("location.pathname!== '/login' && !!document.querySelector('.topo-logado__avatar')")
    }
    async function apiRascunho(caminho='',method='GET',dados=null) {
        return evaluate("(async()=>{const r=await fetch('/api/educadora/materiais/rascunhos'+"+JSON.stringify(caminho)+",{method:"+JSON.stringify(method)+",headers:{Authorization:'Bearer '+sessionStorage.getItem('token'),'Content-Type':'application/json','Idempotency-Key':crypto.randomUUID()},"+(dados?"body:JSON.stringify("+JSON.stringify(dados)+")":"")+"});return {status:r.status,dados:await r.json().catch(()=>null)}})()")
    }
    async function menu(rota) {
        await click('.topo-logado__avatar');await waitFor("!!document.querySelector('.mp-tag')")
        await evaluate("document.querySelector('.mp a[href="+JSON.stringify(rota)+"]').click()")
    }
    async function sair() {
        await click('.topo-logado__avatar');await waitFor("!!document.querySelector('.mp-tag')")
        await evaluate("Array.from(document.querySelectorAll('.mp button')).find(b=>b.textContent.includes('Sair')).click()")
        await waitFor("location.pathname==='/'")
    }
    async function salvar() {
        await click('.em-acoes button:not(.em-publicar)')
        await waitFor("document.querySelector('#em-salvar-feedback')?.textContent.includes('sucesso')")
    }
    await entrar('educadora')
    await menu('/educadora/enviar-material')
    await waitFor("!!document.querySelector('#em-titulo') && !document.querySelector('.em-acoes button:not(.em-publicar)').disabled")
    await click('.em-acoes button:not(.em-publicar)')
    await waitFor("document.querySelector('#em-salvar-feedback')?.textContent.includes('Informe um título')")
    assert.equal((await apiRascunho()).dados.length,0)
    await fill('#em-titulo','Rascunho real de validação')
    await fill('#em-descricao','Preparação privada sem vídeo.')
    await fill('#em-duracao','27')
    await fill('#em-tags','Java, ensino')
    aceitarDialogo=false
    await click('.em-voltar')
    assert.equal(await evaluate('location.pathname'),'/educadora/enviar-material')
    assert.equal(await evaluate("document.querySelector('#em-titulo').value"),'Rascunho real de validação')
    await evaluate("history.back()")
    await pause(350)
    assert.equal(await evaluate('location.pathname'),'/educadora/enviar-material','Voltar descartou alterações sem confirmação')
    assert.equal(await evaluate("document.querySelector('#em-titulo').value"),'Rascunho real de validação')
    await click('.topo-logado__avatar');await waitFor("!!document.querySelector('.mp-tag')")
    await evaluate("Array.from(document.querySelectorAll('.mp button')).find(b=>b.textContent.includes('Sair')).click()")
    assert.ok(await evaluate("!!sessionStorage.getItem('token')"),'Logout ignorou cancelamento')
    await click('.mp-fechar')
    await evaluate("document.querySelector('.em-acoes button:not(.em-publicar)').click();document.querySelector('.em-acoes button:not(.em-publicar)').click()")
    await waitFor("document.querySelector('#em-salvar-feedback')?.textContent.includes('sucesso')")
    const id1=await evaluate("new URLSearchParams(location.search).get('rascunho')")
    assert.ok(id1)
    assert.equal((await apiRascunho()).dados.length,1)
    assert.equal((await apiRascunho('/'+id1)).dados.revisao,1)
    await cdp('Page.reload');await pause(350)
    await waitFor("document.querySelector('#em-titulo')?.value==='Rascunho real de validação'")
    assert.ok(await evaluate("document.querySelector('.em-publicar').disabled"))
    await menu('/educadora/meus-materiais')
    await waitFor("document.querySelectorAll('.mm-material').length===1")
    assert.equal(await evaluate("Array.from(document.querySelectorAll('.mm-indicadores dd')).map(e=>e.textContent).join('|')"),'1|0|1')
    await evaluate("Array.from(document.querySelectorAll('.mm-status button')).find(b=>b.textContent==='Rascunhos').click()")
    await fill('#mm-busca','sem correspondência')
    await waitFor("document.querySelector('.mm-estado h2')?.textContent.includes('Nenhum')")
    await fill('#mm-busca','rascunho real')
    await waitFor("document.querySelectorAll('.mm-material').length===1")
    await click('.mm-visualizar')
    await waitFor("document.querySelector('#em-titulo')?.value==='Rascunho real de validação'")
    await fill('#em-titulo','Rascunho editado')
    await salvar()
    assert.equal((await apiRascunho('/'+id1)).dados.revisao,2)

    // Outra aba modifica a revisão que o editor carregou.
    const externo=await apiRascunho('/'+id1,'PUT',{titulo:'Versão da outra aba',descricao:'Salva externamente',revisao:2})
    assert.equal(externo.status,200)
    await fill('#em-descricao','Minha versão local preservada')
    await click('.em-acoes button:not(.em-publicar)')
    await waitFor("document.querySelector('#em-salvar-feedback')?.textContent.includes('outra aba')")
    assert.equal(await evaluate("document.querySelector('#em-descricao').value"),'Minha versão local preservada')
    aceitarDialogo=true
    await evaluate("Array.from(document.querySelectorAll('#em-salvar-feedback button')).find(b=>b.textContent.includes('Recarregar')).click()")
    await waitFor("document.querySelector('#em-titulo')?.value==='Versão da outra aba'")
    aceitarDialogo=false

    // Perde apenas a resposta, após o servidor concluir a transação.
    let perder='PUT'
    ws.addEventListener('message',async(e)=>{
        const m=JSON.parse(e.data)
        if(m.method!=='Fetch.requestPaused')return
        try {
            if(m.params.request.method===perder){perder=null;await cdp('Fetch.failRequest',{requestId:m.params.requestId,errorReason:'ConnectionFailed'})}
            else await cdp('Fetch.continueRequest',{requestId:m.params.requestId})
        }catch(error){if(!error.message.includes('Invalid InterceptionId'))throw error}
    })
    await cdp('Fetch.enable',{patterns:[{urlPattern:'*/api/educadora/materiais/rascunhos*',requestStage:'Response'}]})
    await fill('#em-descricao','Resposta perdida, dados preservados')
    await click('.em-acoes button:not(.em-publicar)')
    await waitFor("document.querySelector('#em-salvar-feedback')?.textContent.includes('Não foi possível')")
    assert.equal((await apiRascunho('/'+id1)).dados.revisao,4)
    await salvar()
    assert.equal((await apiRascunho('/'+id1)).dados.revisao,4)

    // Ir para Novo Material limpa os campos, sem duplicar o registro anterior.
    await click('.ed-topo-nav .ativa')
    await waitFor("!location.search && document.querySelector('#em-titulo')?.value===''")
    await fill('#em-titulo','Criação com resposta perdida')
    perder='POST'
    await click('.em-acoes button:not(.em-publicar)')
    await waitFor("document.querySelector('#em-salvar-feedback')?.textContent.includes('Não foi possível')")
    assert.equal((await apiRascunho()).dados.length,2)
    await salvar()
    assert.equal((await apiRascunho()).dados.length,2)
    await cdp('Fetch.disable')
    await sair()
    await entrar('educadora')
    await menu('/educadora/meus-materiais')
    await waitFor("document.querySelectorAll('.mm-material').length===2")
    console.log('PASS salvar pendente, título obrigatório, edição, pesquisa, filtro, F5, logout/login, conflito e resposta perdida sem duplicação')

    const reinicio=spawnSync('docker',['restart',api],{windowsHide:true,encoding:'utf8'})
    assert.equal(reinicio.status,0)
    pronta=false
    for(let i=0;i<360;i++){try{if((await fetch(base+'/api/academy/catalogo')).ok){pronta=true;break}}catch{}await pause(500)}
    assert.ok(pronta)
    await cdp('Page.reload');await pause(350)
    await waitFor("document.querySelectorAll('.mm-material').length===2")
    assert.equal((await apiRascunho('/'+id1)).dados.revisao,4)
    for(const width of [390,768,1440]){
        await cdp('Emulation.setDeviceMetricsOverride',{width,height:1000,deviceScaleFactor:1,mobile:width<600})
        await pause(250)
        assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'))
    }
    await click('.mm-visualizar')
    await waitFor("!!document.querySelector('#em-titulo')?.value")
    for(const width of [390,768,1440]){
        await cdp('Emulation.setDeviceMetricsOverride',{width,height:1000,deviceScaleFactor:1,mobile:width<600})
        assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'))
        assert.ok(await evaluate("document.querySelector('.em-publicar').disabled"))
    }
    await sair()
    await entrar('educadora2')
    assert.equal((await apiRascunho()).dados.length,0)
    assert.equal((await apiRascunho('/'+id1)).status,404)
    assert.equal((await apiRascunho('/'+id1,'PUT',{titulo:'Ataque',revisao:4})).status,404)
    await sair()
    for(const perfil of ['usuaria','empresa']){
        await entrar(perfil)
        assert.equal((await apiRascunho()).status,403)
        assert.equal((await apiRascunho('','POST',{titulo:'Ataque'})).status,403)
        await sair()
    }
    assert.deepEqual(errors,[])
    console.log('PASS persistência após reinício isolado, sessão preservada, responsividade 390/768/1440, isolamento real entre Educadoras e bloqueio CyberLadie/Empresa')
    console.log('PASS Console sem erros JavaScript; somente rascunhos privados de teste foram criados, sem aulas ou trilhas.')
} finally { ws?.close(); child.kill() }
