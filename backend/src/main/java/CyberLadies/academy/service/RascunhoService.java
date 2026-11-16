package CyberLadies.academy.service;

import CyberLadies.academy.dto.RascunhoDados.*;
import CyberLadies.academy.repository.RascunhoRepository;
import CyberLadies.auth.service.AcessoConta;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RascunhoService {
    private final RascunhoRepository repository;
    private final AcessoConta acesso;
    public RascunhoService(RascunhoRepository repository,AcessoConta acesso) { this.repository=repository; this.acesso=acesso; }
    public List<Resumo> listar() { return repository.listar(acesso.educadora().id()); }
    public Rascunho buscar(UUID id) { return buscar(acesso.educadora().id(),id); }
    private Rascunho buscar(long dona,UUID id) { return repository.buscar(dona,id).orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Rascunho não encontrado.")); }
    @Transactional
    public Rascunho criar(Entrada entrada,UUID chave) {
        long dona=acesso.educadora().id();
        if(entrada.revisao()!=null) throw erro(HttpStatus.BAD_REQUEST,"Uma criação não recebe revisão.");
        var c=validar(entrada,dona);
        String hash=hash(c,null);
        var id=repository.inserir(dona,c,chave,hash,tagsJson(c.tags()));
        if(id.isPresent()) return buscar(dona,id.get());
        var anterior=repository.porChave(dona,chave);
        if(!anterior.hashCriacao().equals(hash)) throw erro(HttpStatus.CONFLICT,"Esta chave de criação já foi usada com outro conteúdo.");
        return buscar(dona,anterior.id());
    }
    @Transactional
    public Rascunho atualizar(UUID id,Entrada entrada,UUID operacao) {
        long dona=acesso.educadora().id();
        var registro=repository.bloquear(dona,id).orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Rascunho não encontrado."));
        if(entrada.revisao()==null) throw erro(HttpStatus.BAD_REQUEST,"Informe a revisão do rascunho.");
        var c=validar(entrada,dona);
        String hash=hash(c,entrada.revisao());
        if(registro.ultimaOperacao().equals(operacao)) {
            if(!registro.hashUltimaOperacao().equals(hash)) throw erro(HttpStatus.CONFLICT,"Esta operação já foi usada com outro conteúdo.");
            return buscar(dona,id);
        }
        if(registro.revisao()!=entrada.revisao()) throw erro(HttpStatus.CONFLICT,"Este rascunho foi alterado em outra aba. Suas alterações locais foram preservadas. Recarregue a versão salva antes de editar novamente.");
        repository.atualizar(dona,id,c,operacao,hash,tagsJson(c.tags()),entrada.revisao());
        return buscar(dona,id);
    }
    private Conteudo validar(Entrada e,long dona) {
        if(e.titulo()==null || e.titulo().isBlank() || e.titulo().length()>160) throw erro(HttpStatus.BAD_REQUEST,"Informe um título de até 160 caracteres.");
        String descricao=e.descricao()==null?"":e.descricao();
        if(descricao.length()>5000 || e.duracaoMinutos()!=null && e.duracaoMinutos()<=0 || e.tipo()!=null && !e.tipo().equals("VIDEO"))
            throw erro(HttpStatus.BAD_REQUEST,"Confira os dados do rascunho.");
        var tags=new ArrayList<String>(); var vistas=new HashSet<String>();
        for(String tag:e.tags()==null?List.<String>of():e.tags()) {
            if(tag==null || tag.isBlank() || tag.length()>30) throw erro(HttpStatus.BAD_REQUEST,"Use até 8 tags de até 30 caracteres.");
            String valor=tag.strip(); if(vistas.add(valor.toLowerCase(Locale.ROOT))) tags.add(valor);
        }
        if(tags.size()>8) throw erro(HttpStatus.BAD_REQUEST,"Use até 8 tags.");
        String provedor=e.videoProvedor(),video=e.videoId()==null||e.videoId().isBlank()?null:e.videoId().strip();
        if(provedor!=null && !Set.of("YOUTUBE","VIMEO").contains(provedor)) throw erro(HttpStatus.BAD_REQUEST,"Provedor de vídeo inválido.");
        if(video!=null && !("YOUTUBE".equals(provedor)&&video.matches("[A-Za-z0-9_-]{11}") || "VIMEO".equals(provedor)&&video.matches("[1-9][0-9]{0,11}")))
            throw erro(HttpStatus.BAD_REQUEST,"Confira o identificador do vídeo YouTube ou Vimeo.");
        if(e.trilhaId()!=null && !repository.trilhaPropria(dona,e.trilhaId())) throw erro(HttpStatus.BAD_REQUEST,"Selecione uma trilha da sua própria conta.");
        return new Conteudo(e.titulo().strip(),descricao,e.trilhaId(),e.duracaoMinutos(),"VIDEO",List.copyOf(tags),provedor,video,Boolean.TRUE.equals(e.usoAutorizado()));
    }
    private String hash(Conteudo c,Long revisao) {
        try {
            var bytes=new java.io.ByteArrayOutputStream(); var out=new java.io.DataOutputStream(bytes);
            var valores=new ArrayList<String>(List.of(c.titulo(),c.descricao(),String.valueOf(c.trilhaId()),String.valueOf(c.duracaoMinutos()),
                c.tipo(),String.valueOf(c.videoProvedor()),String.valueOf(c.videoId()),String.valueOf(c.usoAutorizado()),String.valueOf(revisao)));
            valores.addAll(c.tags()); out.writeInt(valores.size());
            for(String v:valores) { byte[] b=v.getBytes(StandardCharsets.UTF_8);out.writeInt(b.length);out.write(b); }
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch(java.io.IOException|java.security.NoSuchAlgorithmException erro) { throw new IllegalStateException(erro); }
    }
    // Tags são texto simples. Escapa caracteres de controle/aspas sem aceitar JSON do cliente.
    private String tagsJson(List<String> tags) {
        return tags.stream().map(t->{
            var b=new StringBuilder("\"");
            for(char ch:t.toCharArray()) { if(ch=='"'||ch=='\\')b.append('\\'); if(ch<32)b.append(String.format("\\u%04x",(int)ch));else b.append(ch); }
            return b.append('"').toString();
        }).collect(java.util.stream.Collectors.joining(",","[","]"));
    }
    private ResponseStatusException erro(HttpStatus status,String texto) { return new ResponseStatusException(status,texto); }
}
