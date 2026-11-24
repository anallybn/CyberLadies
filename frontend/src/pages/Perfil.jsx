import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { UserRound, Award, BriefcaseBusiness, Pencil, Plus, Trash2, MessageSquare, ShieldCheck, FileText, Eye, Download } from 'lucide-react'
import TopoLogado from '../components/TopoLogado'
import Circuitos from '../components/Circuitos'
import { carregarPerfil, salvarPerfil, carregarFoto, enviarFoto, removerFoto, salvarRegistro, excluirRegistro, salvarCertificacao, carregarCertificacaoPdf } from '../services/perfilService'
import './Perfil.css'

const TIPO_PERFIL = { EDUCADORA: 'Educadora', EMPRESA: 'Empresa', ADMIN: 'Administradora' }
const INDICADORES_COMUNIDADE = [
    ['publicacoes', 'Publicações no CyberTalk'],
    ['comentarios', 'Comentários feitos'],
    ['curtidas', 'Curtidas realizadas'],
]

const hoje = () => new Date().toLocaleDateString('sv-SE', { timeZone: 'America/Sao_Paulo' })
const dataLegivel = (valor) => new Date(valor + 'T12:00:00').toLocaleDateString('pt-BR')

function EditorPessoal({ pessoa, dadosProfissionais, ocupado, salvar, cancelar }) {
    const [valores, setValores] = useState({
        nome: pessoa.nome, sobre: pessoa.sobre,
        ...(dadosProfissionais ? { formacao: pessoa.formacao ?? '', competencias: pessoa.competencias ?? '' } : {}),
    })
    const mudar = (chave, valor) => setValores((atual) => ({ ...atual, [chave]: valor }))
    return <form className="pf-form" onSubmit={(e) => { e.preventDefault(); if (valores.nome.trim()) salvar(valores) }}>
        <fieldset disabled={ocupado}>
            <legend className="pf-sr">Editar dados pessoais</legend>
            <label>Nome completo<input autoFocus name="nome" required maxLength={120} value={valores.nome} onChange={(e) => mudar('nome', e.target.value)} autoComplete="name" /></label>
            {dadosProfissionais && <label>Formação ou área de estudo<input name="formacao" maxLength={200} value={valores.formacao} onChange={(e) => mudar('formacao', e.target.value)} /></label>}
            <label>Sobre você<textarea name="sobre" rows={5} maxLength={2000} value={valores.sobre} onChange={(e) => mudar('sobre', e.target.value)} /></label>
            {dadosProfissionais && <label>Competências<input name="competencias" maxLength={500} value={valores.competencias} onChange={(e) => mudar('competencias', e.target.value)} /></label>}
            <div className="pf-acoes"><button className="pf-botao" disabled={!valores.nome.trim()} type="submit">{ocupado ? 'Salvando…' : 'Salvar dados'}</button>
                <button className="pf-secundario" type="button" onClick={cancelar}>Cancelar</button></div>
        </fieldset>
    </form>
}

const LIMITE_PDF = 5 * 1024 * 1024

function AnexoCertificacao({ item }) {
    const [ocupado, setOcupado] = useState(false)
    const [erro, setErro] = useState('')

    async function abrir(baixar) {
        setErro('')
        const aba = baixar ? null : window.open('about:blank', '_blank')
        if (!baixar && !aba) {
            setErro('Permita abrir uma nova aba ou use Baixar PDF.')
            return
        }
        if (aba) aba.opener = null
        setOcupado(true)
        try {
            const arquivo = await carregarCertificacaoPdf(item.id, baixar)
            if (aba?.closed) return
            const url = URL.createObjectURL(arquivo)
            if (aba) {
                aba.location.replace(url)
            } else {
                const link = document.createElement('a')
                link.href = url
                link.download = item.nomePdf
                document.body.append(link)
                link.click()
                link.remove()
            }
            setTimeout(() => URL.revokeObjectURL(url), 60_000)
        } catch (falha) {
            aba?.close()
            setErro(falha.message)
        } finally {
            setOcupado(false)
        }
    }

    return <div className="pf-certificado-pdf">
        <small className="pf-certificado-nome"><FileText size={16} aria-hidden="true" /> {item.nomePdf}</small>
        <div className="pf-acoes">
            <button type="button" className="pf-secundario" disabled={ocupado} onClick={() => abrir(false)}
                aria-label={'Visualizar PDF de ' + item.nome}><Eye size={16} aria-hidden="true" />Visualizar</button>
            <button type="button" className="pf-secundario" disabled={ocupado} onClick={() => abrir(true)}
                aria-label={'Baixar PDF de ' + item.nome}><Download size={16} aria-hidden="true" />Baixar</button>
        </div>
        {erro && <p role="alert" className="pf-erro">{erro}</p>}
    </div>
}

function EditorRegistro({ tipo, registro, ocupado, salvar, cancelar }) {
    const certificacao = tipo === 'certificacoes'
    const [valores, setValores] = useState(registro || (certificacao ?
        { nome: '', instituicao: '', data: '' } : { empresa: '', cargo: '', inicio: '', fim: '', descricao: '' }))
    const [erro, setErro] = useState('')
    const [arquivo, setArquivo] = useState(null)
    const [erroPdf, setErroPdf] = useState('')
    const [removerPdf, setRemoverPdf] = useState(false)
    const campoPdf = useRef(null)
    const mudar = (chave, valor) => setValores((atual) => ({ ...atual, [chave]: valor }))

    function selecionarPdf(evento) {
        const selecionado = evento.target.files?.[0]
        if (!selecionado) return
        setRemoverPdf(false)
        if (!/\.pdf$/i.test(selecionado.name) || !selecionado.size || selecionado.size > LIMITE_PDF) {
            evento.target.value = ''
            setArquivo(null)
            setErroPdf('Escolha um arquivo PDF de até 5 MB.')
            return
        }
        setArquivo(selecionado)
        setErroPdf('')
    }

    function limparSelecaoPdf() {
        campoPdf.current.value = ''
        setArquivo(null)
        setErroPdf('')
    }
    const obrigatorios = certificacao ? ['nome', 'instituicao', 'data'] : ['empresa', 'cargo', 'inicio']
    const invalido = obrigatorios.some((chave) => !String(valores[chave] || '').trim()) ||
        (!certificacao && valores.fim && valores.fim < valores.inicio)
    function enviar(evento) {
        evento.preventDefault()
        if (erroPdf) return
        if (invalido) { setErro('Preencha os campos obrigatórios e confira o período.'); return }
        const dados = certificacao ? { nome: valores.nome.trim(), instituicao: valores.instituicao.trim(), data: valores.data } :
            { empresa: valores.empresa.trim(), cargo: valores.cargo.trim(), inicio: valores.inicio, fim: valores.fim || null, descricao: valores.descricao.trim() }
        salvar(dados, arquivo, removerPdf)
    }
    return <form className="pf-form pf-editor-registro" onSubmit={enviar}>
        <fieldset disabled={ocupado}>
            <legend>{registro ? 'Editar' : 'Adicionar'} {certificacao ? 'certificação' : 'experiência'}</legend>
            {certificacao ? <>
                <label>Nome da certificação<input autoFocus name="nome" required maxLength={160} value={valores.nome} onChange={(e) => mudar('nome', e.target.value)} /></label>
                <label>Instituição<input name="instituicao" required maxLength={160} value={valores.instituicao} onChange={(e) => mudar('instituicao', e.target.value)} /></label>
                <label>Data de emissão<input name="data" type="date" required max={hoje()} value={valores.data} onChange={(e) => mudar('data', e.target.value)} /></label>
                <label>Anexar certificado (PDF)
                    <input ref={campoPdf} type="file" accept=".pdf,application/pdf" onChange={selecionarPdf} />
                </label>
                <small>Opcional. Limite de 5 MB.</small>
                {arquivo && <small role="status" className="pf-certificado-nome">
                    <FileText size={16} aria-hidden="true" />{arquivo.name}
                </small>}
                {erroPdf && <p role="alert" className="pf-erro">{erroPdf}</p>}
                {(arquivo || erroPdf) && <div className="pf-acoes">
                    <button type="button" className="pf-secundario" onClick={limparSelecaoPdf}>Limpar seleção</button>
                </div>}
                {registro?.nomePdf && !arquivo && <label className="pf-remover-pdf">
                    <input type="checkbox" checked={removerPdf} onChange={(e) => setRemoverPdf(e.target.checked)} />
                    <span>Remover PDF atual ({registro.nomePdf})</span>
                </label>}
            </> : <>
                <label>Empresa<input autoFocus name="empresa" required maxLength={160} value={valores.empresa} onChange={(e) => mudar('empresa', e.target.value)} /></label>
                <label>Cargo<input name="cargo" required maxLength={160} value={valores.cargo} onChange={(e) => mudar('cargo', e.target.value)} /></label>
                <div className="pf-periodo">
                    <label>Início<input name="inicio" type="date" required max={hoje()} value={valores.inicio} onChange={(e) => mudar('inicio', e.target.value)} /></label>
                    <label>Fim (opcional)<input name="fim" type="date" min={valores.inicio || undefined} max={hoje()} value={valores.fim || ''} onChange={(e) => mudar('fim', e.target.value)} /></label>
                </div>
                <small>Deixe o fim vazio se esta for sua experiência atual.</small>
                <label>Descrição<textarea name="descricao" rows={4} maxLength={2000} value={valores.descricao} onChange={(e) => mudar('descricao', e.target.value)} /></label>
            </>}
            {erro && <p role="alert" className="pf-erro">{erro}</p>}
            <div className="pf-acoes"><button className="pf-botao" disabled={!!invalido || !!erroPdf} type="submit">{ocupado ? 'Salvando…' : 'Salvar registro'}</button>
                <button className="pf-secundario" type="button" onClick={cancelar}>Cancelar</button></div>
        </fieldset>
    </form>
}

export default function Perfil() {
    const [dados, setDados] = useState(null)
    const [erro, setErro] = useState('')
    const [tentativa, setTentativa] = useState(0)
    const [carregando, setCarregando] = useState(true)
    const [ocupado, setOcupado] = useState(false)
    const [mensagem, setMensagem] = useState(null)
    const [editor, setEditor] = useState(null)
    const [exclusao, setExclusao] = useState(null)
    const [foto, setFoto] = useState('')
    const [erroFoto, setErroFoto] = useState('')
    const [versaoFoto, setVersaoFoto] = useState(0)
    const bloqueio = useRef(false)
    const navigate = useNavigate()
    const temFoto = dados?.pessoal.temFoto

    useEffect(() => {
        const controller = new AbortController()
        carregarPerfil(controller.signal)
            .then((perfil) => { setDados(perfil); setErro('') })
            .catch((falha) => {
                if (falha.name === 'AbortError') return
                if (falha.status === 401) navigate('/login?retorno=%2Fperfil', { replace: true })
                else setErro(falha.message)
            })
            .finally(() => { if (!controller.signal.aborted) setCarregando(false) })
        return () => controller.abort()
    }, [tentativa, navigate])

    useEffect(() => {
        if (!temFoto) return
        const controller = new AbortController()
        let url
        carregarFoto(controller.signal).then((blob) => {
            if (controller.signal.aborted) return
            url = URL.createObjectURL(blob)
            setFoto(url)
            setErroFoto('')
        }).catch((falha) => {
            if (falha.name === 'AbortError') return
            if (falha.status === 401) navigate('/login?retorno=%2Fperfil', { replace: true })
            else setErroFoto(falha.message)
        })
        return () => { controller.abort(); if (url) URL.revokeObjectURL(url) }
    }, [temFoto, versaoFoto, navigate])

    async function executar(operacao, sucesso) {
        if (bloqueio.current) return false
        bloqueio.current = true
        setOcupado(true)
        setMensagem(null)
        try {
            await operacao()
            const atualizado = await carregarPerfil()
            setDados(atualizado)
            setMensagem({ erro: false, texto: sucesso })
            return true
        } catch (falha) {
            if (falha.status === 401) navigate('/login?retorno=%2Fperfil', { replace: true })
            else setMensagem({ erro: true, texto: falha.message })
            return false
        } finally {
            bloqueio.current = false
            setOcupado(false)
        }
    }
    async function salvarPessoal(valores) {
        if (await executar(() => salvarPerfil(valores), 'Dados pessoais atualizados.')) setEditor(null)
    }
    async function salvarItem(valores, arquivo, removerPdf) {
        const id = editor.registro?.id
        const operacao = editor.tipo === 'certificacoes'
            ? () => salvarCertificacao(id, valores, arquivo, removerPdf)
            : () => salvarRegistro(editor.tipo, id, valores)
        if (await executar(operacao, 'Registro salvo.')) setEditor(null)
    }
    async function excluirItem() {
        if (await executar(() => excluirRegistro(exclusao.tipo, exclusao.id), 'Registro excluído.')) setExclusao(null)
    }
    async function escolherFoto(evento) {
        const arquivo = evento.target.files?.[0]
        evento.target.value = ''
        if (!arquivo) return
        if (!['image/png', 'image/jpeg'].includes(arquivo.type) || arquivo.size === 0 || arquivo.size > 2 * 1024 * 1024) {
            setMensagem({ erro: true, texto: 'Escolha uma foto PNG ou JPEG de até 2 MB.' })
            return
        }
        if (await executar(() => enviarFoto(arquivo), 'Foto atualizada.')) setVersaoFoto((v) => v + 1)
    }
    function abrirEditor(tipo, registro = null) {
        setEditor({ tipo, registro })
        setExclusao(null)
        setMensagem(null)
    }
    function secaoRegistros(tipo, titulo, Icone) {
        const lista = dados[tipo]
        return <section className="pf-cartao" aria-labelledby={'pf-' + tipo}>
            <div className="pf-secao-topo"><h2 id={'pf-' + tipo}><Icone size={21} aria-hidden="true" />{titulo}</h2>
                <button type="button" className="pf-secundario" disabled={ocupado || editor !== null} onClick={() => abrirEditor(tipo)}><Plus size={16} />Adicionar</button></div>
            {tipo === 'certificacoes' && <p className="pf-ajuda">Certificações informadas por você. Não representam validação ou emissão pela plataforma.</p>}
            {editor?.tipo === tipo && <EditorRegistro key={tipo + (editor.registro?.id || 'novo')} tipo={tipo} registro={editor.registro} ocupado={ocupado} salvar={salvarItem} cancelar={() => setEditor(null)} />}
            {!lista.length ? <p className="pf-vazio">Você ainda não adicionou {tipo === 'certificacoes' ? 'certificações' : 'experiências profissionais'}.</p> :
                <ul className="pf-lista">{lista.map((item) => <li key={item.id}>
                    <div className="pf-registro-texto"><h3>{item.nome || item.cargo}</h3><p>{item.instituicao || item.empresa}</p>
                        <small>{tipo === 'certificacoes' ? dataLegivel(item.data) : dataLegivel(item.inicio) + ' – ' + (item.fim ? dataLegivel(item.fim) : 'Atual')}</small>
                        {item.descricao && <p className="pf-texto">{item.descricao}</p>}
                        {tipo === 'certificacoes' && item.nomePdf && <AnexoCertificacao item={item} />}
                    </div>
                    <div className="pf-registro-acoes">
                        <button type="button" className="pf-icone-botao" aria-label={'Editar ' + (item.nome || item.cargo)} disabled={ocupado || editor !== null} onClick={() => abrirEditor(tipo, item)}><Pencil size={17} /></button>
                        <button type="button" className="pf-icone-botao" aria-label={'Excluir ' + (item.nome || item.cargo)} disabled={ocupado || editor !== null} onClick={() => setExclusao({ tipo, id: item.id, nome: item.nome || item.cargo })}><Trash2 size={17} /></button>
                    </div>
                    {exclusao?.tipo === tipo && exclusao.id === item.id && <div className="pf-confirmacao" role="group" aria-label="Confirmar exclusão">
                        <p>Excluir “{exclusao.nome}”?</p><div className="pf-acoes"><button className="pf-botao pf-perigo" disabled={ocupado} onClick={excluirItem}>Confirmar exclusão</button>
                            <button className="pf-secundario" disabled={ocupado} onClick={() => setExclusao(null)}>Cancelar</button></div>
                    </div>}
                </li>)}</ul>}
        </section>
    }

    return <div className="pf">
        <TopoLogado ativa="perfil" /><Circuitos />
        <main className="pf-conteudo" aria-busy={carregando || ocupado}>
            {carregando ? <div className="pf-estado" role="status">Carregando seu perfil…</div> : erro ? <div className="pf-estado">
                <p role="alert">{erro}</p><button className="pf-botao" onClick={() => { setCarregando(true); setTentativa((t) => t + 1) }}>Tentar novamente</button>
            </div> : dados && <>
                <section className="pf-cabecalho" aria-labelledby="pf-nome">
                    <div className="pf-capa" aria-hidden="true" />
                    <div className="pf-identidade">
                        <div className="pf-avatar">{temFoto && foto ? <img src={foto} alt="Sua foto de perfil" /> : <UserRound size={52} aria-hidden="true" />}</div>
                        <span className="pf-privado"><ShieldCheck size={14} aria-hidden="true" />Seu perfil privado</span>
                        <h1 id="pf-nome">{dados.pessoal.nome}</h1>
                        {TIPO_PERFIL[dados.tipo] && <p>{TIPO_PERFIL[dados.tipo]}</p>}
                        {dados.dadosProfissionais && <p>{dados.pessoal.formacao || 'Adicione sua formação ou área de estudo'}</p>}
                        <div className="pf-acoes pf-foto-acoes">
                            <label className={'pf-secundario pf-upload' + (ocupado ? ' pf-desabilitado' : '')}>Alterar foto
                                <input type="file" accept="image/png,image/jpeg" disabled={ocupado} onChange={escolherFoto} />
                            </label>
                            {temFoto && <button className="pf-secundario" disabled={ocupado} onClick={async () => {
                                if (await executar(removerFoto, 'Foto removida.')) { setFoto(''); setErroFoto('') }
                            }}>Remover foto</button>}
                        </div>
                        <small>PNG ou JPEG, até 2 MB. Visível somente para você.</small>
                        {erroFoto && <p role="alert" className="pf-erro">{erroFoto} <button className="pf-secundario" onClick={() => setVersaoFoto((v) => v + 1)}>Recarregar foto</button></p>}
                    </div>
                </section>
                {mensagem && <div className={'pf-mensagem' + (mensagem.erro ? ' pf-erro' : '')} role={mensagem.erro ? 'alert' : 'status'}>{mensagem.texto}</div>}
                <section className="pf-cartao" aria-labelledby="pf-pessoal">
                    <div className="pf-secao-topo"><h2 id="pf-pessoal">Sobre você</h2><button className="pf-secundario" disabled={ocupado || editor !== null} onClick={() => abrirEditor('pessoal')}><Pencil size={16} />Editar dados</button></div>
                    {editor?.tipo === 'pessoal' ? <EditorPessoal pessoa={dados.pessoal} dadosProfissionais={dados.dadosProfissionais} ocupado={ocupado} salvar={salvarPessoal} cancelar={() => setEditor(null)} /> : <>
                        <p className="pf-texto">{dados.pessoal.sobre || 'Conte um pouco sobre sua trajetória e seus próximos passos na tecnologia.'}</p>
                        <dl className="pf-dados">
                            <div><dt>E-mail da conta</dt><dd>{dados.pessoal.email}</dd></div>
                            {dados.atuacaoEducadora && <>
                                <div><dt>Especialidade</dt><dd>{dados.atuacaoEducadora.especialidade}</dd></div>
                                <div><dt>Instituição</dt><dd>{dados.atuacaoEducadora.instituicao}</dd></div>
                            </>}
                            {dados.empresa && <>
                                <div><dt>CNPJ</dt><dd>{dados.empresa.cnpj}</dd></div>
                                <div><dt>Área de atuação</dt><dd>{dados.empresa.areaAtuacao}</dd></div>
                            </>}
                            {dados.dadosProfissionais && <div><dt>Competências</dt><dd>{dados.pessoal.competencias || 'Nenhuma competência informada.'}</dd></div>}
                        </dl>
                    </>}
                </section>
                {secaoRegistros('certificacoes', 'Certificações', Award)}
                {dados.dadosProfissionais && <>
                    <section className="pf-cartao" aria-labelledby="pf-certificados-plataforma">
                        <h2 id="pf-certificados-plataforma"><Award size={21} aria-hidden="true" />Certificados da plataforma</h2>
                        {!dados.certificadosPlataforma?.length ? <p className="pf-vazio">Conclua uma trilha elegível do CyberAcademy para receber seu certificado.</p> :
                            <ul className="pf-lista">{dados.certificadosPlataforma.map((certificado) => <li key={certificado.id}><div className="pf-registro-texto">
                                <h3>{certificado.tituloTrilha}</h3><p>CyberAcademy · Versão {certificado.versao} · {certificado.cargaMinutos} minutos</p>
                                <small>Emitido em {new Date(certificado.emitidoEm).toLocaleDateString('pt-BR')}</small>
                                <p><Link to={'/academy/certificados/' + certificado.id}>Consultar certificado</Link></p>
                            </div></li>)}</ul>}
                    </section>
                    {secaoRegistros('experiencias', 'Experiência profissional', BriefcaseBusiness)}
                </>}
                <section className="pf-cartao" aria-labelledby="pf-avaliacoes"><h2 id="pf-avaliacoes"><MessageSquare size={21} aria-hidden="true" />Feedbacks e avaliações</h2>
                    <p className="pf-vazio">Ainda não há avaliações disponíveis para seu perfil.</p><p className="pf-ajuda">Esta seção será conectada quando o fluxo de avaliações da plataforma estiver disponível.</p></section>
                {dados.participacao && <section className="pf-cartao" aria-labelledby="pf-participacao"><h2 id="pf-participacao">Participação na plataforma</h2>
                    <p className="pf-ajuda">Seus registros atuais no CyberLadies.</p>
                    <dl className="pf-indicadores">
                        {INDICADORES_COMUNIDADE.map(([chave, rotulo]) =>
                            <div key={chave}><dt>{rotulo}</dt><dd>{dados.participacao[chave]}</dd></div>)}
                        {dados.candidaturas != null && <div><dt>Candidaturas enviadas</dt><dd>{dados.candidaturas}</dd></div>}
                    </dl>
                </section>}
            </>}
        </main>
    </div>
}