import { spawn, spawnSync } from 'node:child_process'
import { randomUUID } from 'node:crypto'
import { mkdtemp, writeFile } from 'node:fs/promises'
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
const chrome = process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const profile = await mkdtemp(join(tmpdir(), 'auth-perfis-browser-'))
const child = spawn(chrome, ['--headless=new', '--disable-gpu', '--no-first-run',
    '--remote-debugging-port=9348', '--user-data-dir=' + profile, 'about:blank'],
{ windowsHide: true, stdio: 'ignore' })
const pause = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
let ws
try {
    let pages
    for (let i = 0; i < 100; i++) {
        try { pages = await fetch('http://127.0.0.1:9348/json/list').then((r) => r.json()); break }
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
        throw new Error('Tempo esgotado: '+expression+'; '+JSON.stringify(await evaluate("({path:location.pathname})")))
    }
    async function fill(selector,value) {
        await waitFor("!!document.querySelector("+JSON.stringify(selector)+")")
        await evaluate("(()=>{const e=document.querySelector("+JSON.stringify(selector)+");const p=e.tagName==='TEXTAREA'?HTMLTextAreaElement.prototype:HTMLInputElement.prototype;Object.getOwnPropertyDescriptor(p,'value').set.call(e,"+JSON.stringify(value)+");e.dispatchEvent(new Event('input',{bubbles:true}));})()")
    }
    async function click(selector) {
        await waitFor("!!document.querySelector("+JSON.stringify(selector)+")")
        await evaluate("document.querySelector("+JSON.stringify(selector)+").click()")
    }

    const upstream='http://127.0.0.1:18085'
    const destinoTeste=spawnSync('docker',['exec','cyberladies-chat-test-api','printenv','DATABASE_URL'],{windowsHide:true,encoding:'utf8'})
    assert.equal(destinoTeste.stdout.trim(),'jdbc:postgresql://banco:5432/cyberladies_auth_chat_935de7fe96')
    function sql(consulta){
        const r=spawnSync('docker',['exec','cyberladies-banco-1','psql','-U','cyberladies','-d','cyberladies_auth_chat_935de7fe96','-Atc',consulta],{windowsHide:true,encoding:'utf8'})
        assert.equal(r.status,0,r.stderr);return JSON.parse(r.stdout)
    }
    const contas=sql("SELECT json_agg(x) FROM (SELECT DISTINCT u.id,u.email,u.tipo perfil FROM talk_usuaria u JOIN chat_contexto c ON u.id IN(c.aluna_id,c.responsavel_id) WHERE c.aluna_id=(SELECT max(a.id) FROM talk_usuaria a JOIN chat_contexto t ON t.aluna_id=a.id WHERE a.email LIKE 'chat-fixture-%@example.invalid' AND t.tipo='TRILHA' AND t.aprovada AND t.aberto) AND c.aprovada AND c.aberto AND u.email LIKE 'chat-fixture-%@example.invalid') x")
    assert.ok(contas?.some(c=>c.perfil==='CYBERLADIE'),'Execute ChatIntegrationTest antes do teste de navegador')
    let falhaLista=false,perderResposta=false,consumiuResposta=false,contagemPolling=0
    const original=ws.onmessage
    ws.onmessage=event=>{
        original(event)
        const mensagem=JSON.parse(event.data)
        if(mensagem.method!=='Fetch.requestPaused')return
        void (async()=>{
            const {requestId,request}=mensagem.params
            const url=new URL(request.url)
            if(url.pathname==='/api/chat/conversas'&&request.method==='GET')contagemPolling++
            if(falhaLista&&url.pathname==='/api/chat/conversas'&&request.method==='GET'){
                await cdp('Fetch.failRequest',{requestId,errorReason:'ConnectionFailed'});return
            }
            const headers={...request.headers};delete headers.Host;delete headers['Content-Length']
            const resposta=await fetch(upstream+url.pathname+url.search,{method:request.method,headers,body:['GET','HEAD'].includes(request.method)?undefined:request.postData})
            const corpo=Buffer.from(await resposta.arrayBuffer())
            if(perderResposta&&request.method==='POST'&&url.pathname.endsWith('/mensagens')){
                perderResposta=false;consumiuResposta=true
                await cdp('Fetch.failRequest',{requestId,errorReason:'ConnectionFailed'});return
            }
            await cdp('Fetch.fulfillRequest',{requestId,responseCode:resposta.status,responseHeaders:[{name:'Content-Type',value:resposta.headers.get('content-type')||'application/json'},{name:'Cache-Control',value:'no-store'}],body:corpo.toString('base64')})
        })().catch(erro=>{if(!erro.message.includes('Invalid InterceptionId'))errors.push(erro.message)})
    }
    await cdp('Fetch.enable',{patterns:[{urlPattern:'*/api/*'}]})
    async function login(conta){
        await cdp('Page.navigate',{url:base+'/login?retorno=%2Fchat'})
        await fill('#email',conta.email);await fill('#senha','TesteCyberChat123!')
        await evaluate("document.querySelector('.login__form').requestSubmit()")
        await waitFor("location.pathname==='/chat' && !!document.querySelector('.cc-painel') && !document.body.innerText.includes('Carregando conversas')")
    }
    async function sair(){
        await click('.topo-logado__avatar');await waitFor("!!document.querySelector('.mp')")
        await evaluate("Array.from(document.querySelectorAll('.mp button')).find(b=>b.innerText.includes('Sair')).click()")
        await waitFor("location.pathname==='/' && !sessionStorage.getItem('token')")
    }
    const aluna=contas.find(c=>c.perfil==='CYBERLADIE')
    await login(aluna)
    await click('.cc-nova');await waitFor("!!document.querySelector('.cc-contextos ul button')")
    await evaluate("Array.from(document.querySelectorAll('.cc-contextos ul button')).find(b=>b.innerText.includes('Trilha chat teste')).click()")
    await waitFor("!!document.querySelector('#cc-texto') && !document.querySelector('#cc-texto').disabled")
    for(const width of [390,768,1440]){
        await cdp('Emulation.setDeviceMetricsOverride',{width,height:1000,deviceScaleFactor:1,mobile:width<600})
        await pause(250)
        const medidas=await evaluate("({width:innerWidth,scroll:document.documentElement.scrollWidth,menu:!!document.querySelector('.topo-logado'),textarea:!!document.querySelector('#cc-texto')})")
        assert.ok(medidas.scroll<=medidas.width,'Overflow '+width);assert.ok(medidas.menu&&medidas.textarea)
        const captura=await cdp('Page.captureScreenshot',{format:'png',captureBeyondViewport:true})
        await writeFile(join(tmpdir(),'cyberchat-'+width+'.png'),Buffer.from(captura.data,'base64'))
        console.log('PASS CyberChat '+width+'px: layout real, sem overflow')
    }
    const texto='Mensagem de validação '+randomUUID()
    await fill('#cc-texto',texto)
    await evaluate("document.querySelector('.cc-compositor').requestSubmit()")
    await waitFor("Array.from(document.querySelectorAll('.cc-mensagem p')).some(e=>e.innerText==="+JSON.stringify(texto)+")")
    assert.equal(await evaluate("document.querySelector('#cc-texto').value"),'')
    const token=await evaluate("sessionStorage.getItem('token')")
    const conversa=await evaluate("new URLSearchParams(location.search).get('conversa')")
    const textoRetry='Retry sem duplicar '+randomUUID()
    perderResposta=true
    await fill('#cc-texto',textoRetry);await evaluate("document.querySelector('.cc-compositor').requestSubmit()")
    await waitFor("!!document.querySelector('.cc-compositor [role=alert]')")
    assert.ok(consumiuResposta);assert.equal(await evaluate("document.querySelector('#cc-texto').value"),textoRetry)
    await evaluate("document.querySelector('.cc-compositor').requestSubmit()")
    await waitFor("document.querySelector('#cc-texto').value===''")
    const historico=await fetch(upstream+'/api/chat/conversas/'+conversa+'/mensagens',{headers:{Authorization:'Bearer '+token}}).then(r=>r.json())
    assert.equal(historico.itens.filter(m=>m.texto===textoRetry).length,1)
    console.log('PASS envio real, resposta perdida e retry idempotente; texto preservado')
    await cdp('Page.reload');await pause(700);await waitFor("!!document.querySelector('#cc-texto') && document.querySelectorAll('.cc-mensagem').length>0")
    await waitFor("document.querySelector('.cc-historico')?.innerText.includes("+JSON.stringify(texto)+")")
    const antesPolling=contagemPolling;await pause(5600);assert.ok(contagemPolling>antesPolling)
    console.log('PASS F5, persistência e polling de aproximadamente cinco segundos')
    await click('.topo-logado__avatar');await waitFor("!!document.querySelector('.mp')")
    assert.ok(await evaluate("!!document.querySelector('.mp a[href=\"/chat\"]')"))
    await evaluate("document.querySelector('.mp-fechar').click()")
    await fill('#cc-busca','Nenhuma conversa '+randomUUID())
    await waitFor("document.body.innerText.includes('Nenhuma conversa encontrada')")
    await fill('#cc-busca','');await waitFor("!!document.querySelector('.cc-item')")
    falhaLista=true;await pause(5500)
    await waitFor("!!document.querySelector('.cc-lista [role=alert]')")
    assert.equal(await evaluate("sessionStorage.getItem('token')"),token)
    falhaLista=false
    await evaluate("document.querySelector('.cc-lista [role=alert] button').click()")
    await waitFor("!!document.querySelector('.cc-item') && !document.querySelector('.cc-lista [role=alert]')")
    await sair()
    await login(aluna);await waitFor("!!document.querySelector('.cc-item')")
    console.log('PASS pesquisa, erro de conexão sem apagar sessão e logout/login')
    await sair()
    for(const perfil of ['EDUCADORA','EMPRESA']){
        const conta=contas.find(c=>c.perfil===perfil);assert.ok(conta)
        await login(conta)
        assert.equal(await evaluate("document.querySelector('#cc-texto')?.value || ''"),'')
        await click('.topo-logado__avatar');await waitFor("!!document.querySelector('.mp')")
        assert.ok(await evaluate("!!document.querySelector('.mp a[href=\"/chat\"]')"))
        if(perfil==='EDUCADORA')assert.ok(await evaluate("!!document.querySelector('.mp.me')"))
        await evaluate("document.querySelector('.mp-fechar').click()")
        await click('.cc-nova');await waitFor("!!(document.querySelector('.cc-contextos ul button') || document.querySelector('.cc-contextos .cc-estado'))")
        const contexto=await evaluate("document.querySelector('.cc-contextos ul button')?.innerText")
        assert.ok(contexto,'Vínculo empresarial/educacional deveria existir')
        await click('.cc-contextos ul button');await waitFor("!!document.querySelector('#cc-texto')")
        await sair()
        console.log('PASS login, menu, vínculo real e troca de conta: '+perfil)
    }

    const semVinculo=sql("SELECT row_to_json(x) FROM (SELECT u.email,u.tipo perfil FROM talk_usuaria u WHERE u.nome='Chat fixture CYBERLADIE' AND u.email LIKE 'chat-fixture-%@example.invalid' AND NOT EXISTS(SELECT 1 FROM chat_contexto c WHERE u.id IN(c.aluna_id,c.responsavel_id)) ORDER BY u.id DESC LIMIT 1) x")
    assert.ok(semVinculo)
    await login(semVinculo)
    assert.equal(await evaluate("document.querySelectorAll('.cc-item').length"),0)
    await click('.cc-nova');await waitFor("!!document.querySelector('.cc-contextos .cc-estado')")
    assert.ok(await evaluate("document.querySelector('.cc-contextos').innerText.includes('Nenhum vínculo disponível')"))
    await sair()
    const pendente=sql("SELECT row_to_json(x) FROM (SELECT u.email,u.tipo perfil FROM talk_usuaria u JOIN auth_educadora e ON e.usuaria_id=u.id WHERE u.nome='Chat fixture EDUCADORA' AND u.email LIKE 'chat-fixture-%@example.invalid' AND e.status='PENDENTE' ORDER BY u.id DESC LIMIT 1) x")
    assert.ok(pendente)
    await login(pendente)
    assert.ok(await evaluate("document.body.innerText.includes('após a aprovação')"))
    assert.equal(await evaluate("document.querySelectorAll('.cc-item').length"),0)
    await sair()
    console.log('PASS estados vazios reais, ausência de vínculos e Educadora pendente sem dados privados')

    await login(aluna)
    const expirado=await evaluate("sessionStorage.getItem('token')")
    await fetch(upstream+'/api/auth/sessao',{method:'DELETE',headers:{Authorization:'Bearer '+expirado}})
    await cdp('Page.reload');await pause(700);await waitFor("location.pathname==='/login' && !sessionStorage.getItem('token')")
    assert.deepEqual(errors,[])
    console.log('PASS sessão revogada, três perfis e ausência de erros JavaScript')
    console.log('As escritas deste teste foram encaminhadas exclusivamente à cópia cyberladies_auth_chat_935de7fe96. Nenhuma fixture no banco principal ou no isolado de uso manual.')
} finally {
    ws?.close();child.kill()
}
