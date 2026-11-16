import { spawn } from 'node:child_process'
import { mkdtemp } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import assert from 'node:assert/strict'

const chrome = process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const profile = await mkdtemp(join(tmpdir(), 'cybervagas-browser-'))
const child = spawn(chrome, ['--headless=new', '--disable-gpu', '--no-first-run',
    '--remote-debugging-port=9337', '--user-data-dir=' + profile, 'about:blank'],
{ windowsHide: true, stdio: 'ignore' })
const pause = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
let ws
try {
    let pages
    for (let i = 0; i < 100; i++) {
        try { pages = await fetch('http://127.0.0.1:9337/json/list').then((r) => r.json()); break }
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
    for (const width of [390, 768, 1440]) {
        await cdp('Emulation.setDeviceMetricsOverride', { width, height: 900, deviceScaleFactor: 1, mobile: width < 600 })
        await cdp('Page.navigate', { url: 'http://localhost:5173/vagas' })
        let ready = false
        for (let i = 0; i < 100; i++) {
            ready = await evaluate("!!document.querySelector('.cv-contador') && !document.querySelector('[aria-busy=\"true\"]')")
            if (ready) break
            await pause(100)
        }
        assert.ok(ready, 'Página não terminou de carregar')
        const state = await evaluate("({width:innerWidth,content:document.documentElement.scrollWidth,text:document.querySelector('main').innerText,filters:document.querySelectorAll('.cv-filtros input[type=checkbox]').length})")
        assert.ok(state.content <= state.width, 'Overflow horizontal em ' + width)
        assert.ok(!state.text.includes('Busca indisponível'), 'Falha de API na interface')
        assert.equal(state.filters, 10)
        await evaluate("document.querySelector('.cv-filtros summary').click()")
        assert.equal(await evaluate("document.querySelector('.cv-filtros details').open"), false)
        await evaluate("document.querySelector('.cv-filtros summary').click()")
        assert.equal(await evaluate("document.querySelector('.cv-filtros details').open"), true)
        console.log('PASS viewport ' + width + ': React, API, filtros recolhíveis e sem overflow')
    }
    assert.deepEqual(errors, [], 'Erros no Console ou JavaScript')
    console.log('PASS Console: sem erros JavaScript nas três larguras')
} finally {
    ws?.close()
    child.kill()
}