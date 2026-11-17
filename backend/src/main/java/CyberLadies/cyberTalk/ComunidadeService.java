package CyberLadies.cyberTalk;

import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
class ComunidadeService {
    private final PublicacaoRepository repository;
    private final AcessoTalk acesso;
    private final PublicacaoConsultaService consultas;
    ComunidadeService(PublicacaoRepository repository,AcessoTalk acesso,PublicacaoConsultaService consultas) {
        this.repository=repository; this.acesso=acesso; this.consultas=consultas;
    }
    AcessoTalk.Usuaria usuaria() { return acesso.usuaria(); }
    @Transactional
    void aderir() { repository.aderir(acesso.usuaria().id()); }
    List<PublicacaoResponse> listar() { return consultas.listar(); }
    PublicacaoResponse consultar(long id) { return consultas.consultar(id); }

    @Transactional
    PublicacaoResponse publicar(String texto) {
        long autora=acesso.membro().id();
        return consultas.buscar(repository.inserir(autora,texto.strip()),autora);
    }
    @Transactional
    PublicacaoResponse publicarComAnexo(String texto,MultipartFile arquivo) throws IOException {
        long autora=acesso.membro().id();
        if(texto.length()>500 || arquivo.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confira o texto e o arquivo.");
        if(arquivo.getSize()>5*1024*1024)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"O limite é 5 MB.");
        byte[] dados=arquivo.getBytes();
        String nome=arquivo.getOriginalFilename();
        if(nome==null || nome.isBlank() || nome.length()>200 || nome.chars().anyMatch(Character::isISOControl)
            || nome.contains("/") || nome.contains("\\"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nome de arquivo inválido.");
        long id=repository.inserir(autora,texto.strip());
        repository.salvarAnexo(id,new AnexoPublicacao(nome,tipoDeAnexo(dados),dados));
        return consultas.buscar(id,autora);
    }
    private static String tipoDeAnexo(byte[] dados) {
        var inicio=java.util.HexFormat.of().formatHex(dados,0,Math.min(dados.length,8));
        if(inicio.startsWith("89504e470d0a1a0a")) return "image/png";
        if(inicio.startsWith("ffd8ff")) return "image/jpeg";
        return "application/octet-stream";
    }
    AnexoPublicacao anexo(long id) {
        acesso.membro();
        return repository.anexo(id).orElseThrow(ComunidadeService::ausente);
    }
    @Transactional
    PublicacaoResponse editar(long id,String texto) {
        long autora=acesso.membro().id();
        exigirAutora(id,autora);
        repository.editar(id,texto.strip());
        return consultas.buscar(id,autora);
    }
    @Transactional
    void excluir(long id) { exigirAutora(id,acesso.membro().id()); repository.excluir(id); }
    @Transactional
    PublicacaoResponse curtir(long id) {
        long pessoa=acesso.membro().id(); bloquearPost(id); repository.curtir(id,pessoa);
        return consultas.buscar(id,pessoa);
    }
    @Transactional
    PublicacaoResponse descurtir(long id) {
        long pessoa=acesso.membro().id(); bloquearPost(id); repository.descurtir(id,pessoa);
        return consultas.buscar(id,pessoa);
    }
    List<ComentarioResponse> comentarios(long id) {
        consultas.buscar(id,acesso.membro().id());
        return repository.comentarios(id);
    }
    @Transactional
    ComentarioResponse comentar(long id,String texto) {
        var autora=acesso.membro(); bloquearPost(id);
        String normalizado=texto.strip();
        return new ComentarioResponse(repository.comentar(id,autora.id(),normalizado),autora.nome(),normalizado);
    }
    private Publicacao bloquearPost(long id) { return repository.bloquear(id).orElseThrow(ComunidadeService::ausente); }
    private void exigirAutora(long id,long pessoa) {
        if(!bloquearPost(id).podeSerAlteradaPor(pessoa))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Apenas a autora pode alterar a publicação.");
    }
    private static ResponseStatusException ausente() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"Publicação não encontrada."); }
}
