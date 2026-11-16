package CyberLadies.notificacao.model;

import java.time.LocalDateTime;

public class Notificacao {

    private Long id;
    private Long usuariaId;
    private String titulo;
    private String mensagem;
    private boolean lida;
    private LocalDateTime dataCriacao;

    public Notificacao() {
    }


    public Notificacao(Long id, Long usuariaId, String titulo, String mensagem, boolean lida, LocalDateTime dataCriacao) {
        this.id = id;
        this.usuariaId = usuariaId;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.lida = lida;
        this.dataCriacao = dataCriacao;
    }

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

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public boolean isLida() {
        return lida;
    }

    public void setLida(boolean lida) {
        this.lida = lida;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
