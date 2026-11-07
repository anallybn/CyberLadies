
package CyberLadies.educadora.model;

// Representa os dados de uma educadora cadastrada no CyberLadies.
public class Educadora {

    private Long id;
    private String nome;
    private String email;
    // Senha protegida por hash. Nunca armazenaremos a senha original.
    private String senhaHash;
    private String especialidade;
    private String instituicao;
    private StatusAprovacao status;
    private String motivoRejeicao;

    public Educadora() {
    }

    // Criando uma nova educadora.
    public Educadora(
            String nome,
            String email,
            String senhaHash,
            String especialidade,
            String instituicao) {

        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.especialidade = especialidade;
        this.instituicao = instituicao;

        // Aguardando aprovação.
        this.status = StatusAprovacao.PENDENTE;
    }

    // Retorna o identificador da educadora.
    public Long getId() {
        return id;
    }

    // Define o identificador da educadora.
    public void setId(Long id) {
        this.id = id;
    }

    // Retorna o nome da educadora.
    public String getNome() {
        return nome;
    }

    // Retorna o e-mail cadastrado.
    public String getEmail() {
        return email;
    }

    // Retorna a senha protegida por hash.
    public String getSenhaHash() {
        return senhaHash;
    }

    // Retorna a especialidade.
    public String getEspecialidade() {
        return especialidade;
    }

    // Retorna a instituição.
    public String getInstituicao() {
        return instituicao;
    }

    // Retorna o status de aprovação.
    public StatusAprovacao getStatus() {
        return status;
    }

    // Retorna o motivo da rejeição.
    public String getMotivoRejeicao() {
        return motivoRejeicao;
    }

    // Atualiza os dados profissionais da educadora.
    public void atualizarDados(
            String nome,
            String especialidade,
            String instituicao) {

        this.nome = nome;
        this.especialidade = especialidade;
        this.instituicao = instituicao;
    }

    // Aprova o cadastro da educadora.
    public void aprovar() {
        this.status = StatusAprovacao.APROVADA;
        this.motivoRejeicao = null;
    }

    // Rejeita o cadastro, exigindo uma justificativa.
    public void rejeitar(String motivo) {

        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                "O motivo da rejeição é obrigatório."
            );
        }

        this.status = StatusAprovacao.REJEITADA;
        this.motivoRejeicao = motivo.strip();
    }

    // Permite nova análise.
    public void solicitarReanalise() {

        if (this.status != StatusAprovacao.REJEITADA) {
            throw new IllegalStateException(
                "Somente cadastros rejeitados podem solicitar reanálise."
            );
        }

        this.status = StatusAprovacao.PENDENTE;
        this.motivoRejeicao = null;
    }
}
