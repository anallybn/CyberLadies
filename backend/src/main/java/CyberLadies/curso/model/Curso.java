package CyberLadies.curso.model;


public class Curso {
    private Long id;
    private Long educadoraId;

    private String titulo;
    private String descricao;
    private String categoria;
    private int cargaHoraria;

    public Curso() {
    }

    // Construtor para criar um novo curso.
    public Curso(Long educadoraId, String titulo, String descricao, String categoria, int cargaHoraria) {
        this.educadoraId = educadoraId;
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.cargaHoraria = cargaHoraria;
    }

    // Métodos para consultar os dados.
    public Long getId() {
        return id;
    }

    public Long getEducadoraId() {
        return educadoraId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getCategoria() {
        return categoria;
    }

    public int getCargaHoraria() {
        return cargaHoraria;
    }
}
