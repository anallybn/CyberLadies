package CyberLadies.candidatura.model;

// Representa a candidatura de uma usuária a uma vaga.
public class Candidatura {

    private Long id;
    private Long usuariaId;
    private Long vagaId;
    private String status;


    public Candidatura() {
    }

    // Cria uma candidatura com status inicial PENDENTE.
    public Candidatura(Long usuariaId, Long vagaId) {
        this.usuariaId = usuariaId;
        this.vagaId = vagaId;
        this.status = "PENDENTE";
    }

    // Métodos para consultar os dados.
    public Long getId() {
        return id;
    }

    public Long getUsuariaId() {
        return usuariaId;
    }

    public Long getVagaId() {
        return vagaId;
    }

    public String getStatus() {
        return status;
    }


    // Define o identificador da candidatura.
    public void setId(Long id) {
        this.id = id;
    }

    // Define a usuária responsável pela candidatura.
    public void setUsuariaId(Long usuariaId) {
        this.usuariaId = usuariaId;
    }

    // Define a vaga à qual a usuária se candidatou.
    public void setVagaId(Long vagaId) {
        this.vagaId = vagaId;
    }

    // Atualiza a situação da candidatura.
    public void setStatus(String status) {
        this.status = status;
    }

}
