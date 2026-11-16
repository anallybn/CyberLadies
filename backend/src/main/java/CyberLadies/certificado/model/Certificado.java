
package CyberLadies.certificado.model;

import java.time.LocalDate;

// Representa um certificado de conclusão de curso ou mentoria.
public class Certificado {

    private Long id;
    private Long usuariaId;
    private String nomeAtividade;
    private LocalDate dataEmissao;
    private String codigoValidacao;

    public Certificado() {
    }

    // preencher os dados do certificado.
    public Certificado(Long id, Long usuariaId, String nomeAtividade, LocalDate dataEmissao, String codigoValidacao) {
        this.id = id;
        this.usuariaId = usuariaId;
        this.nomeAtividade = nomeAtividade;
        this.dataEmissao = dataEmissao;
        this.codigoValidacao = codigoValidacao;
    }

    // Métodos para consultar e alterar os atributos.
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuariaId() { return usuariaId; }
    public void setUsuariaId(Long usuariaId) {
        this.usuariaId = usuariaId;
    }

    public String getNomeAtividade() { return nomeAtividade; }
    public void setNomeAtividade(String nomeAtividade) {
        this.nomeAtividade = nomeAtividade;
    }

    public LocalDate getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDate dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public String getCodigoValidacao() {
        return codigoValidacao;
    }
    public void setCodigoValidacao(String codigoValidacao) {
        this.codigoValidacao = codigoValidacao;
    }
}

