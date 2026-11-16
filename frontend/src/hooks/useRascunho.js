import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useConta } from './useConta'
import { rascunhos } from '../services/rascunhosService'

const VAZIO = { titulo: '', descricao: '', trilha: '', duracao: '', tags: '', provedor: 'YOUTUBE', video: '', autorizado: false }
export function useRascunho() {
    const [params] = useSearchParams()
    const id = params.get('rascunho') || ''
    const navigate = useNavigate()
    const { conta } = useConta()
    const [campos, setCampos] = useState(VAZIO)
    const [base, setBase] = useState(JSON.stringify(campos))
    const [revisao, setRevisao] = useState(null)
    const [chaveCarregada, setChaveCarregada] = useState('')
    const [tentativa, setTentativa] = useState(0)
    const [mensagem, setMensagem] = useState(null)
    const [salvando, setSalvando] = useState(false)
    const [erroCarga, setErroCarga] = useState('')
    const [rotaAnterior, setRotaAnterior] = useState(id)
    if (rotaAnterior !== id) {
        setRotaAnterior(id)
        if (!id) {
            setCampos(VAZIO); setBase(JSON.stringify(VAZIO)); setRevisao(null)
            setChaveCarregada(''); setMensagem(null); setErroCarga('')
        }
    }
    const bloqueio = useRef(false), pendente = useRef(null), confirmado = useRef(null), ativo = useRef(false), rotaAtiva = useRef(id)
    useEffect(() => { ativo.current=true; return () => { ativo.current=false } }, [])
    useEffect(() => { rotaAtiva.current=id }, [id])
    const chave = conta?.id + ':' + id
    const carregando = !!id && chaveCarregada !== chave
    const sujo = JSON.stringify(campos) !== base
    useEffect(() => {
        if (!id) { pendente.current=null; confirmado.current=null; return }
        if (conta?.perfil !== 'EDUCADORA' || confirmado.current === chave) return
        const controle=new AbortController()
        rascunhos('/'+encodeURIComponent(id),{signal:controle.signal}).then((r)=>{
            if(controle.signal.aborted) return
            const valor={titulo:r.titulo,descricao:r.descricao,trilha:r.trilhaId==null?'':String(r.trilhaId),duracao:r.duracaoMinutos==null?'':String(r.duracaoMinutos),tags:r.tags.join(', '),provedor:r.videoProvedor || 'YOUTUBE',video:r.videoId || '',autorizado:r.usoAutorizado}
            setCampos(valor);setBase(JSON.stringify(valor));setRevisao(r.revisao);setChaveCarregada(chave);setErroCarga('');setMensagem(null);pendente.current=null
        }).catch((erro)=>{if(erro.name!=='AbortError'){setErroCarga(erro.status===404?'Rascunho não encontrado nesta conta.':erro.message);if(erro.status===401)navigate('/login?perfil=educadora',{replace:true})}})
        return ()=>controle.abort()
    },[id,chave,conta?.perfil,navigate,tentativa])
    function atualizar(evento) {
        const { name,value,checked,type }=evento.target
        setCampos((antes)=>({...antes,[name]:type==='checkbox'?checked:value}))
        setMensagem(null)
    }
    async function salvar() {
        if(bloqueio.current || carregando || erroCarga || conta?.perfil!=='EDUCADORA') return
        if(!campos.titulo.trim()){setMensagem({erro:true,texto:'Informe um título para salvar o rascunho.'});document.getElementById('em-titulo')?.focus();return}
        const tags=campos.tags.split(',').map(t=>t.trim()).filter(Boolean)
        if(tags.length>8 || tags.some(t=>t.length>30)){setMensagem({erro:true,texto:'Use até 8 tags de até 30 caracteres cada.'});return}
        if(campos.duracao && (!/^[1-9][0-9]*$/.test(campos.duracao)||Number(campos.duracao)>2147483647)){setMensagem({erro:true,texto:'Informe uma duração inteira e positiva em minutos.'});return}
        bloqueio.current=true;setSalvando(true);setMensagem(null)
        if(!pendente.current) {
            const dados={titulo:campos.titulo,descricao:campos.descricao,trilhaId:campos.trilha?Number(campos.trilha):null,duracaoMinutos:campos.duracao?Number(campos.duracao):null,tipo:'VIDEO',tags,videoProvedor:campos.provedor,videoId:campos.video||null,usoAutorizado:campos.autorizado,...(id?{revisao}:{})}
            pendente.current={operacao:crypto.randomUUID(),dados,campos:JSON.stringify(campos),id}
        }
        const envio=pendente.current
        try {
            const r=await rascunhos(envio.id?'/'+envio.id:'',{method:envio.id?'PUT':'POST',dados:envio.dados,operacao:envio.operacao})
            if (!ativo.current || rotaAtiva.current !== envio.id) return
            setRevisao(r.revisao);setBase(envio.campos);setMensagem({erro:false,texto:JSON.stringify(campos) === envio.campos ? 'Rascunho salvo com sucesso.' : 'A versão anterior foi salva. Há alterações locais mais recentes; salve novamente para preservá-las.'});pendente.current=null
            const novaChave=conta.id+':'+r.id;confirmado.current=novaChave;setChaveCarregada(novaChave)
            if(!envio.id) navigate('/educadora/enviar-material?rascunho='+r.id,{replace:true})
        } catch(erro) {
            // Resultado incerto conserva a MESMA operação/payload para uma repetição segura.
            if(erro.status && erro.status<500) pendente.current=null
            setMensagem({erro:true,conflito:erro.status===409,texto:erro.status===409?erro.message:'Não foi possível salvar. '+(erro.status===400?erro.message:'Seus campos foram preservados. Tente novamente.')})
            if(erro.status===401) navigate('/login?perfil=educadora',{replace:true})
        } finally {bloqueio.current=false;setSalvando(false)}
    }
    function recarregar() {
        if(sujo && !window.confirm('Deseja substituir suas alterações locais pela versão salva?')) return
        confirmado.current=null;setTentativa(v=>v+1)
    }
    return { campos,atualizar,salvar,salvando,carregando,erroCarga,mensagem,sujo,id,recarregar,pronto:conta?.perfil==='EDUCADORA'&&!carregando&&!erroCarga }
}
