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
    '--remote-debugging-port=9342', '--user-data-dir=' + profile, 'about:blank'],
{ windowsHide: true, stdio: 'ignore' })
const pause = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
let ws
try {
    let pages
    for (let i = 0; i < 100; i++) {
        try { pages = await fetch('http://127.0.0.1:9342/json/list').then((r) => r.json()); break }
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
    await cdp('Network.enable')

    await cdp('Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false})

    const email='dashboard-browser-'+randomUUID()+'@example.invalid'
    const senha=randomUUID()+'Aa1!'
    const cadastro=await fetch(base+'/api/auth/cadastro/educadora',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({nome:'Educadora de teste',email,senha,especialidade:'Teste',instituicao:'Ambiente isolado'})})
    assert.equal(cadastro.status,201)
    await cdp('Page.navigate',{url:base+'/login?perfil=empresa'})
    await fill('#email',email); await fill('#senha',senha)
    await evaluate("document.querySelector('.login__form').requestSubmit()")
    await waitFor("location.pathname==='/educadora/dashboard' && !!document.querySelector('.ed-indicadores')")
    assert.equal(await evaluate("document.querySelectorAll('.ed-indicadores > div').length"),5)
    assert.equal(await evaluate("document.querySelector('.ed-saudacao h1').innerText"),'Bem-vinda, Educadora de teste!')
    assert.equal(await evaluate("Array.from(document.querySelectorAll('.ed-indicadores dd')).map(e=>e.innerText).join('|')"),'0|0|0|Em breve|Em breve')
    await cdp('Page.reload')
    await waitFor("!!document.querySelector('.ed-indicadores')")
    for(const width of [390,768,1440]) {
        await cdp('Emulation.setDeviceMetricsOverride',{width,height:1000,deviceScaleFactor:1,mobile:width<600})
        await evaluate("document.fonts.ready.then(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))))")
        await pause(700)
        assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'),'Overflow no dashboard em '+width)
        const screenshot=await cdp('Page.captureScreenshot',{format:'png',captureBeyondViewport:true})
        await writeFile(join(tmpdir(),'cyberladies-dashboard-'+width+'.png'),Buffer.from(screenshot.data,'base64'))
        console.log('PASS layout '+width+'px: sem transbordamento horizontal')
    }
    await click('.topo-logado__avatar')
    await waitFor("!!document.querySelector('.mp')")
    assert.equal(await evaluate("Array.from(document.querySelectorAll('.mp a')).filter(a=>a.getAttribute('href').startsWith('/educadora')).length"),4)
    assert.ok(await evaluate("!!document.querySelector('.mp a[href=\"/perfil\"]')"))
    await click('.mp-fechar')
    // Alterar o cache não modifica a identidade nem as permissões no servidor.
    await evaluate("sessionStorage.setItem('perfil','CYBERLADIE');sessionStorage.setItem('statusEducadora','APROVADA')")
    const seguranca=await evaluate("(async()=>{const headers={Authorization:'Bearer '+sessionStorage.getItem('token'),'Content-Type':'application/json'}; const resultado={}; for(const [nome,url,method,body] of [['publicar','/api/educadora/trilhas','POST','{}'],['candidatar','/api/candidaturas','POST',JSON.stringify({vagaId:1})],['estudar','/api/academy/me/progresso','GET'],['participar','/api/comunidade/adesao','PUT']]){resultado[nome]=(await fetch(url,{method,headers,body})).status}return resultado})()")
    assert.deepEqual(seguranca,{publicar:403,candidatar:403,estudar:200,participar:204})
    await evaluate("sessionStorage.setItem('perfil','EDUCADORA')")
    const publicado=await evaluate("(async()=>{const r=await fetch('/api/posts',{method:'POST',headers:{Authorization:'Bearer '+sessionStorage.getItem('token'),'Content-Type':'application/json'},body:JSON.stringify({texto:'Teste automatizado Educadora — ambiente isolado'})});return {status:r.status,post:await r.json()}})()")
    assert.equal(publicado.status,201)
    const removido=await evaluate("(async()=>{return (await fetch('/api/posts/"+publicado.post.id+"',{method:'DELETE',headers:{Authorization:'Bearer '+sessionStorage.getItem('token')}})).status})()")
    assert.equal(removido,204)
    for(const [url,selector] of [['/academy','.ac-hero'],['/comunidade','.topo-logado'],['/vagas','.topo-logado']]) {
        await cdp('Page.navigate',{url:base+url})
        await waitFor("!!document.querySelector("+JSON.stringify(selector)+")")
    }
    await cdp('Page.navigate',{url:base+'/educadora/dashboard'})
    await waitFor("!!document.querySelector('.ed-indicadores')")
    await click('.topo-logado__avatar')
    await evaluate("Array.from(document.querySelectorAll('.mp button')).find(b=>b.innerText.includes('Sair')).click()")
    await waitFor("location.pathname==='/' && !sessionStorage.getItem('token')")
    await cdp('Page.navigate',{url:base+'/educadora/dashboard'})
    await waitFor("location.pathname==='/login'")
    assert.deepEqual(errors,[],'Erros JavaScript no Console')
    console.log('PASS login por perfil real, estados vazios, menu, reload, estudo, Talk, consulta de vagas, bloqueio de publicação/candidatura, logout e rota privada')
    console.log('Capturas: '+join(tmpdir(),'cyberladies-dashboard-{390,768,1440}.png'))
} finally {
    ws?.close()
    child.kill()
}
