package CyberLadies.perfil.service;

import CyberLadies.perfil.dto.PerfilDados.*;
import CyberLadies.perfil.repository.PerfilRepository;
import CyberLadies.auth.AcessoAutenticado;
import CyberLadies.auth.Papel;
import CyberLadies.auth.dto.ContaDados.Tipo;
import CyberLadies.auth.service.AcessoConta;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PerfilService {
    private final PerfilRepository repository;
    private final AcessoAutenticado acesso;
    private final AcessoConta contas;
    private final FotoPerfil fotos;
    private final CyberLadies.academy.service.CertificadoAcademyService certificados;
    public PerfilService(PerfilRepository repository,AcessoAutenticado acesso,AcessoConta contas,FotoPerfil fotos,CyberLadies.academy.service.CertificadoAcademyService certificados) {
        this.repository=repository; this.acesso=acesso; this.contas=contas; this.fotos=fotos; this.certificados=certificados;
    }
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public Perfil consultar() {
        var identidade=acesso.identidade();
        long id=identidade.id();
        var conta=contas.conta(identidade);
        var atuacao=conta.perfil()==Tipo.EDUCADORA && "APROVADA".equals(conta.statusEducadora())
            ? repository.atuacaoEducadora(id).orElse(null) : null;
        Tipo tipo=conta.perfil()==Tipo.EDUCADORA && atuacao==null ? Tipo.CYBERLADIE : conta.perfil();
        var empresa=tipo==Tipo.EMPRESA ? repository.empresa(id).orElse(null) : null;
        boolean profissional=identidade.possui(Papel.USUARIA);
        var pessoal=repository.pessoal(id,profissional);
        var certificacoes=repository.certificacoes(id);
        if (!profissional)
            return new Perfil(pessoal,tipo,atuacao,empresa,false,certificacoes,List.of(),null,null,List.of());
        Long candidaturas=conta.podeCandidatar() ? repository.contarCandidaturas(id) : null;
        return new Perfil(pessoal,tipo,atuacao,empresa,true,certificacoes,repository.experiencias(id),
            repository.participacao(id),candidaturas,certificados.meus());
    }
    @Transactional
    public Pessoal editar(Edicao dados) {
        var identidade=acesso.identidade();
        long id=identidade.id();
        boolean profissional=identidade.possui(Papel.USUARIA);
        if (profissional && (dados.formacao()==null || dados.competencias()==null))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe formação e competências, mesmo que vazias.");
        if (!profissional && (dados.formacao()!=null || dados.competencias()!=null))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Dados profissionais indisponíveis para esta conta.");
        repository.editar(id,dados);
        if (profissional) repository.editarDadosProfissionais(id,dados);
        return repository.pessoal(id,profissional);
    }
    @Transactional
    public Certificacao adicionarCertificacao(CertificacaoEntrada dados, MultipartFile arquivo) {
        long titular=acesso.identidade().id();
        validarData(dados.data());
        var pdf=arquivo==null ? null : PdfCertificacao.ler(arquivo);
        long id=repository.adicionarCertificacao(titular,dados);
        if (pdf!=null) repository.salvarPdfCertificacao(id,pdf);
        return repository.certificacao(titular,id);
    }
    @Transactional
    public Certificacao editarCertificacao(long id, CertificacaoEntrada dados, MultipartFile arquivo, boolean removerPdf) {
        long titular=acesso.identidade().id();
        validarData(dados.data());
        if (arquivo!=null && removerPdf)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecione apenas uma ação para o PDF.");
        var pdf=arquivo==null ? null : PdfCertificacao.ler(arquivo);
        if (!repository.editarCertificacao(titular,id,dados)) throw ausente();
        if (pdf!=null) repository.salvarPdfCertificacao(id,pdf);
        else if (removerPdf) repository.removerPdfCertificacao(id);
        return repository.certificacao(titular,id);
    }
    public ArquivoPdf pdfCertificacao(long id) {
        return repository.pdfCertificacao(acesso.identidade().id(),id).orElseThrow(this::ausente);
    }
    public void excluirCertificacao(long id) {
        if (!repository.excluirCertificacao(acesso.identidade().id(),id)) throw ausente();
    }
    public Experiencia adicionarExperiencia(ExperienciaEntrada dados) {
        long id=titularDadosProfissionais(); validarExperiencia(dados);
        return repository.adicionarExperiencia(id,dados);
    }
    public Experiencia editarExperiencia(long id,ExperienciaEntrada dados) {
        long usuaria=titularDadosProfissionais(); validarExperiencia(dados);
        return repository.editarExperiencia(usuaria,id,dados).orElseThrow(this::ausente);
    }
    public void excluirExperiencia(long id) {
        if (!repository.excluirExperiencia(titularDadosProfissionais(),id)) throw ausente();
    }
    public byte[] foto() { return repository.foto(acesso.identidade().id()).orElseThrow(this::ausente); }
    public void salvarFoto(MultipartFile arquivo) {
        long id=acesso.identidade().id();
        repository.salvarFoto(id,fotos.normalizar(arquivo));
    }
    public void excluirFoto() { repository.excluirFoto(acesso.identidade().id()); }
    private long titularDadosProfissionais() {
        var identidade=acesso.identidade();
        if (!identidade.possui(Papel.USUARIA))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Dados profissionais indisponíveis para esta conta.");
        return identidade.id();
    }
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