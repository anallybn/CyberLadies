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
    '--remote-debugging-port=9341', '--user-data-dir=' + profile, 'about:blank'],
{ windowsHide: true, stdio: 'ignore' })
const pause = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
let ws
try {
    let pages
    for (let i = 0; i < 100; i++) {
        try { pages = await fetch('http://127.0.0.1:9341/json/list').then((r) => r.json()); break }
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
    for(const [perfil,rotulo,tipo,destinoLogin] of [
        ['usuaria','CyberLadie','CYBERLADIE','/perfil'],
        ['educadora','Educadora','EDUCADORA','/educadora/dashboard'],
        ['empresa','Empresa','EMPRESA','/vagas'],
    ]) {
        const email='auth-browser-'+randomUUID()+'@example.invalid',senha=randomUUID()+'Aa1!'
        contas.push({email,senha,tipo})
        await cdp('Page.navigate',{url:base+'/'})
        await click('.home__botao')
        await waitFor("document.querySelectorAll('.perfil__card').length===3")
        await evaluate("Array.from(document.querySelectorAll('.perfil__card')).find(b=>b.innerText.includes("+JSON.stringify(rotulo)+")).click()")
        await waitFor("location.pathname==='/login' && !!document.querySelector('.login__cadastrar')")
        await click('.login__cadastrar')
        await waitFor("location.pathname==='/cadastro/"+perfil+"'")
        await fill('#nome','Fixture '+rotulo)
        await fill('#email',email)
        await fill('#senha',senha)
        await fill('#confirmarSenha',senha)
        if(perfil==='educadora') { await fill('#especialidade','Java'); await fill('#instituicao','Instituição de teste') }
        if(perfil==='empresa') { await fill('#cnpj',cnpjTeste()); await fill('#areaAtuacao','Tecnologia') }
        for(const width of [390,768,1440]) {
            await cdp('Emulation.setDeviceMetricsOverride',{width,height:1000,deviceScaleFactor:1,mobile:width<600})
            assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'),'Overflow no cadastro '+perfil+' em '+width)
        }
        await evaluate("document.querySelector('.cadastro__form').requestSubmit()")
        await waitFor("location.pathname==='/cadastro-concluido'")
        // O perfil da URL é deliberadamente contrário à conta que entrará.
        await cdp('Page.navigate',{url:base+'/login?perfil='+(perfil==='empresa'?'usuaria':'empresa')+'&retorno=%2Fperfil'})
        await fill('#email',email); await fill('#senha',senha)
        await evaluate("document.querySelector('.login__form').requestSubmit()")
        await waitFor("location.pathname==="+JSON.stringify(destinoLogin)+" && !!document.querySelector('.topo-logado')")
        const atual=await minhaConta()
        assert.equal(atual.status,200); assert.equal(atual.conta.perfil,tipo)
        assert.equal(await evaluate("sessionStorage.getItem('perfil')"),tipo)
        if(tipo==='EDUCADORA') assert.equal(atual.conta.statusEducadora,'PENDENTE')
        await click('.topo-logado__avatar')
        await waitFor("!!document.querySelector('.mp-tag')")
        assert.ok(await evaluate("document.querySelector('.mp-tag').innerText.includes("+JSON.stringify(tipo==='CYBERLADIE'?'USUÁRIA':tipo)+")"))
        await click('.mp-fechar')
        if(tipo!=='CYBERLADIE') {
            await evaluate("sessionStorage.setItem('perfil','CYBERLADIE');sessionStorage.setItem('statusEducadora','APROVADA')")
            const negado=await evaluate("(async()=>{const headers={Authorization:'Bearer '+sessionStorage.getItem('token'),'Content-Type':'application/json'};const r=await fetch('/api/educadora/publicacoes',{method:'POST',headers});return r.status})()")
            assert.equal(negado,403)
            assert.equal((await minhaConta()).conta.perfil,tipo)
            await evaluate("sessionStorage.setItem('perfil',"+JSON.stringify(tipo)+")")
        }
        console.log('PASS '+tipo+': popup e formulário existentes, cadastro persistente, login por perfil real, layout 390/768/1440 e privilégios protegidos')
        if(perfil!=='empresa') {
            await click('.topo-logado__avatar')
            await evaluate("Array.from(document.querySelectorAll('.mp button')).find(b=>b.innerText.includes('Sair')).click()")
            await waitFor("location.pathname==='/' && !sessionStorage.getItem('token') && !sessionStorage.getItem('perfil')")
            console.log('PASS logout '+tipo)
        }
    }
    const antes=(await minhaConta()).conta
    const reinicio=spawnSync('docker',['restart',api],{windowsHide:true,encoding:'utf8'})
    assert.equal(reinicio.status,0)
    let pronta=false
    for(let i=0;i<360;i++) {
        try { const r=await fetch(base+'/api/academy/catalogo'); if(r.ok){pronta=true;break} } catch {}
        await pause(500)
    }
    assert.ok(pronta,'API temporária não ficou pronta dentro de 180 segundos')
    await cdp('Page.reload'); await pause(400)
    await waitFor("!!document.querySelector('.topo-logado')")
    const depois=await minhaConta()
    assert.equal(depois.status,200); assert.equal(depois.conta.id,antes.id); assert.equal(depois.conta.perfil,'EMPRESA')
    for(const conta of contas) {
        const r=await fetch(base+'/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:conta.email,senha:conta.senha})})
        assert.equal(r.status,200); const dados=await r.json(); assert.equal(dados.perfil,conta.tipo)
        if(conta.tipo==='EDUCADORA') assert.equal(dados.statusEducadora,'PENDENTE')
    }
    assert.deepEqual(errors,[],'Erros JavaScript no Console')
    console.log('PASS reinicialização: três contas persistentes, tipos preservados e sessão anterior ao restart válida')
    console.log('PASS Console sem erros JavaScript. Fixtures somente no banco isolado.')
} finally {
    ws?.close()
    child.kill()
}