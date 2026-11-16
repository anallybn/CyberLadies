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
    '--remote-debugging-port=9344', '--user-data-dir=' + profile, 'about:blank'],
{ windowsHide: true, stdio: 'ignore' })
const pause = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
let ws
try {
    let pages
    for (let i = 0; i < 100; i++) {
        try { pages = await fetch('http://127.0.0.1:9344/json/list').then((r) => r.json()); break }
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

    const tipos=[['usuaria','CYBERLADIE','/comunidade'],['educadora','EDUCADORA','/educadora/dashboard'],['empresa','EMPRESA','/vagas']]
    let tokenEducadora, tokenRevogadoEducadora
    for(const [perfil,tipo,destinoLogin] of tipos) {
        const email='nav-perfis-'+randomUUID()+'@example.invalid',senha=randomUUID()+'Aa1!'
        const cadastro={nome:'Validação '+tipo,email,senha}
        if(perfil==='educadora') Object.assign(cadastro,{especialidade:'Tecnologia',instituicao:'Ambiente isolado'})
        if(perfil==='empresa') Object.assign(cadastro,{cnpj:cnpjTeste(),areaAtuacao:'Tecnologia'})
        if(perfil==='usuaria') cadastro.competencias='Tecnologia'
        const r=await fetch(base+'/api/auth/cadastro/'+perfil,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(cadastro)})
        assert.equal(r.status,201)
        await cdp('Page.navigate',{url:base+'/login?perfil='+(perfil==='educadora'?'empresa':'educadora')})
        await fill('#email',email); await fill('#senha',senha)
        await evaluate("document.querySelector('.login__form').requestSubmit()")
        await waitFor("location.pathname==="+JSON.stringify(destinoLogin)+" && !!document.querySelector('.topo-logado')")
        if(tipo==='EDUCADORA') tokenRevogadoEducadora=await evaluate("sessionStorage.getItem('token')")
        const paginas=tipo==='EDUCADORA'?['/educadora/dashboard','/educadora/enviar-material','/educadora/meus-materiais','/educadora/minhas-alunas','/academy','/comunidade','/vagas']:['/comunidade','/academy','/vagas']
        for(const pagina of paginas) {
            await evaluate("sessionStorage.setItem('perfil',"+JSON.stringify(tipo==='EDUCADORA'?'EMPRESA':'EDUCADORA')+")")
            await cdp('Page.navigate',{url:base+pagina})
            await waitFor("!!document.querySelector('.topo-logado__avatar')")
            await click('.topo-logado__avatar')
            await waitFor("!!document.querySelector('.mp-tag')")
            assert.equal(await evaluate("document.querySelector('.mp-tag').textContent.trim()"),tipo==='CYBERLADIE'?'USUÁRIA':tipo)
            assert.equal(await evaluate("!!document.querySelector('.me')"),tipo==='EDUCADORA')
            if(tipo==='EDUCADORA') {
                assert.equal(await evaluate("document.querySelectorAll('.me button:disabled').length"),2)
                assert.ok(await evaluate("!!document.querySelector('.me a[href=\"/educadora/dashboard\"]') && !!document.querySelector('.me a[href=\"/educadora/enviar-material\"]')"))
                for(const width of [390,768,1440]) {
                    await cdp('Emulation.setDeviceMetricsOverride',{width,height:900,deviceScaleFactor:1,mobile:width<600})
                    assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'),'Overflow '+pagina+' em '+width)
                    assert.ok(await evaluate("(()=>{const r=document.querySelector('.me').getBoundingClientRect();return r.top>=0&&r.bottom<=innerHeight})()"))
                }
            } else {
                assert.equal(await evaluate("!!document.querySelector('.mp a[href=\"/perfil\"]')"),tipo==='CYBERLADIE')
            }
            await click('.mp-fechar')
            await cdp('Page.reload'); await pause(350)
            await click('.topo-logado__avatar'); await waitFor("!!document.querySelector('.mp-tag')")
            assert.equal(await evaluate("document.querySelector('.mp-tag').textContent.trim()"),tipo==='CYBERLADIE'?'USUÁRIA':tipo)
            await click('.mp-fechar')
            if(tipo==='EDUCADORA' && pagina==='/comunidade') {
                await waitFor("!!document.querySelector('.cm-compositor') || !!document.querySelector('[placeholder=\"Compartilhe algo com a comunidade...\"]')")
                const status=await evaluate("(async()=>{const h={Authorization:'Bearer '+sessionStorage.getItem('token'),'Content-Type':'application/json'};const r=await fetch('/api/posts',{method:'POST',headers:h,body:JSON.stringify({texto:'Verificação de navegação da Educadora — ambiente isolado'})});const p=await r.json();if(r.status!==201)return {criar:r.status};return {criar:r.status,remover:(await fetch('/api/posts/'+p.id,{method:'DELETE',headers:h})).status}})()")
                assert.deepEqual(status,{criar:201,remover:204})
            }
        }
        const permissoes=await evaluate("(async()=>{const headers={Authorization:'Bearer '+sessionStorage.getItem('token'),'Content-Type':'application/json'};return {candidaturas:(await fetch('/api/candidaturas',{headers})).status,candidatar:(await fetch('/api/candidaturas',{method:'POST',headers,body:JSON.stringify({vagaId:1})})).status,publicar:(await fetch('/api/vagas',{method:'POST',headers,body:'{}'})).status}})()")
        assert.equal(permissoes.candidaturas,tipo==='CYBERLADIE'?200:403)
        if(tipo!=='CYBERLADIE') assert.equal(permissoes.candidatar,403)
        assert.equal(permissoes.publicar,403)
        if(tipo==='CYBERLADIE') assert.ok(permissoes.candidatar!==403) // Vaga ausente pode retornar 404.
        await fill('#cv-busca','consulta de validação')
        await pause(400)
        if(tipo!=='CYBERLADIE') assert.ok(await evaluate("Array.from(document.querySelectorAll('.cv-cartao .cv-botao')).every(e=>e.disabled)"))
        if(tipo==='EDUCADORA') {
            const sessao=await fetch(base+'/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email,senha})})
            assert.equal(sessao.status,200)
            tokenEducadora=(await sessao.json()).token
        }        await click('.topo-logado__avatar'); await waitFor("!!document.querySelector('.mp-tag')")
        await evaluate("Array.from(document.querySelectorAll('.mp button')).find(b=>b.textContent.includes('Sair')).click()")
        await waitFor("location.pathname==='/' && !sessionStorage.getItem('token')")
        console.log('PASS '+tipo+': login por perfil real, páginas, menu, F5, cache adulterado, navegação, logout e permissões')
    }

    // Testes controlados de transporte somente no endpoint de consulta da conta.
    let modo='lento',esperando=[]
    ws.addEventListener('message',(evento)=>{
        const mensagem=JSON.parse(evento.data)
        if(mensagem.method!=='Fetch.requestPaused') return
        const request=mensagem.params.requestId
        const executar=async()=>{
            if(modo==='lento'){esperando.push(request);return}
            try {
                if(modo==='offline') await cdp('Fetch.failRequest',{requestId:request,errorReason:'ConnectionFailed'})
                else if(modo==='erro') await cdp('Fetch.fulfillRequest',{requestId:request,responseCode:503,responseHeaders:[{name:'Content-Type',value:'application/json'}],body:Buffer.from('{}').toString('base64')})
                else if(modo==='expirada') await cdp('Fetch.fulfillRequest',{requestId:request,responseCode:401,responseHeaders:[{name:'Content-Type',value:'application/json'}],body:Buffer.from('{}').toString('base64')})
                else await cdp('Fetch.continueRequest',{requestId:request})
            } catch(error) { if(!error.message.includes('Invalid InterceptionId')) throw error }
        }
        executar()
    })
    await cdp('Page.navigate',{url:base+'/vagas'})
    await pause(300)
    await evaluate("sessionStorage.setItem('token',"+JSON.stringify(tokenEducadora)+");sessionStorage.setItem('perfil','EMPRESA')")
    await cdp('Fetch.enable',{patterns:[{urlPattern:'*/api/auth/conta',requestStage:'Request'}]})
    await cdp('Page.reload'); await pause(400)
    await click('.topo-logado__avatar')
    await waitFor("document.querySelector('.mp [role=status]')?.textContent.includes('Confirmando')")
    assert.equal(await evaluate("!!document.querySelector('.mp-tag') || !!document.querySelector('.me')"),false)
    assert.equal(await evaluate("sessionStorage.getItem('token')"),tokenEducadora)
    modo='erro'
    for(const requestId of esperando.splice(0)) await cdp('Fetch.fulfillRequest',{requestId,responseCode:503,responseHeaders:[{name:'Content-Type',value:'application/json'}],body:Buffer.from('{}').toString('base64')}).catch(()=>{})
    await waitFor("!!document.querySelector('.mp [role=alert]')")
    assert.equal(await evaluate("sessionStorage.getItem('token')"),tokenEducadora,'Falha transitória apagou sessão')
    assert.equal(await evaluate("!!document.querySelector('.mp-tag')"),false)
    modo='normal'
    await evaluate("Array.from(document.querySelectorAll('.mp button')).find(b=>b.textContent==='Tentar novamente').click()")
    await waitFor("!!document.querySelector('.me .mp-tag')")
    assert.equal(await evaluate("document.querySelector('.mp-tag').textContent.trim()"),'EDUCADORA')
    await click('.mp-fechar')
    modo='offline'
    await cdp('Page.reload'); await pause(400)
    await click('.topo-logado__avatar')
    await waitFor("!!document.querySelector('.mp [role=alert]')")
    assert.equal(await evaluate("sessionStorage.getItem('token')"),tokenEducadora,'Falha de conexão apagou sessão')
    modo='normal'
    await evaluate("Array.from(document.querySelectorAll('.mp button')).find(b=>b.textContent==='Tentar novamente').click()")
    await waitFor("!!document.querySelector('.me .mp-tag')")
    await click('.mp-fechar')
    modo='expirada'
    await cdp('Page.reload'); await pause(400)
    await click('.topo-logado__avatar')
    await waitFor("!!document.querySelector('.mp a[href=\"/login\"]')")
    assert.equal(await evaluate("sessionStorage.getItem('token')"),null)
    assert.ok(await evaluate("document.querySelector('.mp').textContent.includes('expirou')"))
    await cdp('Fetch.disable')
    await evaluate("sessionStorage.setItem('token',"+JSON.stringify(tokenRevogadoEducadora)+")")
    await cdp('Page.reload'); await pause(400)
    await click('.topo-logado__avatar')
    await waitFor("!!document.querySelector('.mp a[href=\"/login\"]')")
    assert.equal(await evaluate("sessionStorage.getItem('token')"),null,'Sessão revogada não foi reconhecida')
    assert.deepEqual(errors,[],'Erros JavaScript no Console')
    console.log('PASS carregamento neutro, falha 503 sem apagar sessão, recuperação pelo backend e 401 com expiração correta')
} finally { ws?.close(); child.kill() }
