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

    // Permite definir o identificador de um curso.
    public void setId(Long id) {
        this.id = id;
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

    //Atualizações.
    // Permite atualizar o título do curso.
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    // Permite atualizar a descrição do curso.
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    // Permite atualizar a categoria do curso.
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    // Permite atualizar a carga horária do curso.
    public void setCargaHoraria(int cargaHoraria) {
        this.cargaHoraria = cargaHoraria;
    }

    // Permite atualizar a educadora responsável.
    public void setEducadoraId(Long educadoraId) {
        this.educadoraId = educadoraId;
    }

}
