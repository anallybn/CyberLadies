
package CyberLadies.vaga.model;

// Representa uma vaga de emprego publicada no CyberLadies.
public class Vaga {

    private Long id;
    private Long empresaId;

    private String titulo;
    private String descricao;
    private String requisitos;
    private String modalidade;

    
    public Vaga() {
    }

    // Construtor para criar uma nova vaga.
    public Vaga(Long empresaId, String titulo,String descricao, String requisitos, String modalidade) {
        this.empresaId = empresaId;
        this.titulo = titulo;
        this.descricao = descricao;
        this.requisitos = requisitos;
        this.modalidade = modalidade;
    }

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getRequisitos() {
        return requisitos;
    }

    public String getModalidade() {
        return modalidade;
    }
}
