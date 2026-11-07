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
}
