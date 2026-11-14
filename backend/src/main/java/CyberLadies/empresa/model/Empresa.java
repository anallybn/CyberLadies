package CyberLadies.empresa.model;

import com.fasterxml.jackson.annotation.JsonProperty;


// Representa uma empresa cadastrada no CyberLadies.
public class Empresa {

    private Long id;
    private String nome;
    private String cnpj;
    private String email;
    
    // Impede que o hash da senha apareça nas respostas da API.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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

    // Define o identificador da empresa.
    public void setId(Long id) {
        this.id = id;
    }

    // Atualiza o nome da empresa.
    public void setNome(String nome) {
        this.nome = nome;
    }

    // Atualiza o CNPJ da empresa.
    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    // Atualiza o e-mail da empresa.
    public void setEmail(String email) {
        this.email = email;
    }

    // Define o hash da senha da empresa.
    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }


}
