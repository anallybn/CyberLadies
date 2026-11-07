package CyberLadies.empresa.model;

// Representa uma empresa cadastrada no CyberLadies.
public class Empresa {

    private Long id;
    private String nome;
    private String cnpj;
    private String email;
    private String senhaHash;

    public Empresa() {
    }

    // Construtor para cadastrar uma nova empresa.
    public Empresa(String nome, String cnpj, String email, String senhaHash) {
        this.nome = nome;
        this.cnpj = cnpj;
        this.email = email;
        this.senhaHash = senhaHash;
    }

    // Métodos para consultar os dados da empresa.
    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }
}
