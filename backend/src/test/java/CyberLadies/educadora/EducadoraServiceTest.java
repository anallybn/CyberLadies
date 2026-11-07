package CyberLadies.educadora;

import CyberLadies.educadora.model.Educadora;
import CyberLadies.educadora.model.StatusAprovacao;
import CyberLadies.educadora.service.EducadoraService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


// Testa as regras de negócio das educadoras.
public class EducadoraServiceTest {

    // Verifica que uma educadora pendente não pode publicar.
    @Test
    void educadoraPendenteNaoPodePublicar() {

        // Cria uma educadora com status inicial PENDENTE.
        Educadora educadora = new Educadora(
            "Maria Silva",
            "maria@exemplo.com",
            "hash-de-teste",
            "Programação",
            "Instituição Exemplo"
        );

        // Cria o serviço responsável pelas regras.
        EducadoraService service = new EducadoraService();

        // Confirma que a publicação não está permitida.
        assertFalse(service.podePublicar(educadora));
    }

    
    // Verifica que uma educadora aprovada pode publicar.
    @Test
    void educadoraAprovadaPodePublicar() {

        // Cria uma educadora inicialmente PENDENTE.
        Educadora educadora = new Educadora(
            "Ana Souza",
            "ana@exemplo.com",
            "hash-de-teste",
            "Segurança da Informação",
            "Instituição Exemplo"
        );

        // Cria o serviço responsável pelas regras.
        EducadoraService service = new EducadoraService();

        // Aprova o cadastro da educadora.
        service.aprovar(educadora);

        // Confirma que a educadora aprovada pode publicar.
        assertTrue(service.podePublicar(educadora));
    }

    // Verifica se a rejeição altera o status e registra o motivo.
    @Test
    void educadoraRejeitadaDeveTerJustificativa() {

        // Cria uma educadora inicialmente PENDENTE.
        Educadora educadora = new Educadora(
            "Carla Santos",
            "carla@exemplo.com",
            "hash-de-teste",
            "Desenvolvimento Web",
            "Instituição Exemplo"
        );

        // Cria o serviço responsável pelas regras.
        EducadoraService service = new EducadoraService();

        // Rejeita o cadastro informando uma justificativa.
        service.rejeitar(
            educadora,
            "É necessário comprovar a instituição."
        );

        // Confirma que o status foi alterado para REJEITADA.
        assertEquals(
            StatusAprovacao.REJEITADA,
            educadora.getStatus()
        );

        // Confirma que a justificativa foi registrada.
        assertEquals(
            "É necessário comprovar a instituição.",
            educadora.getMotivoRejeicao()
        );
    }
    
    // Verifica se o sistema impede uma rejeição sem justificativa.
    @Test
    void naoPermitirRejeicaoSemMotivo() {

        // Cria uma educadora inicialmente PENDENTE.
        Educadora educadora = new Educadora(
            "Juliana Costa",
            "juliana@exemplo.com",
            "hash-de-teste",
            "Programação",
            "Instituição Exemplo"
        );

        // Cria o serviço responsável pelas regras.
        EducadoraService service = new EducadoraService();

        // Confirma que rejeitar sem motivo gera um erro.
        assertThrows(
            IllegalArgumentException.class,
            () -> service.rejeitar(educadora, "")
        );

        // Confirma que o status continua PENDENTE.
        assertEquals(
            StatusAprovacao.PENDENTE,
            educadora.getStatus()
        );
    }

}
