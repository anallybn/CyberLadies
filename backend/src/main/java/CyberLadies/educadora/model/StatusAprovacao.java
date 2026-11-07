package CyberLadies.educadora.model;

// Define os possíveis estados de aprovação da educadora.
public enum StatusAprovacao {

    // Cadastro aguardando análise do administrador.
    PENDENTE,

    // Cadastro aprovado pelo administrador.
    APROVADA,

    // Cadastro rejeitado, permitindo nova solicitação.
    REJEITADA
}
