export function mascararCnpj(valor) {
     const d = valor.replace(/\D/g, '').slice(0, 14)
     return d
     .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1/$2')
    .replace(/(\d{4})(\d)/, '$1-$2')
}


//Conferimento do CNPJ
export function cnpjValido(valor) {
    const d = valor.replace(/\D/g, '')
    if (d.length !== 14 || /^(\d)\1+$/.test(d)) return false

    const calcular = (base) => {
        let soma = 0
        let peso = base.length - 7
        for (let i = 0; i < base.length; i++) {
            soma += Number(base[i]) * peso 
            peso--
            if (peso < 2) peso = 9
        }
        const resto = soma % 11
        return resto < 2 ? 0 : 11 - resto
    }

    const d1 = calcular(d.slice(0, 12))
    const d2 = calcular(d.slice(0, 12) + d1)
    return d1 == Number (d[12]) && d2 === Number(d[13])
}