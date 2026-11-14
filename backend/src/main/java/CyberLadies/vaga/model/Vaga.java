
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


    // Define o identificador da vaga.
    public void setId(Long id) {
        this.id = id;
    }

    // Define a empresa responsável pela vaga.
    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    // Define o título da vaga.
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    // Define a descrição da vaga.
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    // Define os requisitos necessários para a vaga.
    public void setRequisitos(String requisitos) {
        this.requisitos = requisitos;
    }

    // Define a modalidade de trabalho.
    public void setModalidade(String modalidade) {
        this.modalidade = modalidade;
    }

}
