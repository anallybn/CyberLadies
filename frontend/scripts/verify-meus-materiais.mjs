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
    '--remote-debugging-port=9345', '--user-data-dir=' + profile, 'about:blank'],
{ windowsHide: true, stdio: 'ignore' })
const pause = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
let ws
try {
    let pages
    for (let i = 0; i < 100; i++) {
        try { pages = await fetch('http://127.0.0.1:9345/json/list').then((r) => r.json()); break }
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



    // Somente leitura. Respostas de conta/autoria controladas no navegador para testar
    // componentes e isolamento de estado; o servidor não recebe sessão fictícia válida.
    const banco=destino.stdout.trim().split('/').at(-1)
    const registros=spawnSync('docker',['exec','cyberladies-banco-1','psql','-U','cyberladies','-d',banco,'-Atc',
        "SELECT coalesce(json_agg(x),'[]'::json) FROM (SELECT u.id,u.nome,u.email,u.tipo perfil,e.status AS \"statusEducadora\" FROM talk_usuaria u LEFT JOIN auth_educadora e ON e.usuaria_id=u.id ORDER BY u.id DESC) x"],{windowsHide:true,encoding:'utf8'})
    assert.equal(registros.status,0,registros.stderr)
    const contas=JSON.parse(registros.stdout)
    const educadoras=contas.filter(c=>c.perfil==='EDUCADORA')
    assert.ok(educadoras.length>=2)
    let conta=educadoras[0]
    const catalogo=await fetch(base+'/api/academy/catalogo').then(r=>r.json())
    const publicas=catalogo.trilhas
    assert.ok(publicas.length>=2,'Teste de grade requer duas trilhas públicas existentes no isolado')
    const aulasPorTrilha=new Map()
    for(const trilha of publicas.slice(0,2)) aulasPorTrilha.set(trilha.id,await fetch(base+'/api/academy/trilhas/'+trilha.id+'/aulas').then(r=>r.json()))
    const colecao=[...aulasPorTrilha.entries()].flatMap(([id,aulas])=>aulas.map(a=>({...a,trilha:publicas.find(t=>t.id===id).titulo})))
    let modo='vazio',esperando=[]
    function respostaDashboard() {
        const trilhas=modo==='vazio'?[]:publicas.slice(0,2).map(t=>({id:t.id,titulo:t.titulo,inscricoes:0}))
        return {nome:conta.nome,statusEducadora:conta.statusEducadora,podePublicar:false,trilhas,
            indicadores:{materiaisPublicados:modo==='vazio'?0:colecao.length},materiais:[],mentorias:[]}
    }
    const escritas=[]
    ws.addEventListener('message',async(evento)=>{
        const m=JSON.parse(evento.data)
        if(m.method==='Network.requestWillBeSent' && m.params.request.url.includes('/api/') && !['GET','HEAD','OPTIONS'].includes(m.params.request.method)) escritas.push(m.params.request.method)
        if(m.method!=='Fetch.requestPaused') return
        const id=m.params.requestId
        try {
            if(m.params.request.url.includes('/materiais/rascunhos')) {
                await cdp('Fetch.fulfillRequest',{requestId:id,responseCode:200,responseHeaders:[{name:'Content-Type',value:'application/json'}],body:Buffer.from('[]').toString('base64')})
            } else if(m.params.request.url.includes('/auth/conta')) {
                await cdp('Fetch.fulfillRequest',{requestId:id,responseCode:200,responseHeaders:[{name:'Content-Type',value:'application/json'}],body:Buffer.from(JSON.stringify(conta)).toString('base64')})
            } else if(modo==='lento') esperando.push(id)
            else if(modo==='erro') await cdp('Fetch.fulfillRequest',{requestId:id,responseCode:503,responseHeaders:[{name:'Content-Type',value:'application/json'}],body:Buffer.from('{}').toString('base64')})
            else if(modo==='offline') await cdp('Fetch.failRequest',{requestId:id,errorReason:'ConnectionFailed'})
            else await cdp('Fetch.fulfillRequest',{requestId:id,responseCode:200,responseHeaders:[{name:'Content-Type',value:'application/json'}],body:Buffer.from(JSON.stringify(respostaDashboard())).toString('base64')})
        } catch(error) { if(!error.message.includes('Invalid InterceptionId')) errors.push(error.message) }
    })
    await cdp('Page.addScriptToEvaluateOnNewDocument',{source:"sessionStorage.setItem('token','teste-visual-invalido-no-servidor');sessionStorage.setItem('perfil','EMPRESA')"})
    await cdp('Fetch.enable',{patterns:[{urlPattern:'*/api/educadora/materiais/rascunhos',requestStage:'Request'},{urlPattern:'*/api/auth/conta',requestStage:'Request'},{urlPattern:'*/api/educadora/dashboard',requestStage:'Request'}]})
    const url=base+'/educadora/meus-materiais'
    async function abrir() { await cdp('Page.navigate',{url}); await pause(350); await waitFor("!!document.querySelector('.mm-apresentacao')") }
    await abrir()
    await waitFor("document.querySelector('.mm-estado h2')?.textContent.includes('conhecimento')")
    assert.equal(await evaluate("Array.from(document.querySelectorAll('.mm-indicadores dd')).map(e=>e.textContent).join('|')"),'0|0|0')
    assert.equal(await evaluate("document.querySelectorAll('.mm-material,.mm-pagina img:not(.topo-logado__logo),.mm-pagina iframe').length"),0)
    for(const status of ['Arquivados']) {
        await evaluate("Array.from(document.querySelectorAll('.mm-status button')).find(b=>b.textContent==="+JSON.stringify(status)+").click()")
        assert.ok(await evaluate("document.querySelector('.mm-estado').textContent.includes('ainda não')"))
    }
    await evaluate("document.querySelector('.mm-status button').click()")
    await fill('#mm-busca','busca sem correspondência')
    assert.ok(await evaluate("document.querySelector('.mm-estado h2').textContent.includes('Nenhum material')"))
    modo='lista'
    await cdp('Page.reload'); await pause(350)
    await waitFor("document.querySelectorAll('.mm-material').length==="+colecao.length)
    assert.equal(await evaluate("document.querySelectorAll('#mm-trilha option').length"),3)
    assert.equal(await evaluate("document.querySelectorAll('.mm-material button').length"),0)
    const termo=colecao[0].titulo
    await fill('#mm-busca',termo)
    assert.equal(await evaluate("document.querySelectorAll('.mm-material').length"),colecao.filter(a=>a.titulo.toLowerCase().includes(termo.toLowerCase())).length)
    await fill('#mm-busca','')
    const trilhaTeste=publicas[0].id
    await evaluate("document.querySelector('#mm-trilha').value="+JSON.stringify(String(trilhaTeste))+";document.querySelector('#mm-trilha').dispatchEvent(new Event('change',{bubbles:true}))")
    assert.equal(await evaluate("document.querySelectorAll('.mm-material').length"),aulasPorTrilha.get(trilhaTeste).length)
    await evaluate("document.querySelector('#mm-trilha').value='';document.querySelector('#mm-trilha').dispatchEvent(new Event('change',{bubbles:true}))")
    for(const width of [390,768,1440]) {
        await cdp('Emulation.setDeviceMetricsOverride',{width,height:1000,deviceScaleFactor:1,mobile:width<600})
        await evaluate("document.fonts.ready.then(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))))")
        await pause(500)
        assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'),'Overflow em '+width)
        const colunas=await evaluate("getComputedStyle(document.querySelector('.mm-grade')).gridTemplateColumns.split(' ').length")
        assert.equal(colunas,width===1440?3:width===768?2:1)
        const shot=await cdp('Page.captureScreenshot',{format:'png',captureBeyondViewport:true})
        await writeFile(join(tmpdir(),'meus-materiais-'+width+'.png'),Buffer.from(shot.data,'base64'))
        await click('.topo-logado__avatar');await waitFor("!!document.querySelector('.me')")
        assert.equal(await evaluate("document.querySelector('.me .mp-item--ativo').getAttribute('href')"),'/educadora/meus-materiais')
        assert.equal(await evaluate("document.querySelectorAll('.me button:disabled').length"),1)
        await click('.mp-fechar')
        console.log('PASS Meus Materiais '+width+'px: grade, filtros, fontes, menu e sem overflow')
    }
    // Erros e lista antiga não devem aparecer como sucesso.
    modo='erro'; await cdp('Page.reload');await pause(350)
    await waitFor("!!document.querySelector('.mm-estado[role=alert]')")
    assert.equal(await evaluate("document.querySelectorAll('.mm-material').length"),0)
    assert.equal(await evaluate("document.querySelector('.mm-indicadores dd').textContent"),'—')
    modo='lista'; await evaluate("document.querySelector('.mm-estado button').click()")
    await waitFor("document.querySelectorAll('.mm-material').length==="+colecao.length)
    modo='offline'; await cdp('Page.reload');await pause(350)
    await waitFor("!!document.querySelector('.mm-estado[role=alert]')")
    assert.ok(await evaluate("!!sessionStorage.getItem('token')"))
    modo='lento'; await cdp('Page.reload');await pause(350)
    await waitFor("document.querySelector('.mm-estado[role=status]')?.getAttribute('aria-busy')==='true'")
    modo='vazio'
    for(const requestId of esperando.splice(0)) await cdp('Fetch.fulfillRequest',{requestId,responseCode:200,responseHeaders:[{name:'Content-Type',value:'application/json'}],body:Buffer.from(JSON.stringify(respostaDashboard())).toString('base64')}).catch(()=>{})
    await waitFor("document.querySelector('.mm-estado h2')?.textContent.includes('conhecimento')")
    // Troca de Educadora descarta a coleção anterior.
    conta=educadoras[1]; modo='vazio'; await cdp('Page.reload');await pause(350)
    await waitFor("document.querySelector('.mm-estado h2')?.textContent.includes('conhecimento')")
    assert.equal(await evaluate("document.querySelectorAll('.mm-material').length"),0)
    for(const perfil of ['CYBERLADIE','EMPRESA']) {
        conta=contas.find(c=>c.perfil===perfil)
        assert.ok(conta)
        await cdp('Page.reload');await pause(350)
        await waitFor("document.querySelector('.mm-estado h2')?.textContent.includes('exclusivo')")
        await click('.topo-logado__avatar');await waitFor("!!document.querySelector('.mp-tag')")
        assert.equal(await evaluate("!!document.querySelector('.me')"),false)
        await click('.mp-fechar')
    }
    conta=educadoras[0]; await cdp('Page.reload');await pause(350)
    await waitFor("!!document.querySelector('.mm-acoes a')")
    await click('.mm-acoes a')
    await waitFor("location.pathname==='/educadora/enviar-material' && !!document.querySelector('#em-titulo')")
    assert.deepEqual(escritas,[],'Teste de leitura enviou escrita')
    await cdp('Fetch.disable')
    const negado=await fetch(base+'/api/educadora/dashboard?educadoraId='+educadoras[0].id,{headers:{Authorization:'Bearer teste-invalido'}})
    assert.equal(negado.status,401,'Servidor aceitou ID sem sessão')
    assert.deepEqual(errors,[])
    console.log('PASS estados vazios, pesquisa, trilhas, filtros, 503, desconexão, loading, troca de contas, três perfis, navegação e nenhuma escrita')
    console.log('Dados da grade: aulas públicas existentes; vínculo controlado somente neste navegador, sem alterar autoria no PostgreSQL.')
} finally { ws?.close(); child.kill() }
