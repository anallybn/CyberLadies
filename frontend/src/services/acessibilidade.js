export const CHAVE_ACESSIBILIDADE = 'cyberladies.acessibilidade'
export const PADRAO_ACESSIBILIDADE = { altoContraste: false, tamanhoTexto: 100 }

export function lerPreferencias() {
    try {
        const valor = JSON.parse(localStorage.getItem(CHAVE_ACESSIBILIDADE))
        return {
            altoContraste: valor?.altoContraste === true,
            tamanhoTexto: Number.isFinite(valor?.tamanhoTexto) ? Math.min(200, Math.max(100, valor.tamanhoTexto)) : 100,
        }
    } catch { return { ...PADRAO_ACESSIBILIDADE } }
}

export function salvarPreferencias(valor) {
    try {
        if (!valor.altoContraste && valor.tamanhoTexto === 100) localStorage.removeItem(CHAVE_ACESSIBILIDADE)
        else localStorage.setItem(CHAVE_ACESSIBILIDADE, JSON.stringify(valor))
        return true
    } catch { return false }
}

// Há fontes em px, rem e clamp nas telas existentes. Escalamos as declarações,
// não os ancestrais de cada texto, para não multiplicar fontes herdadas duas vezes.
// As regras originais conservam tamanho, prioridade e media queries no padrão 100%.
const regrasPreparadas = new WeakSet()
function prepararRegras(regras) {
    for (const regra of regras) {
        if (regra.cssRules) prepararRegras(regra.cssRules)
        if (!regra.style || regrasPreparadas.has(regra)) continue
        const tamanho = regra.style.getPropertyValue('font-size').trim()
        if (!tamanho || tamanho.includes('--a11y-escala') ||
            /^(inherit|initial|unset|revert|small|medium|large|smaller|larger|x-small|xx-small|x-large|xx-large)$/.test(tamanho) ||
            /(^|[^a-z])[-\d.]+em\b|%/.test(tamanho) || /^(html|:root)$/.test(regra.selectorText)) continue
        regra.style.setProperty('font-size', `calc(${tamanho} * var(--a11y-escala, 1))`, regra.style.getPropertyPriority('font-size'))
        regrasPreparadas.add(regra)
    }
}

export function prepararTipografia() {
    for (const folha of document.styleSheets) {
        try { prepararRegras(folha.cssRules) }
        catch { /* Fontes externas não precisam ser lidas ou modificadas. */ }
    }
}

export function aplicarPreferencias(valor) {
    prepararTipografia()
    const raiz = document.documentElement
    raiz.style.setProperty('--a11y-escala', String(valor.tamanhoTexto / 100))
    raiz.toggleAttribute('data-a11y-contraste', valor.altoContraste)
    raiz.toggleAttribute('data-a11y-ampliado', valor.tamanhoTexto > 100)
}
