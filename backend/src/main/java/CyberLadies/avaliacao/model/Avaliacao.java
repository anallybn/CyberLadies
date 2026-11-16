package CyberLadies.avaliacao.model;
import java.time.LocalDateTime;

public class Avaliacao {

    private Long id;
    private Long usuariaId;
    private String tipoAtividade;
    private Long atividadeId;
    private Integer nota;
    private String comentario;
    private LocalDateTime dataAvaliacao;

   
    public Avaliacao() {
    }

    public Avaliacao(Long id, Long usuariaId, String tipoAtividade, Long atividadeId, Integer nota, String comentario, LocalDateTime dataAvaliacao) {
        this.id = id;
        this.usuariaId = usuariaId;
        this.tipoAtividade = tipoAtividade;
        this.atividadeId = atividadeId;
        this.nota = nota;
        this.comentario = comentario;
        this.dataAvaliacao = dataAvaliacao;
    }

    // Métodos para consultar e alterar os atributos.

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuariaId() {
        return usuariaId;
    }

    public void setUsuariaId(Long usuariaId) {
        this.usuariaId = usuariaId;
    }

    public String getTipoAtividade() {
        return tipoAtividade;
    }

    public void setTipoAtividade(String tipoAtividade) {
        this.tipoAtividade = tipoAtividade;
    }

    public Long getAtividadeId() {
        return atividadeId;
    }

    public void setAtividadeId(Long atividadeId) {
        this.atividadeId = atividadeId;
    }

    public Integer getNota() {
        return nota;
    }

    public void setNota(Integer nota) {
        this.nota = nota;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public LocalDateTime getDataAvaliacao() {
        return dataAvaliacao;
    }

    public void setDataAvaliacao(LocalDateTime dataAvaliacao) {
        this.dataAvaliacao = dataAvaliacao;
    }
}
