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
    '--remote-debugging-port=9343', '--user-data-dir=' + profile, 'about:blank'],
{ windowsHide: true, stdio: 'ignore' })
const pause = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
let ws
try {
    let pages
    for (let i = 0; i < 100; i++) {
        try { pages = await fetch('http://127.0.0.1:9343/json/list').then((r) => r.json()); break }
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
    ws.addEventListener('message', async(evento) => {
        if (JSON.parse(evento.data).method === 'Page.javascriptDialogOpening')
            await cdp('Page.handleJavaScriptDialog', { accept: true })
    })
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


    // Revisão somente visual: lê dados existentes e fornece esse snapshot ao navegador.
    // Não cadastra contas, não cria sessões e não executa nenhuma operação de escrita.
    const sql=`
        WITH conta AS (SELECT u.id,u.nome,u.email,u.tipo perfil,e.status FROM talk_usuaria u JOIN auth_educadora e ON e.usuaria_id=u.id ORDER BY u.id DESC LIMIT 1),
        minhas AS (SELECT a.trilha_id FROM academy_autoria a JOIN conta c ON c.id=a.educadora_id),
        totais AS (SELECT a.trilha_id,count(*) total FROM academy_aula a JOIN minhas m ON m.trilha_id=a.trilha_id GROUP BY a.trilha_id)
        SELECT json_build_object('conta',json_build_object('id',c.id,'nome',c.nome,'email',c.email,'perfil',c.perfil,'statusEducadora',c.status),'nome',c.nome,'statusEducadora',c.status,'podePublicar',false,
         'indicadores',json_build_object(
          'materiaisPublicados',(SELECT count(*) FROM academy_aula a JOIN minhas m ON m.trilha_id=a.trilha_id),
          'alunasAlcancadas',(SELECT count(DISTINCT i.usuaria_id) FROM academy_inscricao i JOIN minhas m ON m.trilha_id=i.trilha_id),
          'conclusoes',(SELECT count(*) FROM (SELECT i.usuaria_id,i.trilha_id FROM academy_inscricao i JOIN totais t ON t.trilha_id=i.trilha_id LEFT JOIN academy_conclusao x ON x.usuaria_id=i.usuaria_id AND x.trilha_id=i.trilha_id GROUP BY i.usuaria_id,i.trilha_id,t.total HAVING count(x.aula_id)=t.total) feitas)),
         'trilhas',coalesce((SELECT json_agg(x) FROM (SELECT t.id,t.titulo,count(i.usuaria_id) inscricoes FROM minhas m JOIN academy_trilha t ON t.id=m.trilha_id LEFT JOIN academy_inscricao i ON i.trilha_id=t.id GROUP BY t.id) x),'[]'::json),
         'mentorias',coalesce((SELECT json_agg(x) FROM (SELECT a.id,u.nome aluna,coalesce(d.tema,'Tema não informado') tema,d.inicio,d.fim FROM academy_agendamento a JOIN academy_disponibilidade d ON d.id=a.disponibilidade_id JOIN academy_mentora m ON m.id=d.mentora_id JOIN talk_usuaria u ON u.id=a.usuaria_id WHERE m.usuaria_id=c.id AND a.status='CONFIRMADO' AND d.inicio>clock_timestamp() ORDER BY d.inicio,a.id LIMIT 5) x),'[]'::json),
         'materiais',coalesce((SELECT json_agg(x) FROM (SELECT a.id,a.titulo,t.titulo trilha,(SELECT count(*) FROM academy_aula aa WHERE aa.trilha_id=t.id) aulas,'Publicado' status FROM minhas m JOIN academy_trilha t ON t.id=m.trilha_id JOIN academy_aula a ON a.trilha_id=t.id ORDER BY a.criado_em DESC,a.id DESC LIMIT 5) x),'[]'::json))
        FROM conta c
    `
    const banco=destino.stdout.trim().split('/').at(-1)
    function ler() {
        const r=spawnSync('docker',['exec','cyberladies-banco-1','psql','-U','cyberladies','-d',banco,'-Atc',sql],{windowsHide:true,encoding:'utf8'})
        assert.equal(r.status,0,r.stderr)
        assert.ok(r.stdout.trim(),'É necessária uma Educadora existente no ambiente isolado.')
        return JSON.parse(r.stdout)
    }
    const dados=ler()
    const snapshot=JSON.stringify(dados)
    ws.addEventListener('message',async(event)=>{
        const message=JSON.parse(event.data)
        if(message.method==='Fetch.requestPaused') {
            await cdp('Fetch.fulfillRequest',{requestId:message.params.requestId,responseCode:200,responseHeaders:[{name:'Content-Type',value:'application/json'},{name:'Cache-Control',value:'no-store'}],body:Buffer.from(message.params.request.url.includes('/auth/conta')?JSON.stringify(dados.conta):snapshot).toString('base64')}).catch(error=>{if(!error.message.includes('Invalid InterceptionId')) errors.push(error.message)})
        }
    })
    await cdp('Fetch.enable',{patterns:[{urlPattern:'*/api/educadora/dashboard',requestStage:'Request'},{urlPattern:'*/api/auth/conta',requestStage:'Request'}]})
    await cdp('Page.addScriptToEvaluateOnNewDocument',{source:"sessionStorage.setItem('token','somente-validacao-visual-sem-sessao-real')"})
    await cdp('Page.navigate',{url:base+'/educadora/dashboard'})
    await waitFor("!!document.querySelector('.ed-indicadores')")
    assert.equal(await evaluate("document.querySelectorAll('.ed-indicadores > div').length"),5)
    assert.equal(await evaluate("document.querySelector('.ed-saudacao h1').textContent"),'Bem-vinda, '+dados.nome+'!')
    const links=['/educadora/dashboard','/educadora/enviar-material','/educadora/meus-materiais','/educadora/minhas-alunas','/chat','/comunidade','/academy','/vagas']
    for(const [width,height] of [[390,844],[768,1024],[1440,1000],[390,568]]) {
        await cdp('Emulation.setDeviceMetricsOverride',{width,height,deviceScaleFactor:1,mobile:width<600})
        await evaluate("document.fonts.ready.then(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))))")
        await pause(600)
        assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'),'Overflow da página')
        assert.equal(await evaluate("document.querySelector('.ed-topo').getBoundingClientRect().height"),64)
        if(width===1440) {
            const cards=await evaluate("Array.from(document.querySelectorAll('.ed-indicadores > div')).map(e=>{const r=e.getBoundingClientRect();return {left:r.left,right:r.right,width:r.width,top:r.top}})")
            assert.ok(Math.abs(cards[0].width-cards[3].width)<1,'Largura dos cards mudou')
            assert.ok(Math.abs((cards[3].left+cards[4].right)-(cards[0].left+cards[2].right))<2,'Par inferior não centralizado')
        }
        const shot=await cdp('Page.captureScreenshot',{format:'png',captureBeyondViewport:true})
        await writeFile(join(tmpdir(),'educadora-refinada-'+width+'x'+height+'.png'),Buffer.from(shot.data,'base64'))
        await click('.ed-menu-botao')
        await waitFor("!!document.querySelector('.me')")
        await pause(350)
        const geometria=await evaluate("(()=>{const r=document.querySelector('.me').getBoundingClientRect();const n=document.querySelector('.me .mp-lista');return {top:r.top,bottom:r.bottom,width:r.width,height:r.height,client:n.clientHeight,scroll:n.scrollHeight}})()")
        assert.ok(geometria.top>=0 && geometria.bottom<=height,'Popup fora da tela')
        assert.ok(geometria.width<=470,'Popup fora da largura esperada')
        if(width>=768) assert.equal(Math.round(geometria.width),470,'Largura do popup')
        assert.ok((await evaluate("getComputedStyle(document.querySelector('.me .mp-titulo')).fontFamily")).includes('Poppins'),'Fonte do título')
        assert.equal(await evaluate("getComputedStyle(document.querySelector('.me button:disabled')).opacity"),'1','Itens apagados por opacidade')
        if(height===568) assert.ok(geometria.scroll>geometria.client,'Faltou rolagem interna')
        assert.equal(await evaluate("document.querySelector('.me .mp-titulo').textContent"),'Painel da educadora')
        assert.equal(await evaluate("Array.from(document.querySelectorAll('.me-principal .mp-texto strong')).map(e=>e.textContent).join('|')"),'Dashboard|Enviar Material|Meus Materiais|Minhas Alunas|Chat|CyberTalk')
        assert.equal(await evaluate("document.querySelectorAll('.me button:disabled').length"),1)
        assert.equal(await evaluate("document.querySelectorAll('.me .mp-item--ativo').length"),1)
        const hrefs=await evaluate("Array.from(document.querySelectorAll('.me a')).map(a=>a.getAttribute('href'))")
        assert.deepEqual(hrefs,links)
        const menuShot=await cdp('Page.captureScreenshot',{format:'png'})
        await writeFile(join(tmpdir(),'menu-educadora-'+width+'x'+height+'.png'),Buffer.from(menuShot.data,'base64'))
        await evaluate("document.querySelector('.me .mp-lista').scrollTop=10000")
        assert.ok(await evaluate("(()=>{const r=document.querySelector('.me-sair').getBoundingClientRect();const n=document.querySelector('.me .mp-lista').getBoundingClientRect();return r.bottom<=n.bottom+1})()"),'Sair inacessível')
        await cdp('Input.dispatchKeyEvent',{type:'keyDown',key:'Escape',code:'Escape',windowsVirtualKeyCode:27})
        await waitFor("!document.querySelector('.me')")
        await click('.ed-avatar'); await click('.mp-fechar')
        await waitFor("!document.querySelector('.me')")
        await click('.ed-menu-botao')
        await evaluate("document.querySelector('.me-fundo').click()")
        await waitFor("!document.querySelector('.me')")
        console.log('PASS '+width+'x'+height+': cabeçalho 64px, menu compacto, opções, desabilitados, rolagem e fechamento por botão/Escape/fundo')
    }
    await click('.ed-menu-botao')
    await evaluate("document.querySelector('.me a[href=\"/educadora/enviar-material\"]').click()")
    await waitFor("location.pathname==='/educadora/enviar-material' && !!document.querySelector('#em-titulo')")
    assert.ok(await evaluate("!document.querySelector('.me')"))
    await cdp('Page.addScriptToEvaluateOnNewDocument',{source:"sessionStorage.setItem('token','somente-validacao-visual-sem-sessao-real')"})
    await cdp('Page.navigate',{url:base+'/educadora/dashboard'})
    await waitFor("!!document.querySelector('.ed-indicadores')")
    assert.deepEqual(ler(),dados,'Os dados existentes mudaram')

    // Enviar Material: somente interação local e leituras de dados existentes.
    const escritas=[]
    ws.addEventListener('message',(evento)=>{
        const mensagem=JSON.parse(evento.data)
        if(mensagem.method==='Network.requestWillBeSent') {
            const request=mensagem.params.request
            if(request.url.includes('/api/') && !['GET','HEAD','OPTIONS'].includes(request.method)) escritas.push(request.method+' '+request.url)
        }
    })
    await click('.ed-menu-botao')
    await evaluate("document.querySelector('.me a[href=\"/educadora/enviar-material\"]').click()")
    await waitFor("location.pathname==='/educadora/enviar-material' && !!document.querySelector('#em-titulo')")
    assert.equal(await evaluate("document.querySelector('.em-apresentacao h1').textContent"),'Enviar Material')
    assert.equal(await evaluate("document.querySelectorAll('.em-acoes button:disabled').length"),1)
    assert.ok(await evaluate("document.querySelector('#em-categoria').readOnly && document.querySelector('#em-nivel').readOnly"))
    assert.equal(await evaluate("document.querySelectorAll('.em-pagina iframe').length"),0)
    if(!dados.trilhas.length) assert.ok(await evaluate("document.querySelector('#em-trilha').disabled && document.querySelector('#em-trilha').textContent.includes('Nenhuma trilha')"))
    await fill('#em-titulo','Prévia local de validação')
    await fill('#em-descricao','Texto digitado apenas para verificar a prévia. Não salvo.')
    await fill('#em-duracao','25')
    await fill('#em-tags','acessibilidade, ensino')
    await click('#em-autorizado')
    assert.equal(await evaluate("document.querySelector('.em-previa-corpo h3').textContent"),'Prévia local de validação')
    assert.ok(await evaluate("document.querySelector('.em-previa-descricao').textContent.includes('Texto digitado') && document.querySelector('.em-previa-meta').textContent.includes('25 min')"))
    assert.equal(await evaluate("document.querySelectorAll('.em-tags > span').length"),2)
    assert.ok(await evaluate("document.querySelector('#em-autorizado').checked"))
    await fill('#em-video-id','<iframe>')
    assert.equal(await evaluate("document.querySelector('#em-video-id').getAttribute('aria-invalid')"),'true')
    assert.equal(await evaluate("document.querySelectorAll('.em-pagina iframe').length"),0)
    await fill('#em-video-id','')
    await evaluate("document.querySelector('#em-provedor').value='VIMEO';document.querySelector('#em-provedor').dispatchEvent(new Event('change',{bubbles:true}))")
    assert.ok(await evaluate("document.querySelector('#em-ajuda-video').textContent.includes('Vimeo')"))
    for(const width of [390,768,1440]) {
        await cdp('Emulation.setDeviceMetricsOverride',{width,height:1000,deviceScaleFactor:1,mobile:width<600})
        await evaluate("document.fonts.ready.then(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))))")
        await pause(500)
        assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'),'Overflow Enviar Material em '+width)
        const layout=await evaluate("(()=>{const f=document.querySelector('.em-formulario').getBoundingClientRect();const p=document.querySelector('.em-previa').getBoundingClientRect();return {formBottom:f.bottom,formRight:f.right,prevTop:p.top,prevLeft:p.left}})()")
        if(width>900) assert.ok(layout.prevLeft>layout.formRight,'Prévia deveria estar à direita')
        else assert.ok(layout.prevTop>=layout.formBottom,'Prévia deveria estar empilhada')
        await waitFor("!!document.querySelector('.em-mascote')?.complete && document.querySelector('.em-mascote').naturalWidth>0")
        const mascote=await evaluate("(()=>{const i=document.querySelector('.em-mascote'),r=i.getBoundingClientRect(),t=document.querySelector('.em-apresentacao > div').getBoundingClientRect();return {width:r.width,height:r.height,left:r.left,right:r.right,top:r.top,bottom:r.bottom,textRight:t.right,ratio:i.naturalWidth/i.naturalHeight}})()")
        assert.ok(mascote.left>=mascote.textRight,'Mascote sobrepõe a apresentação')
        assert.ok(Math.abs(mascote.width/mascote.height-mascote.ratio)<.02,'Imagem distorcida')
        assert.ok(mascote.right<=width,'Mascote fora da tela')
        if(width===1440) assert.ok(mascote.width>=180 && mascote.width<=190)
        if(width===390) assert.ok(mascote.width<=110)
        const apresentacao=await evaluate("(()=>{const b=document.querySelector('.em-apresentacao'),r=b.getBoundingClientRect(),t=b.firstElementChild.getBoundingClientRect(),i=document.querySelector('.em-mascote').getBoundingClientRect();return {height:r.height,textCenter:(t.top+t.bottom)/2,imageCenter:(i.top+i.bottom)/2,border:parseFloat(getComputedStyle(b).borderTopWidth)}})()")
        assert.ok(Math.abs(apresentacao.textCenter-apresentacao.imageCenter)<1,'Apresentação não centralizada verticalmente')
        assert.equal(apresentacao.border,0)
        if(width===1440) assert.ok(apresentacao.height<=210,'Apresentação excessivamente alta')
        const foto=await cdp('Page.captureScreenshot',{format:'png',captureBeyondViewport:true})
        await writeFile(join(tmpdir(),'enviar-material-'+width+'.png'),Buffer.from(foto.data,'base64'))
        await click('.ed-menu-botao')
        assert.ok(await evaluate("document.querySelector('.me .mp-item--ativo').getAttribute('href')==='/educadora/enviar-material'"))
        await click('.mp-fechar')
        console.log('PASS Enviar Material '+width+'px: campos, prévia, layout, menu e ações indisponíveis')
    }
    // Persistência real é verificada por verify-rascunhos.mjs; esta revisão não grava.
    assert.deepEqual(escritas,[],'A tela visual enviou operações de escrita')
    await cdp('Page.reload'); await pause(400)
    await waitFor("document.querySelector('#em-titulo')?.value===''")
    assert.equal(await evaluate("document.querySelector('#em-titulo').value"),'','Campos não devem simular persistência')
    assert.deepEqual(escritas,[])
    assert.deepEqual(ler(),dados,'A preparação visual alterou os dados')
    console.log('PASS estado local, sem iframe, sem persistência e sem requisições de escrita')
    await cdp('Fetch.disable')
    await cdp('Page.reload')
    await waitFor("location.pathname==='/login'")
    console.log('PASS rota de preparação sem sessão retorna ao login')
    assert.deepEqual(errors,[])
    console.log('PASS navegação, dados existentes preservados e Console sem erros. Nenhuma escrita no banco.')
    console.log('Capturas em '+tmpdir())
} finally { ws?.close(); child.kill() }
