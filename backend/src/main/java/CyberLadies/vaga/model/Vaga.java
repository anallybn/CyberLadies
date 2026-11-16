package CyberLadies.vaga.model;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
public record Vaga(long id, long empresaId, String empresa, String titulo, String descricao,
    String requisitos, String modalidade, String nivel, String localizacao,
    BigDecimal salarioMinimo, BigDecimal salarioMaximo, String tecnologias,
    boolean paraMaes, boolean pcd, boolean semFormacao, OffsetDateTime publicadoEm) {}