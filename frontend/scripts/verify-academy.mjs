import { spawn, spawnSync } from 'node:child_process'
import { randomUUID } from 'node:crypto'
import { mkdtemp, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import assert from 'node:assert/strict'

const base='http://localhost:5174'
const database=process.env.ACADEMY_TEST_DATABASE || 'cyberladies_academy_validacao'
assert.match(database,/^cyberladies_academy_[a-z0-9_]+$/)
function sql(command) {
    const result=spawnSync('docker',['compose','exec','-T','banco','psql','-qAt','-v','ON_ERROR_STOP=1','-U','cyberladies','-d',database,'-c',command],{windowsHide:true,encoding:'utf8'})
    assert.equal(result.status,0,result.stderr)
    return result.stdout.trim()
}
assert.equal(sql('SELECT current_database()'),database)
const marcador='academy-browser-'+randomUUID()
const email=marcador+'@example.invalid'
const senha=randomUUID()+'Aa1!'
const trilha=Number(sql("INSERT INTO academy_trilha(titulo,descricao,categoria,nivel,elegivel_certificado) VALUES ('"+marcador+"','Fixture exclusiva de teste','FRONTEND','INICIANTE',true) RETURNING id"))
const aula1=Number(sql("INSERT INTO academy_aula(trilha_id,titulo,descricao,ordem,duracao_minutos,video_provedor,video_id,uso_autorizado) VALUES ("+trilha+",'Aula E2E 1','Fixture de teste',1,10,'YOUTUBE','M7lc1UVf-VE',true) RETURNING id"))
const aula2=Number(sql("INSERT INTO academy_aula(trilha_id,titulo,descricao,ordem,duracao_minutos,video_provedor,video_id,uso_autorizado) VALUES ("+trilha+",'Aula E2E 2','Fixture de teste',2,20,'YOUTUBE','M7lc1UVf-VE',true) RETURNING id"))
sql("UPDATE academy_aula SET descricao='"+marcador+"' WHERE trilha_id="+trilha)
const mentora=Number(sql("INSERT INTO academy_mentora(nome,especialidade,descricao) VALUES ('"+marcador+"','Testes','Fixture exclusiva do banco isolado') RETURNING id"))
const slot=Number(sql("INSERT INTO academy_disponibilidade(mentora_id,inicio,fim) VALUES ("+mentora+",clock_timestamp()+INTERVAL '2 days',clock_timestamp()+INTERVAL '2 days 1 hour') RETURNING id"))
const consulta=await fetch(base+'/api/academy/trilhas/'+trilha).then(r=>r.json())
assert.equal(consulta.titulo,marcador,'O site de validação não aponta para o banco isolado esperado.')
const cadastro=await fetch(base+'/api/auth/cadastro/usuaria',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({nome:'Fixture navegador Academy',email,senha,competencias:''})})
assert.equal(cadastro.status,201)
const chrome = process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const profile = await mkdtemp(join(tmpdir(), 'academy-browser-'))
const child = spawn(chrome, ['--headless=new', '--disable-gpu', '--no-first-run',
    '--remote-debugging-port=9339', '--user-data-dir=' + profile, 'about:blank'],
{ windowsHide: true, stdio: 'ignore' })
const pause = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
let ws
try {
    let pages
    for (let i = 0; i < 100; i++) {
        try { pages = await fetch('http://127.0.0.1:9339/json/list').then((r) => r.json()); break }
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
        if(data.method==='Fetch.requestPaused') {
            const request=data.params.request
            const headers=Object.entries(request.headers).filter(([name])=>name.toLowerCase()!=='origin').map(([name,value])=>({name,value:String(value)}))
            if(['POST','PUT','DELETE','PATCH'].includes(request.method)) headers.push({name:'Origin',value:'http://localhost:5173'})
            cdp('Fetch.continueRequest',{requestId:data.params.requestId,headers}).catch(error=>errors.push(error.message))
        }
        if (data.method==='Network.requestWillBeSent' && data.params.request.url.endsWith('/api/auth/login')) {
            const body=JSON.parse(data.params.request.postData || '{}')
            console.log('Login diagnóstico: '+JSON.stringify({url:data.params.request.url,emailConfere:body.email===email,senhaConfere:body.senha===senha}))
        }
        if(data.method==='Network.responseReceived' && data.params.response.url.endsWith('/api/auth/login'))
            console.log('Login status: '+data.params.response.status)
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
        for(let i=0;i<150;i++) { if(await evaluate(expression)) return; await pause(100) }
        throw new Error('Tempo esgotado: '+expression+'; '+JSON.stringify(await evaluate('({path:location.pathname,text:document.body.innerText.slice(0,2000)})')))
    }
    async function fill(selector,value) {
        await evaluate("(()=>{const e=document.querySelector("+JSON.stringify(selector)+");if(!e)throw Error('Campo ausente');const p=e.tagName==='SELECT'?HTMLSelectElement.prototype:e.tagName==='TEXTAREA'?HTMLTextAreaElement.prototype:HTMLInputElement.prototype;Object.getOwnPropertyDescriptor(p,'value').set.call(e,"+JSON.stringify(value)+");e.dispatchEvent(new Event(e.tagName==='SELECT'?'change':'input',{bubbles:true}));})()")
    }
    async function click(selector) { await waitFor("!!document.querySelector("+JSON.stringify(selector)+")"); await evaluate("document.querySelector("+JSON.stringify(selector)+").click()") }
    await cdp('Network.enable')
    // A API local autoriza localhost:5173. Não amplia CORS de produção para a porta de testes.
    await cdp('Fetch.enable',{patterns:[{urlPattern:base+'/api/*',requestStage:'Request'}]})
    // Não reproduz conteúdo externo de teste: valida URL/iframe, sem afirmar reprodução real.
    await cdp('Network.setBlockedURLs',{urls:['*youtube*','*vimeo*']})
    await cdp('Page.navigate',{url:base+'/academy'})
    await waitFor("!!document.querySelector('#ac-trilhas') && !document.querySelector('.ac-estado')")
    await fill('#ac-busca',marcador)
    await waitFor("document.querySelectorAll('.ac-trilha').length===1")
    await evaluate("Array.from(document.querySelectorAll('.ac-categorias button')).find(b=>b.innerText==='Dados').click()")
    await waitFor("document.querySelector('#ac-trilhas')?.parentElement.innerText.includes('Nenhuma trilha')")
    await evaluate("Array.from(document.querySelectorAll('.ac-categorias button')).find(b=>b.innerText==='Front-end').click()")
    await waitFor("document.querySelectorAll('.ac-trilha').length===1")
    await fill('.ac-filtros select','INICIANTE')
    await click('.ac-trilha .ac-botao')
    await waitFor("location.pathname==='/login' && !!document.querySelector('#email')")
    await fill('#email',email); await fill('#senha',senha)
    await evaluate("document.querySelector('.login__form').requestSubmit()")
    await waitFor("location.pathname==='/academy/trilhas/"+trilha+"' && !!document.querySelector('.ac-cartao .ac-botao')")
    await click('.ac-cartao .ac-botao')
    await waitFor("!!document.querySelector('progress') && !!document.querySelector('a[href=\"/academy/aulas/"+aula1+"\"]')")
    await cdp('Page.reload'); await pause(400)
    await waitFor("!!document.querySelector('progress')")
    assert.equal(await evaluate("document.querySelector('progress').value"),0)
    console.log('PASS catálogo, filtros, login, inscrição e persistência inicial')
    await evaluate("Array.from(document.querySelectorAll('a')).find(a=>a.innerText==='Continuar estudos').click()")
    await waitFor("location.pathname==='/academy/aulas/"+aula1+"' && !!document.querySelector('h1')")
    await evaluate("Array.from(document.querySelectorAll('button')).find(b=>b.innerText==='Carregar vídeo').click()")
    assert.equal(await evaluate("document.querySelector('iframe').getAttribute('src')"),'https://www.youtube-nocookie.com/embed/M7lc1UVf-VE')
    await evaluate("Array.from(document.querySelectorAll('button')).find(b=>b.innerText==='Marcar como concluída').click()")
    await waitFor("document.querySelector('progress')?.value===50")
    await cdp('Page.reload'); await pause(400)
    await waitFor("document.querySelector('progress')?.value===50")
    await evaluate("Array.from(document.querySelectorAll('a')).find(a=>a.innerText.includes('Próxima aula')).click()")
    await waitFor("location.pathname==='/academy/aulas/"+aula2+"' && Array.from(document.querySelectorAll('button')).some(b=>b.innerText==='Marcar como concluída')")
    await evaluate("Array.from(document.querySelectorAll('button')).find(b=>b.innerText==='Marcar como concluída').click()")
    await waitFor("document.querySelector('progress')?.value===100")
    assert.equal(Number(sql("SELECT count(*) FROM academy_certificado WHERE trilha_id="+trilha)),1)
    await evaluate("Array.from(document.querySelectorAll('a')).find(a=>a.innerText==='Meus certificados').click()")
    await waitFor("!!document.querySelector('#pf-certificados-plataforma') && document.querySelector('#pf-certificados-plataforma').parentElement.innerText.includes("+JSON.stringify(marcador)+")")
    await evaluate("document.getElementById('pf-certificados-plataforma').parentElement.querySelector('a').click()")
    await waitFor("!!document.querySelector('.ac-certificado')")
    console.log('PASS progresso 0/50/100 após F5 e certificado único no Perfil')
    await cdp('Page.navigate',{url:base+'/academy/mentorias?mentora='+mentora})
    await waitFor("!!document.querySelector('.ac-agenda-form option[value=\""+slot+"\"]')")
    await fill('.ac-agenda-form label:nth-of-type(2) select',String(slot))
    await evaluate("document.querySelector('.ac-agenda-form').closest('form').requestSubmit()")
    await waitFor("document.querySelector('main').innerText.includes('Agendamento confirmado.')")
    await cdp('Page.reload'); await pause(400)
    await waitFor("Array.from(document.querySelectorAll('button')).some(b=>b.innerText==='Cancelar agendamento')")
    await evaluate("Array.from(document.querySelectorAll('button')).find(b=>b.innerText==='Cancelar agendamento').click()")
    await evaluate("Array.from(document.querySelectorAll('button')).find(b=>b.innerText==='Confirmar cancelamento').click()")
    await waitFor("document.querySelector('main').innerText.includes('Agendamento cancelado.')")
    console.log('PASS reserva confirmada, persistência após F5 e cancelamento')
    await cdp('Page.navigate',{url:base+'/academy'})
    await waitFor("!!document.querySelector('#ac-trilhas')")
    await fill('#ac-busca',marcador)
    await waitFor("document.querySelectorAll('.ac-trilha').length===1")
    for (const width of [390,768,1440]) {
        await cdp('Emulation.setDeviceMetricsOverride',{width,height:1000,deviceScaleFactor:1,mobile:width<600})
        const dimensoes=await evaluate("({width:innerWidth,content:document.documentElement.scrollWidth})")
        assert.ok(dimensoes.content<=dimensoes.width,'Overflow em '+width)
        console.log('PASS CyberAcademy preenchido em '+width+' px, sem overflow')
    }
    const captura=await cdp('Page.captureScreenshot',{format:'png',captureBeyondViewport:true})
    const screenshot=join(profile,'academy-desktop.png')
    await writeFile(screenshot,Buffer.from(captura.data,'base64'))
    console.log('Screenshot: '+screenshot)
    assert.deepEqual(errors,[],'Erros JavaScript no Console')
    console.log('PASS Console sem erros JavaScript; reprodução externa não testada')
    console.log('Fixtures identificadas como teste permanecem exclusivamente no banco '+database)
} finally {
    ws?.close()
    child.kill()
}