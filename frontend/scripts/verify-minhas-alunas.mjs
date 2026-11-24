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
    '--remote-debugging-port=9347', '--user-data-dir=' + profile, 'about:blank'],
{ windowsHide: true, stdio: 'ignore' })
const pause = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
let ws
try {
    let pages
    for (let i = 0; i < 100; i++) {
        try { pages = await fetch('http://127.0.0.1:9347/json/list').then((r) => r.json()); break }
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




    let pronta=false
    for(let i=0;i<360;i++){try{if((await fetch(base+'/api/academy/catalogo')).ok){pronta=true;break}}catch{}await pause(500)}
    assert.ok(pronta,'API isolada não iniciou')
    const banco=destino.stdout.trim().split('/').at(-1)
    function sql(texto){
        const r=spawnSync('docker',['exec','cyberladies-banco-1','psql','-U','cyberladies','-d',banco,'-Atc',texto],{windowsHide:true,encoding:'utf8'})
        assert.equal(r.status,0,r.stderr);return JSON.parse(r.stdout)
    }
    const contas=sql("SELECT json_agg(x) FROM (SELECT u.id,u.nome,u.tipo perfil,e.status AS \"statusEducadora\" FROM talk_usuaria u LEFT JOIN auth_educadora e ON e.usuaria_id=u.id ORDER BY u.id DESC) x")
    let conta={...contas.find(c=>c.perfil==='EDUCADORA'),statusEducadora:'APROVADA'}
    // Registros já existentes, somente para renderização controlada no navegador.
    // A aprovação e a autoria não são modificadas no servidor.
    const linhas=sql(`
        SELECT coalesce(json_agg(x),'[]'::json) FROM (
         SELECT i.usuaria_id AS "alunaId",i.trilha_id AS "trilhaId",u.nome,t.titulo trilha,
         (SELECT count(*) FROM academy_aula a WHERE a.trilha_id=t.id) AS "totalAulas",
         (SELECT count(*) FROM academy_conclusao c WHERE c.trilha_id=t.id AND c.usuaria_id=i.usuaria_id) AS "aulasConcluidas",
         i.criado_em AS "inscritaEm" FROM academy_inscricao i JOIN talk_usuaria u ON u.id=i.usuaria_id JOIN academy_trilha t ON t.id=i.trilha_id
         ORDER BY u.nome,t.id) x
    `)
    for(const linha of linhas)linha.percentual=linha.totalAulas?Math.floor(100*linha.aulasConcluidas/linha.totalAulas):0
    assert.ok(linhas.length>0)
    const trilhas=[...new Map(linhas.map(i=>[i.trilhaId,{id:i.trilhaId,titulo:i.trilha}])).values()]
    const indicador={totalAlunas:new Set(linhas.map(i=>i.alunaId)).size,inscricoesConcluidas:linhas.filter(i=>i.totalAulas>0&&i.aulasConcluidas===i.totalAulas).length,inscricoes:linhas.length}
    indicador.inscricoesEmAndamento=indicador.inscricoes-indicador.inscricoesConcluidas
    let modo='lista',detalheNegado=false
    const escritas=[]
    ws.addEventListener('message',async(evento)=>{
        const m=JSON.parse(evento.data)
        if(m.method==='Network.requestWillBeSent'&&m.params.request.url.includes('/api/')&&!['GET','HEAD','OPTIONS'].includes(m.params.request.method))escritas.push(m.params.request.method)
        if(m.method!=='Fetch.requestPaused')return
        const requestId=m.params.requestId,url=new URL(m.params.request.url)
        let valor,codigo=200
        try {
            if(url.pathname==='/api/auth/conta')valor=conta
            else if(modo==='erro'){codigo=503;valor={}}
            else if(modo==='offline'){await cdp('Fetch.failRequest',{requestId,errorReason:'ConnectionFailed'});return}
            else if(url.pathname.endsWith('/progresso')){
                if(detalheNegado){codigo=404;valor={}}
                else {
                    const partes=url.pathname.split('/'),alunaId=Number(partes[4]),trilhaId=Number(partes[6])
                    const inscricao=linhas.find(i=>i.alunaId===alunaId&&i.trilhaId===trilhaId)
                    assert.ok(inscricao)
                    const aulas=sql('SELECT json_agg(x) FROM (SELECT a.id,a.titulo,a.ordem,c.aula_id IS NOT NULL concluida,c.concluido_em AS "concluidaEm" FROM academy_aula a LEFT JOIN academy_conclusao c ON c.aula_id=a.id AND c.usuaria_id='+alunaId+' WHERE a.trilha_id='+trilhaId+' ORDER BY a.ordem) x')
                    valor={inscricao,aulas}
                }
            } else {
                let filtradas=modo==='sem-trilhas'||modo==='sem-alunas'?[]:linhas
                const termo=(url.searchParams.get('busca')||'').trim().toLowerCase(),status=url.searchParams.get('status'),trilha=url.searchParams.get('trilhaId')
                filtradas=filtradas.filter(i=>i.nome.toLowerCase().includes(termo)&&(!trilha||String(i.trilhaId)===trilha)&&(status==='CONCLUIDAS'?i.totalAulas>0&&i.aulasConcluidas===i.totalAulas:status==='ANDAMENTO'?!(i.totalAulas>0&&i.aulasConcluidas===i.totalAulas):true))
                valor={indicadores:modo==='sem-trilhas'||modo==='sem-alunas'?{totalAlunas:0,inscricoesConcluidas:0,inscricoesEmAndamento:0,inscricoes:0}:indicador,trilhas:modo==='sem-trilhas'?[]:trilhas,inscricoes:filtradas}
            }
            await cdp('Fetch.fulfillRequest',{requestId,responseCode:codigo,responseHeaders:[{name:'Content-Type',value:'application/json'}],body:Buffer.from(JSON.stringify(valor)).toString('base64')})
        }catch(error){if(!error.message.includes('Invalid InterceptionId'))errors.push(error.message)}
    })
    await cdp('Fetch.enable',{patterns:[{urlPattern:'*/api/auth/conta',requestStage:'Request'},{urlPattern:'*/api/educadora/alunas*',requestStage:'Request'}]})
    await cdp('Page.addScriptToEvaluateOnNewDocument',{source:"sessionStorage.setItem('token','teste-visual-sem-sessao-no-servidor');sessionStorage.setItem('perfil','EMPRESA')"})
    async function abrir(){await cdp('Page.navigate',{url:base+'/educadora/minhas-alunas'});await pause(350);await waitFor("!!document.querySelector('.ma-apresentacao')")}
    await abrir()
    await waitFor("document.querySelectorAll('.ma-tabela tbody tr').length==="+linhas.length)
    assert.equal(await evaluate("document.querySelectorAll('.ma-indicadores > div').length"),4)
    const metricas=await evaluate("Array.from(document.querySelectorAll('.ma-indicadores dd')).map(e=>Number(e.textContent))")
    assert.deepEqual(metricas,[indicador.totalAlunas,indicador.inscricoesConcluidas,indicador.inscricoesEmAndamento,indicador.inscricoes])
    for(const width of [390,768,1440]){
        await cdp('Emulation.setDeviceMetricsOverride',{width,height:1000,deviceScaleFactor:1,mobile:width<600})
        await evaluate("document.fonts.ready.then(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))))")
        await pause(400)
        assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'),'Overflow '+width)
        const foto=await cdp('Page.captureScreenshot',{format:'png',captureBeyondViewport:true})
        await writeFile(join(tmpdir(),'minhas-alunas-'+width+'.png'),Buffer.from(foto.data,'base64'))
        await click('.ma-ver')
        await waitFor("!!document.querySelector('.ma-dialogo h3')")
        assert.ok(await evaluate("document.querySelector('.ma-dialogo').textContent.includes('aulas concluídas')"))
        assert.equal(await evaluate("document.querySelectorAll('.ma-dialogo a[href*=perfil]').length"),0)
        const fotoDetalhe=await cdp('Page.captureScreenshot',{format:'png'})
        await writeFile(join(tmpdir(),'progresso-aluna-'+width+'.png'),Buffer.from(fotoDetalhe.data,'base64'))
        await cdp('Input.dispatchKeyEvent',{type:'keyDown',key:'Escape',code:'Escape',windowsVirtualKeyCode:27})
        await waitFor("!document.querySelector('.ma-dialogo')")
        await click('.topo-logado__avatar');await waitFor("!!document.querySelector('.me')")
        assert.equal(await evaluate("document.querySelector('.me .mp-item--ativo').getAttribute('href')"),'/educadora/minhas-alunas')
        assert.equal(await evaluate("document.querySelectorAll('.me button:disabled').length"),1)
        await click('.mp-fechar')
        console.log('PASS '+width+'px: tabela/cards, menu, detalhes, Escape e ausência de overflow')
    }
    await fill('#ma-busca','nome sem resultado')
    await waitFor("document.querySelector('.ma-estado h3')?.textContent.includes('Nenhuma')")
    await evaluate("document.querySelector('.ma-estado button').click()")
    await waitFor("document.querySelectorAll('.ma-tabela tbody tr').length==="+linhas.length)
    for(const [rotulo,qtd] of [['Concluíram',indicador.inscricoesConcluidas],['Em progresso',indicador.inscricoesEmAndamento]]){
        await evaluate("Array.from(document.querySelectorAll('.ma-filtros button')).find(b=>b.textContent==="+JSON.stringify(rotulo)+").click()")
        if(qtd)await waitFor("document.querySelectorAll('.ma-tabela tbody tr').length==="+qtd)
        else await waitFor("document.querySelector('.ma-estado h3')?.textContent.includes('Nenhuma')")
    }
    await evaluate("Array.from(document.querySelectorAll('.ma-filtros button')).find(b=>b.textContent==='Todas').click()")
    await waitFor("document.querySelectorAll('.ma-tabela tbody tr').length==="+linhas.length)
    if(trilhas.length>1){
        await evaluate("document.querySelector('#ma-trilha').value="+JSON.stringify(String(trilhas[0].id))+";document.querySelector('#ma-trilha').dispatchEvent(new Event('change',{bubbles:true}))")
        await waitFor("document.querySelectorAll('.ma-tabela tbody tr').length==="+linhas.filter(i=>i.trilhaId===trilhas[0].id).length)
    }
    detalheNegado=true
    await click('.ma-ver');await waitFor("!!document.querySelector('.ma-dialogo [role=alert]')")
    assert.equal(await evaluate("document.querySelectorAll('.ma-dialogo h3').length"),0)
    await click('.ma-fechar');detalheNegado=false
    for(const [valor,texto] of [['sem-trilhas','Nenhuma trilha'],['sem-alunas','não há alunas'],['erro','Não foi possível'],['offline','Não foi possível']]){
        modo=valor;await abrir()
        await waitFor("document.querySelector('.ma-estado')?.textContent.includes("+JSON.stringify(texto)+")")
    }
    modo='lista'
    for(const perfil of ['PENDENTE','CYBERLADIE','EMPRESA']){
        conta=perfil==='PENDENTE'?{...contas.find(c=>c.perfil==='EDUCADORA'),statusEducadora:'PENDENTE'}:contas.find(c=>c.perfil===perfil)
        assert.ok(conta);await abrir()
        await waitFor("!!document.querySelector('.ma-estado h2')")
        assert.equal(await evaluate("document.querySelectorAll('.ma-indicadores,.ma-tabela,.ma-dialogo').length"),0)
    }
    await cdp('Fetch.disable')
    assert.equal((await fetch(base+'/api/educadora/alunas?educadoraId=1',{headers:{Authorization:'Bearer token-invalido'}})).status,401)
    assert.deepEqual(escritas,[])
    assert.deepEqual(errors,[])
    console.log('PASS indicadores, pesquisa, filtros, trilha, detalhes negados, estados vazios, falhas e três perfis. Nenhuma escrita no banco.')
    console.log('Interface preenchida usa dados existentes com respostas controladas; autorização e progresso reais cobertos pelos testes transacionais do backend.')
}finally{ws?.close();child.kill()}
