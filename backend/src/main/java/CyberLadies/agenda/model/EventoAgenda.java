package CyberLadies.agenda.model;

import java.time.LocalDateTime;

public class EventoAgenda {

    private Long id;
    private Long usuariaId;
    private String titulo;
    private String descricao;
    private String tipoEvento;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;

    public EventoAgenda() {
    }

    public EventoAgenda(Long id, Long usuariaId, String titulo, String descricao, String tipoEvento, LocalDateTime dataInicio, LocalDateTime dataFim) {
        this.id = id;
        this.usuariaId = usuariaId;
        this.titulo = titulo;
        this.descricao = descricao;
        this.tipoEvento = tipoEvento;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
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

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDateTime getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDateTime dataFim) {
        this.dataFim = dataFim;
    }
}
