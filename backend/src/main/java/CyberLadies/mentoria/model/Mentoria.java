package CyberLadies.mentoria.model;

public class Mentoria {

    private Long id;
    private Long educadoraId;
    private Long usuariaId;
    private String titulo;
    private String descricao;
    private String dataHorario;

    public Mentoria() {
    }

    // Construtor para criar uma nova mentoria.
    public Mentoria(Long educadoraId, Long usuariaId, String titulo, String descricao, String dataHorario) {
        this.educadoraId = educadoraId;
        this.usuariaId = usuariaId;
        this.titulo = titulo;
        this.descricao = descricao;
        this.dataHorario = dataHorario;
    }

    // Métodos para consultar os dados.
    public Long getId() {
        return id;
    }

    public Long getEducadoraId() {
        return educadoraId;
    }

    public Long getUsuariaId() {
        return usuariaId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getDataHorario() {
        return dataHorario;
    }


    // Define o identificador da mentoria.
    public void setId(Long id) {
        this.id = id;
    }

    // Define a educadora responsável pela mentoria.
    public void setEducadoraId(Long educadoraId) {
        this.educadoraId = educadoraId;
    }

    // Define a usuária participante da mentoria.
    public void setUsuariaId(Long usuariaId) {
        this.usuariaId = usuariaId;
    }

    // Define o título da mentoria.
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    // Define a descrição da mentoria.
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    // Define a data e o horário da mentoria.
    public void setDataHorario(String dataHorario) {
        this.dataHorario = dataHorario;
    }

}
