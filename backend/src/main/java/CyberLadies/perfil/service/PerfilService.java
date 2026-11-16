package CyberLadies.perfil.service;

import CyberLadies.perfil.dto.PerfilDados.*;
import CyberLadies.perfil.repository.PerfilRepository;
import CyberLadies.talk.AcessoTalk;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PerfilService {
    private final PerfilRepository repository;
    private final AcessoTalk acesso;
    private final FotoPerfil fotos;
    private final CyberLadies.academy.service.CertificadoAcademyService certificados;
    public PerfilService(PerfilRepository repository,AcessoTalk acesso,FotoPerfil fotos,CyberLadies.academy.service.CertificadoAcademyService certificados) {
        this.repository=repository; this.acesso=acesso; this.fotos=fotos; this.certificados=certificados;
    }
    @Transactional(readOnly=true)
    public Perfil consultar() {
        long id=acesso.usuariaId();
        return new Perfil(repository.pessoal(id),repository.certificacoes(id),repository.experiencias(id),repository.participacao(id),certificados.meus());
    }
    @Transactional
    public Pessoal editar(Edicao dados) {
        long id=acesso.usuariaId();
        repository.editar(id,dados);
        return repository.pessoal(id);
    }
    public Certificacao adicionarCertificacao(CertificacaoEntrada dados) {
        long id=acesso.usuariaId(); validarData(dados.data());
        return repository.adicionarCertificacao(id,dados);
    }
    public Certificacao editarCertificacao(long id,CertificacaoEntrada dados) {
        long usuaria=acesso.usuariaId(); validarData(dados.data());
        return repository.editarCertificacao(usuaria,id,dados).orElseThrow(this::ausente);
    }
    public void excluirCertificacao(long id) {
        if (!repository.excluirCertificacao(acesso.usuariaId(),id)) throw ausente();
    }
    public Experiencia adicionarExperiencia(ExperienciaEntrada dados) {
        long id=acesso.usuariaId(); validarExperiencia(dados);
        return repository.adicionarExperiencia(id,dados);
    }
    public Experiencia editarExperiencia(long id,ExperienciaEntrada dados) {
        long usuaria=acesso.usuariaId(); validarExperiencia(dados);
        return repository.editarExperiencia(usuaria,id,dados).orElseThrow(this::ausente);
    }
    public void excluirExperiencia(long id) {
        if (!repository.excluirExperiencia(acesso.usuariaId(),id)) throw ausente();
    }
    public byte[] foto() { return repository.foto(acesso.usuariaId()).orElseThrow(this::ausente); }
    public void salvarFoto(MultipartFile arquivo) {
        long id=acesso.usuariaId();
        repository.salvarFoto(id,fotos.normalizar(arquivo));
    }
    public void excluirFoto() { repository.excluirFoto(acesso.usuariaId()); }
    private void validarData(LocalDate data) {
        if (data.isAfter(LocalDate.now(ZoneId.of("America/Sao_Paulo"))))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A data não pode estar no futuro.");
    }
    private void validarExperiencia(ExperienciaEntrada dados) {
        validarData(dados.inicio());
        if (dados.fim()!=null) {
            validarData(dados.fim());
            if (dados.fim().isBefore(dados.inicio()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"O fim deve ser igual ou posterior ao início.");
        }
    }
    private ResponseStatusException ausente() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,"Registro não encontrado.");
    }
}